/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class ConditionValueTypes
{
    public static Collection<ConditionValueType<?>> getAll( String applicationId )
    {
        List<ConditionValueType<?>> allConditionValueTypes = new ArrayList<ConditionValueType<?>>();
        allConditionValueTypes.add( new AgeInDaysValueType() );
        allConditionValueTypes.add( new CoordinatesValueType() );
        allConditionValueTypes.add( new FloatValueType() );
        allConditionValueTypes.add( new IntegerValueType() );
        allConditionValueTypes.add( new LabelValueType( applicationId ) );
        allConditionValueTypes.add( new LicenseStatusValueType() );
        allConditionValueTypes.add( new LicenseThreatGroupValueType( applicationId ) );
        allConditionValueTypes.add( new LicenseValueType() );
        allConditionValueTypes.add( new MatchStateValueType() );
        allConditionValueTypes.add( new PercentageValueType() );
        allConditionValueTypes.add( new SecurityVulnerabilityStatusValueType() );
        return allConditionValueTypes;
    }
}
