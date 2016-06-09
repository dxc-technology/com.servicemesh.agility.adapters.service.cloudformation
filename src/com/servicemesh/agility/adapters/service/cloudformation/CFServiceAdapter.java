/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.service.cloudformation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.amazonaws.cloudformation.ObjectFactory;
import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.core.aws.AWSConnectionFactory;
import com.servicemesh.agility.adapters.core.aws.AWSEndpoint;
import com.servicemesh.agility.adapters.core.aws.AWSEndpointFactory;
import com.servicemesh.agility.adapters.service.cloudformation.operations.CFServiceInstanceOperations;
import com.servicemesh.agility.adapters.service.cloudformation.operations.CFServiceProviderOperations;
import com.servicemesh.agility.api.AssetType;
import com.servicemesh.agility.api.Connection;
import com.servicemesh.agility.api.Link;
import com.servicemesh.agility.api.PropertyDefinition;
import com.servicemesh.agility.api.PropertyType;
import com.servicemesh.agility.api.Service;
import com.servicemesh.agility.api.ServiceProvider;
import com.servicemesh.agility.api.ServiceProviderOption;
import com.servicemesh.agility.api.ServiceProviderType;
import com.servicemesh.agility.sdk.service.msgs.RegistrationRequest;
import com.servicemesh.agility.sdk.service.msgs.RegistrationResponse;
import com.servicemesh.agility.sdk.service.msgs.ServiceProviderRequest;
import com.servicemesh.agility.sdk.service.spi.IServiceInstance;
import com.servicemesh.agility.sdk.service.spi.IServiceProvider;
import com.servicemesh.agility.sdk.service.spi.ServiceAdapter;
import com.servicemesh.core.reactor.TimerReactor;
import com.servicemesh.io.proxy.Proxy;

public class CFServiceAdapter extends ServiceAdapter
{
    private static final Logger logger = Logger.getLogger(CFServiceAdapter.class);

    public static final String ADAPTER_VERSION;
    public static final String ADAPTER_VENDOR;

    public static final String SERVICE_PROVIDER_TYPE = "cloudformation-service-provider";
    public static final String SERVICE_PROVIDER_NAME = "AWS CloudFormation Service Provider";
    public static final String SERVICE_PROVIDER_DESCRIPTION;

    private AWSConnectionFactory factory;

    static
    {
        String PROP_FILE = "/resources/cf.properties";
        Properties props = new Properties();
        try
        {
            InputStream rs = CFServiceAdapter.class.getResourceAsStream(PROP_FILE);
            if (rs != null)
            {
                props.load(rs);
            }
            else
            {
                logger.error("Resource not found " + PROP_FILE);
            }
        }
        catch (Exception ex)
        {
            logger.error("Failed to load " + PROP_FILE + ": " + ex);
        }
        ADAPTER_VERSION = props.getProperty("adapter.version", "1.0.0");
        ADAPTER_VENDOR = props.getProperty("adapter.vendor", "");
        SERVICE_PROVIDER_DESCRIPTION = SERVICE_PROVIDER_NAME + " (" + ADAPTER_VERSION + ")";
    }

    public CFServiceAdapter() throws Exception
    {
        super(TimerReactor.getTimerReactor(SERVICE_PROVIDER_NAME));
        factory = AWSConnectionFactory.getInstance();
        logger.info(SERVICE_PROVIDER_DESCRIPTION);
    }

    public AWSConnection getConnection(ServiceProviderRequest request) throws Exception
    {
        ServiceProvider provider = request.getProvider();
        AWSEndpointFactory ef = AWSEndpointFactory.getInstance();
        AWSEndpoint endpoint = ef.getEndpoint(provider.getHostname(), "2010-05-15", ObjectFactory.class);

        Proxy proxy = null;
        List<Proxy> proxies = ServiceAdapter.getProxyConfig(request);
        if (proxies.size() > 0)
        {
            proxy = proxies.get(0);
        }

        return factory.getConnection(request.getSettings(), provider.getCredentials(), proxy, endpoint);
    }

    @Override
    public List<ServiceProviderType> getServiceProviderTypes()
    {
        List<ServiceProviderType> serviceProviderTypes = new ArrayList<ServiceProviderType>();
        ServiceProviderType serviceProviderType = new ServiceProviderType();
        serviceProviderType.setName(SERVICE_PROVIDER_NAME);
        serviceProviderType.setDescription(SERVICE_PROVIDER_DESCRIPTION);

        Link serviceProviderAssetType = new Link();
        serviceProviderAssetType.setName(SERVICE_PROVIDER_TYPE);
        serviceProviderAssetType.setType("application/" + AssetType.class.getName() + "+xml");
        serviceProviderType.setAssetType(serviceProviderAssetType);
        serviceProviderType.getOptions().add(ServiceProviderOption.NO_NETWORKS);
        serviceProviderTypes.add(serviceProviderType);
        return serviceProviderTypes;
    }

    @Override
    public void onRegistration(RegistrationResponse response)
    {
    }

    /**
     * Returns implementations of service provider message handlers.
     */

    @Override
    public IServiceProvider getServiceProviderOperations()
    {
        return new CFServiceProviderOperations(this);
    }

    /**
     * Returns implementations of service instance message handlers.
     */

    @Override
    public IServiceInstance getServiceInstanceOperations()
    {
        return new CFServiceInstanceOperations(this);
    }

    /**
     * Build up the set of asset types exposed by the adapter and return these in the registration request. If the adapter exposes
     * a service to an application via a blueprint, a sub-class of service is defined to expose this functionality and define
     * configuration parameters for the service. A sub-class of service provider exposes configuration parameters for the adapter
     * itself.
     */
    @Override
    public RegistrationRequest getRegistrationRequest()
    {
        logger.debug("getRegistrationRequest");
        // references to common types
        String X_PROPERTY_TYPE = "application/" + PropertyType.class.getName() + "+xml";
        Link string_type = new Link();
        string_type.setName("string-any");
        string_type.setType(X_PROPERTY_TYPE);

        Link integer_type = new Link();
        integer_type.setName("integer-any");
        integer_type.setType(X_PROPERTY_TYPE);

        Link encrypted_type = new Link();
        encrypted_type.setName("encrypted");
        encrypted_type.setType(X_PROPERTY_TYPE);

        String X_SERVICE_TYPE = "application/" + Service.class.getName() + "+xml";
        Link service = new Link();
        service.setName("service");
        service.setType(X_SERVICE_TYPE);

        Link lbaas = new Link();
        lbaas.setName("lbaas");
        lbaas.setType(X_SERVICE_TYPE);

        String X_SERVICE_PROVIDER_TYPE = "application/" + ServiceProviderType.class.getName() + "+xml";
        Link service_provider_type = new Link();
        service_provider_type.setName("serviceprovidertype");
        service_provider_type.setType(X_SERVICE_PROVIDER_TYPE);

        //
        // Connections
        //

        String X_CONNECTION_TYPE = "application/" + Connection.class.getName() + "+xml";
        Link connection_link = new Link();
        connection_link.setName("designconnection");
        connection_link.setType(X_CONNECTION_TYPE);

        //
        // CloudFormation Service Provider
        //

        PropertyDefinition urlPD = new PropertyDefinition();
        urlPD.setName(CFConstants.CF_TEMPLATE);
        urlPD.setDisplayName("CloudFormation Template");
        urlPD.setDescription("Template definition specified as either URL or actual JSON content.");
        urlPD.setReadable(true);
        urlPD.setWritable(true);
        urlPD.setMaxAllowed(255);
        urlPD.setPropertyType(string_type);

        AssetType cfServiceProviderType = new AssetType();
        cfServiceProviderType.setName(SERVICE_PROVIDER_TYPE);
        cfServiceProviderType.setDisplayName(SERVICE_PROVIDER_NAME);
        cfServiceProviderType.getPropertyDefinitions().add(urlPD);
        cfServiceProviderType.setSuperType(service_provider_type);

        //
        // Registration response
        //

        RegistrationRequest registration = new RegistrationRequest();
        registration.setName(SERVICE_PROVIDER_NAME);
        registration.setVersion(ADAPTER_VERSION);
        registration.getAssetTypes().add(cfServiceProviderType);
        registration.getServiceProviderTypes().addAll(getServiceProviderTypes());
        return registration;
    }
}
