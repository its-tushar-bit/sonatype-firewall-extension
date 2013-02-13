/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.sonatype.insight.brain.model.policy.Condition;

public class ConditionFact
{
    private String summary;

    public ConditionFact()
    {
    }

    public ConditionFact( final Condition condition )
    {
        summary = condition.toMessageString();
    }

    public String getSummary()
    {
        return summary;
    }

    @Override
    public String toString()
    {
        return summary;
    }
}
