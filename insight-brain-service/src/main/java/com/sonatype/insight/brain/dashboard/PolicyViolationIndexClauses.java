/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.search.index.FieldIdentifier;

/**
 * Lucene clause builders for policy-type and violation-state filters over {@code POLICY_VIOLATION}
 * documents, shared by every Martha list that scopes results through violation docs.
 * <p>
 * These clauses were originally private to the Violations list. They live here because the
 * Applications list resolves the same two filters through violation-scoped discovery (CLM-43211), and
 * the OPEN clause in particular encodes a correctness rule that must not be re-derived per list: OPEN
 * is the <em>complement</em> of {@link ViolationWaiverStatus#openExclusionStatuses()}, so the filter,
 * the facet count, and the row-state derivation ({@link ViolationWaiverStatus#toState}) all agree that
 * an absent or unknown waiver status is OPEN and that Legacy never leaks into OPEN.
 */
public final class PolicyViolationIndexClauses
{
  private PolicyViolationIndexClauses() {
  }

  /**
   * Policy-type filter ({@code policyViolationThreatCategory:(security license)}). Category names are
   * user-supplied via the API enum, so they are escaped.
   */
  public static String threatCategoryClause(final PolicyThreatCategoryFilter filter) {
    if (filter == null || filter.getPolicyThreatCategories().isEmpty()) {
      return null;
    }
    List<String> names = new ArrayList<>();
    for (PolicyThreatCategory category : filter.getPolicyThreatCategories()) {
      names.add(DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(category.getName()));
    }
    return FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label + ":(" + String.join(" ", names) + ")";
  }

  /**
   * Violation-state filter over {@code policyViolationWaiverStatus}. Returns {@code null} when the
   * filter selects the whole indexed domain (all three states) and therefore narrows nothing.
   */
  public static String stateClause(final PolicyViolationStateFilter filter) {
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
    // the state FILTER agrees with the OPEN facet count and the row-state derivation
    // (ViolationWaiverStatus.toState): a violation with an absent/unknown waiver status is OPEN on all
    // three paths, and OPEN excludes Legacy on all three (or Legacy would leak into OPEN).
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
   * The OPEN state clause: the complement of {@link ViolationWaiverStatus#openExclusionStatuses()}.
   * <p>
   * A bare {@code NOT (...)} is a pure-negative query with no positive anchor. It resolves correctly
   * only when AND-combined with a positive clause (the {@code itemType} base clause, or the OPEN facet's
   * {@code violationQuery}). When OR-combined with another state clause it must carry its own anchor, or
   * Lucene parses the OR into a BooleanQuery whose only positive term is the sibling SHOULD, yielding
   * zero hits ({@code [OPEN, WAIVED]}/{@code [OPEN, LEGACY]} returned empty). {@code anchored=true}
   * prepends {@code *:*} so the negation stands alone inside an OR.
   */
  public static String openClause(final boolean anchored) {
    String negation =
        "NOT (" + FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
            + ViolationWaiverStatus.openExclusionStatuses() + "))";
    return anchored ? "(*:* AND " + negation + ")" : negation;
  }

  /** The WAIVED state clause ({@code :(Waived AutoWaived)}). */
  public static String waivedClause() {
    return FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
        + ViolationWaiverStatus.WAIVED + " " + ViolationWaiverStatus.AUTO_WAIVED + ")";
  }

  /** The LEGACY state clause ({@code :(Legacy)}). */
  public static String legacyClause() {
    return waiverStatusClause(ViolationWaiverStatus.LEGACY);
  }

  /**
   * Single-status waiver clause ({@code policyViolationWaiverStatus:(<status>)}), so the waiver-type
   * filter query and the waiver-type facet-count queries cannot drift apart.
   * <p>
   * The status is a compile-time constant ("Waived"/"AutoWaived") with no Lucene-special characters, so
   * it is intentionally inlined without escapeLuceneTerm — matching {@link #stateClause} (only the
   * user-supplied threat-category terms in {@link #threatCategoryClause} need escaping).
   */
  public static String waiverStatusClause(final String waiverStatus) {
    return FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":(" + waiverStatus + ")";
  }
}
