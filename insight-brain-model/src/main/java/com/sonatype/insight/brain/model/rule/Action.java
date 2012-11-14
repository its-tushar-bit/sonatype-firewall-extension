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

    public ActionType getActionType()
    {
        return actionType;
    }

    public void setActionType( ActionType actionType )
    {
        this.actionType = actionType;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }
}
