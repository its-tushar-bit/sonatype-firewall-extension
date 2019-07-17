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
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
public class DashboardUtils
{
  private final ProductLicense productLicense;

  private final StageTypeService stageTypeService;

  @Inject
  public DashboardUtils(ProductLicense productLicense, StageTypeService stageTypeService) {
    this.productLicense = productLicense;;
    this.stageTypeService = stageTypeService;
  }

  void validateDashboardLicensed() {
    productLicense.validateFeature(LicensedFeature.DASHBOARD);
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

  public Set<String> getStageTypeIds(final Collection<StageType> stageTypes) {
    Set<String> stageTypeIds = new HashSet<>();
    for (StageType stageType : stageTypes) {
      stageTypeIds.add(stageType.getId());
    }
    return stageTypeIds;
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
    return predicates.stream().reduce(Predicate::and).get();
  }
}
