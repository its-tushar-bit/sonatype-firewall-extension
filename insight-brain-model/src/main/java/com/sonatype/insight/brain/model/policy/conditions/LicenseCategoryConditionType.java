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
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseCategoryValueType;

public class LicenseCategoryConditionType
    extends AbstractConditionType
    implements ConditionType
{
    public static final String ID = "LicenseCategory";

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
        return "License Category";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String generateDroolsCode( final Condition condition )
    {
        return "getLicenseCategory() " + ( "is".equals( condition.getOperator() ) ? "==" : "!=" ) + " \""
            + condition.getValue() + "\"";
    }

    @Override
    public String getValueTypeId()
    {
        return LicenseCategoryValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition )
        throws InvalidConditionException
    {
        super.validateCondition( condition );

        if ( LicenseCategoryValueType.getLicenseCategoryById( condition.getValue() ) == null )
        {
            throw new InvalidConditionException( condition, "Value not supported: " + condition.getValue() );
        }
    }
}
