/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DescribeStacksResult", propOrder = { "stacks" })
public class DescribeStacksResult
{
    @XmlElement(name = "Stacks")
    Stacks stacks;

    public Stacks getStacks()
    {
        return stacks;
    }

    public void setStacks(Stacks stacks)
    {
        this.stacks = stacks;
    }
}
