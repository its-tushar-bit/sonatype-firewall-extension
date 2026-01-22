/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.ConditionType;

public class ConditionTypes
{
  private static final Map<String, ConditionType> allConditionTypes = new LinkedHashMap<>();

  // The instances below support the Drools code produced by AbstractConditionType.generateDroolsCode()

  public static AgeInDaysConditionType AgeInDaysConditionType;

  public static CoordinatesConditionType CoordinatesConditionType;

  public static ComponentFormatConditionType ComponentFormatConditionType;

  public static PackageUrlConditionType PackageUrlConditionType;

  public static LabelConditionType LabelConditionType;

  public static LicenseConditionType LicenseConditionType;

  public static LicenseStatusConditionType LicenseStatusConditionType;

  public static LicenseThreatGroupConditionType LicenseThreatGroupConditionType;

  public static LicenseThreatGroupLevelConditionType LicenseThreatGroupLevelConditionType;

  public static RelativePopularityConditionType RelativePopularityConditionType;

  public static MatchStateConditionType MatchStateConditionType;

  @SuppressWarnings("deprecation")
  public static DeprecatedSecurityVulnerabilityConditionType DeprecatedSecurityVulnerabilityConditionType;

  public static SecurityVulnerabilitySeverityConditionType SecurityVulnerabilitySeverityConditionType;

  public static SecurityVulnerabilityStatusConditionType SecurityVulnerabilityStatusConditionType;

  public static SecurityVulnerabilitySourceConditionType SecurityVulnerabilitySourceConditionType;

  public static SecurityVulnerabilityResearchConditionType SecurityVulnerabilityResearchConditionType;

  public static ProprietaryConditionType ProprietaryConditionType;

  public static ProprietaryNameConflictConditionType ProprietaryNameConflictConditionType;

  public static IdentificationSourceConditionType IdentificationSourceConditionType;

  public static ComponentCategoryConditionType ComponentCategoryConditionType;

  public static HygieneRatingConditionType HygieneRatingConditionType;

  public static IntegrityRatingConditionType IntegrityRatingConditionType;

  public static DataSourceConditionType DataSourceConditionType;

  public static DependencyTypeConditionType DependencyTypeConditionType;

  public static SecurityVulnerabilityCategoryConditionType SecurityVulnerabilityCategoryConditionType;

  public static SecurityVulnerabilityCweConditionType SecurityVulnerabilityCweConditionType;

  public static SecurityVulnerabilityCustomRemediationConditionType SecurityVulnerabilityCustomRemediationConditionType;

  public static IacControlConditionType IacControlConditionType;

  public static VulnerabilityGroupConditionType VulnerabilityGroupConditionType;

  public static SecurityVulnerabilityCustomCVSSVectorStringConditionType
      SecurityVulnerabilityCustomCVSSVectorStringConditionType;

  public static ComponentEndOfLifeConditionType ComponentEndOfLifeConditionType;

  public static DerivativeAiModelConditionType DerivativeAiModelConditionType;

  public static AiModelContentConditionType AiModelContentConditionType;

  public static SecurityVulnerabilityDetectionConditionType SecurityVulnerabilityDetectionConditionType;

  public static KevStatusConditionType KevStatusConditionType;

  public static SecurityVulnerabilityEpssScoreConditionType SecurityVulnerabilityEpssScoreConditionType;

  @Inject
  public static void injectConditionTypes(
      final AgeInDaysConditionType ageInDaysConditionType,
      final CoordinatesConditionType coordinatesConditionType,
      final ComponentFormatConditionType componentFormatConditionType,
      final PackageUrlConditionType packageUrlConditionType,
      final LabelConditionType labelConditionType,
      final LicenseConditionType licenseConditionType,
      final LicenseStatusConditionType licenseStatusConditionType,
      final LicenseThreatGroupConditionType licenseThreatGroupConditionType,
      final LicenseThreatGroupLevelConditionType licenseThreatGroupLevelConditionType,
      final RelativePopularityConditionType relativePopularityConditionType,
      final MatchStateConditionType matchStateConditionType,
      final DeprecatedSecurityVulnerabilityConditionType deprecatedSecurityVulnerabilityConditionType,
      final SecurityVulnerabilitySeverityConditionType securityVulnerabilitySeverityConditionType,
      final SecurityVulnerabilityStatusConditionType securityVulnerabilityStatusConditionType,
      final SecurityVulnerabilitySourceConditionType securityVulnerabilitySourceConditionType,
      final SecurityVulnerabilityResearchConditionType securityVulnerabilityResearchConditionType,
      final ProprietaryConditionType proprietaryConditionType,
      final ProprietaryNameConflictConditionType proprietaryNameConflictConditionType,
      final IdentificationSourceConditionType identificationSourceConditionType,
      final ComponentCategoryConditionType componentCategoryConditionType,
      final HygieneRatingConditionType hygieneRatingConditionType,
      final IntegrityRatingConditionType integrityRatingConditionType,
      final DataSourceConditionType dataSourceConditionType,
      final DependencyTypeConditionType dependencyTypeConditionType,
      final SecurityVulnerabilityCategoryConditionType securityVulnerabilityCategoryConditionType,
      final SecurityVulnerabilityCweConditionType securityVulnerabilityCweConditionType,
      final SecurityVulnerabilityCustomRemediationConditionType securityVulnerabilityCustomRemediationConditionType,
      final IacControlConditionType iacControlConditionType,
      final VulnerabilityGroupConditionType vulnerabilityGroupConditionType,
      final SecurityVulnerabilityCustomCVSSVectorStringConditionType
          securityVulnerabilityCustomCVSSVectorStringConditionType,
      final ComponentEndOfLifeConditionType componentEndOfLifeConditionType,
      final DerivativeAiModelConditionType derivativeAiModelConditionType,
      final AiModelContentConditionType aiModelContentConditionType,
      final SecurityVulnerabilityDetectionConditionType securityVulnerabilityDetectionConditionType,
      final KevStatusConditionType kevStatusConditionType,
      final SecurityVulnerabilityEpssScoreConditionType securityVulnerabilityEpssScoreConditionType)
  {
    ConditionTypes.AgeInDaysConditionType = ageInDaysConditionType;
    ConditionTypes.CoordinatesConditionType = coordinatesConditionType;
    ConditionTypes.ComponentFormatConditionType = componentFormatConditionType;
    ConditionTypes.PackageUrlConditionType = packageUrlConditionType;
    ConditionTypes.LabelConditionType = labelConditionType;
    ConditionTypes.LicenseConditionType = licenseConditionType;
    ConditionTypes.LicenseStatusConditionType = licenseStatusConditionType;
    ConditionTypes.LicenseThreatGroupConditionType = licenseThreatGroupConditionType;
    ConditionTypes.LicenseThreatGroupLevelConditionType = licenseThreatGroupLevelConditionType;
    ConditionTypes.RelativePopularityConditionType = relativePopularityConditionType;
    ConditionTypes.MatchStateConditionType = matchStateConditionType;
    ConditionTypes.DeprecatedSecurityVulnerabilityConditionType = deprecatedSecurityVulnerabilityConditionType;
    ConditionTypes.SecurityVulnerabilitySeverityConditionType = securityVulnerabilitySeverityConditionType;
    ConditionTypes.SecurityVulnerabilityStatusConditionType = securityVulnerabilityStatusConditionType;
    ConditionTypes.SecurityVulnerabilitySourceConditionType = securityVulnerabilitySourceConditionType;
    ConditionTypes.SecurityVulnerabilityResearchConditionType = securityVulnerabilityResearchConditionType;
    ConditionTypes.ProprietaryConditionType = proprietaryConditionType;
    ConditionTypes.ProprietaryNameConflictConditionType = proprietaryNameConflictConditionType;
    ConditionTypes.IdentificationSourceConditionType = identificationSourceConditionType;
    ConditionTypes.ComponentCategoryConditionType = componentCategoryConditionType;
    ConditionTypes.HygieneRatingConditionType = hygieneRatingConditionType;
    ConditionTypes.IntegrityRatingConditionType = integrityRatingConditionType;
    ConditionTypes.DataSourceConditionType = dataSourceConditionType;
    ConditionTypes.DependencyTypeConditionType = dependencyTypeConditionType;
    ConditionTypes.SecurityVulnerabilityCategoryConditionType = securityVulnerabilityCategoryConditionType;
    ConditionTypes.SecurityVulnerabilityCweConditionType = securityVulnerabilityCweConditionType;
    ConditionTypes.SecurityVulnerabilityCustomRemediationConditionType =
        securityVulnerabilityCustomRemediationConditionType;
    ConditionTypes.IacControlConditionType = iacControlConditionType;
    ConditionTypes.VulnerabilityGroupConditionType = vulnerabilityGroupConditionType;
    ConditionTypes.SecurityVulnerabilityCustomCVSSVectorStringConditionType =
        securityVulnerabilityCustomCVSSVectorStringConditionType;
    ConditionTypes.ComponentEndOfLifeConditionType = componentEndOfLifeConditionType;
    ConditionTypes.DerivativeAiModelConditionType = derivativeAiModelConditionType;
    ConditionTypes.AiModelContentConditionType = aiModelContentConditionType;
    ConditionTypes.SecurityVulnerabilityDetectionConditionType = securityVulnerabilityDetectionConditionType;
    ConditionTypes.KevStatusConditionType = kevStatusConditionType;
    ConditionTypes.SecurityVulnerabilityEpssScoreConditionType = securityVulnerabilityEpssScoreConditionType;
    allConditionTypes.clear();

    // Don't add DeprecatedSecurityVulnerabilityConditionType
    add(AgeInDaysConditionType);
    add(ComponentCategoryConditionType);
    add(ComponentFormatConditionType);
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
    add(SecurityVulnerabilityCweConditionType);
    add(SecurityVulnerabilityCustomRemediationConditionType);
    add(SecurityVulnerabilityDetectionConditionType);
    add(SecurityVulnerabilitySourceConditionType);
    add(SecurityVulnerabilityCustomCVSSVectorStringConditionType);
    add(VulnerabilityGroupConditionType);
    add(SecurityVulnerabilityResearchConditionType);
    add(DataSourceConditionType);
    add(DependencyTypeConditionType);
    addDisabledConditionType(IacControlConditionType);
    add(ComponentEndOfLifeConditionType);
    add(DerivativeAiModelConditionType);
    add(AiModelContentConditionType);
    add(KevStatusConditionType);
    add(SecurityVulnerabilityEpssScoreConditionType);
  }

  public static Collection<ConditionType> getAll() {
    // Return condition types sorted alphabetically by their display name
    return allConditionTypes.values().stream()
        .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
        .collect(Collectors.toUnmodifiableList());
  }

  public static Collection<ConditionType> getAllWithAutoUnquarantineSupported() {
    return getAll().stream()
        .filter(ConditionType::isAutoUnquarantineSupported)
        .collect(Collectors.toUnmodifiableList());
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
