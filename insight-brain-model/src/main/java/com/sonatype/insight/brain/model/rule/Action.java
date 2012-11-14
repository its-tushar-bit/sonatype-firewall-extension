/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

public class Action
{
    private ActionType actionType;

    private String value;

    protected ActionType getActionType()
    {
        return actionType;
    }

    protected void setActionType( ActionType actionType )
    {
        this.actionType = actionType;
    }

    protected String getValue()
    {
        return value;
    }

    protected void setValue( String value )
    {
        this.value = value;
    }
}
