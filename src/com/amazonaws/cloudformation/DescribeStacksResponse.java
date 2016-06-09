/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DescribeStacksResponse", propOrder = { "describeStacksResult", "responseMetadata" })
public class DescribeStacksResponse
{
    @XmlElement(name = "DescribeStacksResult")
    DescribeStacksResult describeStacksResult;

    @XmlElement(name = "ResponseMetadata")
    ResponseMetadata responseMetadata;

    public DescribeStacksResult getDescribeStacksResult()
    {
        return describeStacksResult;
    }

    public void setDescribeStacksResult(DescribeStacksResult dsr)
    {
        describeStacksResult = dsr;
    }

    public ResponseMetadata getResponseMetadata()
    {
        return responseMetadata;
    }

    public void setResponseMetadata(ResponseMetadata rmd)
    {
        responseMetadata = rmd;
    }

}
