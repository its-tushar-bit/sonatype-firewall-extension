/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Shared Lucene dimension clauses for dashboard index queries (metrics + Martha applications list).
 */
@Named
public class DashboardIndexDimensionQueryBuilder
{
  /**
   * Sentinel org id for a filter that must match zero APPLICATION docs. Valid under metric id
   * patterns but never indexed as an organization owner.
   */
  public static final String NO_MATCH_ORGANIZATION_FILTER_ID = "__no_match__";

  /**
   * Sentinel application id for a filter that must match zero docs. Distinct name from
   * {@link #NO_MATCH_ORGANIZATION_FILTER_ID} even though the wire value is identical — the two
   * sentinels apply to different Lucene fields ({@code applicationId} vs {@code organizationId}).
   */
  public static final String NO_MATCH_APPLICATION_FILTER_ID = "__no_match__";

  private static final Pattern LUCENE_SPECIAL_CHARS = Pattern.compile("[+\\-!(){}\\[\\]^\"~*?:\\\\/]");

  private final OrganizationDAO organizationDAO;

  private final Configuration configuration;

  @Inject
  public DashboardIndexDimensionQueryBuilder(
      final OrganizationDAO organizationDAO,
      final Configuration configuration)
  {
    this.organizationDAO = organizationDAO;
    this.configuration = configuration;
  }

  public String buildOrganizationFilterClause(final Set<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return null;
    }
    if (organizationIds.contains(Organization.ROOT_ORGANIZATION_ID)) {
      return null;
    }
    Set<String> expandedOrgIds = organizationDAO.getAllChildOrganizationIds(organizationIds);
    if (expandedOrgIds.isEmpty()) {
      return "organizationId:(" + NO_MATCH_ORGANIZATION_FILTER_ID + ")";
    }
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (expandedOrgIds.size() > maxClauseCount) {
      throw new BadRequestException(
          "Organization filter expands to too many organizations (max " + maxClauseCount + ").");
    }
    return "organizationId:(" + String.join(" ", sortedCopy(expandedOrgIds)) + ")";
  }

  /**
   * Builds a Lucene organization filter clause per input id using a single descendant-expansion
   * query. Root organization ids and blank ids are omitted from the result (same skip semantics as
   * a {@code null} return from {@link #buildOrganizationFilterClause} for those keys).
   * <p>
   * Unlike {@link #buildOrganizationFilterClause} (used for deliberate org filter selection), this
   * method soft-skips any org whose expanded descendant set exceeds
   * {@code maxAdvancedSearchClauseCount}: the id is omitted from the returned map rather than
   * throwing. Callers (facet search / facet discovery) treat a missing clause as "skip this org" so
   * one oversized hierarchy cannot 400 the whole list request.
   */
  public Map<String, String> buildOrganizationFilterClausesById(final Collection<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return Map.of();
    }
    Set<String> toExpand = new LinkedHashSet<>();
    for (String organizationId : organizationIds) {
      if (StringUtils.isBlank(organizationId)
          || Organization.ROOT_ORGANIZATION_ID.equals(organizationId))
      {
        continue;
      }
      toExpand.add(organizationId);
    }
    if (toExpand.isEmpty()) {
      return Map.of();
    }
    Map<String, Set<String>> expandedByAncestor =
        organizationDAO.getChildOrganizationIdsGroupedByAncestor(toExpand);
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    Map<String, String> clauses = new LinkedHashMap<>(toExpand.size());
    for (String organizationId : toExpand) {
      Set<String> expandedOrgIds = expandedByAncestor.getOrDefault(organizationId, Set.of());
      if (expandedOrgIds.isEmpty()) {
        clauses.put(organizationId, "organizationId:(" + NO_MATCH_ORGANIZATION_FILTER_ID + ")");
        continue;
      }
      if (expandedOrgIds.size() > maxClauseCount) {
        // Soft-skip: facet paths must not abort the list because one named org is too wide.
        continue;
      }
      clauses.put(organizationId, "organizationId:(" + String.join(" ", sortedCopy(expandedOrgIds)) + ")");
    }
    return clauses;
  }

  /** Metrics-style application clause (sorted ids, no escaping or size cap). */
  public String buildApplicationFilterClause(final Set<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return null;
    }
    return "applicationId:(" + String.join(" ", sortedCopy(applicationIds)) + ")";
  }

  public String buildPolicyEvaluationStageFilterClause(final Set<String> stageIds) {
    return buildKeywordSetClause(FieldIdentifier.POLICY_EVALUATION_STAGE.label, stageIds);
  }

  public String buildApplicationViolationStageFilterClause(final Set<String> stageIds) {
    return buildKeywordSetClause(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, stageIds);
  }

  /**
   * Martha list application clause: rejects blank ids, caps clause count, and escapes Lucene terms.
   */
  public String buildEscapedApplicationFilterClause(final Set<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return null;
    }
    rejectBlankFilterIds(applicationIds, "applicationIds");
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (applicationIds.size() > maxClauseCount) {
      throw new BadRequestException(
          "Application filter contains too many ids (max " + maxClauseCount + ").");
    }
    List<String> escapedIds = new ArrayList<>(applicationIds.size());
    for (String applicationId : sortedCopy(applicationIds)) {
      escapedIds.add(escapeLuceneTerm(applicationId));
    }
    return "applicationId:(" + String.join(" ", escapedIds) + ")";
  }

  /**
   * Mirrors {@code escapeLuceneTerm} in nosc/search/useGlobalSearch.ts for index substring queries.
   */
  public static String escapeLuceneTerm(final String input) {
    String escapedSpecials = LUCENE_SPECIAL_CHARS.matcher(input).replaceAll("\\\\$0");
    return escapedSpecials.replace("&&", "\\&\\&").replace("||", "\\|\\|");
  }

  private static String buildKeywordSetClause(final String fieldName, final Set<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    List<String> escapedIds = new ArrayList<>(ids.size());
    for (String id : sortedCopy(ids)) {
      if (StringUtils.isNotBlank(id)) {
        escapedIds.add(escapeLuceneTerm(id));
      }
    }
    if (escapedIds.isEmpty()) {
      return null;
    }
    return fieldName + ":(" + String.join(" ", escapedIds) + ")";
  }

  public static void rejectBlankFilterIds(final Set<String> ids, final String fieldName) {
    for (String id : ids) {
      if (StringUtils.isBlank(id)) {
        throw new BadRequestException("Invalid " + fieldName + " filter id.");
      }
    }
  }

  public static List<String> sortedCopy(final Set<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return new ArrayList<>(new TreeSet<>(ids));
  }
}
