/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.policy.ConditionType;

public class ConditionTypes
{
  private static final Map<String, ConditionType> allConditionTypes = new LinkedHashMap<>();

  // The instances below support the Drools code produced by AbstractConditionType.generateDroolsCode()

  public static final AgeInDaysConditionType AgeInDaysConditionType = new AgeInDaysConditionType();

  public static final CoordinatesConditionType CoordinatesConditionType = new CoordinatesConditionType();

  public static final PackageUrlConditionType PackageUrlConditionType = new PackageUrlConditionType();

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

  @SuppressWarnings("deprecation")
  public static final DeprecatedSecurityVulnerabilityConditionType DeprecatedSecurityVulnerabilityConditionType =
      new DeprecatedSecurityVulnerabilityConditionType();

  public static final SecurityVulnerabilitySeverityConditionType SecurityVulnerabilitySeverityConditionType =
      new SecurityVulnerabilitySeverityConditionType();

  public static final SecurityVulnerabilityStatusConditionType SecurityVulnerabilityStatusConditionType =
      new SecurityVulnerabilityStatusConditionType();

  public static final SecurityVulnerabilitySourceConditionType SecurityVulnerabilitySourceConditionType =
      new SecurityVulnerabilitySourceConditionType();

  public static final ProprietaryConditionType ProprietaryConditionType = new ProprietaryConditionType();

  public static final ProprietaryNameConflictConditionType ProprietaryNameConflictConditionType =
      new ProprietaryNameConflictConditionType();

  public static final IdentificationSourceConditionType IdentificationSourceConditionType =
      new IdentificationSourceConditionType();

  public static final ComponentCategoryConditionType ComponentCategoryConditionType =
      new ComponentCategoryConditionType();

  public static final HygieneRatingConditionType HygieneRatingConditionType = new HygieneRatingConditionType();

  public static final IntegrityRatingConditionType IntegrityRatingConditionType = new IntegrityRatingConditionType();

  public static final DataSourceConditionType
      DataSourceConditionType = new DataSourceConditionType();

  public static final DependencyTypeConditionType DependencyTypeConditionType = new DependencyTypeConditionType();

  public static final SecurityVulnerabilityCategoryConditionType SecurityVulnerabilityCategoryConditionType =
      new SecurityVulnerabilityCategoryConditionType();

  static {
    // Don't add DeprecatedSecurityVulnerabilityConditionType
    add(AgeInDaysConditionType);
    add(ComponentCategoryConditionType);
    add(CoordinatesConditionType);
    add(PackageUrlConditionType);
    addDisabledConditionType(HygieneRatingConditionType);
    addDisabledConditionType(IntegrityRatingConditionType);
    add(IdentificationSourceConditionType);
    add(LabelConditionType);
    add(LicenseConditionType);
    add(LicenseStatusConditionType);
    add(LicenseThreatGroupConditionType);
    add(LicenseThreatGroupLevelConditionType);
    add(MatchStateConditionType);
    add(ProprietaryConditionType);
    add(ProprietaryNameConflictConditionType);
    add(RelativePopularityConditionType);
    add(SecurityVulnerabilitySeverityConditionType);
    add(SecurityVulnerabilityStatusConditionType);
    add(SecurityVulnerabilityCategoryConditionType);
    add(SecurityVulnerabilitySourceConditionType);
    add(DataSourceConditionType);
    add(DependencyTypeConditionType);
  }

  public static Collection<ConditionType> getAll() {
    return Collections.unmodifiableCollection(allConditionTypes.values());
  }

  public static Collection<ConditionType> getAllWithAutoUnquarantineSupported() {
    return Collections.unmodifiableCollection(
        getAll().stream().filter(ConditionType::isAutoUnquarantineSupported).collect(Collectors.toList()));
  }

  public static ConditionType getById(final String conditionTypeId) {
    ConditionType conditionType = allConditionTypes.get(conditionTypeId);
    if (conditionType == null) {
      if (DeprecatedSecurityVulnerabilityConditionType.ID.equals(conditionTypeId)) {
        return DeprecatedSecurityVulnerabilityConditionType;
      }
      throw new IllegalArgumentException("Invalid condition type id: '" + conditionTypeId + "'");
    }

    return conditionType;
  }

  private static void add(final ConditionType conditionType) {
    if (allConditionTypes.keySet().contains(conditionType.getId())) {
      throw new IllegalStateException("Duplicate condition type id: " + conditionType.getId());
    }
    allConditionTypes.put(conditionType.getId(), conditionType);
  }

  private static void addDisabledConditionType(final ConditionType conditionType) {
    conditionType.setEnabled(false);
    add(conditionType);
  }

  public static synchronized void enableConditionType(final ConditionType conditionType) {
    if (!allConditionTypes.keySet().contains(conditionType.getId())) {
      throw new IllegalStateException("Condition type not found with type id: " + conditionType.getId());
    }
    allConditionTypes.get(conditionType.getId()).setEnabled(true);
  }

  public static synchronized void disableConditionType(final ConditionType conditionType) {
    if (allConditionTypes.containsKey(conditionType.getId())) {
      allConditionTypes.get(conditionType.getId()).setEnabled(false);
    }
  }
}
