/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.service.cloudformation.operations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.amazonaws.cloudformation.CreateStackResponse;
import com.amazonaws.cloudformation.DeleteStackResponse;
import com.amazonaws.cloudformation.DescribeStacksResponse;
import com.amazonaws.cloudformation.DescribeStacksResult;
import com.amazonaws.cloudformation.Output;
import com.amazonaws.cloudformation.Outputs;
import com.amazonaws.cloudformation.Stack;
import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.core.aws.util.AWSErrorException;
import com.servicemesh.agility.adapters.service.cloudformation.CFConstants;
import com.servicemesh.agility.adapters.service.cloudformation.CFServiceAdapter;
import com.servicemesh.agility.api.Asset;
import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.ServiceInstance;
import com.servicemesh.agility.api.ServiceProvider;
import com.servicemesh.agility.api.Template;
import com.servicemesh.agility.sdk.service.helper.PropertyHelper;
import com.servicemesh.agility.sdk.service.msgs.ServiceInstanceProvisionRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceInstanceReconfigureRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceInstanceReleaseRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderResponse;
import com.servicemesh.agility.sdk.service.operations.ServiceInstanceOperations;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.reactor.Reactor;
import com.servicemesh.io.http.QueryParam;
import com.servicemesh.io.http.QueryParams;

public class CFServiceInstanceOperations extends ServiceInstanceOperations
{
    private static final Logger logger = Logger.getLogger(CFServiceInstanceOperations.class);
    private static final long POLL_INTERVAL = 30000;

    private CFServiceAdapter adapter;
    private Reactor reactor;

    /**
     * @param adapter
     */
    public CFServiceInstanceOperations(CFServiceAdapter adapter)
    {
        this.adapter = adapter;
        reactor = adapter.getReactor();
    }

    /**
     * Services SDK message handler for ServiceInstance provisioning requests. Provisions the cloud formation template pulling
     * input parameters from the service instance.
     *
     * @param request
     */

    @Override
    public Promise<ServiceProviderResponse> provision(ServiceInstanceProvisionRequest request)
    {
        try
        {
            ServiceInstance instance = request.getServiceInstance();
            ServiceProvider provider = request.getProvider();
            if (instance == null || provider == null)
            {
                return Promise.pure(new Exception("Invalid parameters"));
            }

            final String stackName = toStackName(instance.getName() + "-" + instance.getId());
            AWSConnection connection = adapter.getConnection(request);
            QueryParams params = connection.initQueryParams(CFConstants.CREATE_STACK);
            params.add(new QueryParam(CFConstants.CF_STACK_NAME, stackName));
            params.add(new QueryParam("Capabilities.member.1", "CAPABILITY_IAM"));

            Map<String, String> param_map = new HashMap<String, String>();
            for (AssetProperty ap : instance.getAssetProperties())
            {
                switch (ap.getName())
                {
                    case CFConstants.CF_TEMPLATE:
                        params.add(new QueryParam(CFConstants.CF_TEMPLATE_URL, ap.getStringValue()));
                        break;
                    case CFConstants.CF_TAG:
                        break;
                    default:
                    {
                        String value;
                        if (ap.getFloatValue() != null)
                        {
                            BigDecimal f = ap.getFloatValue();
                            if ((float) f.intValue() == f.floatValue())
                            {
                                value = "" + f.intValue();
                            }
                            else
                            {
                                value = ap.getFloatValue().toString();
                            }
                        }
                        else if (ap.getIntValue() != null)
                        {
                            value = ap.getIntValue().toString();
                        }
                        else
                        {
                            value = ap.getStringValue();
                        }
                        if (value != null)
                        {
                            String current = param_map.get(ap.getName());
                            if (current != null)
                            {
                                param_map.put(ap.getName(), current + "," + value);
                            }
                            else
                            {
                                param_map.put(ap.getName(), value);
                            }
                        }
                        break;
                    }
                }
            }
            int n = 1;
            for (Map.Entry<String, String> e : param_map.entrySet())
            {
                params.add(new QueryParam(CFConstants.CF_PARAMETERS + ".member." + n + ".ParameterKey", e.getKey()));
                params.add(new QueryParam(CFConstants.CF_PARAMETERS + ".member." + n + ".ParameterValue", e.getValue()));
                n++;
            }

            // make an asynchronous stack provisioning request to aws
            Promise<CreateStackResponse> promise = connection.execute(params, CreateStackResponse.class);
            Promise<ServiceProviderResponse> presponse = promise.flatMap((CreateStackResponse csr) -> {
                ServiceInstance service = request.getServiceInstance();
                if (csr.getCreateStackResult() != null && csr.getCreateStackResult().stackId != null)
                {
                    // persist the stack name and id so that it can be used for cleanup later
                    PropertyHelper.setString(service.getConfigurations(), CFConstants.CF_STACK_NAME, stackName);
                    PropertyHelper.setString(service.getConfigurations(), CFConstants.CF_STACK_ID,
                            csr.getCreateStackResult().stackId);

                    // poll for completion of the creation request
                    Promise<ServiceProviderResponse> completed =
                            pollStack(service, connection, csr.getCreateStackResult().stackId);
                    return completed.map((ServiceProviderResponse response) -> {
                        if (response.getStatus() != com.servicemesh.core.messaging.Status.COMPLETE)
                        {
                            return response;
                        }
                        // on success push stack outputs into any connected template as environment variables
                        for (Asset asset : request.getDependents())
                        {
                            if (asset instanceof Template)
                            {
                                Template template = (Template) asset;
                                for (AssetProperty ap : service.getConfigurations())
                                {
                                    AssetProperty var = new AssetProperty();
                                    var.setName(ap.getName());
                                    var.setStringValue(ap.getStringValue());
                                    template.getVariables().add(var);
                                }
                                response.getModified().add(template);
                            }
                        }
                        return response;
                    });
                }
                // shouldn't ever get here
                ServiceProviderResponse response = new ServiceProviderResponse();
                response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
                response.setMessage("Unable to decode create stack response");
                return Promise.pure(response);
            });
            return presponse.recover((Throwable t) -> {

                // catch amazon specific error message and return as failure reason
                ServiceProviderResponse response = new ServiceProviderResponse();
                response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
                if (t instanceof AWSErrorException)
                {
                    response.setMessage(t.toString());
                    logger.error(t.toString());
                }
                else
                {
                    response.setMessage(t.getMessage());
                    logger.error(t.getMessage());
                }
                return response;

            });
        }
        catch (AWSErrorException e)
        {
            ServiceProviderResponse response = new ServiceProviderResponse();
            response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
            response.setMessage(e.toString());
            return Promise.pure(response);
        }
        catch (Throwable t)
        {
            ServiceProviderResponse response = new ServiceProviderResponse();
            response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
            response.setMessage(t.getMessage());
            return Promise.pure(response);
        }
    }

    /**
     * Use reactor timer to asynchronously poll AWS for completion.
     */

    private Promise<ServiceProviderResponse> pollStack(final ServiceInstance service, final AWSConnection connection,
            final String stackId)
    {
        // schedule a lambda to run after the specified delay/poll interval
        Promise<Promise<ServiceProviderResponse>> ppromise = Promise.delayed(reactor, POLL_INTERVAL, () -> {

            QueryParams params = connection.initQueryParams(CFConstants.DESCRIBE_STACKS);
            params.add(new QueryParam(CFConstants.CF_STACK_ID, stackId));

            // perform an asynchronous query to AWS api to check completion results
            Promise<DescribeStacksResponse> promise = connection.execute(params, DescribeStacksResponse.class);
            return promise.flatMap((DescribeStacksResponse dsr) -> {

                DescribeStacksResult result = dsr.getDescribeStacksResult();
                if (result != null && result.getStacks() != null && result.getStacks().getMember().size() > 0)
                {
                    Stack stack = result.getStacks().getMember().get(0);
                    switch (stack.getStackStatus())
                    {
                        case "CREATE_IN_PROGRESS":
                        case "DELETE_IN_PROGRESS":
                        {
                            // not completed so keep polling
                            return pollStack(service, connection, stackId);
                        }
                        case "CREATE_COMPLETE":
                        {
                            // create completed so persist stack outputs
                            Outputs outputs = stack.getOutputs();
                            if (outputs != null)
                            {
                                for (Output output : outputs.getMember())
                                {
                                    PropertyHelper.setString(service.getConfigurations(), output.getOutputKey(),
                                            output.getOutputValue());
                                }
                            }
                            ServiceProviderResponse response = new ServiceProviderResponse();
                            response.setStatus(com.servicemesh.core.messaging.Status.COMPLETE);
                            response.getModified().add(service);
                            return Promise.pure(response);
                        }
                        case "DELETE_COMPLETE":
                        {
                            ServiceProviderResponse response = new ServiceProviderResponse();
                            response.setStatus(com.servicemesh.core.messaging.Status.COMPLETE);
                            response.getModified().add(service);
                            return Promise.pure(response);
                        }
                        default:
                        {
                            // rollback or failure in provisioning
                            ServiceProviderResponse response = new ServiceProviderResponse();
                            response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
                            if (stack.getStackStatusReason() == null)
                            {
                                response.setMessage("Stack api request failed");
                            }
                            else
                            {
                                response.setMessage(stack.getStackStatusReason());
                            }
                            response.getModified().add(service);
                            return Promise.pure(response);
                        }
                    }
                }

                // shouldn't ever get here
                ServiceProviderResponse response = new ServiceProviderResponse();
                response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
                response.setMessage("Unable to determine stack status");
                response.getModified().add(service);
                return Promise.pure(response);
            });
        });
        return ppromise.flatMap((Promise<ServiceProviderResponse> p) -> {
            return p;
        });
    }

    /**
     * Services SDK message handler for ServiceInstance release requests. Tears down the AWS stack corresponding to the
     * ServiceInstance.
     *
     * @param request
     */

    @Override
    public Promise<ServiceProviderResponse> release(ServiceInstanceReleaseRequest request)
    {
        try
        {
            ServiceInstance instance = request.getServiceInstance();
            ServiceProvider provider = request.getProvider();
            if (instance == null || provider == null)
            {
                return Promise.pure(new Exception("Invalid parameters"));
            }
            final String stackId = PropertyHelper.getString(instance.getConfigurations(), CFConstants.CF_STACK_ID, null);
            if (stackId == null)
            {
                ServiceProviderResponse response = new ServiceProviderResponse();
                response.setStatus(com.servicemesh.core.messaging.Status.COMPLETE);
                return Promise.pure(response);
            }
            final String stackName = toStackName(instance.getName() + "-" + instance.getId());

            AWSConnection connection = adapter.getConnection(request);
            QueryParams params = connection.initQueryParams(CFConstants.DELETE_STACK);
            params.add(new QueryParam(CFConstants.CF_STACK_NAME, stackName));
            params.add(new QueryParam(CFConstants.CF_STACK_ID, stackId));

            // post delete request to aws
            Promise<DeleteStackResponse> promise = connection.execute(params, DeleteStackResponse.class);
            Promise<ServiceProviderResponse> presponse = promise.flatMap((DeleteStackResponse csr) -> {

                // poll stack status for completion
                ServiceInstance service = request.getServiceInstance();
                return pollStack(service, connection, stackId);
            });
            return presponse.recover((Throwable t) -> {

                // catch amazon specific error message and return as failure reason
                ServiceProviderResponse response = new ServiceProviderResponse();
                response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
                if (t instanceof AWSErrorException)
                {
                    response.setMessage(t.toString());
                }
                else
                {
                    response.setMessage(t.getMessage());
                }
                return response;

            });
        }
        catch (AWSErrorException e)
        {
            ServiceProviderResponse response = new ServiceProviderResponse();
            response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
            response.setMessage(e.toString());
            return Promise.pure(response);
        }
        catch (Throwable t)
        {
            ServiceProviderResponse response = new ServiceProviderResponse();
            response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
            response.setMessage(t.getMessage());
            return Promise.pure(response);
        }
    }

    /**
     * Change/reconfigure is currently unsupported
     */

    @Override
    public Promise<ServiceProviderResponse> reconfigure(ServiceInstanceReconfigureRequest request)
    {
        ServiceInstance service = request.getServiceInstance();
        ServiceProviderResponse response = new ServiceProviderResponse();
        response.setStatus(com.servicemesh.core.messaging.Status.FAILURE);
        response.setMessage("Unsupported operation for: " + service.getName());
        return Promise.pure(response);
    }

    /**
     * Convert the service instance name into the alpha-numeric format allowed by aws
     */

    private String toStackName(String name)
    {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++)
        {
            char ch = name.charAt(i);
            if (Character.isAlphabetic(ch) || Character.isDigit(ch))
            {
                out.append(ch);
            }
            else
            {
                out.append("-");
            }
        }
        return out.toString();
    }
}
