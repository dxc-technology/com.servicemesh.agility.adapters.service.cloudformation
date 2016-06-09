/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.service.cloudformation.util;

/**
 * General exception for adapter related processing.
 */
public class CFAdapterException extends RuntimeException
{
    private static final long serialVersionUID = 20160601;

    public CFAdapterException(String message)
    {
        super(message);
    }

    public CFAdapterException(Throwable cause)
    {
        super(cause);
    }

    public CFAdapterException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
