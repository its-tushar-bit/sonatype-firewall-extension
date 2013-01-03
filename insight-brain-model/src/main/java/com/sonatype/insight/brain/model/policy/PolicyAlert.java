/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.List;

import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public class PolicyAlert
{
    private PolicyFact trigger;

    private Action[] actions;

    public PolicyAlert()
    {
    }

    public PolicyAlert( final PolicyFact trigger, final List<Action> actions )
    {
        this.trigger = trigger;
        this.actions = actions != null ? actions.toArray( new Action[actions.size()] ) : new Action[0];
    }

    public PolicyFact getTrigger()
    {
        return trigger;
    }

    public Action[] getActions()
    {
        return actions;
    }
}
