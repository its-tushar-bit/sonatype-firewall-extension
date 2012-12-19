/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Policy
{
    private String id;

    private String name;

    private boolean enabled = true;

    private int threatLevel = 5;

    private List<Constraint> constraints;

    private Map<String, List<Action>> actions;

    public Policy()
    {
    }

    public Policy( final String id, final String name )
    {
        this.id = id;
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

    public int getThreatLevel()
    {
        return threatLevel;
    }

    public void setThreatLevel( final int threatLevel )
    {
        this.threatLevel = threatLevel;
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

    public Map<String, List<Action>> getActions()
    {
        return actions;
    }

    public void setActions( final Map<String, List<Action>> actions )
    {
        this.actions = actions;
    }

    public List<Action> getActions( final String stageTypeId )
    {
        return actions != null ? actions.get( stageTypeId ) : null;
    }

    public void setActions( final String stageTypeId, final List<Action> stageActions )
    {
        if ( actions == null )
        {
            actions = new HashMap<String, List<Action>>();
        }
        actions.put( stageTypeId, stageActions );
    }

    public void addAction( final String stageTypeId, final Action action )
    {
        List<Action> stageActions = getActions( stageTypeId );
        if ( stageActions == null )
        {
            setActions( stageTypeId, stageActions = new ArrayList<Action>() );
        }
        stageActions.add( action );
    }

    @Override
    public String toString()
    {
        return "Policy [id=" + id + ", name=" + name + "]";
    }
}
