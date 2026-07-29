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
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
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
  String buildMyScanDataQuery(final VulnerabilitiesListRequestDTO request) {
    return buildMyScanDataQuery(request, true, true, true);
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
   * @param includeSeverity when false, omits severity-band OR clauses (for severity facet counts)
   * @param includeCvss when false, omits min/max CVSS clamp
   * @param includeEcosystems when false, omits ecosystem OR clauses (for ecosystem facet discovery)
   */
  String buildMyScanDataQuery(
      final VulnerabilitiesListRequestDTO request,
      final boolean includeSeverity,
      final boolean includeCvss,
      final boolean includeEcosystems)
  {
    List<String> clauses = new ArrayList<>();
    clauses.add(FieldIdentifier.ITEM_TYPE.label + ":" + ItemType.SECURITY_VULNERABILITY.name());
    addIfPresent(clauses, buildSearchClause(request == null ? null : request.search));
    if (includeSeverity) {
      addIfPresent(clauses, buildSeverityClause(request == null ? null : request.severities));
    }
    if (includeCvss) {
      addIfPresent(
          clauses,
          buildCvssRangeClause(
              request == null ? null : request.minCvssScore,
              request == null ? null : request.maxCvssScore));
    }
    if (includeEcosystems) {
      addIfPresent(clauses, buildEcosystemClause(request == null ? null : request.ecosystems));
    }
    return String.join(" AND ", clauses);
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
