/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

public class ConditionFact
{
    private String summary;

    private String reason;

    public ConditionFact()
    {
    }

    public ConditionFact( final Condition condition, final Component component )
    {
        final ConditionType conditionType = ConditionTypes.getById( condition.getConditionTypeId() );

        summary = conditionType.explainCondition( condition );
        reason = component != null ? conditionType.explainMatch( condition, component ) : "Unknown";
    }

    public String getSummary()
    {
        return summary;
    }

    public String getReason()
    {
        return reason;
    }

    @Override
    public String toString()
    {
        return summary + " because: " + reason;
    }
}
