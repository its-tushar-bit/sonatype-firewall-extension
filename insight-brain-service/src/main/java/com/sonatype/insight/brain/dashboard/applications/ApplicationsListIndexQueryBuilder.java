/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds RBAC-scoped Lucene queries for the Martha Applications list.
 */
@Named
final class ApplicationsListIndexQueryBuilder
{
  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final ApplicationsListViolationScopeResolver violationScopeResolver;

  @Inject
  ApplicationsListIndexQueryBuilder(
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      final ApplicationsListViolationScopeResolver violationScopeResolver)
  {
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.violationScopeResolver = violationScopeResolver;
  }

  String buildApplicationQuery(final ApplicationsListRequestDTO request) {
    ApplicationsListRequestDTO effectiveRequest = applyViolationScopedApplicationIds(request);
    // Age is APPLICATION-doc only — apply on the final query, never on violation discovery.
    List<String> clauses = buildBaseApplicationClauses(effectiveRequest);
    String ageClause = buildAgeClause(effectiveRequest == null ? null : effectiveRequest.ageInDays);
    if (ageClause != null) {
      clauses.add(ageClause);
    }
    return String.join(" AND ", clauses);
  }

  private ApplicationsListRequestDTO applyViolationScopedApplicationIds(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return null;
    }
    if (!ApplicationsListViolationQuerySupport.hasViolationScopedFilters(request)) {
      return request;
    }

    String baseQuery = buildApplicationQueryWithoutViolationScope(request);
    Set<String> scopedApplicationIds = violationScopeResolver.resolveApplicationIds(baseQuery, request);
    Set<String> effectiveApplicationIds =
        effectiveApplicationIdsAfterViolationScope(request, scopedApplicationIds);

    ApplicationsListRequestDTO scoped = new ApplicationsListRequestDTO();
    scoped.search = request.search;
    // Violation-scoped application ids already reflect org/search/RBAC constraints from
    // baseQuery; OR-ing organizationIds here would bypass stage/threat filtering.
    scoped.organizationIds = null;
    scoped.applicationIds = effectiveApplicationIds;
    // Stage/threat are already applied via violation-scope discovery into applicationIds.
    // Keep them on the DTO for card enrichment only — buildBaseApplicationClauses ignores them.
    // Policy type / violation state are applied via violation-scope discovery alongside stage and
    // threat; they stay on the DTO for card enrichment only.
    scoped.stageIds = request.stageIds;
    scoped.tagIds = request.tagIds;
    scoped.policyThreatCategories = request.policyThreatCategories;
    scoped.policyThreatLevelRange = request.policyThreatLevelRange;
    scoped.policyThreatLevelRanges = request.policyThreatLevelRanges;
    scoped.policyViolationStates = request.policyViolationStates;
    scoped.ageInDays = request.ageInDays;
    scoped.orderBy = request.orderBy;
    scoped.page = request.page;
    scoped.pageSize = request.pageSize;
    scoped.includeFacets = request.includeFacets;
    return scoped;
  }

  /**
   * Pre-discovery APPLICATION query for violation-scope resolution. Omits {@code ageInDays}:
   * {@code applicationLastEvaluationTimeEpochMs} exists only on APPLICATION docs, and discovery
   * rewrites this query onto POLICY_VIOLATION docs via
   * {@link ApplicationsListViolationQuerySupport#toViolationQuery}.
   */
  private String buildApplicationQueryWithoutViolationScope(final ApplicationsListRequestDTO request) {
    return String.join(" AND ", buildBaseApplicationClauses(request));
  }

  private List<String> buildBaseApplicationClauses(final ApplicationsListRequestDTO request) {
    List<String> clauses = new ArrayList<>();
    clauses.add("itemType:" + ItemType.APPLICATION.name());

    String searchClause = buildSearchClause(request == null ? null : request.search);
    if (searchClause != null) {
      clauses.add(searchClause);
    }

    Set<String> organizationIds = request == null ? null : request.organizationIds;
    if (organizationIds != null && !organizationIds.isEmpty()) {
      DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(organizationIds, "organizationIds");
    }
    String organizationClause = dimensionQueryBuilder.buildOrganizationFilterClause(organizationIds);
    String applicationClause = dimensionQueryBuilder.buildEscapedApplicationFilterClause(
        request == null ? null : request.applicationIds);
    if (organizationClause != null || applicationClause != null) {
      List<String> dimensionClauses = new ArrayList<>();
      if (organizationClause != null) {
        dimensionClauses.add(organizationClause);
      }
      if (applicationClause != null) {
        dimensionClauses.add(applicationClause);
      }
      clauses.add("(" + String.join(" OR ", dimensionClauses) + ")");
    }

    return clauses;
  }

  private static String buildAgeClause(final Integer ageInDays) {
    if (ageInDays == null) {
      return null;
    }
    long upper = System.currentTimeMillis();
    long lower = upper - TimeUnit.DAYS.toMillis(ageInDays);
    return FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label + ":[" + lower + " TO " + upper + "]";
  }

  /**
   * When violation-scoped filters are active, {@code scopedApplicationIds} already reflects the
   * org/app OR union encoded in the pre-scope base query. Narrow to explicit application ids only
   * when no organization filter is present.
   */
  private static Set<String> effectiveApplicationIdsAfterViolationScope(
      final ApplicationsListRequestDTO request,
      final Set<String> scopedApplicationIds)
  {
    if (scopedApplicationIds == null || scopedApplicationIds.isEmpty()) {
      return Set.of(DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID);
    }
    Set<String> requestedApplicationIds = request.applicationIds;
    if (requestedApplicationIds == null || requestedApplicationIds.isEmpty()) {
      return scopedApplicationIds;
    }
    boolean hasOrgFilter = request.organizationIds != null && !request.organizationIds.isEmpty();
    if (hasOrgFilter) {
      return scopedApplicationIds;
    }
    return intersectApplicationIds(requestedApplicationIds, scopedApplicationIds);
  }

  private static Set<String> intersectApplicationIds(
      final Set<String> requestedApplicationIds,
      final Set<String> scopedApplicationIds)
  {
    if (requestedApplicationIds == null || requestedApplicationIds.isEmpty()) {
      return scopedApplicationIds;
    }
    LinkedHashSet<String> intersection = new LinkedHashSet<>();
    for (String applicationId : scopedApplicationIds) {
      if (requestedApplicationIds.contains(applicationId)) {
        intersection.add(applicationId);
      }
    }
    if (intersection.isEmpty()) {
      // Same impossible-id sentinel as the empty-scope branch above.
      return Set.of(DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID);
    }
    return intersection;
  }

  private static String buildSearchClause(final String search) {
    if (StringUtils.isBlank(search)) {
      return null;
    }
    // Leading-wildcard clauses mirror global search; index-side optimizations can follow scale testing.
    String[] tokens = search.trim().split("\\s+");
    List<String> tokenClauses = new ArrayList<>(tokens.length);
    for (String token : tokens) {
      if (StringUtils.isBlank(token)) {
        continue;
      }
      String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(token);
      tokenClauses.add("(" + String.join(
          " OR ",
          "applicationName:*" + safe + "*",
          "applicationPublicId:*" + safe + "*",
          "organizationName:*" + safe + "*") + ")");
    }
    if (tokenClauses.isEmpty()) {
      return null;
    }
    if (tokenClauses.size() == 1) {
      return tokenClauses.get(0);
    }
    return "(" + String.join(" AND ", tokenClauses) + ")";
  }
}
