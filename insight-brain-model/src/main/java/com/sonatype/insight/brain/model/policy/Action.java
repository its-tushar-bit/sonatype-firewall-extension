/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public class Action
{
    private String actionTypeId;

    private String target;

    public Action()
    {
    }

    public Action( final String actionTypeId )
    {
        this.actionTypeId = actionTypeId;
    }

    public String getActionTypeId()
    {
        return actionTypeId;
    }

    public void setActionTypeId( final String actionTypeId )
    {
        this.actionTypeId = actionTypeId;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget( final String target )
    {
        this.target = target;
    }

    @Override
    public String toString()
    {
        return "Action [actionTypeId=" + actionTypeId + ", target=" + target + "]";
    }
}
