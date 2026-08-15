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

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexOrTermSetGroup;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
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
   * sentinels apply to different Lucene fields ({@code applicationId} vs {@code parentOrganizationId}).
   */
  public static final String NO_MATCH_APPLICATION_FILTER_ID = "__no_match__";

  private static final Pattern LUCENE_SPECIAL_CHARS = Pattern.compile("[+\\-!(){}\\[\\]^\"~*?:\\\\/]");

  private final Configuration configuration;

  @Inject
  public DashboardIndexDimensionQueryBuilder(final Configuration configuration) {
    this.configuration = configuration;
  }

  public String buildOrganizationFilterClause(final Set<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return null;
    }
    if (organizationIds.contains(Organization.ROOT_ORGANIZATION_ID)) {
      return null;
    }
    // Ancestor-match on PARENT_ORGANIZATION_ID: each doc carries its full ancestor closure in
    // PARENT_ORGANIZATION_ID, so filtering to org X matches docs whose closure contains X.
    // Blank ids are dropped rather than escaped into a whitespace term that matches nothing, matching
    // buildOrganizationFilterClausesById.
    List<String> escapedIds = new ArrayList<>(organizationIds.size());
    for (String organizationId : sortedCopy(organizationIds)) {
      if (StringUtils.isNotBlank(organizationId)) {
        escapedIds.add(escapeLuceneTerm(organizationId));
      }
    }
    if (escapedIds.isEmpty()) {
      return null;
    }
    if (escapedIds.size() == 1) {
      return FieldIdentifier.PARENT_ORGANIZATION_ID.label + ":" + escapedIds.get(0);
    }
    return FieldIdentifier.PARENT_ORGANIZATION_ID.label + ":(" + String.join(" ", escapedIds) + ")";
  }

  /**
   * Builds a Lucene organization filter clause per input id using ancestor-match on
   * PARENT_ORGANIZATION_ID. Root organization ids and blank ids are omitted from the result (same
   * skip semantics as a {@code null} return from {@link #buildOrganizationFilterClause} for those keys).
   * <p>
   * No clause-count guard per-org, since ancestor-match keeps the per-org clause count constant
   * regardless of hierarchy depth.
   */
  public Map<String, String> buildOrganizationFilterClausesById(final Collection<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return Map.of();
    }
    Map<String, String> clauses = new LinkedHashMap<>(organizationIds.size());
    for (String organizationId : organizationIds) {
      if (StringUtils.isBlank(organizationId)
          || Organization.ROOT_ORGANIZATION_ID.equals(organizationId))
      {
        continue;
      }
      clauses.put(organizationId,
          FieldIdentifier.PARENT_ORGANIZATION_ID.label + ":" + escapeLuceneTerm(organizationId));
    }
    return clauses;
  }

  /** Metrics-style application clause (sorted ids, no escaping or size cap). */
  /**
   * Organization ids for a budget-exempt term-set filter on {@code parentOrganizationId}, or
   * {@code null} when unrestricted (null/empty input, or the root organization present).
   * <p>
   * The ids are the caller's selection, not an expansion of it: every organization-carrying document
   * indexes its full ancestor closure in {@code parentOrganizationId}, so one term per selected
   * organization already matches that organization's whole subtree. Blank ids are dropped rather than
   * turned into a term that matches nothing, matching {@link #buildOrganizationFilterClause}.
   */
  public Set<String> organizationFilterIds(final Set<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return null;
    }
    if (organizationIds.contains(Organization.ROOT_ORGANIZATION_ID)) {
      return null;
    }
    Set<String> ids = new LinkedHashSet<>();
    for (String organizationId : sortedCopy(organizationIds)) {
      if (StringUtils.isNotBlank(organizationId)) {
        ids.add(organizationId);
      }
    }
    return ids.isEmpty() ? Set.of(NO_MATCH_ORGANIZATION_FILTER_ID) : Set.copyOf(ids);
  }

  /**
   * Per-input-id organization term-set ids for facet sibling counts: each requested organization maps to
   * a single-element set containing itself, matched against {@code parentOrganizationId}.
   * <p>
   * The set is a singleton because the indexed ancestor closure already resolves the subtree, so no
   * organization needs its descendants enumerated and no clause-count ceiling applies. Root and blank
   * ids are omitted, matching {@link #buildOrganizationFilterClausesById}.
   */
  public Map<String, Set<String>> organizationFilterIdsById(final Collection<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Set<String>> byId = new LinkedHashMap<>();
    for (String organizationId : organizationIds) {
      if (StringUtils.isNotBlank(organizationId)
          && !Organization.ROOT_ORGANIZATION_ID.equals(organizationId))
      {
        byId.put(organizationId, Set.of(organizationId));
      }
    }
    return Map.copyOf(byId);
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
   * <p>
   * Root organization is unrestricted on the org dimension ({@link #organizationFilterIds}
   * returns {@code null}). Combined with an explicit application filter that therefore becomes
   * application-only — it narrows to those apps rather than widening back to all orgs (CLM-42254).
   */
  public List<IndexFilterRestriction> buildScopeFilterRestrictions(
      final Set<String> organizationIds,
      final Set<String> applicationIds)
  {
    Set<String> orgs = organizationFilterIds(organizationIds);
    Set<String> apps = applicationFilterIds(applicationIds);
    if (orgs == null && apps == null) {
      return List.of();
    }
    if (orgs != null && apps == null) {
      return IndexTermSetRestriction.singleton(FieldIdentifier.PARENT_ORGANIZATION_ID.label, orgs);
    }
    if (orgs == null) {
      return IndexTermSetRestriction.singleton(FieldIdentifier.APPLICATION_ID.label, apps);
    }
    return IndexOrTermSetGroup.singleton(
        IndexTermSetRestriction.of(FieldIdentifier.PARENT_ORGANIZATION_ID.label, orgs),
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
    Set<String> orgs = organizationFilterIds(organizationIds);
    if (orgs != null) {
      restrictions.add(IndexTermSetRestriction.of(FieldIdentifier.PARENT_ORGANIZATION_ID.label, orgs));
    }
    Set<String> apps = applicationFilterIds(applicationIds);
    if (apps != null) {
      restrictions.add(IndexTermSetRestriction.of(FieldIdentifier.APPLICATION_ID.label, apps));
    }
    return List.copyOf(restrictions);
  }

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
