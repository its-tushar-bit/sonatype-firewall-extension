/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds Martha sidebar facet counts for the Violations list from RBAC-scoped index count queries.
 * <p>
 * All facet counts are computed against the same {@code violationQuery} as the list rows, so active
 * sidebar filters narrow both the result set and the facet counts (Martha V1 narrowing semantics;
 * filter interactions land in CLM-42258). Dimensions with a zero count are omitted from the response.
 * <p>
 * Violation volume is far higher than application volume, so discovery of organization/application
 * dimensions is capped hard: {@link #MAX_FACET_DISCOVERY_HITS} hits are inspected and per-dimension
 * {@code count()} calls are capped by {@link #MAX_ORGANIZATION_FACET_COUNT_QUERIES} /
 * {@link #MAX_APPLICATION_FACET_COUNT_QUERIES}. State, threat-category, and stage facets are bounded
 * by small fixed vocabularies and are always exact. Full aggregate facets land under CLM-42262.
 * <p>
 * The three caps default to conservative values but can be overridden with the {@code
 * nexusOne.violations.facets.*} system properties so scale tuning needs no code change (see CLM-42262).
 * <p>
 * <b>Cap-ordering limitation (V1):</b> only non-root-org items consume the org count-query budget —
 * root-org items produce a null clause and are skipped without decrementing it. Because discovery
 * inspects the first {@link #MAX_FACET_DISCOVERY_HITS} hits in index order, an estate whose earliest
 * hits are dominated by non-root orgs can exhaust {@link #MAX_ORGANIZATION_FACET_COUNT_QUERIES} before
 * reaching orgs that appear later, so the org facet is a best-effort sample rather than an exhaustive
 * list. The bucketed aggregation in CLM-42262 removes both the discovery cap and this ordering
 * sensitivity; until then the omission is deliberate and bounded.
 */
@Named
@Singleton
final class ViolationsListFacetsBuilder
{
  static final int MAX_FACET_DISCOVERY_HITS =
      Integer.getInteger("nexusOne.violations.facets.maxDiscoveryHits", 200);

  static final int MAX_ORGANIZATION_FACET_COUNT_QUERIES =
      Integer.getInteger("nexusOne.violations.facets.maxOrganizationCountQueries", 15);

  static final int MAX_APPLICATION_FACET_COUNT_QUERIES =
      Integer.getInteger("nexusOne.violations.facets.maxApplicationCountQueries", 15);

  private final SearchIndexClient searchIndexClient;

  private final StageTypeService stageTypeService;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Inject
  ViolationsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService,
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
    this.dimensionQueryBuilder = dimensionQueryBuilder;
  }

  ViolationsListFacetsDTO buildFacets(final String violationQuery, final long totalViolations) {
    ViolationsListFacetsDTO facets = new ViolationsListFacetsDTO();
    facets.totalViolations = totalViolations;
    if (totalViolations == 0) {
      return facets;
    }

    facets.states = countStates(violationQuery);
    facets.threatCategories = countThreatCategories(violationQuery);
    facets.stages = countLicensedStages(violationQuery);

    LinkedHashMap<String, SearchResultItemDTO> discovered = discoverViolationItems(violationQuery);
    if (!discovered.isEmpty()) {
      facets.organizations = countOrganizations(violationQuery, discovered);
      facets.applications = countApplications(violationQuery, discovered);
    }
    return facets;
  }

  private Map<String, Long> countStates(final String violationQuery) {
    Map<String, Long> counts = new LinkedHashMap<>();
    String waivedClause = FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
        + ViolationWaiverStatus.WAIVED + " " + ViolationWaiverStatus.AUTO_WAIVED + ")";
    // OPEN mirrors the row-side derivation in ViolationWaiverStatus.toState: anything that is not
    // Waived/AutoWaived — including a missing/unrecognized waiver status — is OPEN. Counting
    // "NOT waived" (rather than ":(Active)") keeps the OPEN facet in agreement with the row states so
    // the two paths can never disagree, even if a violation doc ever lacks an explicit waiver status.
    long open = searchIndexClient.count(violationQuery + " AND NOT (" + waivedClause + ")");
    if (open > 0) {
      counts.put(PolicyViolationState.OPEN.name(), open);
    }
    long waived = searchIndexClient.count(violationQuery + " AND " + waivedClause);
    if (waived > 0) {
      counts.put(PolicyViolationState.WAIVED.name(), waived);
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countThreatCategories(final String violationQuery) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      String clause = FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(category.getName()) + ")";
      long count = searchIndexClient.count(violationQuery + " AND " + clause);
      if (count > 0) {
        counts.put(category.getName(), count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countLicensedStages(final String violationQuery) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      String stageId = stageType.getId();
      String clause = FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId) + ")";
      long count = searchIndexClient.count(violationQuery + " AND " + clause);
      if (count > 0) {
        counts.put(stageId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private LinkedHashMap<String, SearchResultItemDTO> discoverViolationItems(final String violationQuery) {
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(violationQuery, MAX_FACET_DISCOVERY_HITS, 0, false, false, List.of());
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (searchResult.groupingByDTOS != null) {
      for (var group : searchResult.groupingByDTOS) {
        if (group.searchResultItemDTOS == null) {
          continue;
        }
        for (SearchResultItemDTO item : group.searchResultItemDTOS) {
          if (!ItemType.POLICY_VIOLATION.name().equals(item.itemType) || StringUtils.isBlank(item.applicationId)) {
            continue;
          }
          items.putIfAbsent(item.applicationId, item);
        }
      }
    }
    return items;
  }

  private Map<String, Long> countOrganizations(
      final String violationQuery,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    Set<String> organizationIds = new LinkedHashSet<>();
    discovered.values().forEach(item -> {
      if (StringUtils.isNotBlank(item.organizationId)) {
        organizationIds.add(item.organizationId);
      }
    });
    if (organizationIds.isEmpty()) {
      return null;
    }

    // Each org count expands the org to itself + all descendant orgs (buildOrganizationFilterClause ->
    // getAllChildOrganizationIds), so a parent and a child that are both present count overlapping
    // violations and the per-org counts intentionally do NOT sum to totalViolations. This is a known
    // N+1 (one child-id DB lookup per org) bounded by MAX_ORGANIZATION_FACET_COUNT_QUERIES; the
    // bucketed-aggregation replacement that removes the fan-out is tracked under CLM-42262.
    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String organizationId : organizationIds) {
      if (queries >= MAX_ORGANIZATION_FACET_COUNT_QUERIES) {
        break;
      }
      String orgClause = dimensionQueryBuilder.buildOrganizationFilterClause(Set.of(organizationId));
      // Root-organization ids bypass the org filter clause (see DashboardIndexDimensionQueryBuilder);
      // skip them rather than counting against the full RBAC-scoped query twice.
      if (orgClause == null) {
        continue;
      }
      counts.put(organizationId, searchIndexClient.count(violationQuery + " AND " + orgClause));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countApplications(
      final String violationQuery,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String applicationId : discovered.keySet()) {
      if (queries >= MAX_APPLICATION_FACET_COUNT_QUERIES) {
        break;
      }
      String appClause = dimensionQueryBuilder.buildEscapedApplicationFilterClause(Set.of(applicationId));
      if (appClause == null) {
        continue;
      }
      counts.put(applicationId, searchIndexClient.count(violationQuery + " AND " + appClause));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }
}
