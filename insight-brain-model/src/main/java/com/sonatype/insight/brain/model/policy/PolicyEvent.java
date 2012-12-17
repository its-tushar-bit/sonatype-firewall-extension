/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public class PolicyEvent
{
    private PolicyFact trigger;

    private List<Action> actions;

    public PolicyEvent()
    {
    }

    public PolicyEvent( final PolicyFact trigger, final List<Action> actions )
    {
        this.trigger = trigger;
        this.actions = actions;
    }

    public PolicyFact getTrigger()
    {
        return trigger;
    }

    public List<Action> getActions()
    {
        return actions;
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
