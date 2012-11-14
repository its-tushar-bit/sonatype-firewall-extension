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
