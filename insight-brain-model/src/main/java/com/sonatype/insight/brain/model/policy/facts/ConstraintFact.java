/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.Constraint;

public class ConstraintFact
{
    private String constraintId;

    private String constraintName;

    private List<ComponentFact> componentFacts;

    public ConstraintFact()
    {
    }

    public ConstraintFact( final Constraint constraint )
    {
        this.constraintId = constraint.getId();
        this.constraintName = constraint.getName();
    }

    public String getConstraintId()
    {
        return constraintId;
    }

    public String getConstraintName()
    {
        return constraintName;
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

    @Override
    public String toString()
    {
        return "\n Constraint(" + constraintName + ") " + componentFacts + " ";
    }
}
