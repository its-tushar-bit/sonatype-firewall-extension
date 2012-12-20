/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseValueType;

public abstract class AbstractLicenseInListConditionType
    implements ConditionType
{
    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "in list" );
        supportedOperators.add( "not in list" );
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Deprecated
    @Override
    public List<String> getAvailableValues()
    {
        return null;
    }

    @Deprecated
    @Override
    public boolean isRequiresValue()
    {
        return true;
    }

    @Override
    public String getValueTypeId()
    {
        return LicenseValueType.ID;
    }
}
