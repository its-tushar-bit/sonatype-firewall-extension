/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
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

    private int threatLevel;

    private List<ConstraintFact> constraintFacts;

    public PolicyFact()
    {
    }

    public PolicyFact( final Policy policy )
    {
        this.policyId = policy.getId();
        this.policyName = policy.getName();
        this.threatLevel = policy.getThreatLevel();
    }

    public String getPolicyId()
    {
        return policyId;
    }

    public String getPolicyName()
    {
        return policyName;
    }

    public int getThreatLevel()
    {
        return threatLevel;
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
