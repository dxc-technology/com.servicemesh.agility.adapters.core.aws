/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception triggered for an AWS error response.
 */
public class AWSErrorException extends RuntimeException
{
    private static final long serialVersionUID = 20150630;
    private List<AWSError> _errors;

    public AWSErrorException(String message, AWSError error)
    {
        super(message);
        _errors = new ArrayList<AWSError>();
        _errors.add(error);
    }

    public AWSErrorException(String message, List<AWSError> errors)
    {
        super(message);
        _errors = new ArrayList<AWSError>();
        _errors.addAll(errors);
    }

    public List<AWSError> getErrors()
    {
        return _errors;
    }

    /** Returns a string representation suitable for logging. */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ");

        String msg = this.getLocalizedMessage();
        if (msg != null)
            sb.append(msg);

        for (AWSError error : _errors) {
            sb.append(" { ").append(error.toString()).append("}");
        }
        return sb.toString();
    }
}
