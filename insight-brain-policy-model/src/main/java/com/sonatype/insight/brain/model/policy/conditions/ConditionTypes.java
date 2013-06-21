/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.ConditionType;

public class ConditionTypes
{
    private static final Map<String, ConditionType<?>> allConditionTypes =
        new LinkedHashMap<String, ConditionType<?>>();

    // The instances below support the Drools code produced by AbstractConditionType.generateDroolsCode()

    public static final AgeInDaysConditionType AgeInDaysConditionType = new AgeInDaysConditionType();

    public static final CoordinatesConditionType CoordinatesConditionType = new CoordinatesConditionType();

    public static final LabelConditionType LabelConditionType = new LabelConditionType();

    public static final LicenseConditionType LicenseConditionType = new LicenseConditionType();

    public static final LicenseStatusConditionType LicenseStatusConditionType = new LicenseStatusConditionType();

    public static final LicenseThreatGroupConditionType LicenseThreatGroupConditionType =
        new LicenseThreatGroupConditionType();

    public static final LicenseThreatGroupLevelConditionType LicenseThreatGroupLevelConditionType =
        new LicenseThreatGroupLevelConditionType();

    public static final RelativePopularityConditionType RelativePopularityConditionType =
        new RelativePopularityConditionType();

    public static final MatchStateConditionType MatchStateConditionType = new MatchStateConditionType();

    public static final SecurityVulnerabilityConditionType SecurityVulnerabilityConditionType =
        new SecurityVulnerabilityConditionType();

    public static final SecurityVulnerabilitySeverityConditionType SecurityVulnerabilitySeverityConditionType =
        new SecurityVulnerabilitySeverityConditionType();

    public static final SecurityVulnerabilityStatusConditionType SecurityVulnerabilityStatusConditionType =
        new SecurityVulnerabilityStatusConditionType();

    public static final ProprietaryConditionType ProprietaryConditionType = new ProprietaryConditionType();

    public static final IdentificationSourceConditionType IdentificationSourceConditionType =
        new IdentificationSourceConditionType();

    static
    {
        // Note: The order condition types are added here determines the order they are displayed in the UI
        add( LabelConditionType );
        add( LicenseConditionType );
        add( LicenseStatusConditionType );
        add( LicenseThreatGroupConditionType );
        add( LicenseThreatGroupLevelConditionType );
        add( SecurityVulnerabilityConditionType );
        add( SecurityVulnerabilitySeverityConditionType );
        add( SecurityVulnerabilityStatusConditionType );
        add( RelativePopularityConditionType );
        add( AgeInDaysConditionType );
        add( MatchStateConditionType );
        add( CoordinatesConditionType );
        add( ProprietaryConditionType );
        add( IdentificationSourceConditionType );
    }

    public static Collection<ConditionType<?>> getAll()
    {
        return allConditionTypes.values();
    }

    public static ConditionType<?> getById( final String conditionTypeId )
    {
        // TODO throw exception if conditionTypeId is unknown
        return allConditionTypes.get( conditionTypeId );
    }

    private static void add( final ConditionType<?> conditionType )
    {
        if ( allConditionTypes.keySet().contains( conditionType.getId() ) )
        {
            throw new IllegalStateException( "Duplicate condition type id: " + conditionType.getId() );
        }
        allConditionTypes.put( conditionType.getId(), conditionType );
    }
}
