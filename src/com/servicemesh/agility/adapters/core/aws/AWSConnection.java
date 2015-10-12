/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws;

import java.util.Map;

import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.QueryParams;

/**
 * Performs actions for Amazon Web Services Query APIs.
 */
public interface AWSConnection
{
    /**
     * Returns the endpoint associated with this connection.
     */
    public AWSEndpoint getEndpoint();

    /**
     * Initializes query parameters.
     *
     * @param action
     *            The AWS action to be performed.
     */
    public QueryParams initQueryParams(String action);

    /**
     * Performs an AWS action via a HTTP GET method.
     *
     * @param params
     *            Query parameters
     * @param responseClass
     *            The class of resource to be retrieved.
     * @return A Promise for the retrieved resource.
     */
    public <T> Promise<T> execute(QueryParams params, final Class<T> responseClass);

    /**
     * Performs an AWS action via the specified HTTP method.
     *
     * @param method
     *            HTTP method
     * @param params
     *            Query parameters
     * @param responseClass
     *            The class of resource to be retrieved.
     * @return A Promise for the retrieved resource.
     */
    public <T> Promise<T> execute(HttpMethod method, QueryParams params, final Class<T> responseClass);

    /**
     * Performs an AWS action via the specified HTTP method.
     *
     * @param method
     *            HTTP method
     * @param params
     *            Query parameters
     * @return A Promise with the HTTP response.
     */
    public Promise<IHttpResponse> execute(HttpMethod method, QueryParams params);

    /**
     * Performs an AWS action via the specified HTTP method.
     *
     * @param method
     *            HTTP method
     * @param requestURI
     *            The URI specific to retrieving a resource. Optional, may be null.
     * @param headers
     *            HTTP headers. Optional, may be null.
     * @param params
     *            Query parameters. Optional, may be null.
     * @param resource
     *            Request element. If type is String it is directly used while any other type is encoded. Optional, may be null.
     * @param responseClass
     *            The class of resource to be retrieved.
     * @return A Promise for the retrieved resource.
     */
    public <T> Promise<T> execute(HttpMethod method, String requestURI, Map<String, String> headers, QueryParams params,
            Object resource, final Class<T> responseClass);
}
