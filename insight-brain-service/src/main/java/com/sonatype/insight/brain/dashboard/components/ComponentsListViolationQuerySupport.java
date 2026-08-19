/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Shared Lucene helpers for Martha components list violation-scoped filters (stages, threat levels).
 */
final class ComponentsListViolationQuerySupport
{
  static final String COMPONENT_ITEM_TYPE_CLAUSE =
      "(itemType:" + ItemType.NON_VULNERABLE_COMPONENT.name()
          + " OR itemType:" + ItemType.SECURITY_VULNERABILITY.name() + ")";

  private static final String VIOLATION_ITEM_TYPE_CLAUSE = "itemType:" + ItemType.POLICY_VIOLATION.name();

  private ComponentsListViolationQuerySupport() {
  }

  /**
   * Swaps the component item-type prefix for POLICY_VIOLATION so stage/threat filters query violation docs.
   */
  static String toViolationQuery(final String componentQuery) {
    if (!componentQuery.startsWith(COMPONENT_ITEM_TYPE_CLAUSE)) {
      throw new IllegalStateException(
          "Component list query must start with " + COMPONENT_ITEM_TYPE_CLAUSE + " but was: " + componentQuery);
    }
    return VIOLATION_ITEM_TYPE_CLAUSE + componentQuery.substring(COMPONENT_ITEM_TYPE_CLAUSE.length());
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

  static List<PolicyThreatLevelFilter> effectiveThreatFilters(final ComponentsListRequestDTO request) {
    if (request == null) {
      return List.of();
    }
    List<PolicyThreatLevelFilter> ranges = request.policyThreatLevelRanges;
    if (ranges != null) {
      return ranges;
    }
    PolicyThreatLevelFilter singular = request.policyThreatLevelRange;
    if (singular != null && buildThreatFilterClause(singular) != null) {
      return List.of(singular);
    }
    return List.of();
  }

  static boolean hasViolationScopedFilters(final ComponentsListRequestDTO request) {
    if (request == null) {
      return false;
    }
    if (request.stageIds != null && !request.stageIds.isEmpty()) {
      return true;
    }
    return buildThreatFilterClause(effectiveThreatFilters(request)) != null;
  }

  /**
   * Maps Martha sidebar threat buckets to the singular {@link PolicyThreatLevelFilter} shape expected
   * by Classic card enrichment. Multiple OR-selected buckets become a {@link PolicyThreatLevelOrFilter}.
   */
  static PolicyThreatLevelFilter threatLevelFilterForCardEnrichment(final ComponentsListRequestDTO request) {
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
