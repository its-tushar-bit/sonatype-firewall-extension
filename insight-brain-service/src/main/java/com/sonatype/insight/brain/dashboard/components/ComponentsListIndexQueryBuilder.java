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
import com.sonatype.insight.error.exception.BadRequestException;

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
    ComponentsListRequestDTO effectiveRequest = applyViolationScopedComponentHashes(request);
    return String.join(" AND ", buildBaseComponentClauses(effectiveRequest));
  }

  private ComponentsListRequestDTO applyViolationScopedComponentHashes(final ComponentsListRequestDTO request) {
    if (request == null) {
      return null;
    }
    if (!ComponentsListViolationQuerySupport.hasViolationScopedFilters(request)) {
      return request;
    }

    String baseQuery = buildComponentQueryWithoutViolationScope(request);
    List<PolicyThreatLevelFilter> threatFilters =
        ComponentsListViolationQuerySupport.effectiveThreatFilters(request);
    Set<String> scopedHashes = violationScopeResolver.resolveComponentHashes(
        baseQuery,
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

    String hashClause = buildComponentHashFilterClause(request == null ? null : request.componentHashes);
    if (hashClause != null) {
      clauses.add(hashClause);
    }

    return clauses;
  }

  /**
   * Max hashes in a single {@code componentHash:(… OR …)} clause. Kept well below the generic
   * advanced-search clause budget so page/facet walks do not trip Lucene {@code TooManyClauses}.
   */
  static final int MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES = 512;

  private static String buildComponentHashFilterClause(final Set<String> componentHashes) {
    if (componentHashes == null || componentHashes.isEmpty()) {
      return null;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(componentHashes, "componentHashes");
    if (componentHashes.size() > MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES) {
      throw new BadRequestException(
          "Too many componentHashes filters (max " + MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES
              + "). Narrow stage/threat filters.");
    }
    List<String> terms = new ArrayList<>(componentHashes.size());
    for (String hash : DashboardIndexDimensionQueryBuilder.sortedCopy(componentHashes)) {
      terms.add(DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(hash));
    }
    if (terms.size() == 1) {
      return "componentHash:" + terms.get(0);
    }
    return "componentHash:(" + String.join(" OR ", terms) + ")";
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
