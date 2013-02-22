/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sonatype.insight.brain.model.policy.Policy;

public class PolicyFact
    implements Cloneable
{
    private String policyId;

    private String policyName;

    private int threatLevel;

    @JsonInclude( Include.NON_EMPTY )
    private List<ComponentFact> componentFacts;

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

    public List<ComponentFact> getComponentFacts()
    {
        return componentFacts;
    }

    public void addComponentFact( final ComponentFact componentFact )
    {
        if ( componentFacts == null )
        {
            componentFacts = new ArrayList<ComponentFact>();
        }
        componentFacts.add( componentFact );
    }

    public PolicyFact with( final List<ComponentFact> newComponentFacts )
    {
        try
        {
            // shallow copy (field-by-field)
            final PolicyFact clone = (PolicyFact) this.clone();
            clone.componentFacts = newComponentFacts;
            return clone;
        }
        catch ( final CloneNotSupportedException e )
        {
            throw new UnsupportedOperationException();
        }
    }

    public PolicyFact with( final ComponentFact... newComponentFacts )
    {
        return with( Arrays.asList( newComponentFacts ) );
    }

    @Override
    public String toString()
    {
        return "\nPolicy(" + policyName + ") " + componentFacts;
    }
}
