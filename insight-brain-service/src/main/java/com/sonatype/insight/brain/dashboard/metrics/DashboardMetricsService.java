/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsShadowComparisonService;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsScopeResolver;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlCoordinator;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlMode;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlModeProvider;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadiness;
import com.sonatype.insight.brain.dashboard.metrics.sql.ResolvedScope;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
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

  public static final String METRIC_SOURCE_SQL = "sql";

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

  private static final List<String> COMPONENT_HASH_KEY_FIELDS =
      List.of(FieldIdentifier.COMPONENT_HASH.label);

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

  static final String NO_MATCH_APPLICATION_FILTER_ID =
      DashboardIndexDimensionQueryBuilder.NO_MATCH_APPLICATION_FILTER_ID;

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

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final DashboardMetricsSqlModeProvider sqlModeProvider;

  private final DashboardMetricsSqlReadiness sqlReadiness;

  private final DashboardMetricsScopeResolver sqlScopeResolver;

  private final DashboardMetricsSqlCoordinator sqlCoordinator;

  private final DashboardMetricsShadowComparisonService shadowComparisonService;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final OwnerDAO ownerDAO;

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
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      DashboardMetricsSqlModeProvider sqlModeProvider,
      DashboardMetricsSqlReadiness sqlReadiness,
      DashboardMetricsScopeResolver sqlScopeResolver,
      DashboardMetricsSqlCoordinator sqlCoordinator,
      DashboardMetricsShadowComparisonService shadowComparisonService,
      DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      OwnerDAO ownerDAO,
      Configuration configuration,
      StageTypeService stageTypeService,
      CurrentUser currentUser)
  {
    this.searchIndexClient = searchIndexClient;
    this.metricFilterValidator = metricFilterValidator;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.sqlModeProvider = sqlModeProvider;
    this.sqlReadiness = sqlReadiness;
    this.sqlScopeResolver = sqlScopeResolver;
    this.sqlCoordinator = sqlCoordinator;
    this.shadowComparisonService = shadowComparisonService;
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.ownerDAO = ownerDAO;
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
    Instant requestStartedAt = Instant.now();
    DashboardMetricsSqlMode effectiveMode =
        sqlReadiness.effectiveMode(sqlModeProvider.configuredMode());
    Set<String> stageIds = request == null ? null : request.stageIds;
    MetricsLoadPlan plan = MetricsLoadPlan.from(request, effectiveMode, stageIds);
    ResolvedScope sqlScope = null;
    MetricFilterContext filterContext =
        plan.needsIndexFilterContext() ? buildMetricFilterContext(request) : MetricFilterContext.empty();
    if (plan.needsWaiverScope() || plan.needsMigratedScope()) {
      sqlScope = sqlScopeResolver.resolve(request);
    }
    boolean includeSummary = plan.includeSummary();
    boolean includeHeavy = plan.includeHeavy();
    boolean useSqlForMigratedMetrics = plan.useSqlForMigratedMetrics();

    MetricValueDTO applicationsMetric = null;
    MetricValueDTO organizationsMetric = null;
    MetricValueDTO policiesMetric = null;
    MetricValueDTO waiversMetric = null;
    if (includeSummary) {
      if (useSqlForMigratedMetrics) {
        applicationsMetric = sqlCoordinator.countApplications(sqlScope);
        organizationsMetric = sqlCoordinator.countOrganizations(sqlScope);
        policiesMetric = sqlCoordinator.countPolicies(sqlScope);
      }
      else if (filterContext.tagFilterUnsupported()) {
        // Oversized tag→appId set: degrade tag-scoped tiles instead of 400ing the dashboard.
        applicationsMetric = MetricValueDTO.unsupported(List.of("tagIds"));
        long organizationsStartedAt = System.nanoTime();
        organizationsMetric = new MetricValueDTO(
            searchIndexClient.count(buildFilteredMetricQuery(ItemType.ORGANIZATION, filterContext)),
            null,
            METRIC_SOURCE_INDEX);
        logBenchmarkDuration("organizations", organizationsStartedAt);
        policiesMetric = MetricValueDTO.unsupported(List.of("tagIds"));
      }
      else {
        long applicationsStartedAt = System.nanoTime();
        // Stage filter: match Martha Applications list — distinct applicationId on
        // POLICY_VIOLATION docs at policyEvaluationStage (includes waived/legacy). Unfiltered:
        // count APPLICATION docs.
        long applications = hasFilter(stageIds)
            ? searchIndexClient.countDistinct(
                buildFilteredMetricQuery(ItemType.POLICY_VIOLATION, filterContext),
                List.of(FieldIdentifier.APPLICATION_ID.label))
            : searchIndexClient.count(buildFilteredMetricQuery(ItemType.APPLICATION, filterContext));
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
      }

      // Waivers are SQL-backed: stageIds are not applicable (unsupported), but tagIds are applied by
      // the waivers scope query.
      if (hasFilter(stageIds)) {
        waiversMetric = MetricValueDTO.unsupported(List.of("stageIds"));
      }
      else {
        long waiversStartedAt = System.nanoTime();
        waiversMetric = countWaivers(sqlScope);
        logBenchmarkDuration("waivers", waiversStartedAt);
      }
    }

    MetricValueDTO violationsMetric = null;
    MetricValueDTO componentsMetric = null;
    MetricValueDTO vulnerabilitiesMetric = null;
    MetricValueDTO legalMetric = null;
    if (includeHeavy) {
      if (useSqlForMigratedMetrics) {
        violationsMetric = sqlCoordinator.countViolations(sqlScope);
      }
      else if (filterContext.tagFilterUnsupported()) {
        violationsMetric = MetricValueDTO.unsupported(List.of("tagIds"));
      }
      else {
        long violationsStartedAt = System.nanoTime();
        MetricAggregationResult violationsAggregation = searchIndexClient.aggregateCountByField(
            buildFilteredMetricQuery(ItemType.POLICY_VIOLATION, filterContext),
            FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
            VIOLATIONS_THREAT_LEVEL_BANDS);
        logBenchmarkDuration("violations", violationsStartedAt);
        violationsMetric = new MetricValueDTO(
            violationsAggregation.total, violationsAggregation.buckets, METRIC_SOURCE_INDEX);
      }
      if (filterContext.tagFilterUnsupported()) {
        componentsMetric = MetricValueDTO.unsupported(List.of("tagIds"));
        vulnerabilitiesMetric = MetricValueDTO.unsupported(List.of("tagIds"));
        legalMetric = MetricValueDTO.unsupported(List.of("tagIds"));
      }
      else {
        long componentsStartedAt = System.nanoTime();
        componentsMetric =
            new MetricValueDTO(countScannedComponents(filterContext, stageIds), null, METRIC_SOURCE_INDEX);
        logBenchmarkDuration("components", componentsStartedAt);
        long vulnerabilitiesStartedAt = System.nanoTime();
        vulnerabilitiesMetric = countVulnerabilities(filterContext);
        logBenchmarkDuration("vulnerabilities", vulnerabilitiesStartedAt);
        long legalStartedAt = System.nanoTime();
        legalMetric = countLegalObligations(filterContext);
        logBenchmarkDuration("legal", legalStartedAt);
      }
    }

    // Capture index freshness for every tier that can serve index-backed metrics — including
    // heavy-only requests — so SHADOW persistent classification is never snapshot-blind.
    Long lastUpdatedAt = null;
    if (includeSummary || includeHeavy) {
      lastUpdatedAt = searchIndexClient.getLastIndexTime();
    }

    DashboardMetricsDTO response = new DashboardMetricsDTO(
        applicationsMetric,
        violationsMetric,
        componentsMetric,
        organizationsMetric,
        policiesMetric,
        vulnerabilitiesMetric,
        legalMetric,
        waiversMetric,
        lastUpdatedAt);
    if (effectiveMode == DashboardMetricsSqlMode.SHADOW) {
      shadowComparisonService.maybeSchedule(
          request,
          response,
          requestStartedAt,
          response.lastUpdatedAt);
    }
    return response;
  }

  private MetricValueDTO countWaivers(final ResolvedScope scope) {
    if (scope.kind() == ResolvedScope.Kind.DENY_ALL) {
      if (scope.denyReason() == ResolvedScope.DenyReason.RESOLUTION_FAILED) {
        return MetricValueDTO.unavailable(METRIC_SOURCE_SQL);
      }
      return new MetricValueDTO(
          0L,
          Map.of("existing", 0L, "requested", 0L, "expiring", 0L),
          METRIC_SOURCE_SQL);
    }
    Set<String> accessibleOwnerIds = scope.ownerIds();
    Date now = new Date();
    Date upperBound = expiringCountUpperBound();
    // Both manual and auto-waivers are "already granted" — the Ana index (WAIVER doc type) indexes
    // both and the Waivers list page shows both, so the dashboard total counts both under
    // {@code existing}. {@code requested} covers only pending waiver requests.
    long manualWaivers = policyWaiverDAO.selectCount(accessibleOwnerIds);
    long autoWaivers = autoPolicyWaiverDAO.selectCount(accessibleOwnerIds);
    long existingWaivers = manualWaivers + autoWaivers;
    long requestedWaivers = policyWaiverRequestDAO.selectCount(accessibleOwnerIds);
    long expiringWaivers = policyWaiverDAO.selectExpiringCount(accessibleOwnerIds, now, upperBound);
    return new MetricValueDTO(
        existingWaivers + requestedWaivers,
        Map.of(
            "existing", existingWaivers,
            "requested", requestedWaivers,
            "expiring", expiringWaivers),
        METRIC_SOURCE_SQL);
  }

  /**
   * Classic {@code IN_7_DAYS} upper bound: start of the current UTC day + 7 days + 1 day.
   * Inclusive {@code le(upperBound)} so waivers expiring on the last calendar day of the
   * window are counted; matches {@code PolicyWaiverService} expiration-date filtering.
   */
  static Date expiringCountUpperBound() {
    return Date.from(
        Instant.now().truncatedTo(ChronoUnit.DAYS).plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS));
  }

  private static void logBenchmarkDuration(String metric, long startedAt) {
    // Fractional ms so sub-millisecond index counts are not collapsed to durationMs=0.
    log.debug("DASHBOARD_BENCHMARK metric={} durationMs={}", metric,
        (System.nanoTime() - startedAt) / 1_000_000.0);
  }

  private static boolean hasFilter(Set<String> ids) {
    return ids != null && !ids.isEmpty();
  }

  /**
   * Counts each scanned component hash once. Unfiltered: distinct hash across clean and vulnerable
   * component documents. With a stage filter: distinct hash on POLICY_VIOLATION docs at
   * {@code policyEvaluationStage}, matching Martha Components list violation-scoped discovery.
   */
  private long countScannedComponents(MetricFilterContext filterContext, Set<String> stageIds) {
    if (hasFilter(stageIds)) {
      return searchIndexClient.countDistinct(
          buildFilteredMetricQuery(ItemType.POLICY_VIOLATION, filterContext),
          COMPONENT_HASH_KEY_FIELDS);
    }
    return searchIndexClient.countDistinct(
        buildFilteredScannedComponentsQuery(filterContext),
        COMPONENT_HASH_KEY_FIELDS);
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
   * breakdown counts affected applications and distinct {@code (applicationId, componentHash)} pairs.
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
    // Tags are always resolved to application ids (OwnerDAO), never to applicationCategoryName.
    // Category names repeat across orgs; id resolution keeps every index tile on the same scope.
    Set<String> organizationIds = request == null ? null : request.organizationIds;
    Set<String> applicationIds = request == null ? null : request.applicationIds;
    Set<String> stageIds = request == null ? null : request.stageIds;
    Set<String> tagIds = request == null ? null : request.tagIds;
    Set<String> taggedIds = taggedApplicationIds(tagIds, applicationIds);
    boolean tagFilterUnsupported = false;
    String taggedApplicationClause = null;
    if (hasFilter(tagIds)) {
      int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
      // Soft-degrade when a broad tag expands past the Lucene clause cap — do not 400 the dashboard.
      if (taggedIds.size() > maxClauseCount) {
        tagFilterUnsupported = true;
      }
      else {
        taggedApplicationClause = dimensionQueryBuilder.buildEscapedApplicationFilterClause(taggedIds);
      }
    }
    return MetricFilterContext.of(
        dimensionQueryBuilder.buildOrganizationFilterClause(organizationIds),
        dimensionQueryBuilder.buildApplicationFilterClause(applicationIds),
        dimensionQueryBuilder.buildPolicyEvaluationStageFilterClause(stageIds),
        taggedApplicationClause,
        tagFilterUnsupported);
  }

  /**
   * Routing decisions for {@link #loadMetrics}: which tiers to load, whether SQL vs index serves
   * migrated summary/heavy tiles, and whether OwnerDAO tag resolution / SQL scope are needed.
   */
  private record MetricsLoadPlan(
      boolean includeSummary,
      boolean includeHeavy,
      boolean useSqlForMigratedMetrics,
      boolean needsWaiverScope,
      boolean needsMigratedScope,
      boolean needsIndexFilterContext)
  {
    static MetricsLoadPlan from(
        final DashboardMetricsRequestDTO request,
        final DashboardMetricsSqlMode effectiveMode,
        final Set<String> stageIds)
    {
      boolean compatibilityMode = request == null || request.includeHeavyMetrics == null;
      boolean includeSummary = compatibilityMode || Boolean.FALSE.equals(request.includeHeavyMetrics);
      boolean includeHeavy = compatibilityMode || Boolean.TRUE.equals(request.includeHeavyMetrics);
      boolean hasStageFilter = hasFilter(stageIds);
      // SHADOW serves the index path and schedules dual-run comparison; SQL serving is ON only.
      boolean useSqlServing = effectiveMode == DashboardMetricsSqlMode.ON;
      boolean useSqlForMigratedMetrics = useSqlServing && !hasStageFilter;
      boolean needsWaiverScope = includeSummary && !hasStageFilter;
      boolean needsMigratedScope = useSqlServing && useSqlForMigratedMetrics && (includeSummary || includeHeavy);
      // Index filter context resolves tag→appIds via OwnerDAO. Skip it when every selected tier is
      // SQL-served (summary-only + migrated SQL path) so tag filters do not pay unused DB lookups.
      boolean needsIndexFilterContext = includeHeavy || (includeSummary && !useSqlForMigratedMetrics);
      return new MetricsLoadPlan(
          includeSummary,
          includeHeavy,
          useSqlForMigratedMetrics,
          needsWaiverScope,
          needsMigratedScope,
          needsIndexFilterContext);
    }
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
    String dimensionClause = buildDimensionClause(organizationClause, applicationClause);

    List<String> clauses = new ArrayList<>();
    clauses.add(baseQuery);
    addIfPresent(clauses, dimensionClause);
    addIfPresent(clauses, stageClauseForItemType(itemType, filterContext));
    addIfPresent(clauses, taggedApplicationClauseForItemType(itemType, filterContext));
    return String.join(" AND ", clauses);
  }

  private static String buildDimensionClause(
      final String organizationClause,
      final String applicationClause)
  {
    if (organizationClause == null && applicationClause == null) {
      return null;
    }

    List<String> filterClauses = new ArrayList<>();
    if (organizationClause != null) {
      filterClauses.add(organizationClause);
    }
    if (applicationClause != null) {
      filterClauses.add(applicationClause);
    }
    return "(" + String.join(" OR ", filterClauses) + ")";
  }

  private static String buildFilteredScannedComponentsQuery(MetricFilterContext filterContext) {
    String baseQuery = "(itemType:" + ItemType.NON_VULNERABLE_COMPONENT.searchFieldName()
        + " OR itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName() + ")";
    String organizationClause = filterContext.organizationClause();
    String applicationClause = filterContext.applicationClause();
    String dimensionClause = buildDimensionClause(organizationClause, applicationClause);

    List<String> clauses = new ArrayList<>();
    clauses.add(baseQuery);
    addIfPresent(clauses, dimensionClause);
    // Stage is not applied on component docs — stage-filtered scanned components use
    // POLICY_VIOLATION / policyEvaluationStage via countScannedComponents instead.
    addIfPresent(clauses, filterContext.taggedApplicationClause());
    return String.join(" AND ", clauses);
  }

  private Set<String> taggedApplicationIds(final Set<String> tagIds, final Set<String> applicationIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return Set.of();
    }
    Set<String> safeApplicationIds = applicationIds == null ? null : new LinkedHashSet<>(applicationIds);
    Set<String> safeTagIds = new LinkedHashSet<>(tagIds);
    Set<String> taggedApplicationIds = ownerDAO.getOwnersByAppTagsAndOrgs(safeApplicationIds, safeTagIds, Set.of())
        .stream()
        .filter(owner -> owner != null && owner.getType() == OwnerType.APPLICATION)
        .map(Owner::getId)
        .filter(id -> id != null && !id.isBlank())
        .collect(Collectors.toSet());
    if (taggedApplicationIds.isEmpty()) {
      return Set.of(NO_MATCH_APPLICATION_FILTER_ID);
    }
    return taggedApplicationIds;
  }

  /**
   * Stage filters use {@code policyEvaluationStage} on violation/vulnerability/legal docs.
   * APPLICATION docs are not stage-filtered here — the Applications tile with a stage filter counts
   * distinct {@code applicationId} on POLICY_VIOLATION docs (Martha list parity).
   */
  private static String stageClauseForItemType(
      final ItemType itemType,
      final MetricFilterContext filterContext)
  {
    return switch (itemType) {
      case POLICY_VIOLATION, SECURITY_VULNERABILITY, LEGAL_VIOLATION -> filterContext.policyEvaluationStageClause();
      default -> null;
    };
  }

  private static String taggedApplicationClauseForItemType(
      final ItemType itemType,
      final MetricFilterContext filterContext)
  {
    return switch (itemType) {
      case APPLICATION, POLICY, POLICY_VIOLATION, SECURITY_VULNERABILITY, LEGAL_VIOLATION -> filterContext
          .taggedApplicationClause();
      default -> null;
    };
  }

  private static void addIfPresent(final List<String> clauses, final String clause) {
    if (clause != null) {
      clauses.add(clause);
    }
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

  /**
   * Lucene clauses for one metrics request. Factory methods name the two application-derived
   * clauses so a positional swap of {@code applicationClause} vs {@code taggedApplicationClause}
   * is harder to introduce unnoticed.
   */
  private record MetricFilterContext(
      String organizationClause,
      String applicationClause,
      String policyEvaluationStageClause,
      String taggedApplicationClause,
      boolean tagFilterUnsupported)
  {
    static MetricFilterContext empty() {
      return new MetricFilterContext(null, null, null, null, false);
    }

    static MetricFilterContext of(
        final String organizationClause,
        final String applicationClause,
        final String policyEvaluationStageClause,
        final String taggedApplicationClause,
        final boolean tagFilterUnsupported)
    {
      return new MetricFilterContext(
          organizationClause,
          applicationClause,
          policyEvaluationStageClause,
          taggedApplicationClause,
          tagFilterUnsupported);
    }
  }
}
