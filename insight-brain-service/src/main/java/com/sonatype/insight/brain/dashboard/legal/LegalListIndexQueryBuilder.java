/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds RBAC-scoped Lucene queries for the Nexus One Legal list.
 * <p>
 * The base clause is {@code itemType:LEGAL_VIOLATION} plus non-blank identity fields so
 * {@code total}/{@code hasNextPage} only count docs that can become list rows. RBAC is applied
 * programmatically by the search client (see {@code SearchIndexClient}). License threat group
 * names are quoted phrases so multi-word values (e.g. {@code Weak Copyleft}) match exactly.
 * Stage filters are restricted to dashboard-licensed stages — the same domain as stage facets.
 */
@Named
@Singleton
final class LegalListIndexQueryBuilder
{
  /** License threat levels are a fixed 0–10 domain; open-ended filter bounds clamp to it. */
  static final int MIN_THREAT_LEVEL = 0;

  static final int MAX_THREAT_LEVEL = 10;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final StageTypeService stageTypeService;

  @Inject
  LegalListIndexQueryBuilder(
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      final StageTypeService stageTypeService)
  {
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.stageTypeService = stageTypeService;
  }

  String buildLegalQuery(final LegalListRequestDTO request) {
    return String.join(" AND ", baseClauses(request));
  }

  private List<String> baseClauses(final LegalListRequestDTO request) {
    List<String> clauses = new ArrayList<>();
    clauses.add(FieldIdentifier.ITEM_TYPE.label + ":" + ItemType.LEGAL_VIOLATION.name());
    // Align counted hits with row-eligibility in LegalListIndexItems.compositeLegalFindingId.
    clauses.add(FieldIdentifier.APPLICATION_ID.label + ":[* TO *]");
    clauses.add(FieldIdentifier.COMPONENT_HASH.label + ":[* TO *]");
    clauses.add(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID.label + ":[* TO *]");
    clauses.add(FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":[* TO *]");

    addIfPresent(clauses, buildSearchClause(request == null ? null : request.search));
    addIfPresent(clauses, buildDimensionClause(request));
    addIfPresent(clauses, buildStageClause(request == null ? null : request.stageIds));
    addIfPresent(clauses, buildLicenseThreatGroupClause(request == null ? null : request.licenseThreatGroupNames));
    addIfPresent(clauses, buildThreatLevelClause(request == null ? null : request.licenseThreatLevelRange));

    return clauses;
  }

  private String buildDimensionClause(final LegalListRequestDTO request) {
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
    return "(" + String.join(" OR ", dimensionClauses) + ")";
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
          FieldIdentifier.COMPONENT_NAME.label + ":*" + safe + "*",
          FieldIdentifier.APPLICATION_NAME.label + ":*" + safe + "*",
          FieldIdentifier.APPLICATION_PUBLIC_ID.label + ":*" + safe + "*",
          FieldIdentifier.ORGANIZATION_NAME.label + ":*" + safe + "*",
          FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME.label + ":*" + safe + "*",
          FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label + ":*" + safe + "*") + ")");
    }
    if (tokenClauses.isEmpty()) {
      return null;
    }
    if (tokenClauses.size() == 1) {
      return tokenClauses.get(0);
    }
    return "(" + String.join(" AND ", tokenClauses) + ")";
  }

  private String buildStageClause(final Set<String> stageIds) {
    if (stageIds == null || stageIds.isEmpty()) {
      return null;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(stageIds, "stageIds");
    Set<String> licensedStageIds = stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)
        .stream()
        .map(StageType::getId)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());
    for (String stageId : stageIds) {
      if (!licensedStageIds.contains(stageId)) {
        throw new BadRequestException(
            "Invalid stageId: " + stageId + ". Must be a dashboard-licensed stage.");
      }
    }
    List<String> escaped = new ArrayList<>(stageIds.size());
    for (String stageId : DashboardIndexDimensionQueryBuilder.sortedCopy(stageIds)) {
      escaped.add(DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId));
    }
    return FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":(" + String.join(" ", escaped) + ")";
  }

  private static String buildLicenseThreatGroupClause(final Set<String> licenseThreatGroupNames) {
    if (licenseThreatGroupNames == null || licenseThreatGroupNames.isEmpty()) {
      return null;
    }
    DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(licenseThreatGroupNames, "licenseThreatGroupNames");
    List<String> phrases = new ArrayList<>(licenseThreatGroupNames.size());
    for (String name : DashboardIndexDimensionQueryBuilder.sortedCopy(licenseThreatGroupNames)) {
      phrases.add(quotedPhrase(name));
    }
    return FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label + ":(" + String.join(" ", phrases) + ")";
  }

  /** Exact-match phrase for multi-word LTG display names (spaces would otherwise OR-split). */
  static String quotedPhrase(final String value) {
    return "\"" + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(value) + "\"";
  }

  private static String buildThreatLevelClause(final PolicyThreatLevelFilter filter) {
    if (filter == null) {
      return null;
    }
    int min = filter.getMinPolicyThreatLevel();
    int max = filter.getMaxPolicyThreatLevel();
    if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
      return null;
    }
    int lower = min == Integer.MIN_VALUE ? MIN_THREAT_LEVEL : min;
    int upper = max == Integer.MAX_VALUE ? MAX_THREAT_LEVEL : max;
    return FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label + ":[" + lower + " TO " + upper + "]";
  }

  private static void addIfPresent(final List<String> clauses, final String clause) {
    if (clause != null) {
      clauses.add(clause);
    }
  }
}
