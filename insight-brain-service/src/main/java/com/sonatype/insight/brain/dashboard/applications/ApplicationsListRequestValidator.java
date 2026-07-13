/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Set;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates Applications list request filters at the API boundary.
 * <p>
 * Filters that cannot yet affect index pagination/total return {@link BadRequestException}
 * rather than being silently ignored until sidebar filter work lands.
 */
@Named
@Singleton
final class ApplicationsListRequestValidator
{
  /** Martha V1 fixed default — latest evaluation descending (CLM-42229). */
  static final String DEFAULT_ORDER_BY = "-lastEvaluationTime";

  private static final String ORDER_BY_LAST_EVALUATION_ASC = "lastEvaluationTime";

  private static final String ORDER_BY_LAST_EVALUATION_DESC = "-" + ORDER_BY_LAST_EVALUATION_ASC;

  void validate(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return;
    }
    rejectUnsupportedFilters(request);
  }

  private static void rejectUnsupportedFilters(final ApplicationsListRequestDTO request) {
    if (hasNonEmptyFilterSet(request.stageIds)) {
      throw new BadRequestException("stageIds filter is not yet supported on the applications list.");
    }
    if (hasNonEmptyFilterSet(request.tagIds)) {
      throw new BadRequestException("tagIds filter is not yet supported on the applications list.");
    }
    if (hasPolicyThreatCategoryFilter(request.policyThreatCategories)) {
      throw new BadRequestException("policyThreatCategories filter is not yet supported on the applications list.");
    }
    if (hasPolicyThreatLevelFilter(request.policyThreatLevelRange)) {
      throw new BadRequestException("policyThreatLevelRange filter is not yet supported on the applications list.");
    }
    if (hasPolicyViolationStateFilter(request.policyViolationStates)) {
      throw new BadRequestException("policyViolationStates filter is not yet supported on the applications list.");
    }
    if (StringUtils.isNotBlank(request.orderBy)) {
      validateOrderBy(request.orderBy);
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

  private static boolean hasPolicyThreatLevelFilter(final PolicyThreatLevelFilter filter) {
    return filter != null
        && (filter.getMinPolicyThreatLevel() != Integer.MIN_VALUE
            || filter.getMaxPolicyThreatLevel() != Integer.MAX_VALUE);
  }

  private static boolean hasPolicyViolationStateFilter(final PolicyViolationStateFilter filter) {
    return filter != null && !filter.getPolicyViolationStates().isEmpty();
  }
}
