/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Stack
{
    @XmlElement(name = "StackName")
    public String stackName;
    @XmlElement(name = "StackId")
    public String stackId;
    @XmlElement(name = "StackStatus")
    public String stackStatus;
    @XmlElement(name = "StackStatusReason")
    public String stackStatusReason;
    @XmlElement(name = "Outputs")
    Outputs outputs;

    public String getStackName()
    {
        return stackName;
    }

    public void setStackName(String stackName)
    {
        this.stackName = stackName;
    }

    public String getStackId()
    {
        return stackId;
    }

    public void setStackId(String stackId)
    {
        this.stackId = stackId;
    }

    public String getStackStatus()
    {
        return stackStatus;
    }

    public void setStackStatus(String stackStatus)
    {
        this.stackStatus = stackStatus;
    }

    public String getStackStatusReason()
    {
        return stackStatusReason;
    }

    public void setStackStatusReason(String reason)
    {
        stackStatusReason = reason;
    }

    public Outputs getOutputs()
    {
        return outputs;
    }

    public void setOutputs(Outputs outputs)
    {
        this.outputs = outputs;
    }

}
