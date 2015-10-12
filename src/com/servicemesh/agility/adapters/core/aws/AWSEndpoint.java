/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws;

import javax.xml.bind.JAXBContext;

import com.servicemesh.io.http.IHttpResponse;

/**
 * Provides data and serialization required for operations directed at AWS.
 */
public interface AWSEndpoint
{
    public static final int DEFAULT_URL_EXPIRE_SECS = 30;
    public static final String DEFAULT_CONTENT_TYPE = "application/xml";
    public static final String CHAR_SET = "UTF-8";

    /** Returns the base address. */
    public String getAddress();

    /** Returns the host name. */
    public String getHostName();

    /** Returns the AWS service name. */
    public String getServiceName();

    /** Returns the AWS region name. */
    public String getRegionName();

    /** Returns the value for a Content-Type header. */
    public String getContentType();

    /** Returns the version. */
    public String getVersion();

    /** Returns the expiration time in seconds for a signed URL. */
    public int getUrlExpireSecs();

    /** Returns the default JAXB context. */
    public JAXBContext getContext();

    /**
     * Returns the JAXB context for a specified context path. Must use the same class loader as the default context.
     */
    public JAXBContext getContext(String contextPath);

    /**
     * Decodes a HTTP response.
     *
     * @param response
     *            An HTTP response.
     * @param responseClass
     *            The class contained in the body of a successful response.
     * @return An object of type responseClass for a successful response.
     */
    public <T> T decode(IHttpResponse response, Class<T> responseClass);

    /**
     * Decodes a HTTP response.
     *
     * @param response
     *            An HTTP response.
     * @param responseClass
     *            The class contained in the body of a successful response.
     * @param responseContextPath
     *            The context path for the responseClass if the default JAXB context is not to be used. Must use the same class
     *            loader as the default context.
     * @return An object of type responseClass for a successful response.
     */
    public <T> T decode(IHttpResponse response, String responseContextPath, Class<T> responseClass);

    /**
     * Encodes an object.
     *
     * @param obj
     *            The object to be encoded.
     * @return The object encoded as an XML string.
     */
    public String encode(Object obj);

    /**
     * Encodes an object.
     *
     * @param objContextPath
     *            The context path for the object if the default JAXB context is not to be used. Must use the same class loader as
     *            the default context.
     * @param obj
     *            The object to be encoded.
     * @return The object encoded as an XML string.
     */
    public String encode(String objContextPath, Object obj);
}
