/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CreateStackResponse", propOrder = { "createStackResult", "responseMetadata" })
public class DeleteStackResponse
{
    @XmlElement(name = "CreateStackResult")
    CreateStackResult createStackResult;

    @XmlElement(name = "ResponseMetadata")
    ResponseMetadata responseMetadata;

    public CreateStackResult getCreateStackResult()
    {
        return createStackResult;
    }

    public void setCreateStackResult(CreateStackResult csr)
    {
        createStackResult = csr;
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
