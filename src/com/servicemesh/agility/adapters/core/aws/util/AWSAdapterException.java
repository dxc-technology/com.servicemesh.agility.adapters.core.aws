/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

/**
 * General exception for adapter related processing.
 */
public class AWSAdapterException extends RuntimeException
{
    private static final long serialVersionUID = 20150630;

    public AWSAdapterException(String message)
    {
        super(message);
    }

    public AWSAdapterException(Throwable cause)
    {
        super(cause);
    }

    public AWSAdapterException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
