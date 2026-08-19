/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.Set;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates Violations list request filters at the API boundary.
 * <p>
 * Filters that cannot yet be resolved from the search index return {@link BadRequestException}
 * rather than being silently ignored until later stories wire them. {@code ageInDays} remains
 * rejected (first-seen is SQL page-enrich only; not range-queryable on {@code POLICY_VIOLATION}
 * docs). {@code applicationCategoryIds} is accepted and resolved to category names for an index
 * TERMS clause.
 */
@Named
@Singleton
final class ViolationsListRequestValidator
{
  static final String ORDER_BY_POLICY_THREAT_LEVEL_ASC = "policyThreatLevel";

  static final String ORDER_BY_POLICY_THREAT_LEVEL_DESC = "-policyThreatLevel";

  static final String DEFAULT_ORDER_BY = ORDER_BY_POLICY_THREAT_LEVEL_DESC;

  /** Matches {@code application_component.hash} / index componentHash storage width. */
  static final int MAX_COMPONENT_HASH_LENGTH = 40;

  private static final Set<String> SUPPORTED_ORDER_BY = Set.of(
      ORDER_BY_POLICY_THREAT_LEVEL_ASC,
      ORDER_BY_POLICY_THREAT_LEVEL_DESC);

  void validate(final ViolationsListRequestDTO request) {
    if (request == null) {
      return;
    }
    rejectUnsupportedFilters(request);
    validateComponentHash(request.componentHash);
    validateOrderBy(request.orderBy);
  }

  private static void validateComponentHash(final String componentHash) {
    if (componentHash == null) {
      return;
    }
    String trimmed = componentHash.trim();
    if (trimmed.isEmpty()) {
      return;
    }
    if (trimmed.length() > MAX_COMPONENT_HASH_LENGTH) {
      throw new BadRequestException(
          "componentHash exceeds maximum length of " + MAX_COMPONENT_HASH_LENGTH + " characters.");
    }
  }

  private static void rejectUnsupportedFilters(final ViolationsListRequestDTO request) {
    if (request.ageInDays != null) {
      throw new BadRequestException("ageInDays filter is not yet supported on the violations list.");
    }
    // applicationCategoryIds, waivedWithAutoWaiver, and all three policyViolationStates
    // (OPEN/WAIVED/LEGACY_VIOLATION) are supported and need no rejection here. Category ids resolve
    // to APPLICATION_CATEGORY_NAME TERMS in ViolationsListIndexQueryBuilder. State mapping lives in
    // ViolationWaiverStatus.
  }

  private static void validateOrderBy(final String orderBy) {
    if (StringUtils.isBlank(orderBy)) {
      return;
    }
    if (!SUPPORTED_ORDER_BY.contains(orderBy)) {
      throw new BadRequestException(
          "Invalid orderBy: " + orderBy + ". Supported values are "
              + ORDER_BY_POLICY_THREAT_LEVEL_ASC + " and " + ORDER_BY_POLICY_THREAT_LEVEL_DESC + ".");
    }
  }
}
