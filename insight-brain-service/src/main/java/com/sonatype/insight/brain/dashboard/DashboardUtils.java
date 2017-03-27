/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

@Named
public class DashboardUtils
{
  private final CLMLicenseManager licenseManager;

  private final StageTypeService stageTypeService;

  static final Function<HasStringId, String> hasIdIdSelector = new Function<HasStringId, String>()
  {
    @Override
    public String apply(final HasStringId hasStringId) {
      return hasStringId.getId();
    }
  };

  @Inject
  public DashboardUtils(CLMLicenseManager licenseManager, StageTypeService stageTypeService) {
    this.licenseManager = licenseManager;
    this.stageTypeService = stageTypeService;
  }

  void validateDashboardLicensed() {
    if (!licenseManager.hasDashboard()) {
      throw new InvalidLicenseException();
    }
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
          && (stageTypeIds.isEmpty() || stageTypeIds.contains(stageType.getId()))) {
        stages.add(stageType);
      }
    }

    return stages;
  }

  Set<String> getStageTypeIds(final Collection<StageType> stageTypes) {
    Set<String> stageTypeIds = new HashSet<>();
    for (StageType stageType : stageTypes) {
      stageTypeIds.add(stageType.getId());
    }
    return stageTypeIds;
  }

  Set<String> getApplicationIds(Collection<Application> applications) {
    Set<String> appIds = new HashSet<>();
    for (Application application : applications) {
      appIds.add(application.getId());
    }
    return appIds;
  }

  Predicate<PolicyViolation> buildViolationFilter(PolicyThreatCategoryFilter threatCategoryFilter,
                                                  PolicyThreatLevelFilter threatLevelFilter,
                                                  PolicyViolationStateFilter violationStatusFilter)
  {
    if (threatCategoryFilter == null && threatLevelFilter == null && violationStatusFilter == null) {
      return null;
    }
    List<Predicate<PolicyViolation>> predicates = new ArrayList<>();
    if (threatCategoryFilter != null) {
      predicates.add(threatCategoryFilter.asPolicyViolationPredicate());
    }
    if (threatLevelFilter != null) {
      predicates.add(threatLevelFilter.asPolicyViolationPredicate());
    }
    if (violationStatusFilter != null) {
      predicates.add(violationStatusFilter.asPolicyViolationPredicate());
    }
    return Predicates.and(predicates);
  }

  List<PolicyViolation> filter(List<PolicyViolation> violations, Predicate<PolicyViolation> violationFilter) {
    if (violationFilter == null || violations == null || violations.isEmpty()) {
      return violations;
    }

    return Lists.newArrayList(Collections2.filter(violations, violationFilter));
  }
}
