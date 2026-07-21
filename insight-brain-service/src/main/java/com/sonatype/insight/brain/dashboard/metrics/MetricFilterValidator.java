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

  /** Upper bound on filter id set size. */
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
