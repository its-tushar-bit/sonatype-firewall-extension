/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.ConditionType;

public class ConditionTypes
{
    private static final Map<String, ConditionType> allConditionTypes = new LinkedHashMap<String, ConditionType>();

    static
    {
        add( new LicenseCategoryConditionType() );
        add( new LicenseInListConditionType() );
        add( new DeclaredLicenseInListConditionType() );
        add( new ObservedLicenseInListConditionType() );
        add( new SecurityVulnerabilityConditionType() );
        add( new SecurityVulnerabilitySeverityConditionType() );
        add( new RelativePopularityConditionType() );
    }

    public static Collection<ConditionType> getAll()
    {
        return allConditionTypes.values();
    }

    public static ConditionType getById( final String conditionTypeId )
    {
        // TODO throw exception if conditionTypeId is unknown
        return allConditionTypes.get( conditionTypeId );
    }

    private static void add( final ConditionType conditionType )
    {
        if ( allConditionTypes.keySet().contains( conditionType.getId() ) )
        {
            throw new IllegalStateException( "Duplicate condition type id: " + conditionType.getId() );
        }
        allConditionTypes.put( conditionType.getId(), conditionType );
    }
}
