/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.MatchStateValueType;

public class MatchStateConditionType
    extends AbstractConditionType
    implements ConditionType
{
    public static final String ID = "MatchState";

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "is" );
        supportedOperators.add( "is not" );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Match State";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String generateDroolsCode( final Condition condition )
    {
        String operator;
        if ( "is".equals( condition.getOperator() ) )
        {
            operator = "==";
        }
        else
        {
            operator = "!=";
        }
        return "getMatchState().getId() " + operator + " \"" + condition.getValue() + "\"";
    }

    @Override
    public String getValueTypeId()
    {
        return MatchStateValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition )
        throws InvalidConditionException
    {
        super.validateCondition( condition );

        if ( MatchState.getById( condition.getValue() ) == null )
        {
            throw new InvalidConditionException( condition, "Value not supported: " + condition.getValue() );
        }
    }
}
