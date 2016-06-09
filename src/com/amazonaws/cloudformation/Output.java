/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Output
{
    @XmlElement(name = "OutputKey")
    public String outputKey;
    @XmlElement(name = "OutputValue")
    public String outputValue;

    public String getOutputKey()
    {
        return outputKey;
    }

    public void setOutputKey(String outputKey)
    {
        this.outputKey = outputKey;
    }

    public String getOutputValue()
    {
        return outputValue;
    }

    public void setOutputValue(String outputValue)
    {
        this.outputValue = outputValue;
    }
}
