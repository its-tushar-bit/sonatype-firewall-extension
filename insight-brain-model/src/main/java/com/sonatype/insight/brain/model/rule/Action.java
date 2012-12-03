/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

public class Action
{
    private String actionTypeId;

    private String value;

    public Action()
    {
    }

    public Action( final String actionTypeId )
    {
        this.actionTypeId = actionTypeId;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( final String value )
    {
        this.value = value;
    }

    public String getActionTypeId()
    {
        return actionTypeId;
    }

    public void setActionTypeId( final String actionTypeId )
    {
        this.actionTypeId = actionTypeId;
    }
}
