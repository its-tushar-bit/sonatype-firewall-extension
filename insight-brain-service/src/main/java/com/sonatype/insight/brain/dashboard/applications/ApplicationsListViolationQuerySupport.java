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
import com.sonatype.insight.brain.dashboard.PolicyViolationIndexClauses;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.commons.lang3.StringUtils;

/**
 * Shared Lucene helpers for Martha applications list violation-scoped filters (stages, threat levels,
 * policy types, violation states).
 * <p>
 * Policy type and violation state are resolved through violation-scoped discovery rather than the
 * denormalized {@code applicationViolationPolicyType} / {@code applicationViolationState} fields on
 * APPLICATION documents. Those fields are cheaper but cannot express a conjunction across a single
 * violation: an application holding a waived Security violation and an open Quality violation carries
 * both {@code security} and {@code open} at the application level and would match
 * "Security AND Open" even though no such violation exists. Scoping through POLICY_VIOLATION docs
 * evaluates both predicates against the same document (CLM-43211).
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

  /**
   * Policy-type clause over violation docs. Delegates to the shared builder so the Applications filter
   * and the Violations list emit identical Lucene for the same selection.
   */
  static String buildPolicyTypeFilterClause(final PolicyThreatCategoryFilter categoryFilter) {
    return PolicyViolationIndexClauses.threatCategoryClause(categoryFilter);
  }

  /**
   * Violation-state clause over violation docs. Returns {@code null} when the selection covers the whole
   * indexed domain and therefore narrows nothing.
   */
  static String buildViolationStateFilterClause(final PolicyViolationStateFilter stateFilter) {
    return PolicyViolationIndexClauses.stateClause(stateFilter);
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

  /**
   * Resolve threat filters used for violation-scoped discovery.
   * <p>
   * When {@code policyThreatLevelRanges} is non-null it is authoritative — including an empty
   * list, which means no threat filter (do not fall through to singular
   * {@code policyThreatLevelRange}). Singular is used only when the plural field is omitted
   * ({@code null}), for Classic/API compatibility. The validator accepts both fields.
   */
  static List<PolicyThreatLevelFilter> effectiveThreatFilters(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return List.of();
    }
    List<PolicyThreatLevelFilter> ranges = request.policyThreatLevelRanges;
    if (ranges != null) {
      return ranges;
    }
    PolicyThreatLevelFilter singular = request.policyThreatLevelRange;
    if (singular != null) {
      if (buildThreatFilterClause(singular) != null) {
        return List.of(singular);
      }
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
    if (buildPolicyTypeFilterClause(request.policyThreatCategories) != null) {
      return true;
    }
    if (buildViolationStateFilterClause(request.policyViolationStates) != null) {
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
