/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import java.io.Serializable;

/**
 * Data container for one AWS error.
 */
public class AWSError implements Serializable
{
    private static final long serialVersionUID = 20150630;
    private String _code;
    private String _message;
    private String _resource;
    private String _requestId;

    public void setCode(String code)
    {
        _code = code;
    }

    public String getCode()
    {
        return _code;
    }

    public void setMessage(String message)
    {
        _message = message;
    }

    public String getMessage()
    {
        return _message;
    }

    public void setResource(String resource)
    {
        _resource = resource;
    }

    public String getResource()
    {
        return _resource;
    }

    public void setRequestId(String requestId)
    {
        _requestId = requestId;
    }

    public String getRequestId()
    {
        return _requestId;
    }

    /** Returns a string representation suitable for logging. */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        append("code", _code, sb);
        append("message", _message, sb);
        append("resource", _resource, sb);
        append("requestId", _requestId, sb);
        return sb.toString();
    }

    private void append(String name, String value, StringBuilder sb)
    {
        if (value != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(name).append("=").append(value);
        }
    }
}
