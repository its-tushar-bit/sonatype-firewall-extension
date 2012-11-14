/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.List;

public class Rule
{
    private String id;

    private String name;

    private List<SimpleCondition> conditions;

    private LogicalOperator operator;

    private Action action;

    private boolean enabled = true;

    public List<SimpleCondition> getConditions()
    {
        return conditions;
    }

    public void setConditions( List<SimpleCondition> conditions )
    {
        this.conditions = conditions;
    }

    public LogicalOperator getOperator()
    {
        return operator;
    }

    public void setOperator( LogicalOperator operator )
    {
        this.operator = operator;
    }

    public Action getAction()
    {
        return action;
    }

    public void setAction( Action action )
    {
        this.action = action;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }
}
