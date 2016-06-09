/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * com.amazonaws.ec2.doc._2013_10_15 package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content. The Java
 * representation of XML content can consist of schema derived interfaces and classes representing the binding of schema type
 * definitions, element declarations and model groups. Factory methods for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory
{
    private final static QName _CreateStackResponse_QNAME =
            new QName("http://cloudformation.amazonaws.com/doc/2010-05-15/", "CreateStackResponse");

    private final static QName _DeleteStackResponse_QNAME =
            new QName("http://cloudformation.amazonaws.com/doc/2010-05-15/", "DeleteStackResponse");

    private final static QName _DescribeStacksResponse_QNAME =
            new QName("http://cloudformation.amazonaws.com/doc/2010-05-15/", "DescribeStacksResponse");

    public ObjectFactory()
    {
    }

    /**
     * Create an instance of {@link CreateStackResponse }
     */
    public CreateStackResponse createCreateStackResponse()
    {
        return new CreateStackResponse();
    }

    /**
     * Create an instance of {@link DescribeStacksResponse }
     */
    public DescribeStacksResponse createDescribeStacksResponse()
    {
        return new DescribeStacksResponse();
    }

    @XmlElementDecl(namespace = "http://cloudformation.amazonaws.com/doc/2010-05-15/", name = "CreateStackResponse")
    public JAXBElement<CreateStackResponse> createCreateStackResponse(CreateStackResponse value)
    {
        return new JAXBElement<CreateStackResponse>(_CreateStackResponse_QNAME, CreateStackResponse.class, null, value);
    }

    @XmlElementDecl(namespace = "http://cloudformation.amazonaws.com/doc/2010-05-15/", name = "DeleteStackResponse")
    public JAXBElement<DeleteStackResponse> createDeleteStackResponse(DeleteStackResponse value)
    {
        return new JAXBElement<DeleteStackResponse>(_DeleteStackResponse_QNAME, DeleteStackResponse.class, null, value);
    }

    @XmlElementDecl(namespace = "http://cloudformation.amazonaws.com/doc/2010-05-15/", name = "DescribeStacksResponse")
    public JAXBElement<DescribeStacksResponse> createDescribeStacksResponse(DescribeStacksResponse value)
    {
        return new JAXBElement<DescribeStacksResponse>(_DescribeStacksResponse_QNAME, DescribeStacksResponse.class, null, value);
    }

}
