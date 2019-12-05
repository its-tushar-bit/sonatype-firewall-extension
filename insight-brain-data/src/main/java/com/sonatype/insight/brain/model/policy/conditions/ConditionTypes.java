/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  public static final ProprietaryConditionType ProprietaryConditionType = new ProprietaryConditionType();

  public static final IdentificationSourceConditionType IdentificationSourceConditionType =
      new IdentificationSourceConditionType();

  static {
    // Don't add DeprecatedSecurityVulnerabilityConditionType
    add(AgeInDaysConditionType);
    add(CoordinatesConditionType);
    add(PackageUrlConditionType);
    add(IdentificationSourceConditionType);
    add(LabelConditionType);
    add(LicenseConditionType);
    add(LicenseStatusConditionType);
    add(LicenseThreatGroupConditionType);
    add(LicenseThreatGroupLevelConditionType);
    add(MatchStateConditionType);
    add(ProprietaryConditionType);
    add(RelativePopularityConditionType);
    add(SecurityVulnerabilitySeverityConditionType);
    add(SecurityVulnerabilityStatusConditionType);
  }

  public static Collection<ConditionType> getAll() {
    return allConditionTypes.values();
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
}
