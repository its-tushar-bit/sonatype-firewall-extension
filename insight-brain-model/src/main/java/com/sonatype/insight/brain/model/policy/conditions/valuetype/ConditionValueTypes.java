/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class ConditionValueTypes
{
    private static final Map<String, ConditionValueType<?>> allConditionValueTypes =
        new LinkedHashMap<String, ConditionValueType<?>>();

    static
    {
        add( new AgeInDaysValueType() );
        add( new CoordinatesValueType() );
        add( new FloatValueType() );
        add( new LicenseStatusValueType() );
        add( new LicenseValueType() );
        add( new MatchStateValueType() );
        add( new PercentageValueType() );
        add( new SecurityVulnerabilityStatusValueType() );
    }

    public static Collection<ConditionValueType<?>> getAll()
    {
        return allConditionValueTypes.values();
    }

    public static ConditionValueType<?> getById( final String conditionValueTypeId )
    {
        // TODO throw exception if conditionValueTypeId is unknown
        return allConditionValueTypes.get( conditionValueTypeId );
    }

    private static void add( final ConditionValueType<?> conditionValueType )
    {
        if ( allConditionValueTypes.keySet().contains( conditionValueType.getId() ) )
        {
            throw new IllegalStateException( "Duplicate condition value type id: " + conditionValueType.getId() );
        }
        allConditionValueTypes.put( conditionValueType.getId(), conditionValueType );
    }
}
