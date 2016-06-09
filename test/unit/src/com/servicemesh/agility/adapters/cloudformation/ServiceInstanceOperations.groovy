/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */


import com.amazonaws.cloudformation.CreateStackResponse;
import com.amazonaws.cloudformation.CreateStackResult;
import com.amazonaws.cloudformation.DeleteStackResponse;
import com.amazonaws.cloudformation.DescribeStacksResponse;
import com.amazonaws.cloudformation.DescribeStacksResult;
import com.amazonaws.cloudformation.Output;
import com.amazonaws.cloudformation.Outputs;
import com.amazonaws.cloudformation.Stack;
import com.amazonaws.cloudformation.Stacks;
import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.ServiceInstance;
import com.servicemesh.agility.api.ServiceProvider;
import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.service.cloudformation.CFServiceAdapter;
import com.servicemesh.agility.adapters.service.cloudformation.operations.CFServiceInstanceOperations;
import com.servicemesh.agility.sdk.service.msgs.ServiceInstanceProvisionRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceInstanceReleaseRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderResponse;
import com.servicemesh.agility.sdk.service.helper.PropertyHelper;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.reactor.Reactor;
import com.servicemesh.core.reactor.TimerHandler;
import com.servicemesh.core.messaging.Status;
import com.servicemesh.io.http.QueryParams;


class CFServiceInstanceOperationsSpec extends spock.lang.Specification
{
  def "dispatch ServiceInstanceProvision request"()
  {
    // setup provisioning request
    ServiceInstanceProvisionRequest request = new ServiceInstanceProvisionRequest();
    ServiceInstance instance = new ServiceInstance();
    instance.setName("Stack");
    instance.setId(1);
    PropertyHelper.setString(instance.getAssetProperties(), "prop1", "val1");
    request.setServiceInstance(instance);
    request.setProvider(new ServiceProvider());

    // mock adapter and connection
    AWSConnection conn = Mock(AWSConnection);
    Reactor reactor = Mock(Reactor);
    CFServiceAdapter adapter  = [ getConnection: { return conn; }, getReactor: { return reactor; } ] as CFServiceAdapter; 
    CFServiceInstanceOperations ops = new CFServiceInstanceOperations(adapter);

    // mock aws create stack response
    CreateStackResponse csr = new CreateStackResponse();
    csr.setCreateStackResult(new CreateStackResult());
    csr.getCreateStackResult().setStackId("stack1");
    Promise<CreateStackResponse> csr_promise = Promise.pure(csr);

    // mock aws describe stack response
    DescribeStacksResult dsresult = new DescribeStacksResult();
    Stack stack = new Stack();
    stack.setStackName("Stack-1");
    stack.setStackStatus("CREATE_COMPLETE");
    Output output = new Output();
    output.setOutputKey("Out-1");
    output.setOutputValue("Val-1");
    stack.setOutputs(new Outputs());
    stack.getOutputs().getMember().add(output);
    dsresult.setStacks(new Stacks());
    dsresult.getStacks().getMember().add(stack);

    DescribeStacksResponse dsresponse = new DescribeStacksResponse();
    dsresponse.setDescribeStacksResult(dsresult);
    Promise<DescribeStacksResponse> dsr_promise = Promise.pure(dsresponse);

    when:
       conn.initQueryParams(_) >> new QueryParams();
       conn.execute(_, CreateStackResponse.class) >> csr_promise;
       conn.execute(_, DescribeStacksResponse.class) >> dsr_promise;
       def promise = ops.provision(request);

    then:
       1*reactor.timerCreateRel(_,_) >> { arguments -> 
          final TimerHandler handler = arguments[1];
          handler.timerFire(0,0);
          return null;
       };
       def response = promise.get();
       response.getStatus() == Status.COMPLETE;
       instance.getConfigurations().size() == 2 + stack.getOutputs().getMember().size(); // StackId + StackName + output values
  }


  def "dispatch ServiceInstanceRelease request"()
  {
    // setup provisioning request
    ServiceInstanceReleaseRequest request = new ServiceInstanceReleaseRequest();
    ServiceInstance instance = new ServiceInstance();
    instance.setName("Stack");
    instance.setId(1);
    PropertyHelper.setString(instance.getConfigurations(), "StackId", "stack1");
    PropertyHelper.setString(instance.getConfigurations(), "StackName", "Stack-1");
    request.setServiceInstance(instance);
    request.setProvider(new ServiceProvider());

    // mock adapter and connection
    AWSConnection conn = Mock(AWSConnection);
    Reactor reactor = Mock(Reactor);
    CFServiceAdapter adapter  = [ getConnection: { return conn; }, getReactor: { return reactor; } ] as CFServiceAdapter; 
    CFServiceInstanceOperations ops = new CFServiceInstanceOperations(adapter);

    // mock aws create stack response
    DeleteStackResponse delete_sr = new DeleteStackResponse();
    Promise<DeleteStackResponse> delete_promise = Promise.pure(delete_sr);

    // mock aws describe stack response
    DescribeStacksResult dsresult = new DescribeStacksResult();
    Stack stack = new Stack();
    stack.setStackName("Stack-1");
    stack.setStackStatus("DELETE_COMPLETE");
    dsresult.setStacks(new Stacks());
    dsresult.getStacks().getMember().add(stack);

    DescribeStacksResponse dsresponse = new DescribeStacksResponse();
    dsresponse.setDescribeStacksResult(dsresult);
    Promise<DescribeStacksResponse> describe_promise = Promise.pure(dsresponse);

    when:
       conn.initQueryParams(_) >> new QueryParams();
       conn.execute(_, DeleteStackResponse.class) >> delete_promise;
       conn.execute(_, DescribeStacksResponse.class) >> describe_promise;
       def promise = ops.release(request);

    then:
       1*reactor.timerCreateRel(_,_) >> { arguments -> 
          final TimerHandler handler = arguments[1];
          handler.timerFire(0,0);
          return null;
       };
       def response = promise.get();
       response.getStatus() == Status.COMPLETE;
  }
}

