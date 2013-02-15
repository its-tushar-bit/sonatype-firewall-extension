/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseStatusValueType;

public class LicenseStatusConditionType
    extends AbstractConditionType
{
    public static final String ID = "LicenseStatus";

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
        return "License Status";
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
        return "getLicenseStatus().getId() " + operator + " \"" + condition.getValue() + "\"";
    }

    @Override
    public String explainMatch( final Condition condition, final Component component )
    {
        return "License Status was '" + component.getLicenseStatus().getId() + "'";
    }

    @Override
    public String getValueTypeId()
    {
        return LicenseStatusValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, applicationId );

        if ( LicenseStatus.getById( condition.getValue() ) == null )
        {
            throw new InvalidConditionException( condition, "Value not supported: " + condition.getValue() );
        }
    }
}
