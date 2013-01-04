/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.AgeInDaysValueType;

public class AgeInDaysConditionType
    extends AbstractConditionType
    implements ConditionType
{
    public static final String ID = "AgeInDays";

    public static final long DAY_IN_MILLISECONDS = 24L * 3600L * 1000L;

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "older than" );
        supportedOperators.add( "younger than" );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Age";
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
        if ( "older than".equals( condition.getOperator() ) )
        {
            operator = ">";
        }
        else
        {
            operator = "<";
        }
        return "getCatalogDate() != null && ( System.currentTimeMillis() - getCatalogDate() ) / " + DAY_IN_MILLISECONDS
            + " " + operator + " " + condition.getValue();
    }

    @Override
    public String getValueTypeId()
    {
        return AgeInDaysValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition )
        throws InvalidConditionException
    {
        super.validateCondition( condition );

        try
        {
            Integer.parseInt( condition.getValue() );
        }
        catch ( NumberFormatException e )
        {
            throw new InvalidConditionException( condition, "Invalid age (in days): "
                + condition.getValue() );
        }
    }

    @Override
    public String getValueHint()
    {
        return "Enter number of days";
    }
}
