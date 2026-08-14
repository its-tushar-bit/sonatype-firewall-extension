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
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexOrTermSetGroup;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Shared organization/application scope helpers for dashboard index queries (metrics + Martha lists).
 * <p>
 * Preferred path (CLM-44783): {@link #expandOrganizationFilterIds}, {@link #applicationFilterIds}, and
 * {@link #buildScopeFilterRestrictions} — budget-exempt term sets (size is not charged against
 * {@code maxAdvancedSearchClauseCount}). Deprecated string clause builders still enforce that cap so
 * transitional Lucene-string callers cannot compose an unsafely large bool query.
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

  /**
   * Expanded organization ids for a budget-exempt term-set filter.
   * <p>
   * {@code null} means unrestricted (null/empty input, or root present). Empty expansion yields a
   * singleton no-match sentinel. Size is not charged against {@code maxAdvancedSearchClauseCount}.
   */
  public Set<String> expandOrganizationFilterIds(final Set<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return null;
    }
    if (organizationIds.contains(Organization.ROOT_ORGANIZATION_ID)) {
      return null;
    }
    Set<String> expandedOrgIds = organizationDAO.getAllChildOrganizationIds(organizationIds);
    if (expandedOrgIds.isEmpty()) {
      return Set.of(NO_MATCH_ORGANIZATION_FILTER_ID);
    }
    return Set.copyOf(expandedOrgIds);
  }

  /**
   * Normalized application ids for a budget-exempt term-set filter, or {@code null} when absent.
   * Rejects blank ids. Size is not charged against {@code maxAdvancedSearchClauseCount}.
   */
  public Set<String> applicationFilterIds(final Set<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return null;
    }
    rejectBlankFilterIds(applicationIds, "applicationIds");
    return Set.copyOf(applicationIds);
  }

  /**
   * Classic-union scope restrictions: org-only or app-only is a single term set; both present is an
   * {@link IndexOrTermSetGroup} (OR). Empty list means unrestricted.
   */
  public List<IndexFilterRestriction> buildScopeFilterRestrictions(
      final Set<String> organizationIds,
      final Set<String> applicationIds)
  {
    Set<String> expandedOrgs = expandOrganizationFilterIds(organizationIds);
    Set<String> apps = applicationFilterIds(applicationIds);
    if (expandedOrgs == null && apps == null) {
      return List.of();
    }
    if (expandedOrgs != null && apps == null) {
      return IndexTermSetRestriction.singleton(FieldIdentifier.ORGANIZATION_ID.label, expandedOrgs);
    }
    if (expandedOrgs == null) {
      return IndexTermSetRestriction.singleton(FieldIdentifier.APPLICATION_ID.label, apps);
    }
    return IndexOrTermSetGroup.singleton(
        IndexTermSetRestriction.of(FieldIdentifier.ORGANIZATION_ID.label, expandedOrgs),
        IndexTermSetRestriction.of(FieldIdentifier.APPLICATION_ID.label, apps));
  }

  /**
   * AND semantics for org + app (Vulnerabilities list): each present dimension is its own term set.
   */
  public List<IndexFilterRestriction> buildScopeFilterRestrictionsAnd(
      final Set<String> organizationIds,
      final Set<String> applicationIds)
  {
    List<IndexFilterRestriction> restrictions = new ArrayList<>(2);
    Set<String> expandedOrgs = expandOrganizationFilterIds(organizationIds);
    if (expandedOrgs != null) {
      restrictions.add(IndexTermSetRestriction.of(FieldIdentifier.ORGANIZATION_ID.label, expandedOrgs));
    }
    Set<String> apps = applicationFilterIds(applicationIds);
    if (apps != null) {
      restrictions.add(IndexTermSetRestriction.of(FieldIdentifier.APPLICATION_ID.label, apps));
    }
    return List.copyOf(restrictions);
  }

  /**
   * Expanded org ids per input id for facet sibling counts. Soft-skips any org whose expanded set
   * exceeds {@code maxAdvancedSearchClauseCount} (safety ceiling; not a Lucene bool-clause charge).
   * Root and blank ids are omitted.
   */
  public Map<String, Set<String>> expandOrganizationFilterIdsById(final Collection<String> organizationIds) {
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
    Map<String, Set<String>> byId = new LinkedHashMap<>(toExpand.size());
    for (String organizationId : toExpand) {
      Set<String> expandedOrgIds = expandedByAncestor.getOrDefault(organizationId, Set.of());
      if (expandedOrgIds.isEmpty()) {
        byId.put(organizationId, Set.of(NO_MATCH_ORGANIZATION_FILTER_ID));
        continue;
      }
      if (maxClauseCount > 0 && expandedOrgIds.size() > maxClauseCount) {
        // Soft-skip: facet paths must not abort the list because one named org is too wide.
        continue;
      }
      byId.put(organizationId, Set.copyOf(expandedOrgIds));
    }
    return byId;
  }

  /**
   * @deprecated Prefer {@link #expandOrganizationFilterIds} / {@link #buildScopeFilterRestrictions}.
   *             Still enforces {@code maxAdvancedSearchClauseCount} because the result is inlined into
   *             the Lucene string query.
   */
  @Deprecated
  public String buildOrganizationFilterClause(final Set<String> organizationIds) {
    Set<String> expandedOrgIds = expandOrganizationFilterIds(organizationIds);
    if (expandedOrgIds == null) {
      return null;
    }
    rejectIfExceedsClauseBudget(expandedOrgIds.size(), "Organization filter expands to too many organizations");
    return "organizationId:(" + String.join(" ", sortedCopy(expandedOrgIds)) + ")";
  }

  /**
   * @deprecated Prefer {@link #expandOrganizationFilterIdsById}.
   */
  @Deprecated
  public Map<String, String> buildOrganizationFilterClausesById(final Collection<String> organizationIds) {
    Map<String, Set<String>> expandedById = expandOrganizationFilterIdsById(organizationIds);
    Map<String, String> clauses = new LinkedHashMap<>(expandedById.size());
    for (Map.Entry<String, Set<String>> entry : expandedById.entrySet()) {
      clauses.put(entry.getKey(), "organizationId:(" + String.join(" ", sortedCopy(entry.getValue())) + ")");
    }
    return clauses;
  }

  /**
   * Metrics-style application clause (sorted ids, no escaping). Prefer {@link #applicationFilterIds}.
   *
   * @deprecated Prefer term-set scope. Still enforces {@code maxAdvancedSearchClauseCount}.
   */
  @Deprecated
  public String buildApplicationFilterClause(final Set<String> applicationIds) {
    Set<String> ids = applicationFilterIds(applicationIds);
    if (ids == null) {
      return null;
    }
    rejectIfExceedsClauseBudget(ids.size(), "Application filter contains too many ids");
    return "applicationId:(" + String.join(" ", sortedCopy(ids)) + ")";
  }

  public String buildPolicyEvaluationStageFilterClause(final Set<String> stageIds) {
    return buildKeywordSetClause(FieldIdentifier.POLICY_EVALUATION_STAGE.label, stageIds);
  }

  public String buildApplicationViolationStageFilterClause(final Set<String> stageIds) {
    return buildKeywordSetClause(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, stageIds);
  }

  /**
   * @deprecated Prefer {@link #applicationFilterIds}. Blank rejection preserved; still enforces
   *             {@code maxAdvancedSearchClauseCount} for Lucene-string callers.
   */
  @Deprecated
  public String buildEscapedApplicationFilterClause(final Set<String> applicationIds) {
    Set<String> ids = applicationFilterIds(applicationIds);
    if (ids == null) {
      return null;
    }
    rejectIfExceedsClauseBudget(ids.size(), "Application filter contains too many ids");
    List<String> escapedIds = new ArrayList<>(ids.size());
    for (String applicationId : sortedCopy(ids)) {
      escapedIds.add(escapeLuceneTerm(applicationId));
    }
    return "applicationId:(" + String.join(" ", escapedIds) + ")";
  }

  private void rejectIfExceedsClauseBudget(final int idCount, final String messagePrefix) {
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (maxClauseCount > 0 && idCount > maxClauseCount) {
      throw new BadRequestException(messagePrefix + " (max " + maxClauseCount + ").");
    }
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
