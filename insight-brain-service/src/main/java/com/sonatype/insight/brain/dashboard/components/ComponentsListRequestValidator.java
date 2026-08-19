/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.ComponentRiskOrderByEnum;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates Components list request filters at the API boundary.
 * <p>
 * {@code orderBy} accepts Classic {@link ComponentRiskOrderByEnum} tokens only (optional {@code -}
 * prefix). Unsupported filters return {@link BadRequestException} rather than being silently ignored.
 */
@Named
@Singleton
final class ComponentsListRequestValidator
{
  /** Martha V1 default — highest total risk first (Classic enum token). */
  static final String DEFAULT_ORDER_BY = "-TOTAL_RISK";

  /** Matches the selectable Martha threat buckets (Critical/Severe/Moderate/Low/None). */
  static final int MAX_POLICY_THREAT_LEVEL_RANGES = 5;

  private static final int MIN_THREAT_LEVEL = 0;

  private static final int MAX_THREAT_LEVEL = 10;

  private static final int DEFAULT_MAX_STAGE_IDS = 2048;

  private final Configuration configuration;

  @Inject
  ComponentsListRequestValidator(final Configuration configuration) {
    this.configuration = configuration;
  }

  void validate(final ComponentsListRequestDTO request) {
    if (request == null) {
      return;
    }
    rejectUnsupportedFilters(request);
    validateStageIds(request);
    validateComponentHashes(request);
    validateThreatLevelFilters(request);
  }

  private void validateStageIds(final ComponentsListRequestDTO request) {
    if (request.stageIds == null || request.stageIds.isEmpty()) {
      return;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(request.stageIds, "stageIds");
    int maxIds = maxClauseCount();
    if (request.stageIds.size() > maxIds) {
      throw new BadRequestException("stageIds contains too many ids (max " + maxIds + ").");
    }
  }

  private void validateComponentHashes(final ComponentsListRequestDTO request) {
    if (request.componentHashes == null || request.componentHashes.isEmpty()) {
      return;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(request.componentHashes, "componentHashes");
    // Soft UX ceiling — hashes are applied as budget-exempt term sets (CLM-44783), not Lucene OR clauses.
    int maxIds = ComponentsListIndexQueryBuilder.MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES;
    if (request.componentHashes.size() > maxIds) {
      throw new BadRequestException("componentHashes contains too many ids (max " + maxIds + ").");
    }
  }

  private int maxClauseCount() {
    int maxIds = configuration.getMaxAdvancedSearchClauseCount();
    return maxIds <= 0 ? DEFAULT_MAX_STAGE_IDS : maxIds;
  }

  private static void rejectUnsupportedFilters(final ComponentsListRequestDTO request) {
    if (hasNonEmptyFilterSet(request.tagIds)) {
      throw new BadRequestException("tagIds filter is not yet supported on the components list.");
    }
    if (hasPolicyThreatCategoryFilter(request.policyThreatCategories)) {
      throw new BadRequestException("policyThreatCategories filter is not yet supported on the components list.");
    }
    if (hasPolicyViolationStateFilter(request.policyViolationStates)) {
      throw new BadRequestException("policyViolationStates filter is not yet supported on the components list.");
    }
    if (StringUtils.isNotBlank(request.orderBy)) {
      validateOrderBy(request.orderBy);
    }
  }

  private static void validateThreatLevelFilters(final ComponentsListRequestDTO request) {
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
    String token = orderBy.startsWith("-") ? orderBy.substring(1) : orderBy;
    try {
      ComponentRiskOrderByEnum.valueOf(token);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("Unsupported orderBy on the components list: " + orderBy, e);
    }
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
