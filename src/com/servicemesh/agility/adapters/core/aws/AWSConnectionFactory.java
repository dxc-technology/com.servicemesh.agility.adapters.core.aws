/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws;

import java.util.List;

import com.servicemesh.agility.adapters.core.aws.impl.AWSConnectionImpl;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpRangeItemType;
import com.servicemesh.agility.adapters.core.aws.util.EC2SecurityGroupOperations;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;
import com.servicemesh.io.proxy.Proxy;

/**
 * Provides a connection to Amazon Web Services.
 */
public class AWSConnectionFactory
{
    private static final String EC2_GENERAL_ENDPOINT = "ec2.amazonaws.com";
    private static final String EC2_DEFAULT_REGION = "us-east-1";
    private static final String EC2_VERSION = "2015-10-01";

    private AWSConnectionFactory()
    {
    }

    private static class Holder
    {
        private static final AWSConnectionFactory _instance = new AWSConnectionFactory();
    }

    /**
     * Gets a connection factory.
     */
    public static AWSConnectionFactory getInstance()
    {
        return Holder._instance;
    }

    /**
     * Gets an AWS connection.
     *
     * @param settings
     *            The configuration settings for the connection. Optional, may be empty or null.
     * @param credential
     *            Must be a credential that contains a public and private key.
     * @param proxy
     *            The proxy to be utilized. Optional, may be null.
     * @param endpoint
     *            Provides data specific to an AWS service.
     * @see com.servicemesh.agility.adapters.core.aws.AWSConfig
     * @return An AWS connection.
     */
    public AWSConnection getConnection(List<Property> settings, Credential credential, Proxy proxy, AWSEndpoint endpoint)
            throws Exception
    {
        return new AWSConnectionImpl(settings, credential, proxy, endpoint);
    }

    /**
     * Gets an EC2 AWS Connection that can be used for making EC2 Query Api calls. This connection can be used for
     * {@link EC2SecurityGroupOperations}.
     *
     * @param settings
     *            The configuration settings for the connection. Optional, may be empty or null.
     * @param credential
     *            Must be a credential that contains a public and private key.
     * @param proxy
     *            The proxy to be utilized. Optional, may be null.
     * @param AmazonURI
     *            The base URI to be used in the Query Api call. i.e. "https://ec2.us-east-1.amazonaws.com/"
     * @return An AWSConnection for EC2.
     * @throws Exception
     */
    public AWSConnection getSecurityGroupConnection(List<Property> settings, Credential credential, Proxy proxy, String AmazonURI)
            throws Exception
    {
        AWSEndpointFactory endpointFactory = AWSEndpointFactory.getInstance();
        AWSEndpoint endpoint;
        if (AmazonURI.contains(EC2_GENERAL_ENDPOINT))
        {
            endpoint = endpointFactory.getEndpoint(AmazonURI, EC2_DEFAULT_REGION, EC2_VERSION, IpRangeItemType.class);
        }
        else
        { // region should be contained within the address
            endpoint = endpointFactory.getEndpoint(AmazonURI, EC2_VERSION, IpRangeItemType.class);
        }
        return new AWSConnectionImpl(settings, credential, proxy, endpoint);
    }
}
