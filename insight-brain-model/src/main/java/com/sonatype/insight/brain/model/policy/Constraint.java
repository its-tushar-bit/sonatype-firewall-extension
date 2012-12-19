/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.List;

public class Constraint
{
    private String id;

    private String name;

    private boolean enabled = true;

    private LogicalOperator operator = LogicalOperator.AND;

    private List<Condition> conditions;

    public Constraint()
    {
    }

    public Constraint( final String id, final String name, final LogicalOperator operator )
    {
        this.id = id;
        this.name = name;
        this.operator = operator;
    }

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public LogicalOperator getOperator()
    {
        return operator;
    }

    public void setOperator( final LogicalOperator operator )
    {
        this.operator = operator;
    }

    public List<Condition> getConditions()
    {
        return conditions;
    }

    public void setConditions( final List<Condition> conditions )
    {
        this.conditions = conditions;
    }

    public void addCondition( final Condition condition )
    {
        if ( conditions == null )
        {
            conditions = new ArrayList<Condition>();
        }
        conditions.add( condition );
    }

    @Override
    public String toString()
    {
        return "Constraint [id=" + id + ", name=" + name + ", enabled=" + enabled + ", operator=" + operator + "]";
    }
}
