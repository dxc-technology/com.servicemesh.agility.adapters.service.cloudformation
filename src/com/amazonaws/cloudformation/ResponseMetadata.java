/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResponseMetadata", propOrder = { "requestId" })
public class ResponseMetadata
{
    @XmlElement(name = "RequestId")
    public String requestId;

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String id)
    {
        requestId = id;
    }
}
