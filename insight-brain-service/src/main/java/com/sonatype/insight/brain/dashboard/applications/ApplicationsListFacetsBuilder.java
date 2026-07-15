/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds Martha sidebar facet counts from RBAC-scoped index queries (CLM-42228 Phase B).
 * <p>
 * Discovery is capped at {@link #MAX_FACET_DISCOVERY_HITS} APPLICATION hits for per-org and
 * per-application count queries; {@link ApplicationsListFacetsDTO#totalApplications} reflects the
 * full RBAC-scoped total from the list query. Full aggregate facets are tracked under CLM-42230.
 */
@Named
@Singleton
final class ApplicationsListFacetsBuilder
{
  static final int MAX_FACET_DISCOVERY_HITS = 100;

  static final int MAX_ORGANIZATION_FACET_COUNT_QUERIES = 25;

  static final int MAX_APPLICATION_FACET_ENTRIES = 25;

  private final SearchIndexClient searchIndexClient;

  private final StageTypeService stageTypeService;

  @Inject
  ApplicationsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
  }

  ApplicationsListFacetsDTO buildFacets(
      final String applicationQuery,
      final long totalApplications)
  {
    ApplicationsListFacetsDTO facets = new ApplicationsListFacetsDTO();
    facets.totalApplications = totalApplications;

    LinkedHashMap<String, SearchResultItemDTO> discovered = discoverApplicationItems(applicationQuery);
    if (discovered.isEmpty()) {
      return facets;
    }

    facets.organizations = countOrganizations(applicationQuery, discovered);
    facets.applications = applicationFacetCountsFromDiscovery(discovered);
    facets.stages = countLicensedStages(applicationQuery);
    return facets;
  }

  private LinkedHashMap<String, SearchResultItemDTO> discoverApplicationItems(final String applicationQuery) {
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(applicationQuery, MAX_FACET_DISCOVERY_HITS, 0, false, false, List.of());
    return ApplicationsListIndexItems.extractApplicationItems(searchResult);
  }

  private Map<String, Long> countOrganizations(
      final String applicationQuery,
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

    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String organizationId : organizationIds) {
      if (queries >= MAX_ORGANIZATION_FACET_COUNT_QUERIES) {
        break;
      }
      String orgClause = buildExactOrganizationFilterClause(organizationId);
      counts.put(organizationId, searchIndexClient.count(applicationQuery + " AND " + orgClause));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * APPLICATION index docs are 1:1 with applications, so per-app counts under the RBAC-scoped query are
   * always {@code 1} for discovered hits. Derive the facet map from discovery instead of N count() calls.
   */
  private static Map<String, Long> applicationFacetCountsFromDiscovery(
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    int entries = 0;
    for (String applicationId : discovered.keySet()) {
      if (entries >= MAX_APPLICATION_FACET_ENTRIES) {
        break;
      }
      counts.put(applicationId, 1L);
      entries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countLicensedStages(final String applicationQuery) {
    String violationQuery = toViolationFacetQuery(applicationQuery);
    Map<String, Long> counts = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      String stageId = stageType.getId();
      String stageClause = ApplicationsListViolationQuerySupport.buildStageFilterClause(Set.of(stageId));
      long count = searchIndexClient.countDistinct(
          violationQuery + " AND " + stageClause,
          List.of("applicationId"));
      if (count > 0) {
        counts.put(stageId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private static String toViolationFacetQuery(final String applicationQuery) {
    return ApplicationsListViolationQuerySupport.toViolationQuery(applicationQuery);
  }

  private static String buildExactOrganizationFilterClause(final String organizationId) {
    return "organizationId:(" + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(organizationId) + ")";
  }
}
