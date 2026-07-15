/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates Applications list request filters at the API boundary.
 * <p>
 * Unsupported filters return {@link BadRequestException} rather than being silently ignored.
 * Stage and threat-level filters are validated for size and domain bounds.
 */
@Named
@Singleton
final class ApplicationsListRequestValidator
{
  /** Martha V1 fixed default — latest evaluation descending. */
  static final String DEFAULT_ORDER_BY = "-lastEvaluationTime";

  private static final String ORDER_BY_LAST_EVALUATION_ASC = "lastEvaluationTime";

  private static final String ORDER_BY_LAST_EVALUATION_DESC = "-" + ORDER_BY_LAST_EVALUATION_ASC;

  /** Matches the selectable Martha threat buckets (Critical/Severe/Moderate/Low/None). */
  static final int MAX_POLICY_THREAT_LEVEL_RANGES = 5;

  private static final int MIN_THREAT_LEVEL = 0;

  private static final int MAX_THREAT_LEVEL = 10;

  private static final int DEFAULT_MAX_STAGE_IDS = 2048;

  private final Configuration configuration;

  @Inject
  ApplicationsListRequestValidator(final Configuration configuration) {
    this.configuration = configuration;
  }

  void validate(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return;
    }
    rejectUnsupportedFilters(request);
    validateStageIds(request);
    validateThreatLevelFilters(request);
  }

  private void validateStageIds(final ApplicationsListRequestDTO request) {
    if (request.stageIds == null || request.stageIds.isEmpty()) {
      return;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(request.stageIds, "stageIds");
    int maxStageIds = configuration.getMaxAdvancedSearchClauseCount();
    if (maxStageIds <= 0) {
      maxStageIds = DEFAULT_MAX_STAGE_IDS;
    }
    if (request.stageIds.size() > maxStageIds) {
      throw new BadRequestException("stageIds contains too many ids (max " + maxStageIds + ").");
    }
  }

  private static void rejectUnsupportedFilters(final ApplicationsListRequestDTO request) {
    if (hasNonEmptyFilterSet(request.tagIds)) {
      throw new BadRequestException("tagIds filter is not yet supported on the applications list.");
    }
    if (hasPolicyThreatCategoryFilter(request.policyThreatCategories)) {
      throw new BadRequestException("policyThreatCategories filter is not yet supported on the applications list.");
    }
    if (hasPolicyViolationStateFilter(request.policyViolationStates)) {
      throw new BadRequestException("policyViolationStates filter is not yet supported on the applications list.");
    }
    if (StringUtils.isNotBlank(request.orderBy)) {
      validateOrderBy(request.orderBy);
    }
  }

  private static void validateThreatLevelFilters(final ApplicationsListRequestDTO request) {
    if (request.policyThreatLevelRanges != null) {
      if (request.policyThreatLevelRanges.size() > MAX_POLICY_THREAT_LEVEL_RANGES) {
        throw new BadRequestException(
            "policyThreatLevelRanges contains too many entries (max " + MAX_POLICY_THREAT_LEVEL_RANGES + ").");
      }
      for (PolicyThreatLevelFilter range : request.policyThreatLevelRanges) {
        if (range == null) {
          throw new BadRequestException("policyThreatLevelRanges must not contain null elements.");
        }
        validateThreatLevelFilterBounds(range);
      }
    }
    // Legacy singular field is accepted (not 400) so Classic/API callers still get violation scope.
    validateThreatLevelFilterBounds(request.policyThreatLevelRange);
  }

  private static void validateThreatLevelFilterBounds(final PolicyThreatLevelFilter filter) {
    if (filter == null) {
      return;
    }
    int min = filter.getMinPolicyThreatLevel();
    int max = filter.getMaxPolicyThreatLevel();
    if (min != Integer.MIN_VALUE && (min < MIN_THREAT_LEVEL || min > MAX_THREAT_LEVEL)) {
      throw new BadRequestException(
          "minPolicyThreatLevel must be between " + MIN_THREAT_LEVEL + " and " + MAX_THREAT_LEVEL + ".");
    }
    if (max != Integer.MAX_VALUE && (max < MIN_THREAT_LEVEL || max > MAX_THREAT_LEVEL)) {
      throw new BadRequestException(
          "maxPolicyThreatLevel must be between " + MIN_THREAT_LEVEL + " and " + MAX_THREAT_LEVEL + ".");
    }
  }

  private static void validateOrderBy(final String orderBy) {
    if (ORDER_BY_LAST_EVALUATION_DESC.equals(orderBy) || ORDER_BY_LAST_EVALUATION_ASC.equals(orderBy)) {
      return;
    }
    throw new BadRequestException("Unsupported orderBy on the applications list: " + orderBy);
  }

  private static boolean hasNonEmptyFilterSet(final Set<String> ids) {
    return ids != null && !ids.isEmpty();
  }

  private static boolean hasPolicyThreatCategoryFilter(final PolicyThreatCategoryFilter filter) {
    return filter != null && !filter.getPolicyThreatCategories().isEmpty();
  }

  private static boolean hasPolicyViolationStateFilter(final PolicyViolationStateFilter filter) {
    return filter != null && !filter.getPolicyViolationStates().isEmpty();
  }
}
