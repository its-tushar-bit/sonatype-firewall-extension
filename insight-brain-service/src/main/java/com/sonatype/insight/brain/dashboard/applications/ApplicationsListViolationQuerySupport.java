/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.commons.lang3.StringUtils;

/**
 * Shared Lucene helpers for Martha applications list violation-scoped filters (stages, threat levels).
 */
final class ApplicationsListViolationQuerySupport
{
  private static final String APPLICATION_ITEM_TYPE_CLAUSE = "itemType:" + ItemType.APPLICATION.name();

  private static final String VIOLATION_ITEM_TYPE_CLAUSE = "itemType:" + ItemType.POLICY_VIOLATION.name();

  private ApplicationsListViolationQuerySupport() {
  }

  /**
   * Swaps the APPLICATION item-type prefix for POLICY_VIOLATION so stage/threat filters query violation docs.
   * <p>
   * Free-text search clauses from the application query are carried over verbatim. Those clauses
   * only affect violation-scoped filters when violation documents carry the same denormalized
   * application and organization name fields as APPLICATION hits (Martha V1 assumption).
   */
  static String toViolationQuery(final String applicationQuery) {
    if (!applicationQuery.startsWith(APPLICATION_ITEM_TYPE_CLAUSE)) {
      throw new IllegalStateException(
          "Application list query must start with " + APPLICATION_ITEM_TYPE_CLAUSE + " but was: " + applicationQuery);
    }
    return VIOLATION_ITEM_TYPE_CLAUSE + applicationQuery.substring(APPLICATION_ITEM_TYPE_CLAUSE.length());
  }

  static String buildStageFilterClause(final Set<String> stageIds) {
    return buildStageFilterClause(stageIds, Integer.MAX_VALUE);
  }

  static String buildStageFilterClause(final Set<String> stageIds, final int maxClauseCount) {
    if (stageIds == null || stageIds.isEmpty()) {
      return null;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(stageIds, "stageIds");
    if (stageIds.size() > maxClauseCount) {
      throw new BadRequestException("Stage filter contains too many ids (max " + maxClauseCount + ").");
    }
    List<String> stageClauses = new ArrayList<>(stageIds.size());
    for (String stageId : DashboardIndexDimensionQueryBuilder.sortedCopy(stageIds)) {
      stageClauses.add(
          "policyEvaluationStage:" + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId));
    }
    if (stageClauses.size() == 1) {
      return stageClauses.get(0);
    }
    return "(" + String.join(" OR ", stageClauses) + ")";
  }

  static String buildThreatFilterClause(final PolicyThreatLevelFilter threatFilter) {
    if (threatFilter == null) {
      return null;
    }
    int min = threatFilter.getMinPolicyThreatLevel();
    int max = threatFilter.getMaxPolicyThreatLevel();
    if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
      return null;
    }
    // Clamp to the valid threat-level domain (0–10); partial sentinels map to the domain edges.
    int effectiveMin = min == Integer.MIN_VALUE ? 0 : Math.max(0, Math.min(min, 10));
    int effectiveMax = max == Integer.MAX_VALUE ? 10 : Math.max(0, Math.min(max, 10));
    if (effectiveMin > effectiveMax) {
      return null;
    }
    return "policyViolationThreatLevel:[" + effectiveMin + " TO " + effectiveMax + "]";
  }

  static String buildThreatFilterClause(final List<PolicyThreatLevelFilter> threatFilters) {
    if (threatFilters == null || threatFilters.isEmpty()) {
      return null;
    }
    List<String> rangeClauses = new ArrayList<>(threatFilters.size());
    for (PolicyThreatLevelFilter threatFilter : threatFilters) {
      String rangeClause = buildThreatFilterClause(threatFilter);
      if (rangeClause != null) {
        rangeClauses.add(rangeClause);
      }
    }
    if (rangeClauses.isEmpty()) {
      return null;
    }
    if (rangeClauses.size() == 1) {
      return rangeClauses.get(0);
    }
    return "(" + String.join(" OR ", rangeClauses) + ")";
  }

  static List<PolicyThreatLevelFilter> effectiveThreatFilters(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return List.of();
    }
    if (request.policyThreatLevelRanges != null && !request.policyThreatLevelRanges.isEmpty()) {
      return request.policyThreatLevelRanges;
    }
    if (request.policyThreatLevelRange != null && buildThreatFilterClause(request.policyThreatLevelRange) != null) {
      return List.of(request.policyThreatLevelRange);
    }
    return List.of();
  }

  static boolean hasViolationScopedFilters(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return false;
    }
    if (request.stageIds != null && !request.stageIds.isEmpty()) {
      return true;
    }
    if (buildThreatFilterClause(effectiveThreatFilters(request)) != null) {
      return true;
    }
    return false;
  }

  /**
   * Maps Martha sidebar threat buckets to the singular {@link PolicyThreatLevelFilter} shape expected
   * by Classic card enrichment. Multiple OR-selected buckets become a {@link PolicyThreatLevelOrFilter}.
   */
  static PolicyThreatLevelFilter threatLevelFilterForCardEnrichment(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return null;
    }
    List<PolicyThreatLevelFilter> ranges = effectiveThreatFilters(request);
    if (ranges.isEmpty()) {
      return null;
    }
    if (ranges.size() == 1) {
      return ranges.get(0);
    }
    return new PolicyThreatLevelOrFilter(ranges);
  }

  static String appendClauses(final String baseQuery, final List<String> extraClauses) {
    if (extraClauses == null || extraClauses.isEmpty()) {
      return baseQuery;
    }
    List<String> clauses = new ArrayList<>();
    if (StringUtils.isNotBlank(baseQuery)) {
      clauses.add(baseQuery);
    }
    clauses.addAll(extraClauses);
    return String.join(" AND ", clauses);
  }
}
