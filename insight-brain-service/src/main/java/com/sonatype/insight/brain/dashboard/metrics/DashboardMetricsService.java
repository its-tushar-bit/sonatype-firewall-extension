/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

@Named
@Singleton
public class DashboardMetricsService
{
  static final String METRIC_SOURCE_INDEX = "index";

  static final Map<String, int[]> VIOLATIONS_THREAT_LEVEL_BANDS = ThreatLevel.searchAggregationBands();

  /**
   * Sentinel org id for a filter that must match zero APPLICATION docs. Valid under
   * {@link MetricFilterValidator#ID_PATTERN} but never indexed as an organization owner.
   */
  static final String NO_MATCH_ORGANIZATION_FILTER_ID = "__no_match__";

  private static final int CACHE_MAXIMUM_SIZE = 128;

  /**
   * Freshness window for the coalescing cache (the documented ≤10s-fresh / 5s-coalescing
   * contract). Kept next to {@link #CACHE_MAXIMUM_SIZE} so the guarantee is visible and
   * tunable in one place rather than inline in the builder.
   */
  private static final Duration CACHE_TTL = Duration.ofSeconds(5);

  private final SearchIndexClient searchIndexClient;

  private final MetricFilterValidator metricFilterValidator;

  private final OrganizationDAO organizationDAO;

  private final Configuration configuration;

  private final CurrentUser currentUser;

  private final TenantReference<Cache<DashboardMetricsCacheKey, DashboardMetricsDTO>> caches;

  @Inject
  public DashboardMetricsService(
      SearchIndexClient searchIndexClient,
      MetricFilterValidator metricFilterValidator,
      OrganizationDAO organizationDAO,
      Configuration configuration,
      CurrentUser currentUser)
  {
    this.searchIndexClient = searchIndexClient;
    this.metricFilterValidator = metricFilterValidator;
    this.organizationDAO = organizationDAO;
    this.configuration = configuration;
    this.currentUser = currentUser;
    this.caches = new TenantReference<>(this::createCache);
  }

  /**
   * Returns RBAC-scoped dashboard metrics, optionally narrowed by request filters.
   * <p>
   * Request-supplied {@code organizationIds}/{@code applicationIds} cannot widen the caller's
   * readable scope: {@link SearchIndexClient#count} and
   * {@link SearchIndexClient#aggregateCountByField} always AND the user's allowed contexts with
   * the server-built metric query, so ids the caller cannot read match zero documents rather than
   * leaking foreign counts.
   */
  public DashboardMetricsDTO getMetrics(DashboardMetricsRequestDTO request) {
    metricFilterValidator.validate(request);
    DashboardMetricsCacheKey cacheKey = DashboardMetricsCacheKey.forRequest(currentUser.getUserPrincipal(), request);
    try {
      return getCache().get(cacheKey, () -> loadMetrics(request));
    }
    catch (ExecutionException | UncheckedExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Failed to load dashboard metrics", cause);
    }
  }

  /**
   * Loads each metric independently against the search index. {@code count} and
   * {@code aggregateCountByField} each open their own reader/snapshot today, so applications and
   * violations are not guaranteed to come from the same point-in-time view (acceptable behind the
   * 5s coalescing cache). A batched {@link SearchIndexClient} entry point (e.g. OpenSearch
   * multi-search) is planned when additional KPIs land.
   */
  private DashboardMetricsDTO loadMetrics(DashboardMetricsRequestDTO request) {
    MetricFilterContext filterContext = buildMetricFilterContext(request);

    long applications = searchIndexClient.count(
        buildFilteredMetricQuery(ItemType.APPLICATION, filterContext));

    MetricAggregationResult violationsAggregation = searchIndexClient.aggregateCountByField(
        buildFilteredMetricQuery(ItemType.POLICY_VIOLATION, filterContext),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        VIOLATIONS_THREAT_LEVEL_BANDS);
    Long lastUpdatedAt = searchIndexClient.getLastIndexTime();

    MetricValueDTO applicationsMetric = new MetricValueDTO(applications, null, METRIC_SOURCE_INDEX);
    MetricValueDTO violationsMetric = new MetricValueDTO(
        violationsAggregation.total, violationsAggregation.buckets, METRIC_SOURCE_INDEX);
    return new DashboardMetricsDTO(applicationsMetric, violationsMetric, lastUpdatedAt);
  }

  private MetricFilterContext buildMetricFilterContext(DashboardMetricsRequestDTO request) {
    return new MetricFilterContext(
        buildOrganizationFilterClause(request.organizationIds),
        buildApplicationFilterClause(request.applicationIds));
  }

  /**
   * Builds a metric query for the given item type. RBAC scopes to the user's readable contexts inside
   * {@link SearchIndexClient#count} / {@link SearchIndexClient#aggregateCountByField}.
   * <p>
   * APPLICATION index documents store only the <em>direct</em> owning org in
   * {@code parentOrganizationId} (see {@code DocumentBuilder#setOwner}) — they are not
   * ancestor-denormalized today. Hierarchy-inclusive org filtering therefore expands requested org
   * ids to descendant ids via {@link OrganizationDAO#getAllChildOrganizationIds} before matching on
   * {@code organizationId} (rewritten to {@code parentOrganizationId} at query time). Index
   * ancestor denormalization is tracked separately; this path remains until that data is complete.
   * <p>
   * POLICY_VIOLATION documents carry {@code organizationId}, {@code parentOrganizationId}, and
   * {@code applicationId}, so the same filter clauses narrow violations consistently with applications.
   * <p>
   * When both {@code organizationIds} and {@code applicationIds} are present, the dimension
   * clauses are combined with {@code OR}, matching Classic dashboard resolution in
   * {@link com.sonatype.insight.brain.organization.ApplicationService#getAppsByIds} (union of
   * apps in selected org subtrees plus explicitly selected apps).
   */
  private static String buildFilteredMetricQuery(ItemType itemType, MetricFilterContext filterContext) {
    String baseQuery = "itemType:" + itemType.searchFieldName();
    String organizationClause = filterContext.organizationClause();
    String applicationClause = filterContext.applicationClause();

    if (organizationClause == null && applicationClause == null) {
      return baseQuery;
    }

    List<String> filterClauses = new ArrayList<>();
    if (organizationClause != null) {
      filterClauses.add(organizationClause);
    }
    if (applicationClause != null) {
      filterClauses.add(applicationClause);
    }
    return baseQuery + " AND (" + String.join(" OR ", filterClauses) + ")";
  }

  private String buildOrganizationFilterClause(Set<String> organizationIds) {
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

  private static String buildApplicationFilterClause(Set<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return null;
    }
    return "applicationId:(" + String.join(" ", sortedCopy(applicationIds)) + ")";
  }

  private Cache<DashboardMetricsCacheKey, DashboardMetricsDTO> createCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(CACHE_MAXIMUM_SIZE)
        .build();
  }

  private Cache<DashboardMetricsCacheKey, DashboardMetricsDTO> getCache() {
    return caches.get();
  }

  private static List<String> sortedCopy(Set<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ids.stream().sorted().toList();
  }

  private record MetricFilterContext(String organizationClause, String applicationClause)
  {
  }
}
