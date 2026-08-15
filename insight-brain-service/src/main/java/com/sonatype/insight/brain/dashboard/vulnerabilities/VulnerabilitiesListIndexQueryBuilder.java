/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.utils.CvssV3Severity;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds RBAC-scoped Lucene queries for the Martha Vulnerabilities My Scan Data list.
 * <p>
 * The base clause is {@code itemType:SECURITY_VULNERABILITY}; RBAC is applied programmatically by the
 * search client.
 */
@Named
@Singleton
final class VulnerabilitiesListIndexQueryBuilder
{
  /**
   * A filter dimension that the facet builder can drop from the query so that dimension's own
   * sibling buckets stay visible while every other active filter still applies.
   */
  enum FacetDimension
  {
    NONE,
    SEVERITY,
    ECOSYSTEM,
    STAGE,
    ORGANIZATION,
    APPLICATION,
    /** Organization and application together: they are one owner dimension, dropped as a unit. */
    OWNER_GROUP
  }

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Inject
  VulnerabilitiesListIndexQueryBuilder(final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder) {
    this.dimensionQueryBuilder = dimensionQueryBuilder;
  }

  String buildMyScanDataQuery(final VulnerabilitiesListRequestDTO request) {
    return buildMyScanDataQuery(request, FacetDimension.NONE);
  }

  /**
   * Builds the same query as {@link #buildMyScanDataQuery(VulnerabilitiesListRequestDTO)}
   * but omits the owner-dimension clauses (organization + application filters).
   * Used as the base for owner facet aggregations so selecting an org or app does not collapse
   * the org/app rails.
   */
  String buildVulnerabilityQueryWithoutOwner(final VulnerabilitiesListRequestDTO request) {
    return buildMyScanDataQuery(request, FacetDimension.OWNER_GROUP);
  }

  /**
   * Exact vulnerabilityId filter for Impact-on-portfolio affected-app collection.
   */
  String buildAffectedApplicationsQuery(final String vulnerabilityId) {
    String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(vulnerabilityId.trim());
    return FieldIdentifier.ITEM_TYPE.label + ":" + ItemType.SECURITY_VULNERABILITY.name()
        + " AND " + FieldIdentifier.VULNERABILITY_ID.label + ":" + safe;
  }

  /**
   * @param omitted the dimension to leave out, so a facet count for that dimension still reflects
   *          every other active filter. The CVSS clamp travels with {@code SEVERITY} because both
   *          narrow the same {@code vulnerabilitySeverity} field.
   */
  String buildMyScanDataQuery(
      final VulnerabilitiesListRequestDTO request,
      final FacetDimension omitted)
  {
    List<String> clauses = new ArrayList<>();
    clauses.add(FieldIdentifier.ITEM_TYPE.label + ":" + ItemType.SECURITY_VULNERABILITY.name());
    addIfPresent(clauses, buildSearchClause(request == null ? null : request.search));
    if (omitted != FacetDimension.SEVERITY) {
      addIfPresent(clauses, buildSeverityClause(request == null ? null : request.severities));
      addIfPresent(
          clauses,
          buildCvssRangeClause(
              request == null ? null : request.minCvssScore,
              request == null ? null : request.maxCvssScore));
    }
    if (omitted != FacetDimension.ECOSYSTEM) {
      addIfPresent(clauses, buildEcosystemClause(request == null ? null : request.ecosystems));
    }
    // Organization and application scope is not query text: it travels as budget-exempt term-set
    // restrictions, and organization and application are ONE owner dimension that is omitted as a unit
    // (see buildScopeRestrictions and FacetDimension.OWNER_GROUP).
    if (omitted != FacetDimension.STAGE) {
      addIfPresent(clauses, buildStageClause(request == null ? null : request.stageIds));
    }
    return String.join(" AND ", clauses);
  }

  /**
   * AND-style scope restrictions for the vulnerabilities list (org narrows AND app narrows independently).
   * Respects {@code omitted} so facet per-dimension counts remain correct.
   */
  List<IndexFilterRestriction> buildScopeRestrictions(
      final VulnerabilitiesListRequestDTO request,
      final FacetDimension omitted)
  {
    // OWNER_GROUP omits organization AND application together: they are one dimension, so an owner-facet
    // base must withhold both or selecting an org would still collapse the application rail.
    boolean omitOwnerGroup = omitted == FacetDimension.OWNER_GROUP;
    Set<String> orgIds = (omitted == FacetDimension.ORGANIZATION || omitOwnerGroup || request == null)
        ? null
        : request.organizationIds;
    Set<String> appIds = (omitted == FacetDimension.APPLICATION || omitOwnerGroup || request == null)
        ? null
        : request.applicationIds;
    // Organization and application are ONE owner dimension, so the two term sets are OR-ed rather than
    // ANDed: selecting an organization plus an application in a different subtree returns the union,
    // matching Classic resolution in ApplicationService#getAppsByIds. An intersection would be empty.
    return dimensionQueryBuilder.buildScopeFilterRestrictions(orgIds, appIds);
  }

  List<IndexFilterRestriction> buildScopeRestrictions(final VulnerabilitiesListRequestDTO request) {
    return buildScopeRestrictions(request, FacetDimension.NONE);
  }

  private static String buildStageClause(final Set<String> stageIds) {
    if (stageIds == null || stageIds.isEmpty()) {
      return null;
    }
    List<String> escapedIds = new ArrayList<>(stageIds.size());
    for (String stageId : DashboardIndexDimensionQueryBuilder.sortedCopy(stageIds)) {
      if (StringUtils.isBlank(stageId)) {
        continue;
      }
      escapedIds.add(DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId.trim()));
    }
    if (escapedIds.isEmpty()) {
      return null;
    }
    return FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":(" + String.join(" ", escapedIds) + ")";
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
          FieldIdentifier.VULNERABILITY_ID.label + ":*" + safe + "*",
          FieldIdentifier.VULNERABILITY_DESCRIPTION.label + ":*" + safe + "*",
          FieldIdentifier.COMPONENT_NAME.label + ":*" + safe + "*") + ")");
    }
    if (tokenClauses.isEmpty()) {
      return null;
    }
    if (tokenClauses.size() == 1) {
      return tokenClauses.get(0);
    }
    return "(" + String.join(" AND ", tokenClauses) + ")";
  }

  private static String buildSeverityClause(final Set<String> severities) {
    if (severities == null || severities.isEmpty()) {
      return null;
    }
    List<String> bandClauses = new ArrayList<>(severities.size());
    for (String token : severities) {
      if (StringUtils.isBlank(token)) {
        continue;
      }
      CvssV3Severity band = VulnerabilitiesListRequestValidator.toCvssV3Severity(token);
      bandClauses.add(severityRangeClause(band.getStartScoreRange(), band.getEndScoreRange()));
    }
    if (bandClauses.isEmpty()) {
      return null;
    }
    if (bandClauses.size() == 1) {
      return bandClauses.get(0);
    }
    return "(" + String.join(" OR ", bandClauses) + ")";
  }

  private static String buildCvssRangeClause(final Float minCvssScore, final Float maxCvssScore) {
    if (minCvssScore == null && maxCvssScore == null) {
      return null;
    }
    float min = minCvssScore == null ? VulnerabilitiesListRequestValidator.CVSS_MIN : minCvssScore;
    float max = maxCvssScore == null ? VulnerabilitiesListRequestValidator.CVSS_MAX : maxCvssScore;
    if (min <= VulnerabilitiesListRequestValidator.CVSS_MIN
        && max >= VulnerabilitiesListRequestValidator.CVSS_MAX)
    {
      return null;
    }
    return severityRangeClause(min, max);
  }

  private static String severityRangeClause(final float min, final float max) {
    return FieldIdentifier.VULNERABILITY_SEVERITY.label + ":[" + min + " TO " + max + "]";
  }

  private static String buildEcosystemClause(final Set<String> ecosystems) {
    if (ecosystems == null || ecosystems.isEmpty()) {
      return null;
    }
    List<String> formatClauses = new ArrayList<>(ecosystems.size());
    for (String ecosystem : ecosystems) {
      if (StringUtils.isBlank(ecosystem)) {
        continue;
      }
      String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(ecosystem.trim().toLowerCase(Locale.ROOT));
      formatClauses.add(FieldIdentifier.COMPONENT_FORMAT.label + ":" + safe);
    }
    if (formatClauses.isEmpty()) {
      return null;
    }
    if (formatClauses.size() == 1) {
      return formatClauses.get(0);
    }
    return "(" + String.join(" OR ", formatClauses) + ")";
  }

  private static void addIfPresent(final List<String> clauses, final String clause) {
    if (clause != null) {
      clauses.add(clause);
    }
  }
}
