/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
  public static Collection<ConditionValueType<?>> getAll(String ownerId) {
    List<ConditionValueType<?>> allConditionValueTypes = new ArrayList<>();
    allConditionValueTypes.add(new AgeInDaysValueType());
    allConditionValueTypes.add(new ComponentCategoryValueType());
    allConditionValueTypes.add(new ComponentFormatValueType());
    allConditionValueTypes.add(new CoordinatesValueType());
    allConditionValueTypes.add(new PackageUrlValueType());
    allConditionValueTypes.add(new FloatValueType());
    allConditionValueTypes.add(new HygieneRatingValueType());
    allConditionValueTypes.add(new IntegrityRatingValueType());
    allConditionValueTypes.add(new IntegerValueType());
    allConditionValueTypes.add(new IdentificationSourceValueType());
    allConditionValueTypes.add(new LabelValueType(ownerId));
    allConditionValueTypes.add(new LicenseStatusValueType());
    allConditionValueTypes.add(new LicenseThreatGroupValueType(ownerId));
    allConditionValueTypes.add(new LicenseValueType());
    allConditionValueTypes.add(new MatchStateValueType());
    allConditionValueTypes.add(new PercentageValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityStatusValueType());
    allConditionValueTypes.add(new DataSourceValueType());
    allConditionValueTypes.add(new DependencyTypeValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityCategoryValueType());
    allConditionValueTypes.add(new SecurityVulnerabilitySourceValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityCweValueType());
    allConditionValueTypes.add(new IacControlValueType());
    return allConditionValueTypes;
  }
}
