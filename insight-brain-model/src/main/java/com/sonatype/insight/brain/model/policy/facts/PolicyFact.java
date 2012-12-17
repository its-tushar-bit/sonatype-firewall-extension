/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.Policy;

public class PolicyFact
{
    private String policyId;

    private String policyName;

    private List<ConstraintFact> constraintFacts;

    public PolicyFact()
    {
    }

    public PolicyFact( final Policy policy )
    {
        this.policyId = policy.getId();
        this.policyName = policy.getName();
    }

    public String getPolicyId()
    {
        return policyId;
    }

    public String getPolicyName()
    {
        return policyName;
    }

    public List<ConstraintFact> getConstraintFacts()
    {
        return constraintFacts;
    }

    public void addConstraintFact( final ConstraintFact constraintFact )
    {
        if ( constraintFacts == null )
        {
            constraintFacts = new ArrayList<ConstraintFact>();
        }
        constraintFacts.add( constraintFact );
    }

    @Override
    public String toString()
    {
        return "\nPolicy(" + policyName + ") " + constraintFacts;
    }
}
