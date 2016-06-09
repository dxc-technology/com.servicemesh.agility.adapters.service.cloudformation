/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.service.cloudformation;

import java.util.List;

import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.Property;

/**
 * Provides configuration settings for AWS connections.
 */
public class CFConfig
{
    public static final String REQUEST_RETRIES = "Kubernetes.RequestRetries";
    public static final String CONNECTION_TIMEOUT = "Kubernetes.ConnTimeoutMillis";
    public static final String SOCKET_TIMEOUT = "Kubernetes.SocketTimeoutMillis";

    public static final int REQUEST_RETRIES_DEFAULT = 2;
    public static final int CONNECTION_TIMEOUT_DEFAULT_SECS = 240;
    public static final int SOCKET_TIMEOUT_DEFAULT_SECS = 20;

    /**
     * Returns the number of retries upon failure of an HTTP request.
     *
     * @param settings
     *            Configuration data - if empty or null a default value is returned.
     * @return The request retries value.
     */
    public static int getRequestRetries(List<Property> settings)
    {
        return getPropertyAsInteger(CFConfig.REQUEST_RETRIES, settings, CFConfig.REQUEST_RETRIES_DEFAULT);
    }

    /**
     * Returns the number of milliseconds to wait for a successful HTTP connection/response.
     *
     * @param settings
     *            Configuration data - if empty or null a default value is returned.
     * @return The connection timeout value.
     */
    public static int getConnectionTimeout(List<Property> settings)
    {
        return getPropertyAsInteger(CFConfig.CONNECTION_TIMEOUT, settings, CFConfig.CONNECTION_TIMEOUT_DEFAULT_SECS * 1000);
    }

    /**
     * Returns the number of milliseconds to wait for a successful HTTP socket connection.
     *
     * @param settings
     *            Configuration data - if empty or null a default value is returned.
     * @return The socket timeout value.
     */
    public static int getSocketTimeout(List<Property> settings)
    {
        return getPropertyAsInteger(CFConfig.SOCKET_TIMEOUT, settings, CFConfig.SOCKET_TIMEOUT_DEFAULT_SECS * 1000);
    }

    /**
     * Returns the requested property as an integer value.
     *
     * @param name
     *            The name of the requested property.
     * @param properties
     *            Configuration data. Optional, may be empty or null.
     * @param defaultValue
     *            The default value to return if property is not found in properties parameter.
     * @return The int value of the property.
     */
    public static int getPropertyAsInteger(String name, List<Property> properties, int defaultValue)
    {
        int value = defaultValue;
        if (properties != null)
        {
            for (Property property : properties)
            {
                if (property.getName().equals(name))
                {
                    value = Integer.parseInt(property.getValue());
                    break;
                }
            }
        }
        return value;
    }

    /**
     * Returns the requested asset property as a string value.
     *
     * @param name
     *            The name of the requested asset property.
     * @param properties
     *            Configuration data. Optional, may be empty or null.
     * @return The string value of the property.
     */
    public static String getAssetPropertyAsString(String name, List<AssetProperty> properties)
    {
        AssetProperty ap = getAssetProperty(name, properties);
        return (ap != null) ? ap.getStringValue() : null;
    }

    /**
     * Returns the requested asset property.
     *
     * @param name
     *            The name of the requested asset property.
     * @param properties
     *            Configuration data. Optional, may be empty or null.
     * @return The asset property with the given name.
     */
    public static AssetProperty getAssetProperty(String name, List<AssetProperty> properties)
    {
        AssetProperty property = null;
        if (properties != null)
        {
            for (AssetProperty ap : properties)
            {
                if (ap.getName().equals(name))
                {
                    property = ap;
                    break;
                }
            }
        }
        return property;
    }
}
