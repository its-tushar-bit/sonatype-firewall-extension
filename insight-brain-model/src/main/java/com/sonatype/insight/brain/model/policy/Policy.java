/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.List;

public class Policy
{
    private String id;

    private String name;

    private List<Constraint> constraints;

    private List<Action> actions;

    private boolean enabled = true;

    public Policy()
    {
    }

    public Policy( final String id, final String name )
    {
        this.id = id;
        this.name = name;
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

    public List<Constraint> getConstraints()
    {
        return constraints;
    }

    public void setConstraints( final List<Constraint> constraints )
    {
        this.constraints = constraints;
    }

    public void addConstraint( final Constraint constraint )
    {
        if ( constraints == null )
        {
            constraints = new ArrayList<Constraint>();
        }
        constraints.add( constraint );
    }

    public List<Action> getActions()
    {
        return actions;
    }

    public void setActions( final List<Action> actions )
    {
        this.actions = actions;
    }

    public void addAction( final Action action )
    {
        if ( actions == null )
        {
            actions = new ArrayList<Action>();
        }
        actions.add( action );
    }
}
