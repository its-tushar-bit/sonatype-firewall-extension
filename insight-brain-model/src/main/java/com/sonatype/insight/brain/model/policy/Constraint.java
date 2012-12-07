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

    private List<SimpleCondition> conditions;

    private LogicalOperator operator;

    private boolean enabled = true;

    public Constraint()
    {
    }

    public Constraint( final String id, final String name, final LogicalOperator operator )
    {
        this.id = id;
        this.name = name;
        this.operator = operator;
    }

    public List<SimpleCondition> getConditions()
    {
        return conditions;
    }

    public void setConditions( final List<SimpleCondition> conditions )
    {
        this.conditions = conditions;
    }

    public void addCondition( final SimpleCondition condition )
    {
        if ( conditions == null )
        {
            conditions = new ArrayList<SimpleCondition>();
        }
        conditions.add( condition );
    }

    public LogicalOperator getOperator()
    {
        return operator;
    }

    public void setOperator( final LogicalOperator operator )
    {
        this.operator = operator;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }
}
