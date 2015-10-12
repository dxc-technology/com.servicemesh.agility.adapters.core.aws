/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws;

import javax.xml.bind.JAXBContext;

import com.servicemesh.agility.adapters.core.aws.impl.AWSEndpointImpl;

/**
 * Provides an AWS endpoint.
 */
public class AWSEndpointFactory
{
    private AWSEndpointFactory()
    {
    }

    private static class Holder
    {
        private static final AWSEndpointFactory _instance = new AWSEndpointFactory();
    }

    /**
     * Gets an endpoint factory.
     */
    public static AWSEndpointFactory getInstance()
    {
        return Holder._instance;
    }

    /**
     * Gets an AWS endpoint.
     *
     * @param address
     *            The address of the AWS access point. The address should have the form
     *            "[<uri-scheme>://]<service-name>.<region-name>.<domain>",
     *            e.g. "elasticloadbalancing.us-east-1.amazonaws.com". Defaults
     *            to "https://" if the address only consists of
     *            "<service-name>.<region-name>.<domain>"
     * @param version
     *            The API version, e.g. "2012-06-01"
     * @param contextClass
     *            One of the JAXB classes for the AWS API. Used to initialize the default context for the endpoint.
     * @return An AWS endpoint.
     */
    public <T> AWSEndpoint getEndpoint(String address, String version, Class<T> contextClass) throws Exception
    {
        return new AWSEndpointImpl(address, null, version, contextClass);
    }

    /**
     * Gets an AWS endpoint.
     *
     * @param address
     *            The address of the AWS access point. The address should have the form
     *            "[<uri-scheme>://]<service-name>.<domain>", e.g.
     *            "elasticloadbalancing.amazonaws.com". Defaults to "https://"
     *            if the address only consists "<service-name>.<domain>"
     * @param regionName
     *            The AWS region name, e.g. "us-east-1"
     * @param version
     *            The API version, e.g. "2012-06-01"
     * @param contextClass
     *            One of the JAXB classes for the AWS API. Used to initialize the default context for the endpoint.
     * @return An AWS endpoint.
     */
    public <T> AWSEndpoint getEndpoint(String address, String regionName, String version, Class<T> contextClass) throws Exception
    {
        return new AWSEndpointImpl(address, regionName, version, contextClass);
    }

    /**
     * Gets an AWS endpoint.
     *
     * @param uriScheme
     *            The URI scheme, e.g. "https"
     * @param hostName
     *            The AWS host name, e.g. "mybucket.s3.amazonws.com"
     * @param serviceName
     *            The AWS service name, e.g. "s3"
     * @param regionName
     *            The AWS region name, e.g. "us-east-1"
     * @param version
     *            The API version, e.g. "2012-06-01"
     * @param urlExpireSecs
     *            The expiration time in seconds for a signed URL. Defaults to
     *            AWSEndpoint.DEFAULT_URL_EXPIRE_SECS if urlExpireSecs <= 0,
     *            is defAWSEndpoint
     * @param contextClass
     *            One of the JAXB classes for the AWS API. Used to initialize the default context for the endpoint.
     * @return An AWS endpoint.
     */
    public <T> AWSEndpoint getEndpoint(String uriScheme, String hostName, String serviceName, String regionName, String version,
            int urlExpireSecs, Class<T> contextClass) throws Exception
    {
        return new AWSEndpointImpl(uriScheme, hostName, serviceName, regionName, version, urlExpireSecs, contextClass);
    }

    /**
     * Returns the JAXB context for a namespace.
     *
     * @param contextPath
     *            The namespace containing JAXB classes for the AWS API.
     * @return The JAXBContext for the namespace.
     */
    public JAXBContext lookupContext(String contextPath)
    {
        return AWSEndpointImpl.lookupContext(contextPath);
    }

    /**
     * Unregisters a namespace.
     *
     * @param contextPath
     *            The namespace containing JAXB classes for the AWS API.
     */
    public void unregisterContext(String contextPath)
    {
        AWSEndpointImpl.unregisterContext(contextPath);
    }
}
