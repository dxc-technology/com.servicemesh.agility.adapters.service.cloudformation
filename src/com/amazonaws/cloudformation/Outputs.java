/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Outputs
{
    public List<Output> member;

    public List<Output> getMember()
    {
        if (member == null)
        {
            member = new ArrayList<Output>();
        }
        return member;
    }

    public void setMember(List<Output> member)
    {
        this.member = member;
    }
}
