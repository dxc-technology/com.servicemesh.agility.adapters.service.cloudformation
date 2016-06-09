/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CreateStackResult", propOrder = { "stackId" })
public class CreateStackResult
{
    @XmlElement(name = "StackId")
    public String stackId;

    public String getStackId()
    {
        return stackId;
    }

    public void setStackId(String id)
    {
        stackId = id;
    }
}
