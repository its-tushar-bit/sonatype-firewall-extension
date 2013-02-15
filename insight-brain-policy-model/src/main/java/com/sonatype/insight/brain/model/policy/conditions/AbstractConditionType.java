/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;

public abstract class AbstractConditionType
    implements ConditionType
{
    @Override
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        if ( condition.getOperator() == null )
        {
            throw new InvalidConditionException( condition, "Operator is null" );
        }
        if ( !getSupportedOperators().contains( condition.getOperator() ) )
        {
            throw new InvalidConditionException( condition, "Operator is not supported" );
        }
        if ( getValueTypeId() != null && condition.getValue() == null )
        {
            throw new InvalidConditionException( condition, "Value is null" );
        }
    }

    @Override
    public String getValueHint()
    {
        return null;
    }

    @Override
    public String explainMatch( Condition condition, Component component )
    {
        return "Placeholder";
    }
}
