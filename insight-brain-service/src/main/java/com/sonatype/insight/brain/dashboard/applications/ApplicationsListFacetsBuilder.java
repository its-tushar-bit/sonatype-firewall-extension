/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger log = LoggerFactory.getLogger(ApplicationsListFacetsBuilder.class);

  static final int MAX_FACET_DISCOVERY_HITS = 100;

  static final int MAX_ORGANIZATION_FACET_COUNT_QUERIES = 25;

  static final int MAX_ORGANIZATION_FACET_ENTRIES = 500;

  static final int MAX_APPLICATION_FACET_ENTRIES = 25;

  private final SearchIndexClient searchIndexClient;

  private final StageTypeService stageTypeService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  ApplicationsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
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
    // Intentional on both read paths: Martha filter-rail labels need organizationNames /
    // applicationNames even when facet ids are not on the current page (otherwise the UI
    // falls back to raw internal ids). DAO fallback only runs for ids missing from discovery.
    attachDisplayNames(facets, discovered);
    return facets;
  }

  ApplicationsListFacetsDTO buildFacets(
      final IndexReadSession session,
      final Query applicationQuery,
      final Query violationQuery,
      final long totalApplications)
  {
    ApplicationsListFacetsDTO facets = new ApplicationsListFacetsDTO();
    facets.totalApplications = totalApplications;

    LinkedHashMap<String, SearchResultItemDTO> discovered = discoverApplicationItems(session, applicationQuery);
    if (discovered.isEmpty()) {
      return facets;
    }

    facets.organizations = countOrganizations(session, applicationQuery, discovered);
    facets.applications = applicationFacetCountsFromDiscovery(discovered);
    facets.stages = countLicensedStages(session, violationQuery);
    // See legacy overload: display-name maps are required for both read paths.
    attachDisplayNames(facets, discovered);
    return facets;
  }

  private void attachDisplayNames(
      final ApplicationsListFacetsDTO facets,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    facets.organizationNames = resolveOrganizationNames(facets.organizations, discovered);
    facets.applicationNames = resolveApplicationNames(facets.applications, discovered);
  }

  private Map<String, String> resolveOrganizationNames(
      final Map<String, Long> organizationCounts,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    return resolveNames(
        organizationCounts,
        names -> discovered.values().forEach(item -> {
          if (StringUtils.isNotBlank(item.organizationId) && StringUtils.isNotBlank(item.organizationName)) {
            names.putIfAbsent(item.organizationId, item.organizationName);
          }
        }),
        missing -> {
          Map<String, String> loaded = new LinkedHashMap<>();
          for (Organization organization : organizationDAO.getByIds(missing)) {
            if (organization != null && StringUtils.isNotBlank(organization.getName())) {
              loaded.put(organization.getId(), organization.getName());
            }
          }
          return loaded;
        });
  }

  private Map<String, String> resolveApplicationNames(
      final Map<String, Long> applicationCounts,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    return resolveNames(
        applicationCounts,
        names -> discovered.forEach((applicationId, item) -> {
          if (StringUtils.isNotBlank(item.applicationName)) {
            names.putIfAbsent(applicationId, item.applicationName);
          }
        }),
        missing -> {
          Map<String, String> loaded = new LinkedHashMap<>();
          for (Application application : applicationDAO.getByIds(missing)) {
            if (application != null && StringUtils.isNotBlank(application.getName())) {
              loaded.put(application.getId(), application.getName());
            }
          }
          return loaded;
        });
  }

  private Map<String, String> resolveNames(
      final Map<String, Long> counts,
      final Consumer<Map<String, String>> seedFromDiscovered,
      final Function<Set<String>, Map<String, String>> loadMissing)
  {
    if (counts == null || counts.isEmpty()) {
      return null;
    }

    Map<String, String> names = new LinkedHashMap<>();
    seedFromDiscovered.accept(names);

    Set<String> missing = new HashSet<>();
    for (String id : counts.keySet()) {
      if (!names.containsKey(id)) {
        missing.add(id);
      }
    }
    if (!missing.isEmpty()) {
      names.putAll(loadMissing.apply(missing));
    }
    return names.isEmpty() ? null : names;
  }

  private LinkedHashMap<String, SearchResultItemDTO> discoverApplicationItems(final String applicationQuery) {
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(applicationQuery, MAX_FACET_DISCOVERY_HITS, 0, false, false, List.of());
    return ApplicationsListIndexItems.extractApplicationItems(searchResult);
  }

  private LinkedHashMap<String, SearchResultItemDTO> discoverApplicationItems(
      final IndexReadSession session,
      final Query applicationQuery)
  {
    IndexPageResult result = session.searchPage(new IndexPageRequest(
        applicationQuery,
        ApplicationsListService.stableSessionSort(),
        MAX_FACET_DISCOVERY_HITS,
        List.of()));
    return ApplicationsListIndexItems.extractApplicationItems(result == null ? List.of() : result.docs());
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
   * Session-path org facets intentionally allow up to {@link #MAX_ORGANIZATION_FACET_ENTRIES}
   * (vs legacy {@link #MAX_ORGANIZATION_FACET_COUNT_QUERIES}) and sort by display name. This is an
   * intentional improvement of the flag-gated path, not parity with the N+1 count() legacy cap.
   */
  private Map<String, Long> countOrganizations(
      final IndexReadSession session,
      final Query applicationQuery,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    List<IndexTermsBucket> buckets = session.termsAggregation(
        applicationQuery,
        FieldIdentifier.ORGANIZATION_ID.label,
        MAX_ORGANIZATION_FACET_ENTRIES);
    if (buckets.isEmpty()) {
      return null;
    }

    Map<String, String> organizationNames = new LinkedHashMap<>();
    discovered.values().forEach(item -> {
      if (StringUtils.isNotBlank(item.organizationId) && StringUtils.isNotBlank(item.organizationName)) {
        organizationNames.putIfAbsent(item.organizationId, item.organizationName);
      }
    });

    List<IndexTermsBucket> nonZeroBuckets = new ArrayList<>();
    for (IndexTermsBucket bucket : buckets) {
      if (bucket.count() > 0 && StringUtils.isNotBlank(bucket.key())) {
        nonZeroBuckets.add(bucket);
      }
    }
    nonZeroBuckets.sort(Comparator
        .comparing((IndexTermsBucket bucket) -> organizationNames.getOrDefault(bucket.key(), bucket.key()))
        .thenComparing(IndexTermsBucket::key));

    Map<String, Long> counts = new LinkedHashMap<>();
    for (IndexTermsBucket bucket : nonZeroBuckets) {
      counts.put(bucket.key(), bucket.count());
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

  private Map<String, Long> countLicensedStages(
      final IndexReadSession session,
      final Query violationQuery)
  {
    List<String> licensedStageIds = stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)
        .stream()
        .map(StageType::getId)
        .toList();
    if (licensedStageIds.isEmpty()) {
      return null;
    }

    Map<String, Long> collectedCounts;
    try {
      // Stage sidebar is Lucene-only until Track B docValues cardinality; OpenSearch/hybrid
      // sessions omit stages (org/app facets still return) — accepted V1 degradation.
      collectedCounts = session.countDistinctGroupedBy(
          violationQuery,
          FieldIdentifier.POLICY_EVALUATION_STAGE.label,
          FieldIdentifier.APPLICATION_ID.label,
          licensedStageIds);
    }
    catch (UnsupportedOperationException e) {
      log.warn(
          "Applications list stage facets unavailable for {} session; returning org/application facets without stages",
          session.backendId(),
          e);
      return null;
    }
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String stageId : licensedStageIds) {
      long count = collectedCounts.getOrDefault(stageId, 0L);
      if (count > 0) {
        counts.put(stageId, count);
      }
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
          List.of(FieldIdentifier.APPLICATION_ID.label));
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
