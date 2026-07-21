/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DashboardMetricsService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsService.class);

  static final String METRIC_SOURCE_INDEX = "index";

  static final String METRIC_SOURCE_SQL = "sql";

  static final Map<String, int[]> VIOLATIONS_THREAT_LEVEL_BANDS = ThreatLevel.searchAggregationBands();

  /**
   * CVSS bands for the Vulnerabilities tile breakdown. Keys match the frontend contract
   * ({@code critical}, {@code high}, {@code medium}, {@code low}). {@link CvssV3Severity#NONE}
   * is omitted — unscored CVEs contribute to {@code total} only.
   */
  private static final List<CvssV3Severity> VULNERABILITY_SEVERITY_BUCKETS =
      List.of(CvssV3Severity.CRITICAL, CvssV3Severity.HIGH, CvssV3Severity.MEDIUM, CvssV3Severity.LOW);

  /**
   * Index item types with no {@code applicationId} field on indexed documents ({@link ItemType#ORGANIZATION} only
   * today). {@link ItemType#POLICY} is intentionally <em>not</em> excluded: it can be org- or app-scoped via
   * {@link DocumentBuilder#setOwner}, so an {@code applicationIds}-only request returns app-scoped policies only
   * (org-level policies have no {@code applicationId} and are omitted) while the {@code organizations} tile still
   * reflects all readable orgs.
   */
  private static final Set<ItemType> APPLICATION_FILTER_EXCLUDED_ITEM_TYPES = Set.of(ItemType.ORGANIZATION);

  private static final List<String> APPLICATION_COMPONENT_KEY_FIELDS =
      List.of(FieldIdentifier.APPLICATION_ID.label, FieldIdentifier.COMPONENT_HASH.label);

  /**
   * Distinct {@code vulnerabilityId} (CVE/advisory) for the Vulnerabilities tile — estate-level
   * "how many unique vulnerabilities impact my scope", not per-(app, component) exposure instances.
   * Blast radius (which apps/components) is drill-down; {@link #countScannedComponents()} and
   * Violations cover instance-level counts separately.
   */
  private static final List<String> ESTATE_VULNERABILITY_KEY_FIELDS =
      List.of(FieldIdentifier.VULNERABILITY_ID.label);

  private static final List<String> LEGAL_OBLIGATION_KEY_FIELDS = List.of(
      FieldIdentifier.APPLICATION_ID.label,
      FieldIdentifier.COMPONENT_HASH.label,
      FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID.label);

  /**
   * Sentinel org id for a filter that must match zero APPLICATION docs. Valid under
   * {@link MetricFilterValidator#ID_PATTERN} but never indexed as an organization owner.
   */
  static final String NO_MATCH_ORGANIZATION_FILTER_ID =
      DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID;

  private static final int CACHE_MAXIMUM_SIZE = 128;

  /**
   * Freshness window for the coalescing cache (the documented ≤10s-fresh / 5s-coalescing
   * contract). Kept next to {@link #CACHE_MAXIMUM_SIZE} so the guarantee is visible and
   * tunable in one place rather than inline in the builder.
   */
  private static final Duration CACHE_TTL = Duration.ofSeconds(5);

  private final SearchIndexClient searchIndexClient;

  private final MetricFilterValidator metricFilterValidator;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final DashboardMetricsWaiverScopeService waiverScopeService;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final Configuration configuration;

  private final StageTypeService stageTypeService;

  private final CurrentUser currentUser;

  private final TenantReference<Cache<DashboardMetricsCacheKey, DashboardMetricsDTO>> caches;

  @Inject
  public DashboardMetricsService(
      SearchIndexClient searchIndexClient,
      MetricFilterValidator metricFilterValidator,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyWaiverRequestDAO policyWaiverRequestDAO,
      DashboardMetricsWaiverScopeService waiverScopeService,
      DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      Configuration configuration,
      StageTypeService stageTypeService,
      CurrentUser currentUser)
  {
    this.searchIndexClient = searchIndexClient;
    this.metricFilterValidator = metricFilterValidator;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
    this.waiverScopeService = waiverScopeService;
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.configuration = configuration;
    this.stageTypeService = stageTypeService;
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
   * Loads each selected metric tier on the request thread. A null tier flag retains the
   * complete backward-compatible payload; explicit false/true values select non-overlapping
   * summary/heavy tiers.
   */
  private DashboardMetricsDTO loadMetrics(DashboardMetricsRequestDTO request) {
    boolean compatibilityMode = request == null || request.includeHeavyMetrics == null;
    boolean includeSummary = compatibilityMode || Boolean.FALSE.equals(request.includeHeavyMetrics);
    boolean includeHeavy = compatibilityMode || Boolean.TRUE.equals(request.includeHeavyMetrics);
    List<String> unsupportedIndexDimensions = unsupportedIndexDimensions(request);
    MetricFilterContext filterContext =
        unsupportedIndexDimensions.isEmpty() ? buildMetricFilterContext(request) : null;

    MetricValueDTO applicationsMetric = null;
    MetricValueDTO organizationsMetric = null;
    MetricValueDTO policiesMetric = null;
    MetricValueDTO waiversMetric = null;
    Long lastUpdatedAt = null;
    if (includeSummary) {
      if (unsupportedIndexDimensions.isEmpty()) {
        long applicationsStartedAt = System.nanoTime();
        long applications = searchIndexClient.count(
            buildFilteredMetricQuery(ItemType.APPLICATION, filterContext));
        logBenchmarkDuration("applications", applicationsStartedAt);
        applicationsMetric = new MetricValueDTO(
            applications,
            Map.of("stages", licensedDashboardStageCount()),
            METRIC_SOURCE_INDEX);
        long organizationsStartedAt = System.nanoTime();
        organizationsMetric = new MetricValueDTO(
            searchIndexClient.count(buildFilteredMetricQuery(ItemType.ORGANIZATION, filterContext)),
            null,
            METRIC_SOURCE_INDEX);
        logBenchmarkDuration("organizations", organizationsStartedAt);
        long policiesStartedAt = System.nanoTime();
        policiesMetric = new MetricValueDTO(
            searchIndexClient.count(buildFilteredMetricQuery(ItemType.POLICY, filterContext)),
            null,
            METRIC_SOURCE_INDEX);
        logBenchmarkDuration("policies", policiesStartedAt);
        lastUpdatedAt = searchIndexClient.getLastIndexTime();
      }
      else {
        applicationsMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
        organizationsMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
        policiesMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
      }

      // Waivers are SQL-backed: stageIds are not applicable (unsupported), but tagIds are applied by
      // the waivers scope query. Tag-only filters therefore return a real waiver count while
      // index-backed cards report UNSUPPORTED_FILTER_COMBINATION for tagIds.
      if (hasFilter(request == null ? null : request.stageIds)) {
        waiversMetric = MetricValueDTO.unsupported(List.of("stageIds"));
      }
      else {
        long waiversStartedAt = System.nanoTime();
        Set<String> accessibleOwnerIds = waiverScopeService.resolveAccessibleOwnerIds(request);
        long existingWaivers = policyWaiverDAO.selectCount(accessibleOwnerIds);
        long requestedWaivers = policyWaiverRequestDAO.selectCount(accessibleOwnerIds);
        waiversMetric = new MetricValueDTO(
            existingWaivers + requestedWaivers,
            Map.of("existing", existingWaivers, "requested", requestedWaivers),
            METRIC_SOURCE_SQL);
        logBenchmarkDuration("waivers", waiversStartedAt);
      }
    }

    MetricValueDTO violationsMetric = null;
    MetricValueDTO componentsMetric = null;
    MetricValueDTO vulnerabilitiesMetric = null;
    MetricValueDTO legalMetric = null;
    if (includeHeavy) {
      if (unsupportedIndexDimensions.isEmpty()) {
        long violationsStartedAt = System.nanoTime();
        MetricAggregationResult violationsAggregation = searchIndexClient.aggregateCountByField(
            buildFilteredMetricQuery(ItemType.POLICY_VIOLATION, filterContext),
            FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
            VIOLATIONS_THREAT_LEVEL_BANDS);
        logBenchmarkDuration("violations", violationsStartedAt);
        violationsMetric = new MetricValueDTO(
            violationsAggregation.total, violationsAggregation.buckets, METRIC_SOURCE_INDEX);
        long componentsStartedAt = System.nanoTime();
        componentsMetric = new MetricValueDTO(countScannedComponents(filterContext), null, METRIC_SOURCE_INDEX);
        logBenchmarkDuration("components", componentsStartedAt);
        long vulnerabilitiesStartedAt = System.nanoTime();
        vulnerabilitiesMetric = countVulnerabilities(filterContext);
        logBenchmarkDuration("vulnerabilities", vulnerabilitiesStartedAt);
        long legalStartedAt = System.nanoTime();
        legalMetric = countLegalObligations(filterContext);
        logBenchmarkDuration("legal", legalStartedAt);
      }
      else {
        violationsMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
        componentsMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
        vulnerabilitiesMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
        legalMetric = MetricValueDTO.unsupported(unsupportedIndexDimensions);
      }
    }

    return new DashboardMetricsDTO(
        applicationsMetric,
        violationsMetric,
        componentsMetric,
        organizationsMetric,
        policiesMetric,
        vulnerabilitiesMetric,
        legalMetric,
        waiversMetric,
        lastUpdatedAt);
  }

  private static void logBenchmarkDuration(String metric, long startedAt) {
    // Fractional ms so sub-millisecond index counts are not collapsed to durationMs=0.
    log.debug("DASHBOARD_BENCHMARK metric={} durationMs={}", metric,
        (System.nanoTime() - startedAt) / 1_000_000.0);
  }

  private static List<String> unsupportedIndexDimensions(DashboardMetricsRequestDTO request) {
    List<String> dimensions = new ArrayList<>();
    if (hasFilter(request == null ? null : request.stageIds)) {
      dimensions.add("stageIds");
    }
    if (hasFilter(request == null ? null : request.tagIds)) {
      dimensions.add("tagIds");
    }
    return List.copyOf(dimensions);
  }

  private static boolean hasFilter(Set<String> ids) {
    return ids != null && !ids.isEmpty();
  }

  /**
   * Counts each scanned component once per {@code (applicationId, componentHash)}. A clean component may be indexed
   * as one {@link ItemType#NON_VULNERABLE_COMPONENT} document per stage, and a vulnerable component as one
   * {@link ItemType#SECURITY_VULNERABILITY} document per CVE; naive {@link SearchIndexClient#count(String)} on
   * either item type over-counts. The total is therefore {@code countDistinct(NON_VULNERABLE_COMPONENT, …) +
   * countDistinct(SECURITY_VULNERABILITY, …)} with the same composite key fields. Both sub-queries reuse
   * {@link #buildFilteredMetricQuery} so request-level org/app filters (including org-hierarchy descendant
   * expansion) and the RBAC filter apply consistently.
   */
  private long countScannedComponents(MetricFilterContext filterContext) {
    long distinctCleanComponents = searchIndexClient.countDistinct(
        buildFilteredMetricQuery(ItemType.NON_VULNERABLE_COMPONENT, filterContext),
        APPLICATION_COMPONENT_KEY_FIELDS);
    long distinctVulnerableComponents = searchIndexClient.countDistinct(
        buildFilteredMetricQuery(ItemType.SECURITY_VULNERABILITY, filterContext),
        APPLICATION_COMPONENT_KEY_FIELDS);
    return distinctCleanComponents + distinctVulnerableComponents;
  }

  /**
   * Estate-level distinct CVE count plus per-band breakdown using {@link CvssV3Severity} ranges on
   * {@link FieldIdentifier#VULNERABILITY_SEVERITY}. Each scored CVE appears in exactly one band;
   * unscored CVEs ({@link CvssV3Severity#NONE}) contribute to {@code total} only.
   */
  private MetricValueDTO countVulnerabilities(MetricFilterContext filterContext) {
    String baseQuery = buildFilteredMetricQuery(ItemType.SECURITY_VULNERABILITY, filterContext);
    long total = searchIndexClient.countDistinct(baseQuery, ESTATE_VULNERABILITY_KEY_FIELDS);
    Map<String, Long> breakdown = new LinkedHashMap<>();
    for (CvssV3Severity band : VULNERABILITY_SEVERITY_BUCKETS) {
      breakdown.put(
          band.getDisplayName().toLowerCase(),
          searchIndexClient.countDistinct(
              appendVulnerabilitySeverityRange(baseQuery, band),
              ESTATE_VULNERABILITY_KEY_FIELDS));
    }
    return new MetricValueDTO(total, breakdown, METRIC_SOURCE_INDEX);
  }

  private static String appendVulnerabilitySeverityRange(String baseQuery, CvssV3Severity band) {
    return baseQuery + " AND " + FieldIdentifier.VULNERABILITY_SEVERITY.label + ":["
        + band.getStartScoreRange() + " TO " + band.getEndScoreRange() + "]";
  }

  /**
   * Licensed dashboard stage count for the Applications tile secondary stat — metadata from
   * {@link StageTypeService}, not an index aggregation.
   */
  private long licensedDashboardStageCount() {
    return stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT).size();
  }

  /**
   * Legal total is distinct {@code (applicationId, componentHash, componentEffectiveLicenseId)} obligations;
   * breakdown uses the same {@code [applicationId, componentHash]} key as {@link #countScannedComponents()}.
   */
  private MetricValueDTO countLegalObligations(MetricFilterContext filterContext) {
    String legalQuery = buildFilteredMetricQuery(ItemType.LEGAL_VIOLATION, filterContext);
    long legalApplications =
        searchIndexClient.countDistinct(legalQuery, List.of(FieldIdentifier.APPLICATION_ID.label));
    long legalComponents = searchIndexClient.countDistinct(legalQuery, APPLICATION_COMPONENT_KEY_FIELDS);
    Map<String, Long> legalBreakdown = new LinkedHashMap<>();
    legalBreakdown.put("applications", legalApplications);
    legalBreakdown.put("components", legalComponents);
    return new MetricValueDTO(
        searchIndexClient.countDistinct(legalQuery, LEGAL_OBLIGATION_KEY_FIELDS),
        legalBreakdown,
        METRIC_SOURCE_INDEX);
  }

  private MetricFilterContext buildMetricFilterContext(DashboardMetricsRequestDTO request) {
    return new MetricFilterContext(
        dimensionQueryBuilder.buildOrganizationFilterClause(request == null ? null : request.organizationIds),
        dimensionQueryBuilder.buildApplicationFilterClause(request == null ? null : request.applicationIds));
  }

  /**
   * Builds a metric query for the given item type. RBAC scopes to the user's readable contexts inside
   * {@link SearchIndexClient#count}, {@link SearchIndexClient#aggregateCountByField}, and
   * {@link SearchIndexClient#countDistinct}.
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
   * <p>
   * {@link ItemType#ORGANIZATION} documents have no {@code applicationId} in the index. For that item type,
   * only the organization clause is applied; {@code applicationIds} in the request are ignored so organization
   * totals are not zeroed. {@link ItemType#POLICY} can be org- or app-scoped and keeps the application clause.
   */
  private static String buildFilteredMetricQuery(ItemType itemType, MetricFilterContext filterContext) {
    String baseQuery = "itemType:" + itemType.searchFieldName();
    String organizationClause = filterContext.organizationClause();
    String applicationClause =
        APPLICATION_FILTER_EXCLUDED_ITEM_TYPES.contains(itemType) ? null : filterContext.applicationClause();

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

  private Cache<DashboardMetricsCacheKey, DashboardMetricsDTO> createCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(CACHE_MAXIMUM_SIZE)
        .build();
  }

  private Cache<DashboardMetricsCacheKey, DashboardMetricsDTO> getCache() {
    return caches.get();
  }

  private record MetricFilterContext(String organizationClause, String applicationClause)
  {
  }
}
