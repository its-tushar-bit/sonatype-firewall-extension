/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds RBAC-scoped Lucene queries for the Martha Violations list.
 * <p>
 * The base clause is {@code itemType:POLICY_VIOLATION}; RBAC is applied programmatically by the
 * search client (see {@code SearchIndexClient}).
 */
@Named
@Singleton
final class ViolationsListIndexQueryBuilder
{
  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Inject
  ViolationsListIndexQueryBuilder(final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder) {
    this.dimensionQueryBuilder = dimensionQueryBuilder;
  }

  String buildViolationQuery(final ViolationsListRequestDTO request) {
    List<String> clauses = baseClauses(request);
    addIfPresent(clauses, buildWaiverTypeClause(request == null ? null : request.waivedWithAutoWaiver));
    return String.join(" AND ", clauses);
  }

  /**
   * Same as {@link #buildViolationQuery} but with the waiver-type clause omitted. The waiver-type facet
   * is a single-select radio, so counting it against the fully-narrowed query would zero out (and hide)
   * the unselected option once the user picks one. Counting against this waiver-excluded query instead
   * keeps both AUTO and MANUAL showing the count the user would get if they switched (still narrowed by
   * every other active filter). See {@link ViolationsListFacetsBuilder#buildFacets}.
   */
  String buildViolationQueryExcludingWaiverType(final ViolationsListRequestDTO request) {
    return String.join(" AND ", baseClauses(request));
  }

  private List<String> baseClauses(final ViolationsListRequestDTO request) {
    List<String> clauses = new ArrayList<>();
    clauses.add(FieldIdentifier.ITEM_TYPE.label + ":" + ItemType.POLICY_VIOLATION.name());

    addIfPresent(clauses, buildSearchClause(request == null ? null : request.search));
    addIfPresent(clauses, buildDimensionClause(request));
    addIfPresent(clauses, buildStageClause(request == null ? null : request.stageIds));
    addIfPresent(clauses, buildThreatLevelClause(request == null ? null : request.policyThreatLevelRange));
    addIfPresent(clauses, buildThreatCategoryClause(request == null ? null : request.policyThreatCategories));
    addIfPresent(clauses, buildStateClause(request == null ? null : request.policyViolationStates));

    return clauses;
  }

  private String buildDimensionClause(final ViolationsListRequestDTO request) {
    Set<String> organizationIds = request == null ? null : request.organizationIds;
    if (organizationIds != null && !organizationIds.isEmpty()) {
      DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(organizationIds, "organizationIds");
    }
    String organizationClause = dimensionQueryBuilder.buildOrganizationFilterClause(organizationIds);
    String applicationClause =
        dimensionQueryBuilder.buildEscapedApplicationFilterClause(request == null ? null : request.applicationIds);
    if (organizationClause == null && applicationClause == null) {
      return null;
    }
    List<String> dimensionClauses = new ArrayList<>();
    if (organizationClause != null) {
      dimensionClauses.add(organizationClause);
    }
    if (applicationClause != null) {
      dimensionClauses.add(applicationClause);
    }
    // Org and app clauses are OR-ed (union). Note the root-organization interaction: a root org id
    // produces a null org clause ("all orgs"), so combining organizationIds=[ROOT] with
    // applicationIds=[X] yields just "(applicationId:X)" — the explicit application filter takes
    // precedence and narrows to X rather than widening back to all orgs. That matches user intent
    // (an explicit app filter should narrow) and RBAC still bounds the result either way.
    return "(" + String.join(" OR ", dimensionClauses) + ")";
  }

  private static String buildSearchClause(final String search) {
    if (StringUtils.isBlank(search)) {
      return null;
    }
    // Leading-wildcard clauses mirror the Applications list; index-side optimizations can follow
    // scale testing (CLM-42262).
    String[] tokens = search.trim().split("\\s+");
    List<String> tokenClauses = new ArrayList<>(tokens.length);
    for (String token : tokens) {
      if (StringUtils.isBlank(token)) {
        continue;
      }
      String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(token);
      tokenClauses.add("(" + String.join(
          " OR ",
          FieldIdentifier.COMPONENT_NAME.label + ":*" + safe + "*",
          FieldIdentifier.APPLICATION_NAME.label + ":*" + safe + "*",
          FieldIdentifier.APPLICATION_PUBLIC_ID.label + ":*" + safe + "*",
          FieldIdentifier.ORGANIZATION_NAME.label + ":*" + safe + "*",
          FieldIdentifier.POLICY_VIOLATION_POLICY_NAME.label + ":*" + safe + "*") + ")");
    }
    if (tokenClauses.isEmpty()) {
      return null;
    }
    if (tokenClauses.size() == 1) {
      return tokenClauses.get(0);
    }
    return "(" + String.join(" AND ", tokenClauses) + ")";
  }

  private static String buildStageClause(final Set<String> stageIds) {
    if (stageIds == null || stageIds.isEmpty()) {
      return null;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(stageIds, "stageIds");
    List<String> escaped = new ArrayList<>(stageIds.size());
    for (String stageId : DashboardIndexDimensionQueryBuilder.sortedCopy(stageIds)) {
      escaped.add(DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId));
    }
    return FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":(" + String.join(" ", escaped) + ")";
  }

  /** Policy threat levels are a fixed 0–10 domain; open-ended filter bounds clamp to it. */
  static final int MIN_THREAT_LEVEL = 0;

  static final int MAX_THREAT_LEVEL = 10;

  private static String buildThreatLevelClause(final PolicyThreatLevelFilter filter) {
    if (filter == null) {
      return null;
    }
    int min = filter.getMinPolicyThreatLevel();
    int max = filter.getMaxPolicyThreatLevel();
    if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
      return null;
    }
    // Replace the filter's Integer.MIN/MAX sentinels for an unset bound with the real threat-level
    // domain so the emitted range query stays within 0–10 instead of leaking 2147483647-style magic
    // numbers into Lucene.
    int lower = min == Integer.MIN_VALUE ? MIN_THREAT_LEVEL : min;
    int upper = max == Integer.MAX_VALUE ? MAX_THREAT_LEVEL : max;
    return FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label + ":[" + lower + " TO " + upper + "]";
  }

  private static String buildThreatCategoryClause(final PolicyThreatCategoryFilter filter) {
    if (filter == null || filter.getPolicyThreatCategories().isEmpty()) {
      return null;
    }
    List<String> names = new ArrayList<>();
    for (PolicyThreatCategory category : filter.getPolicyThreatCategories()) {
      names.add(DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(category.getName()));
    }
    return FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label + ":(" + String.join(" ", names) + ")";
  }

  private static String buildStateClause(final PolicyViolationStateFilter filter) {
    if (filter == null || filter.getPolicyViolationStates().isEmpty()) {
      return null;
    }
    Set<PolicyViolationState> states = filter.getPolicyViolationStates();
    boolean wantsOpen = states.contains(PolicyViolationState.OPEN);
    boolean wantsWaived = states.contains(PolicyViolationState.WAIVED);
    boolean wantsLegacy = states.contains(PolicyViolationState.LEGACY_VIOLATION);
    // Selecting all three states is the whole indexed domain, so no state narrowing is needed. (The
    // filter's set can only contain OPEN/WAIVED/LEGACY_VIOLATION.)
    if (wantsOpen && wantsWaived && wantsLegacy) {
      return null;
    }
    // Each selected state contributes a positive clause; OPEN is the complement of the excluded set so
    // the state FILTER agrees with the OPEN facet count (ViolationsListFacetsBuilder.countStates) and
    // the row-state derivation (ViolationWaiverStatus.toState): a violation with an absent/unknown
    // waiver status is OPEN on all three paths, and OPEN excludes Legacy on all three (or Legacy would
    // leak into OPEN). The excluded set is the shared ViolationWaiverStatus.openExclusionStatuses().
    List<String> stateClauses = new ArrayList<>();
    boolean orCombined = numberOfSelectedStates(wantsOpen, wantsWaived, wantsLegacy) > 1;
    if (wantsOpen) {
      stateClauses.add(openClause(orCombined));
    }
    if (wantsWaived) {
      stateClauses.add(waivedClause());
    }
    if (wantsLegacy) {
      stateClauses.add(legacyClause());
    }
    if (stateClauses.size() == 1) {
      return stateClauses.get(0);
    }
    return "(" + String.join(" OR ", stateClauses) + ")";
  }

  private static int numberOfSelectedStates(final boolean open, final boolean waived, final boolean legacy) {
    return (open ? 1 : 0) + (waived ? 1 : 0) + (legacy ? 1 : 0);
  }

  /**
   * The OPEN state clause: the complement of the excluded set
   * ({@link ViolationWaiverStatus#openExclusionStatuses()}). Shared with
   * {@link ViolationsListFacetsBuilder} so filter, facet count and row-state derivation cannot drift.
   * <p>
   * A bare {@code NOT (...)} is a pure-negative query with no positive anchor. It resolves correctly
   * only when AND-combined with a positive clause (the {@code itemType} base clause, or the OPEN facet's
   * {@code violationQuery}). When OR-combined with another state clause it must carry its own anchor, or
   * Lucene parses the OR into a BooleanQuery whose only positive term is the sibling SHOULD, yielding
   * zero hits ({@code [OPEN, WAIVED]}/{@code [OPEN, LEGACY]} returned empty). {@code anchored=true}
   * prepends {@code *:*} so the negation stands alone inside an OR.
   */
  static String openClause(final boolean anchored) {
    String negation =
        "NOT (" + FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
            + ViolationWaiverStatus.openExclusionStatuses() + "))";
    return anchored ? "(*:* AND " + negation + ")" : negation;
  }

  /** The WAIVED state clause ({@code :(Waived AutoWaived)}). Shared with {@link ViolationsListFacetsBuilder}. */
  static String waivedClause() {
    return FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
        + ViolationWaiverStatus.WAIVED + " " + ViolationWaiverStatus.AUTO_WAIVED + ")";
  }

  /** The LEGACY state clause ({@code :(Legacy)}). Shared with {@link ViolationsListFacetsBuilder}. */
  static String legacyClause() {
    return waiverStatusClause(ViolationWaiverStatus.LEGACY);
  }

  /**
   * Waiver-type filter (CLM-42261). {@code true} narrows to auto-waived violations
   * ({@code policyViolationWaiverStatus:(AutoWaived)}); {@code false} narrows to manually-waived only
   * ({@code :(Waived)}); {@code null} adds no clause. Both non-null values imply the WAIVED state, so an
   * AND with {@code policyViolationStates:[OPEN]} correctly yields an empty result.
   */
  private static String buildWaiverTypeClause(final Boolean waivedWithAutoWaiver) {
    if (waivedWithAutoWaiver == null) {
      return null;
    }
    String status = waivedWithAutoWaiver ? ViolationWaiverStatus.AUTO_WAIVED : ViolationWaiverStatus.WAIVED;
    return waiverStatusClause(status);
  }

  /**
   * Single-status waiver clause ({@code policyViolationWaiverStatus:(<status>)}). Shared with
   * {@link ViolationsListFacetsBuilder} so the waiver-type filter query and the waiver-type facet-count
   * queries cannot drift apart.
   * <p>
   * The status is a compile-time constant ("Waived"/"AutoWaived") with no Lucene-special characters, so
   * it is intentionally inlined without escapeLuceneTerm — matching {@link #buildStateClause} (only the
   * user-supplied threat-category terms in {@link #buildThreatCategoryClause} need escaping).
   */
  static String waiverStatusClause(final String waiverStatus) {
    return FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":(" + waiverStatus + ")";
  }

  private static void addIfPresent(final List<String> clauses, final String clause) {
    if (clause != null) {
      clauses.add(clause);
    }
  }
}
