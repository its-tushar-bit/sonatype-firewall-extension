package com.sonatype.insight.brain.model.rule;

import java.util.List;

public class Rule
{
    private String name;

    private List<SimpleCondition> conditions;

    private LogicalOperator operator;

    private Action action;

    protected List<SimpleCondition> getConditions()
    {
        return conditions;
    }

    protected void setConditions( List<SimpleCondition> conditions )
    {
        this.conditions = conditions;
    }

    protected LogicalOperator getOperator()
    {
        return operator;
    }

    protected void setOperator( LogicalOperator operator )
    {
        this.operator = operator;
    }

    protected Action getAction()
    {
        return action;
    }

    protected void setAction( Action action )
    {
        this.action = action;
    }

    protected String getName()
    {
        return name;
    }

    protected void setName( String name )
    {
        this.name = name;
    }
}
