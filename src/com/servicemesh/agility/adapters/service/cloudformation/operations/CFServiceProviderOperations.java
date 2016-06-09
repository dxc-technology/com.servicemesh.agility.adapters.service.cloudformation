/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.service.cloudformation.operations;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicemesh.agility.adapters.service.cloudformation.CFConstants;
import com.servicemesh.agility.adapters.service.cloudformation.CFServiceAdapter;
import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.AssetType;
import com.servicemesh.agility.api.Connection;
import com.servicemesh.agility.api.ConnectionDefinition;
import com.servicemesh.agility.api.ImportMode;
import com.servicemesh.agility.api.Link;
import com.servicemesh.agility.api.PrimitiveType;
import com.servicemesh.agility.api.PropertyDefinition;
import com.servicemesh.agility.api.PropertyType;
import com.servicemesh.agility.api.PropertyTypeValue;
import com.servicemesh.agility.api.Service;
import com.servicemesh.agility.api.ServiceProvider;
import com.servicemesh.agility.api.ServiceProviderType;
import com.servicemesh.agility.api.ValueConstraintType;
import com.servicemesh.agility.api.Workload;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderPingRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderPostCreateRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderPostUpdateRequest;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderResponse;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderSyncRequest;
import com.servicemesh.agility.sdk.service.operations.ServiceProviderOperations;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.messaging.Status;

public class CFServiceProviderOperations extends ServiceProviderOperations
{
    private final String X_SERVICE_TYPE = "application/" + Service.class.getName() + "+xml";
    private final String X_ASSET_TYPE = "application/" + AssetType.class.getName() + "+xml";

    private CFServiceAdapter adapter;

    public CFServiceProviderOperations(CFServiceAdapter adapter)
    {
        this.adapter = adapter;
    }

    @Override
    public Promise<ServiceProviderResponse> ping(ServiceProviderPingRequest request)
    {
        try
        {
            ServiceProviderResponse response = new ServiceProviderResponse();
            response.setStatus(Status.COMPLETE);
            return Promise.pure(response);
        }
        catch (Throwable t)
        {
            return Promise.pure(t);
        }
    }

    /**
     * Services SDK message handler for ServiceProvider lifecycle notifications. Use this hook to update our service definitions
     * corresponding to the cloud formation templates.
     *
     * @param request
     */

    @Override
    public Promise<ServiceProviderResponse> postCreate(ServiceProviderPostCreateRequest request)
    {
        return updateProvider(request.getProvider(), request.getServiceProviderTypes());
    }

    /**
     * Services SDK message handler for ServiceProvider lifecycle notifications. Use this hook to update our service definitions
     * corresponding to the cloud formation templates.
     *
     * @param request
     */

    @Override
    public Promise<ServiceProviderResponse> postUpdate(ServiceProviderPostUpdateRequest request)
    {
        return updateProvider(request.getProvider(), request.getServiceProviderTypes());
    }

    /**
     * Services SDK message handler for ServiceProvider lifecycle notifications. Use this hook to update our service definitions
     * corresponding to the cloud formation templates.
     *
     * @param request
     */

    @Override
    public Promise<ServiceProviderResponse> sync(ServiceProviderSyncRequest request)
    {
        return updateProvider(request.getProvider(), request.getServiceProviderTypes());
    }

    /**
     * Pull each of the cloud formation templates defined on the provider and build an asset type that models the corresponding
     * input parameters defined for each.
     *
     * @param provider
     * @param serviceProviderTypes
     */

    Promise<ServiceProviderResponse> updateProvider(ServiceProvider provider, List<ServiceProviderType> serviceProviderTypes)
    {
        try
        {
            ServiceProviderResponse response = new ServiceProviderResponse();
            Map<String, Link> cfTypes = new HashMap<String, Link>();
            for (AssetProperty ap : provider.getProperties())
            {
                if (ap.getName().equals(CFConstants.CF_TEMPLATE))
                {
                    String[] urls = ap.getStringValue().split("\n");
                    for (String url : urls)
                    {
                        AssetType cfType = parseTemplate(url);
                        response.getModified().add(cfType);

                        //
                        // For cloud formation template allow a workload to have a dependency on the corresponding asset type
                        //

                        String X_CONNECTION_TYPE = "application/" + Connection.class.getName() + "+xml";
                        Link connection_link = new Link();
                        connection_link.setName("designconnection");
                        connection_link.setType(X_CONNECTION_TYPE);

                        Link workloadTypeLink = new Link();
                        workloadTypeLink.setName("designworkload");
                        workloadTypeLink.setType("application/" + Workload.class.getName() + "+xml");

                        AssetType cfTypeToWorkload = new AssetType();
                        cfTypeToWorkload.setName(cfType.getName() + "-workload");
                        cfTypeToWorkload.setDisplayName(cfType.getDisplayName() + " Workload Connection");
                        cfTypeToWorkload.setAllowExtensions(false);
                        cfTypeToWorkload.setSuperType(connection_link);
                        cfTypeToWorkload.setDescription(cfType.getDisplayName() + " Connection To Workload");

                        ConnectionDefinition workloadToCFTypeConnection = new ConnectionDefinition();
                        workloadToCFTypeConnection.setImportMode(ImportMode.CREATE_OR_UPDATE);
                        workloadToCFTypeConnection.setName("workload-" + cfType.getName());
                        workloadToCFTypeConnection.setDisplayName("Workload-" + cfType.getDisplayName());
                        workloadToCFTypeConnection.setDescription("Workload connection to " + cfType.getDisplayName());
                        workloadToCFTypeConnection.setConnectionType(connection_link);
                        workloadToCFTypeConnection.setSourceType(workloadTypeLink);
                        cfType.getDestConnections().add(workloadToCFTypeConnection);

                        Link link = new Link();
                        link.setName(cfType.getName());
                        link.setType("application/" + AssetType.class.getName() + "+xml");
                        cfTypes.put(cfType.getName(), link);
                    }
                }
            }

            // Associate the template asset types with the service provider type. This is required for the blueprint deployer
            // to map these types to the adapter/provider that implements them.

            for (ServiceProviderType spt : serviceProviderTypes)
            {
                if (spt.getName().equals(provider.getType().getName()))
                {
                    spt.getServiceTypes().addAll(new ArrayList<Link>(cfTypes.values()));
                    response.getModified().add(spt);
                }
            }

            response.setStatus(Status.COMPLETE);
            return Promise.pure(response);
        }
        catch (Throwable t)
        {
            return Promise.pure(t);
        }
    }

    /**
     * Parse the template and for any defined input parameters and build up an asset type with a corresponding set of property
     * definitions.
     *
     * @param template
     * @return
     * @throws Exception
     */
    private AssetType parseTemplate(String template) throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json;
        String name = null;
        int index = template.indexOf("{");
        if (index >= 0)
        {
            json = mapper.readTree(template);
        }
        else
        {
            URL url = new URL(template);
            URLConnection conn = url.openConnection();
            name = url.getPath();
            index = name.lastIndexOf("/");
            if (index > 0)
            {
                name = name.substring(index + 1);
            }
            index = name.indexOf(".");
            if (index > 0)
            {
                name = name.substring(0, index);
            }
            json = mapper.readTree(conn.getInputStream());
        }
        JsonNode description = json.findPath("Description");
        if (name == null)
        {
            name = description.asText();
        }

        JsonNode params = json.findPath("Parameters");

        Link serviceType = new Link();
        serviceType.setName("service");
        serviceType.setType(X_SERVICE_TYPE);

        Link string_type = new Link();
        string_type.setName("string-any");
        string_type.setType(X_ASSET_TYPE);

        Link numeric_type = new Link();
        numeric_type.setName("numeric-any");
        numeric_type.setType(X_ASSET_TYPE);

        AssetType cfType = new AssetType();
        cfType.setImportMode(ImportMode.CREATE_OR_UPDATE);
        cfType.setName(name);
        cfType.setDisplayName(name);
        cfType.setAllowExtensions(false);
        cfType.setSuperType(serviceType);
        cfType.setDescription(description.asText());

        Iterator<Entry<String, JsonNode>> fields = params.fields();
        while (fields.hasNext())
        {
            Entry<String, JsonNode> e = fields.next();
            String paramName = e.getKey();
            JsonNode param = e.getValue();

            JsonNode paramType = param.path("Type");
            JsonNode paramDescription = param.path("Description");
            JsonNode paramDefault = param.path("Default");
            JsonNode paramValues = param.path("AllowedValues");

            PropertyDefinition pd = new PropertyDefinition();
            pd.setName(paramName);
            pd.setDisplayName(paramName);
            pd.setDescription(paramDescription.asText());
            pd.setReadable(true);
            pd.setWritable(true);
            cfType.getPropertyDefinitions().add(pd);

            PrimitiveType prim = null;
            switch (paramType.asText())
            {
                case "Number":
                    pd.setMinRequired(1);
                    pd.setMaxAllowed(1);
                    pd.setPropertyType(numeric_type);
                    prim = PrimitiveType.NUMERIC;
                    break;
                case "List<Number>":
                    pd.setMinRequired(1);
                    pd.setMaxAllowed(255);
                    pd.setPropertyType(numeric_type);
                    prim = PrimitiveType.NUMERIC;
                    break;
                case "CommaDelimitedList":
                case "List<String>":
                    pd.setMinRequired(1);
                    pd.setMaxAllowed(255);
                    pd.setPropertyType(string_type);
                    prim = PrimitiveType.STRING;
                    break;
                case "String":
                default:
                    pd.setMinRequired(1);
                    if (paramType.asText().startsWith("List"))
                    {
                        pd.setMaxAllowed(255);
                    }
                    else
                    {
                        pd.setMaxAllowed(1);
                    }
                    pd.setPropertyType(string_type);
                    prim = PrimitiveType.STRING;
                    break;
            }

            if (!paramValues.isMissingNode())
            {
                PropertyType pt = new PropertyType();
                pt.setName(paramName);
                pt.setType(prim);
                pt.setDisplayName(paramName);
                pt.setValueConstraint(ValueConstraintType.LIST);
                Iterator<JsonNode> values = paramValues.iterator();
                while (values.hasNext())
                {
                    JsonNode value = values.next();
                    PropertyTypeValue ptv = new PropertyTypeValue();
                    ptv.setName(value.asText());
                    ptv.setDisplayName(value.asText());
                    ptv.setValue(value.asText());
                    pt.getRootValues().add(ptv);
                }
                pd.setPropertyTypeValue(pt);
            }
            if (!paramDefault.isMissingNode())
            {
                AssetProperty defaultValue = new AssetProperty();
                String value = paramDefault.asText();
                defaultValue.setName(value);
                switch (paramType.asText())
                {
                    case "Number":
                    case "List<Number>":
                        defaultValue.setIntValue(Integer.valueOf(value));
                        defaultValue.setFloatValue(BigDecimal.valueOf(Double.parseDouble(value)));
                        break;
                    case "CommaDelimitedList":
                    case "List<String>":
                    case "String":
                    default:
                        defaultValue.setStringValue(value);
                        break;
                }
                defaultValue.setStringValue(value);
                pd.getDefaultValues().add(defaultValue);
            }
        }

        AssetProperty ap = new AssetProperty();
        ap.setName(CFConstants.CF_TEMPLATE);
        ap.setStringValue(template);

        PropertyDefinition pd = new PropertyDefinition();
        pd.setName(CFConstants.CF_TEMPLATE);
        pd.setDisplayName("Template");
        pd.setDescription("CloudFormation template body or URL");
        pd.setPropertyType(string_type);
        ;
        pd.setReadable(false);
        pd.setWritable(false);
        pd.setMinRequired(1);
        pd.setMaxAllowed(1);
        pd.getDefaultValues().add(ap);
        cfType.getPropertyDefinitions().add(pd);
        return cfType;
    }

}
