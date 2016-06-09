/**
 *              Copyright (c) 2015-Present Computer Sciences Corporation
 */

package com.amazonaws.cloudformation;

import java.util.ArrayList;
import java.util.List;

public class Stacks
{
    public List<Stack> member;

    public List<Stack> getMember()
    {
        if (member == null)
        {
            member = new ArrayList<Stack>();
        }
        return member;
    }

    public void setMember(List<Stack> member)
    {
        this.member = member;
    }
}
