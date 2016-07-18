/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws;

import java.util.List;

import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;

/**
 * Provides configuration settings for AWS connections.
 */
public class AWSConfig
{
    public static final String REQUEST_RETRIES = "AgilityManager.aws.RequestRetries";

    public static final String CONNECTION_TIMEOUT = "AgilityManager.aws.ConnTimeoutMillis";

    public static final String SOCKET_TIMEOUT = "AgilityManager.aws.SocketTimeoutMillis";

    
    public static final String SERVER_BUSY_RETRIES = "AgilityManager.EC2.ServerBusyRetries";
    
    public static final String SERVER_BUSY_RETRIES_INTERVAL = "AgilityManager.EC2.ServerBusyRetryInterval";
    
    public static final int REQUEST_RETRIES_DEFAULT = 2;
    public static final int CONNECTION_TIMEOUT_DEFAULT_SECS = 240;
    public static final int SOCKET_TIMEOUT_DEFAULT_SECS = 20;

    public static final int SERVER_BUSY_RETRIES_DEFAULT = 3;
    public static final int SERVER_BUSY_RETRIES_INTERVAL_DEFAULT = 20;
    
    public static final String AWS_ACCESS_KEY = "access-key";
    public static final String AWS_SECRET_KEY = "secret-key";

    /**
     * Returns the number of retries upon failure of an HTTP request.
     *
     * @param settings
     *            Configuration data - if empty or null a default value is returned.
     * @return The request retries value.
     */
    public static int getRequestRetries(List<Property> settings)
    {
        return getPropertyAsInteger(AWSConfig.REQUEST_RETRIES, settings, AWSConfig.REQUEST_RETRIES_DEFAULT);
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
        return getPropertyAsInteger(AWSConfig.CONNECTION_TIMEOUT, settings, AWSConfig.CONNECTION_TIMEOUT_DEFAULT_SECS * 1000);
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
        return getPropertyAsInteger(AWSConfig.SOCKET_TIMEOUT, settings, AWSConfig.SOCKET_TIMEOUT_DEFAULT_SECS * 1000);
    }
    
    /**
     * Returns the number of retries when server sends busy condition.
     *
     * @param settings
     *            Configuration data - if empty or null a default value is returned.
     * @return The server busy retries value.
     */
    public static int getServerBusyRetries(List<Property> settings)
    {
        return getPropertyAsInteger(AWSConfig.SERVER_BUSY_RETRIES, settings, AWSConfig.SERVER_BUSY_RETRIES_DEFAULT * 1000);
    }
    
    /**
     * Returns the number of milliseconds to wait for a server busy results.
     *
     * @param settings
     *            Configuration data - if empty or null a default value is returned.
     * @return The server busy retry interval value.
     */
    public static long getServerBusyRetryInterval(List<Property> settings)
    {
        return getPropertyAsInteger(AWSConfig.SERVER_BUSY_RETRIES_INTERVAL, settings,
                                    AWSConfig.SERVER_BUSY_RETRIES_DEFAULT * 1000);
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
     * Returns a Credential with AWS access/secret key values as the Credential's public/private attributes.
     *
     * @param properties
     *            Configuration data containing AWS_ACCESS_KEY and AWS_SECRET_KEY properties.
     * @return a Credential with AWS access/secret key values as the Credential's public/private attributes.
     */
    public static Credential getAWSCredentials(List<AssetProperty> properties)
    {
        Credential cred = null;
        String accessKey = AWSConfig.getAssetPropertyAsString(AWS_ACCESS_KEY, properties);
        
        if ((accessKey != null) && (!accessKey.isEmpty()))
        {
            String secretKey = AWSConfig.getAssetPropertyAsString(AWS_SECRET_KEY, properties);
            if ((secretKey != null) && (!secretKey.isEmpty()))
            {
                cred = new Credential();
                cred.setPublicKey(accessKey);
                cred.setPrivateKey(secretKey);
            }
        }
        return cred;
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
