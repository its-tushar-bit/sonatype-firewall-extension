/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.license.model.LicensedFeature;

public class ConditionValueTypes
{
  private static ComponentCategoryDAO componentCategoryDAO;

  private static LicenseDAO licenseDAO;

  private static OwnerDAO ownerDAO;

  private static LicenseThreatGroupDAO licenseThreatGroupDAO;

  private static LabelDAO labelDAO;

  private static VulnerabilityGroupDAO vulnerabilityGroupDAO;

  /**
   * There are some condition value types need data from the database, so this method is intended to properly initialize
   * the DAOs used to get that information
   */
  @Inject
  public static void injectConditionValueTypes(
      final ComponentCategoryDAO componentCategoryDAO,
      final LicenseDAO licenseDAO,
      final OwnerDAO ownerDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LabelDAO labelDAO,
      final VulnerabilityGroupDAO vulnerabilityGroupDAO)
  {
    ConditionValueTypes.componentCategoryDAO = componentCategoryDAO;
    ConditionValueTypes.licenseDAO = licenseDAO;
    ConditionValueTypes.ownerDAO = ownerDAO;
    ConditionValueTypes.licenseThreatGroupDAO = licenseThreatGroupDAO;
    ConditionValueTypes.labelDAO = labelDAO;
    ConditionValueTypes.vulnerabilityGroupDAO = vulnerabilityGroupDAO;
  }

  public static Collection<ConditionValueType<?>> getAll(String ownerId, final Set<LicensedFeature> features) {
    List<ConditionValueType<?>> allConditionValueTypes = new ArrayList<>();
    allConditionValueTypes.add(new AgeInDaysValueType());
    allConditionValueTypes.add(new ComponentCategoryValueType(componentCategoryDAO));
    allConditionValueTypes.add(new ComponentFormatValueType());
    allConditionValueTypes.add(new CoordinatesValueType());
    allConditionValueTypes.add(new PackageUrlValueType());
    allConditionValueTypes.add(new FloatValueType());
    allConditionValueTypes.add(new HygieneRatingValueType());
    allConditionValueTypes.add(new IntegrityRatingValueType());
    allConditionValueTypes.add(new IntegerValueType());
    allConditionValueTypes.add(new IdentificationSourceValueType());
    allConditionValueTypes.add(new LabelValueType(ownerId, labelDAO));
    allConditionValueTypes.add(new LicenseStatusValueType());
    allConditionValueTypes.add(new LicenseThreatGroupValueType(ownerId, ownerDAO, licenseThreatGroupDAO));
    allConditionValueTypes.add(new LicenseValueType(licenseDAO));
    allConditionValueTypes.add(new MatchStateValueType());
    allConditionValueTypes.add(new PercentageValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityStatusValueType());
    allConditionValueTypes.add(new DataSourceValueType());
    allConditionValueTypes.add(new DependencyTypeValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityCategoryValueType());
    allConditionValueTypes.add(new SecurityVulnerabilitySourceValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityCweValueType());
    allConditionValueTypes.add(new SecurityVulnerabilityDetectionValueType(features));
    allConditionValueTypes.add(new SecurityVulnerabilityResearchValueType(features));
    allConditionValueTypes.add(new IacControlValueType());
    allConditionValueTypes.add(new VulnerabilityGroupValueType(ownerId, ownerDAO, vulnerabilityGroupDAO));
    allConditionValueTypes.add(new SecurityVulnerabilityCustomDetailsCVSSVectorStringValueType());
    allConditionValueTypes.add(new AiModelContentValueType());
    allConditionValueTypes.add(new KevStatusValueType());
    allConditionValueTypes.add(new DoubleValueType());
    return allConditionValueTypes;
  }
}
