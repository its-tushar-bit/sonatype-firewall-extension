/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.component.LicenseCategory;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseCategoryValueType;

public class LicenseCategoryConditionType
    implements ConditionType
{
    public static final String ID = "LicenseCategory";

    private static List<String> supportedOperators = new ArrayList<String>();

    private static List<String> licenseCategoryNames = new ArrayList<String>();

    private static Map<String, String> licenseCategoryIdsByName = new LinkedHashMap<String, String>();

    static
    {
        supportedOperators.add( "is" );
        supportedOperators.add( "is not" );

        for ( LicenseCategory licenseCategory : new LicenseCategoryValueType().getAvailableValues() )
        {
            licenseCategoryNames.add( licenseCategory.getName() );
            licenseCategoryIdsByName.put( licenseCategory.getName(), licenseCategory.getId() );
        }
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

    @Deprecated
    @Override
    public List<String> getAvailableValues()
    {
        return licenseCategoryNames;
    }

    @Override
    public String generateDroolsCode( final Condition condition )
    {
        return "getLicenseThreat() " + ( "is".equals( condition.getOperator() ) ? "==" : "!=" ) + " \""
            + licenseCategoryIdsByName.get( condition.getValue() ) + "\"";
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
        return LicenseCategoryValueType.ID;
    }
}
