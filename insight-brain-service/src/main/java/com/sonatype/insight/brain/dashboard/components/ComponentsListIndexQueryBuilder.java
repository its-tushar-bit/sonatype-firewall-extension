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
    return new ComponentsIndexQuery(query, buildComponentTermSets(effectiveRequest));
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

  /**
   * The same query as {@link #buildComponentIndexQuery} but with the organization/application term sets
   * withheld. Used as the base for owner facet aggregations so selecting an org or app does not collapse
   * the org/app rails.
   * <p>
   * The component-hash term set is retained: the violation-scoped filters (stage, threat) reach a
   * component document by resolving to component hashes, and they must still narrow the rails or a
   * stage-filtered page would show owner counts for the whole estate.
   */
  ComponentsIndexQuery buildComponentIndexQueryWithoutOwner(final ComponentsListRequestDTO request) {
    ComponentsListRequestDTO effectiveRequest = applyViolationScopedComponentHashes(request);
    String query = String.join(" AND ", buildBaseComponentClauses(effectiveRequest));
    return new ComponentsIndexQuery(
        query,
        componentHashTermSets(effectiveRequest == null ? null : effectiveRequest.componentHashes));
  }

  /**
   * The same query as {@link #buildComponentIndexQuery} but with the stage filter omitted (every other
   * active filter, including owner and threat, is retained). Stage on the Components rail is a
   * violation-scoped filter resolved into component hashes, so it is removed by clearing
   * {@code stageIds} before hash resolution. Used as the base for the {@code stages} facet so selecting
   * a stage does not collapse the other stages. See {@link ComponentsListFacetsBuilder}.
   */
  ComponentsIndexQuery buildComponentIndexQueryExcludingStage(final ComponentsListRequestDTO request) {
    if (request == null) {
      return buildComponentIndexQuery(null);
    }
    ComponentsListRequestDTO stageRemoved = request.copy();
    stageRemoved.stageIds = null;
    return buildComponentIndexQuery(stageRemoved);
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
      // No component satisfies the violation-scoped filters. The no-match sentinel goes on the hash
      // dimension, not the owner dimension: the owner-removed facet base drops owner clauses, so an
      // owner-carried sentinel would vanish there and the rails would aggregate the whole estate while the
      // results page was empty.
      return request.withComponentHashes(Set.of(NO_MATCH_COMPONENT_HASH));
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

    // Organization, application and component-hash scope are not query text: they travel as
    // budget-exempt term-set restrictions, so the owner dimension is excluded by withholding the
    // organization/application term sets while the component-hash set is retained.

    // org/app scope and componentHashes are applied as budget-exempt term-set restrictions (CLM-44783).
    return clauses;
  }

  /**
   * Soft UX ceiling on scoped hash discovery (not a Lucene bool-clause budget). Violation-scope
   * resolution still uses this to bound discovery walks.
   */
  static final int MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES = 512;

  /**
   * Stands in for "no component matches" when the violation-scoped filters resolve to nothing. A real hash
   * is a hex digest, so this can never collide with one.
   */
  static final String NO_MATCH_COMPONENT_HASH = "__no_match__";

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
