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
import com.sonatype.insight.brain.dashboard.PolicyViolationIndexClauses;
import com.sonatype.insight.brain.dashboard.ViolationWaiverStatus;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
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
    addIfPresent(clauses, buildComponentHashClause(request == null ? null : request.componentHash));
    addIfPresent(clauses, buildDimensionClause(request));
    addIfPresent(clauses, buildStageClause(request == null ? null : request.stageIds));
    addIfPresent(clauses, buildThreatLevelClause(request == null ? null : request.policyThreatLevelRange));
    addIfPresent(clauses, buildThreatCategoryClause(request == null ? null : request.policyThreatCategories));
    addIfPresent(clauses, buildStateClause(request == null ? null : request.policyViolationStates));

    return clauses;
  }

  private static String buildComponentHashClause(final String componentHash) {
    if (StringUtils.isBlank(componentHash)) {
      return null;
    }
    String trimmed = componentHash.trim();
    String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(trimmed);
    return FieldIdentifier.COMPONENT_HASH.label + ":" + safe;
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
    return PolicyViolationIndexClauses.threatCategoryClause(filter);
  }

  private static String buildStateClause(final PolicyViolationStateFilter filter) {
    return PolicyViolationIndexClauses.stateClause(filter);
  }

  /** @see PolicyViolationIndexClauses#openClause(boolean) */
  static String openClause(final boolean anchored) {
    return PolicyViolationIndexClauses.openClause(anchored);
  }

  /** @see PolicyViolationIndexClauses#waivedClause() */
  static String waivedClause() {
    return PolicyViolationIndexClauses.waivedClause();
  }

  /** @see PolicyViolationIndexClauses#legacyClause() */
  static String legacyClause() {
    return PolicyViolationIndexClauses.legacyClause();
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

  /** @see PolicyViolationIndexClauses#waiverStatusClause(String) */
  static String waiverStatusClause(final String waiverStatus) {
    return PolicyViolationIndexClauses.waiverStatusClause(waiverStatus);
  }

  private static void addIfPresent(final List<String> clauses, final String clause) {
    if (clause != null) {
      clauses.add(clause);
    }
  }
}
