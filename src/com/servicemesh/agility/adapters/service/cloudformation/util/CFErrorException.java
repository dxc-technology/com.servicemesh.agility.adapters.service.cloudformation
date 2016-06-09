/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.service.cloudformation.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception triggered for an AWS error response.
 */
public class CFErrorException extends RuntimeException
{
    private static final long serialVersionUID = 20160601;
    private List<CFError> _errors;

    public CFErrorException(String message, CFError error)
    {
        super(message);
        _errors = new ArrayList<CFError>();
        _errors.add(error);
    }

    public CFErrorException(String message, List<CFError> errors)
    {
        super(message);
        _errors = new ArrayList<CFError>();
        _errors.addAll(errors);
    }

    public List<CFError> getErrors()
    {
        return _errors;
    }

    /** Returns a string representation suitable for logging. */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ");

        String msg = getLocalizedMessage();
        if (msg != null)
        {
            sb.append(msg);
        }

        for (CFError error : _errors)
        {
            sb.append(" { ").append(error.toString()).append("}");
        }
        return sb.toString();
    }
}
