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
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
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

  /**
   * Violation-scoped filter dimensions that can be individually excluded from the pre-discovery
   * violation-scope query, so a facet can be counted against a base where every OTHER active filter
   * still narrows the violation-scoped application ids but that facet's own filter does not.
   */
  private enum ExcludableViolationScopedDimension
  {
    STAGE,
    POLICY_TYPE,
    VIOLATION_STATE
  }

  String buildApplicationQuery(final ApplicationsListRequestDTO request) {
    return buildApplicationIndexQuery(request).query();
  }

  ApplicationsIndexQuery buildApplicationIndexQuery(final ApplicationsListRequestDTO request) {
    return finalizeApplicationQuery(applyViolationScopedApplicationIds(request));
  }

  /**
   * The same query and restrictions as {@link #buildApplicationIndexQuery} but with the caller's own
   * organization/application selection cleared. Used as the base for owner facet aggregations so
   * selecting an org or app does not collapse the org/app rails. Age (an APPLICATION-doc filter, not an
   * owner dimension) is still applied.
   * <p>
   * Only the owner dimension is dropped: the violation-scoped filters (stage, threat, policy type,
   * violation state) must still narrow the rails, or a stage-filtered page would show owner counts for
   * the whole estate. Those filters reach an APPLICATION document by resolving to application ids, which
   * is also how the user's own application filter is expressed. Clearing the owner selection *before*
   * resolution keeps them separable: the resolved ids become the only term-set restriction, so the owner
   * dimension is absent while violation narrowing survives.
   */
  ApplicationsIndexQuery buildApplicationIndexQueryWithoutOwner(final ApplicationsListRequestDTO request) {
    return finalizeApplicationQuery(
        applyViolationScopedApplicationIds(request == null ? null : copyWithoutOwnerSelection(request)));
  }

  /** A copy of {@code request} with the owner-dimension selection cleared; other filters are preserved. */
  private static ApplicationsListRequestDTO copyWithoutOwnerSelection(final ApplicationsListRequestDTO request) {
    ApplicationsListRequestDTO copy = new ApplicationsListRequestDTO();
    copy.search = request.search;
    copy.organizationIds = null;
    copy.applicationIds = null;
    copy.tagIds = request.tagIds;
    copy.stageIds = request.stageIds;
    copy.policyThreatCategories = request.policyThreatCategories;
    copy.policyThreatLevelRange = request.policyThreatLevelRange;
    copy.policyThreatLevelRanges = request.policyThreatLevelRanges;
    copy.policyViolationStates = request.policyViolationStates;
    copy.ageInDays = request.ageInDays;
    return copy;
  }

  /**
   * Same as {@link #buildApplicationQuery(ApplicationsListRequestDTO)} but the violation-scoped
   * discovery that resolves the owner-dimension application ids ignores the {@code stageIds} filter
   * (every other active filter, including threat/policy-type/violation-state, still narrows). Used as
   * the base for the {@code stages} facet so selecting a stage does not collapse the other stages.
   */
  ApplicationsIndexQuery buildApplicationIndexQueryExcludingStage(final ApplicationsListRequestDTO request) {
    return finalizeApplicationQuery(applyViolationScopedApplicationIds(
        withDimensionExcluded(request, ExcludableViolationScopedDimension.STAGE)));
  }

  /**
   * Same as {@link #buildApplicationQuery(ApplicationsListRequestDTO)} but the violation-scoped
   * discovery ignores the {@code policyThreatCategories} filter. Used as the base for the
   * {@code policyTypes} facet so selecting a policy type does not collapse the other policy types.
   */
  ApplicationsIndexQuery buildApplicationIndexQueryExcludingPolicyType(
      final ApplicationsListRequestDTO request)
  {
    return finalizeApplicationQuery(applyViolationScopedApplicationIds(
        withDimensionExcluded(request, ExcludableViolationScopedDimension.POLICY_TYPE)));
  }

  /**
   * Same as {@link #buildApplicationQuery(ApplicationsListRequestDTO)} but the violation-scoped
   * discovery ignores the {@code policyViolationStates} filter. Used as the base for the
   * {@code violationStates} facet so selecting a violation state does not collapse the other states.
   */
  ApplicationsIndexQuery buildApplicationIndexQueryExcludingViolationState(
      final ApplicationsListRequestDTO request)
  {
    return finalizeApplicationQuery(applyViolationScopedApplicationIds(
        withDimensionExcluded(request, ExcludableViolationScopedDimension.VIOLATION_STATE)));
  }

  /** Age is APPLICATION-doc only — applied on the final query, never on violation discovery. */
  private ApplicationsIndexQuery finalizeApplicationQuery(final ApplicationsListRequestDTO effectiveRequest) {
    // Age is APPLICATION-doc only - apply on the final query, never on violation discovery.
    List<String> clauses = buildBaseApplicationClauses(effectiveRequest);
    String ageClause = buildAgeClause(effectiveRequest == null ? null : effectiveRequest.ageInDays);
    if (ageClause != null) {
      clauses.add(ageClause);
    }
    return new ApplicationsIndexQuery(String.join(" AND ", clauses), buildScopeRestrictions(effectiveRequest));
  }

  /**
   * Shallow-copies {@code request} with one violation-scoped filter field nulled, so
   * {@link #applyViolationScopedApplicationIds} resolves violation-scoped application ids without that
   * one dimension narrowing them (every other field, including owner ids, is unchanged).
   */
  private static ApplicationsListRequestDTO withDimensionExcluded(
      final ApplicationsListRequestDTO request,
      final ExcludableViolationScopedDimension excluded)
  {
    if (request == null) {
      return null;
    }
    ApplicationsListRequestDTO copy = copyOfRequest(request);
    switch (excluded) {
      case STAGE -> copy.stageIds = null;
      case POLICY_TYPE -> copy.policyThreatCategories = null;
      case VIOLATION_STATE -> copy.policyViolationStates = null;
    }
    return copy;
  }

  private static ApplicationsListRequestDTO copyOfRequest(final ApplicationsListRequestDTO request) {
    ApplicationsListRequestDTO copy = new ApplicationsListRequestDTO();
    copy.search = request.search;
    copy.page = request.page;
    copy.pageSize = request.pageSize;
    copy.organizationIds = request.organizationIds;
    copy.applicationIds = request.applicationIds;
    copy.stageIds = request.stageIds;
    copy.tagIds = request.tagIds;
    copy.policyThreatCategories = request.policyThreatCategories;
    copy.policyThreatLevelRange = request.policyThreatLevelRange;
    copy.policyThreatLevelRanges = request.policyThreatLevelRanges;
    copy.policyViolationStates = request.policyViolationStates;
    copy.ageInDays = request.ageInDays;
    copy.orderBy = request.orderBy;
    copy.includeFacets = request.includeFacets;
    return copy;
  }

  private ApplicationsListRequestDTO applyViolationScopedApplicationIds(final ApplicationsListRequestDTO request) {
    if (request == null) {
      return null;
    }
    if (!ApplicationsListViolationQuerySupport.hasViolationScopedFilters(request)) {
      return request;
    }

    String baseQuery = buildApplicationQueryWithoutViolationScope(request);
    List<IndexFilterRestriction> scopeRestrictions = buildScopeRestrictions(request);
    Set<String> scopedApplicationIds =
        violationScopeResolver.resolveApplicationIds(baseQuery, scopeRestrictions, request);
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

  /**
   * Builds the base application query clauses. Organization and application scope is not included: it
   * travels as budget-exempt term-set restrictions alongside the query.
   */
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
    Set<String> applicationIds = request == null ? null : request.applicationIds;
    if (applicationIds != null && !applicationIds.isEmpty()) {
      DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(applicationIds, "applicationIds");
    }

    // Organization and application are ONE owner dimension and are not query text: they travel as
    // budget-exempt term-set restrictions, so the owner dimension is excluded by clearing the selection
    // before resolution rather than by omitting a clause here.
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

  List<IndexFilterRestriction> buildScopeRestrictions(final ApplicationsListRequestDTO request) {
    return dimensionQueryBuilder.buildScopeFilterRestrictions(
        request == null ? null : request.organizationIds,
        request == null ? null : request.applicationIds);
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
