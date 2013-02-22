/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public class PolicyAlert
    implements Cloneable
{
    private PolicyFact trigger;

    private List<Action> actions;

    public PolicyAlert()
    {
    }

    public PolicyAlert( final PolicyFact trigger, final List<Action> actions )
    {
        this.trigger = trigger;
        this.actions = actions != null ? actions : Collections.<Action> emptyList();
    }

    public PolicyFact getTrigger()
    {
        return trigger;
    }

    public List<Action> getActions()
    {
        return actions;
    }

    public PolicyAlert with( final PolicyFact newTrigger )
    {
        try
        {
            // shallow copy (field-by-field)
            final PolicyAlert clone = (PolicyAlert) this.clone();
            clone.trigger = newTrigger;
            return clone;
        }
        catch ( final CloneNotSupportedException e )
        {
            throw new UnsupportedOperationException();
        }
    }
}
