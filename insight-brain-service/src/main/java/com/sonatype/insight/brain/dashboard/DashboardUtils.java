/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

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
      throw new InvalidLicenseException("Invalid license for the Dashboard feature");
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

  Set<String> getStageIds(final Collection<StageType> stageTypes) {
    Set<String> stageIdsToSearch = new HashSet<>();
    for (StageType stageType : stageTypes) {
      stageIdsToSearch.add(stageType.getId());
    }
    return stageIdsToSearch;
  }

  Predicate<PolicyViolation> buildViolationFilter(PolicyThreatCategoryFilter threatCategoryFilter,
      PolicyThreatLevelFilter threatLevelFilter)
  {
    if (threatCategoryFilter == null && threatLevelFilter == null) {
      return null;
    }
    else if (threatCategoryFilter != null && threatLevelFilter != null) {
      return Predicates.and(threatCategoryFilter.asPolicyViolationPredicate(),
          threatLevelFilter.asPolicyViolationPredicate());
    }

    return (threatCategoryFilter != null) ? threatCategoryFilter.asPolicyViolationPredicate() : threatLevelFilter
        .asPolicyViolationPredicate();
  }

  List<PolicyViolation> filter(List<PolicyViolation> violations, Predicate<PolicyViolation> violationFilter) {
    if (violationFilter == null || violations == null || violations.isEmpty()) {
      return violations;
    }

    return Lists.newArrayList(Collections2.filter(violations, violationFilter));
  }
}
