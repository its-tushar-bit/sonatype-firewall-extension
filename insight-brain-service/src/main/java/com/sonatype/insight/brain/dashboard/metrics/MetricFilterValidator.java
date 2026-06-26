/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;

/**
 * Validates dashboard metrics filter ids at the API boundary.
 * <p>
 * Internal owner and tag ids are UUIDs without dashes ({@code TemporaryEntity#uuid()}), well-known
 * constants such as {@code ROOT_ORGANIZATION_ID}, or stage type ids (lowercase with hyphens, e.g.
 * {@code build}, {@code stage-release}). This validator rejects query-syntax characters and other
 * values outside that conservative allowlist before ids are applied programmatically as search terms.
 */
@Named
@Singleton
public class MetricFilterValidator
{
  /**
   * Allowlist of 1–64 chars from {@code [A-Za-z0-9_-]}, covering dashless UUIDs, underscore
   * constants (e.g. {@code ROOT_ORGANIZATION_ID}), and hyphenated stage type ids (e.g.
   * {@code stage-release}). Intentionally broader than hex/fixed-length — do not narrow this to
   * match a UUID shape or stage ids and constants will start failing validation.
   */
  static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  /** Cap filter id sets before PR2 enables filter application. */
  static final int MAX_FILTER_IDS = 1000;

  public void validate(DashboardMetricsRequestDTO request) {
    if (request == null) {
      return;
    }
    validateIdSet(request.organizationIds, "organizationIds");
    validateIdSet(request.applicationIds, "applicationIds");
    validateIdSet(request.stageIds, "stageIds");
    validateIdSet(request.tagIds, "tagIds");
  }

  /**
   * PR1 walking skeleton returns RBAC-scoped totals only. Reject non-empty filter sets so clients
   * cannot mistake a no-op filter for a scoped count; hierarchy-inclusive filtering lands in PR2.
   */
  public void rejectUnsupportedFilters(DashboardMetricsRequestDTO request) {
    if (request == null) {
      return;
    }
    if (hasNonEmptyFilterSet(request.organizationIds)
        || hasNonEmptyFilterSet(request.applicationIds)
        || hasNonEmptyFilterSet(request.stageIds)
        || hasNonEmptyFilterSet(request.tagIds))
    {
      throw new BadRequestException("Request filters are not supported yet.");
    }
  }

  private static boolean hasNonEmptyFilterSet(Set<String> ids) {
    return ids != null && !ids.isEmpty();
  }

  private static void validateIdSet(Set<String> ids, String fieldName) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    if (ids.size() > MAX_FILTER_IDS) {
      throw new BadRequestException("Too many " + fieldName + " filter ids (max " + MAX_FILTER_IDS + ").");
    }
    for (String id : ids) {
      if (id == null || !ID_PATTERN.matcher(id).matches()) {
        throw new BadRequestException("Invalid " + fieldName + " filter id.");
      }
    }
  }
}
