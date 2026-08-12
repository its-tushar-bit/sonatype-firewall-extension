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
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
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
  /** CLM-44036: risk-first default for cross-page Applications list ordering. */
  static final String DEFAULT_ORDER_BY = "-maxPolicyThreatLevel";

  static final String ORDER_BY_MAX_POLICY_THREAT_LEVEL_ASC = "maxPolicyThreatLevel";

  static final String ORDER_BY_MAX_POLICY_THREAT_LEVEL_DESC = "-" + ORDER_BY_MAX_POLICY_THREAT_LEVEL_ASC;

  static final String ORDER_BY_LAST_EVALUATION_ASC = "lastEvaluationTime";

  static final String ORDER_BY_LAST_EVALUATION_DESC = "-" + ORDER_BY_LAST_EVALUATION_ASC;

  /** Matches the selectable Martha threat buckets (Critical/Severe/Moderate/Low/None). */
  static final int MAX_POLICY_THREAT_LEVEL_RANGES = 5;

  private static final int MIN_THREAT_LEVEL = 0;

  private static final int MAX_THREAT_LEVEL = 10;

  private static final int DEFAULT_MAX_STAGE_IDS = 2048;

  /** Inclusive upper bound for {@code ageInDays}; FE currently offers 7/30/90. */
  static final int MAX_AGE_IN_DAYS = 3650;

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
    validateAgeInDays(request.ageInDays);
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
    // policyThreatCategories and policyViolationStates are violation-scoped filters (CLM-43211). Both
    // deserialize into EnumSets, so the indexed domain bounds their size and membership and there is
    // nothing further to validate here — an unknown name already fails at deserialization.
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
    // When policyThreatLevelRanges is non-null (including empty), effectiveThreatFilters treats it as
    // authoritative and ignores policyThreatLevelRange. Singular applies only when plural is omitted.
    validateThreatLevelFilterBounds(request.policyThreatLevelRange);
  }

  private static void validateAgeInDays(final Integer ageInDays) {
    if (ageInDays == null) {
      return;
    }
    if (ageInDays <= 0 || ageInDays > MAX_AGE_IN_DAYS) {
      throw new BadRequestException(
          "ageInDays must be a positive integer no greater than " + MAX_AGE_IN_DAYS + ".");
    }
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
    if (ORDER_BY_MAX_POLICY_THREAT_LEVEL_DESC.equals(orderBy)
        || ORDER_BY_MAX_POLICY_THREAT_LEVEL_ASC.equals(orderBy)
        || ORDER_BY_LAST_EVALUATION_DESC.equals(orderBy)
        || ORDER_BY_LAST_EVALUATION_ASC.equals(orderBy))
    {
      return;
    }
    throw new BadRequestException("Unsupported orderBy on the applications list: " + orderBy);
  }

  private static boolean hasNonEmptyFilterSet(final Set<String> ids) {
    return ids != null && !ids.isEmpty();
  }
}
