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

    private List<ConditionFact> conditionFacts;

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

    public List<ConditionFact> getConditionFacts()
    {
        return conditionFacts;
    }

    public void addConditionFact( final ConditionFact conditionFact )
    {
        if ( conditionFacts == null )
        {
            conditionFacts = new ArrayList<ConditionFact>();
        }
        conditionFacts.add( conditionFact );
    }

    @Override
    public String toString()
    {
        return "\n  Constraint(" + constraintName + ") " + conditionFacts + " ";
    }
}
