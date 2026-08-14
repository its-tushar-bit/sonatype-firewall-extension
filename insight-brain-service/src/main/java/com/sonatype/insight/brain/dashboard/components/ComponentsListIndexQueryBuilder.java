/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds RBAC-scoped Lucene queries for the Martha Components list.
 */
@Named
final class ComponentsListIndexQueryBuilder
{
  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final ComponentsListViolationScopeResolver violationScopeResolver;

  @Inject
  ComponentsListIndexQueryBuilder(
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      final ComponentsListViolationScopeResolver violationScopeResolver)
  {
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.violationScopeResolver = violationScopeResolver;
  }

  String buildComponentQuery(final ComponentsListRequestDTO request) {
    return buildComponentIndexQuery(request).query();
  }

  ComponentsIndexQuery buildComponentIndexQuery(final ComponentsListRequestDTO request) {
    ComponentsListRequestDTO effectiveRequest = applyViolationScopedComponentHashes(request);
    String query = String.join(" AND ", buildBaseComponentClauses(effectiveRequest));
    List<IndexFilterRestriction> termSets = buildComponentTermSets(effectiveRequest);
    return new ComponentsIndexQuery(query, termSets);
  }

  private List<IndexFilterRestriction> buildComponentTermSets(final ComponentsListRequestDTO request) {
    List<IndexFilterRestriction> scopeRestrictions = dimensionQueryBuilder.buildScopeFilterRestrictions(
        request == null ? null : request.organizationIds,
        request == null ? null : request.applicationIds);
    List<IndexFilterRestriction> hashRestrictions = componentHashTermSets(
        request == null ? null : request.componentHashes);
    if (scopeRestrictions.isEmpty()) {
      return hashRestrictions;
    }
    if (hashRestrictions.isEmpty()) {
      return scopeRestrictions;
    }
    List<IndexFilterRestriction> merged = new ArrayList<>(scopeRestrictions.size() + hashRestrictions.size());
    merged.addAll(scopeRestrictions);
    merged.addAll(hashRestrictions);
    return List.copyOf(merged);
  }

  private ComponentsListRequestDTO applyViolationScopedComponentHashes(final ComponentsListRequestDTO request) {
    if (request == null) {
      return null;
    }
    if (!ComponentsListViolationQuerySupport.hasViolationScopedFilters(request)) {
      return request;
    }

    String baseQuery = buildComponentQueryWithoutViolationScope(request);
    List<IndexFilterRestriction> scopeRestrictions = dimensionQueryBuilder.buildScopeFilterRestrictions(
        request.organizationIds,
        request.applicationIds);
    List<PolicyThreatLevelFilter> threatFilters =
        ComponentsListViolationQuerySupport.effectiveThreatFilters(request);
    Set<String> scopedHashes = violationScopeResolver.resolveComponentHashes(
        baseQuery,
        scopeRestrictions,
        request.stageIds,
        threatFilters);

    // Stage/threat already applied via hash discovery; keep them on the DTO for future enrichment.
    if (scopedHashes == null || scopedHashes.isEmpty()) {
      ComponentsListRequestDTO scoped = request.copy();
      scoped.organizationIds = Set.of(DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID);
      scoped.applicationIds = null;
      scoped.componentHashes = null;
      return scoped;
    }
    return request.withComponentHashes(scopedHashes);
  }

  private String buildComponentQueryWithoutViolationScope(final ComponentsListRequestDTO request) {
    return String.join(" AND ", buildBaseComponentClauses(request));
  }

  private List<String> buildBaseComponentClauses(final ComponentsListRequestDTO request) {
    List<String> clauses = new ArrayList<>();
    clauses.add(ComponentsListViolationQuerySupport.COMPONENT_ITEM_TYPE_CLAUSE);

    String searchClause = buildSearchClause(request == null ? null : request.search);
    if (searchClause != null) {
      clauses.add(searchClause);
    }

    Set<String> organizationIds = request == null ? null : request.organizationIds;
    if (organizationIds != null && !organizationIds.isEmpty()) {
      DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(organizationIds, "organizationIds");
    }

    // org/app scope and componentHashes are applied as budget-exempt term-set restrictions (CLM-44783).
    return clauses;
  }

  /**
   * Soft UX ceiling on scoped hash discovery (not a Lucene bool-clause budget). Violation-scope
   * resolution still uses this to bound discovery walks.
   */
  static final int MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES = 512;

  private static List<IndexFilterRestriction> componentHashTermSets(final Set<String> componentHashes) {
    if (componentHashes == null || componentHashes.isEmpty()) {
      return List.of();
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(componentHashes, "componentHashes");
    return IndexTermSetRestriction.singleton(
        FieldIdentifier.COMPONENT_HASH.label,
        DashboardIndexDimensionQueryBuilder.sortedCopy(componentHashes));
  }

  private static String buildSearchClause(final String search) {
    if (StringUtils.isBlank(search)) {
      return null;
    }
    String[] tokens = search.trim().split("\\s+");
    List<String> tokenClauses = new ArrayList<>(tokens.length);
    for (String token : tokens) {
      if (StringUtils.isBlank(token)) {
        continue;
      }
      String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(token);
      tokenClauses.add("(" + String.join(
          " OR ",
          "componentName:*" + safe + "*",
          "componentHash:*" + safe + "*",
          "componentFormat:*" + safe + "*",
          "componentCoordinateGroupId:*" + safe + "*",
          "componentCoordinateArtifactId:*" + safe + "*",
          "componentCoordinateName:*" + safe + "*") + ")");
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
