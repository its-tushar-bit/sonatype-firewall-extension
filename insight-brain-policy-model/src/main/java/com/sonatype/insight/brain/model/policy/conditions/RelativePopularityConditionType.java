/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.List;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.FloatValueType;

public class RelativePopularityConditionType
    extends AbstractConditionType
    implements ConditionType
{
    public static final String ID = "RelativePopularity";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Relative Popularity (Percentage)";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return NumericOperators.LIST;
    }

    @Override
    public String generateDroolsCode( final Condition condition )
    {
        return "getRelativePopularity() " + NumericOperators.getDroolsOperator( condition.getOperator() ) + " "
            + condition.getValue();
    }

    @Override
    public String getValueTypeId()
    {
        return FloatValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, applicationId );

        try
        {
            int value = Integer.parseInt( condition.getValue() );
            if ( value < 0 || value > 100 )
            {
                throw new InvalidConditionException( condition, "Relative popularity must be between 0 and 100" );
            }
        }
        catch ( NumberFormatException e )
        {
            throw new InvalidConditionException( condition, "Invalid relative popularity: "
                + condition.getValue() );
        }
    }

    @Override
    public String getValueHint()
    {
        return "Enter percent value, 1 to 100";
    }
}