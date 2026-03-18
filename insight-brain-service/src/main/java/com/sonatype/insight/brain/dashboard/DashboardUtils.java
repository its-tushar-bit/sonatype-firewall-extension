/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTO_WAIVERS;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;

@Named
public class DashboardUtils
{
  private final ProductLicense productLicense;

  private final StageTypeService stageTypeService;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final OwnerDAO ownerDAO;

  private final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  @Inject
  public DashboardUtils(
      ProductLicense productLicense,
      StageTypeService stageTypeService,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      OwnerDAO ownerDAO,
      AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO)
  {
    this.productLicense = productLicense;
    this.stageTypeService = stageTypeService;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.ownerDAO = ownerDAO;
    this.autoPolicyWaiverExclusionDAO = autoPolicyWaiverExclusionDAO;
  }

  void validateDashboardLicensedAndEnabledForApplications() {
    productLicense.validateFeature(LicensedFeature.DASHBOARD);

    if (systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED) != null) {
      throw new ConflictException("The dashboard feature has been disabled.");
    }
  }

  void validateDashboardLicensedAndEnabled() {
    productLicense.validateFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);

    if (systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED) != null) {
      throw new ConflictException("The dashboard feature has been disabled.");
    }
  }

  boolean isAutoWaiverFeatureFlagEnabled() {
    SystemConfigurationProperty autoWaiver = systemConfigurationPropertyDAO.getByName(AUTO_WAIVERS);
    return autoWaiver == null || (autoWaiver.getValue().equalsIgnoreCase("true"));
  }

  Set<StageType> getStageTypes(Set<String> stageTypeIds) {
    Collection<StageType> licensedStageTypes = stageTypeService.getLicensedStageTypes();

    if (stageTypeIds == null) {
      stageTypeIds = Collections.emptySet();
    }
    else {
      for (String stageTypeId : stageTypeIds) {
        StageType stage = StageTypes.getById(stageTypeId);
        if (stage == null || StageTypes.isIgnoredForDashboard(stage.getId())) {
          throw new BadRequestException("Invalid stage type: " + stageTypeId + ".");
        }
        else if (!licensedStageTypes.contains(stage)) {
          throw new BadRequestException("Current license does not support stage type: " + stageTypeId + ".");
        }
      }
    }

    Set<StageType> stages = new LinkedHashSet<>();

    for (StageType stageType : licensedStageTypes) {
      if (!StageTypes.isIgnoredForDashboard(stageType.getId())
          && (stageTypeIds.isEmpty() || stageTypeIds.contains(stageType.getId())))
      {
        stages.add(stageType);
      }
    }

    return stages;
  }

  public boolean isDashboardDisabled() {
    return systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED) != null;
  }

  public Set<String> getStageTypeIds(final Collection<StageType> stageTypes) {
    Set<String> stageTypeIds = new HashSet<>();
    for (StageType stageType : stageTypes) {
      stageTypeIds.add(stageType.getId());
    }
    return stageTypeIds;
  }

  public boolean hasExistingAutoWaiverExclusion(
      String applicationId,
      String autoPolicyWaiverId,
      String policyViolationId)
  {
    final List<String> ownerIds = ownerDAO.getOwnerIds(applicationId);
    return ownerIds.stream()
        .map(ownerId -> autoPolicyWaiverExclusionDAO.getByOwnerIdPolicyViolation(ownerId, autoPolicyWaiverId,
            policyViolationId))
        .anyMatch(Objects::nonNull);
  }

  // Auto waiver exclusions should be accounted for when the filter is set to only display waived violations
  public static boolean shouldOnlyShowWaivedViolations(final PolicyViolationStateFilter policyViolationStateFilter) {
    return policyViolationStateFilter != null && policyViolationStateFilter.getPolicyViolationStates().size() == 1
        && policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.WAIVED);
  }
}
