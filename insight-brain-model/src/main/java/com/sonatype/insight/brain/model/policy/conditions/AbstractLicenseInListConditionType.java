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
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseValueType;

public abstract class AbstractLicenseInListConditionType
    extends AbstractConditionType
    implements ConditionType
{
    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "is" );
        supportedOperators.add( "is not" );
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String getValueTypeId()
    {
        return LicenseValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition )
        throws InvalidConditionException
    {
        super.validateCondition( condition );
        
        String licenseId = condition.getValue();
        if ( LicenseValueType.getLicenseById( licenseId ) == null )
        {
            throw new InvalidConditionException( condition, "Invalid license id: " + licenseId );
        }
    }
}
