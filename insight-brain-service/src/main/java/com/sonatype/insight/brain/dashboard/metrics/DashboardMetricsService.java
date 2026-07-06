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
import jakarta.ws.rs.BadRequestException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
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

  static final String METRIC_SOURCE_SQL = "sql";

  static final Map<String, int[]> VIOLATIONS_THREAT_LEVEL_BANDS = ThreatLevel.searchAggregationBands();

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

  private static final List<String> VULNERABILITY_KEY_FIELDS = List.of(
      FieldIdentifier.APPLICATION_ID.label,
      FieldIdentifier.COMPONENT_HASH.label,
      FieldIdentifier.VULNERABILITY_ID.label);

  private static final List<String> LEGAL_OBLIGATION_KEY_FIELDS = List.of(
      FieldIdentifier.APPLICATION_ID.label,
      FieldIdentifier.COMPONENT_HASH.label,
      FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID.label);

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

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final DashboardMetricsWaiverScopeService waiverScopeService;

  private final Configuration configuration;

  private final CurrentUser currentUser;

  private final TenantReference<Cache<DashboardMetricsCacheKey, DashboardMetricsDTO>> caches;

  @Inject
  public DashboardMetricsService(
      SearchIndexClient searchIndexClient,
      MetricFilterValidator metricFilterValidator,
      OrganizationDAO organizationDAO,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyWaiverRequestDAO policyWaiverRequestDAO,
      DashboardMetricsWaiverScopeService waiverScopeService,
      Configuration configuration,
      CurrentUser currentUser)
  {
    this.searchIndexClient = searchIndexClient;
    this.metricFilterValidator = metricFilterValidator;
    this.organizationDAO = organizationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
    this.waiverScopeService = waiverScopeService;
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
   * Loads each metric independently against the search index. {@code count}, {@code countDistinct}, and
   * {@code aggregateCountByField} each open their own reader/snapshot today, so applications, components, and
   * violations are not guaranteed to come from the same point-in-time view (acceptable behind the 5s coalescing
   * cache). Cheap-tier metrics (organizations, policies, vulnerabilities, legal) add further independent
   * round-trips. Waivers use two SQL {@code selectCount} queries scoped via
   * {@link DashboardMetricsWaiverScopeService}. A batched {@link SearchIndexClient} entry point (e.g. OpenSearch
   * multi-search) may consolidate index round-trips later.
   */
  private DashboardMetricsDTO loadMetrics(DashboardMetricsRequestDTO request) {
    MetricFilterContext filterContext = buildMetricFilterContext(request);

    long applications = searchIndexClient.count(
        buildFilteredMetricQuery(ItemType.APPLICATION, filterContext));

    MetricAggregationResult violationsAggregation = searchIndexClient.aggregateCountByField(
        buildFilteredMetricQuery(ItemType.POLICY_VIOLATION, filterContext),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        VIOLATIONS_THREAT_LEVEL_BANDS);

    long components = countScannedComponents(filterContext);
    Long lastUpdatedAt = searchIndexClient.getLastIndexTime();

    MetricValueDTO applicationsMetric = new MetricValueDTO(applications, null, METRIC_SOURCE_INDEX);
    MetricValueDTO violationsMetric = new MetricValueDTO(
        violationsAggregation.total, violationsAggregation.buckets, METRIC_SOURCE_INDEX);
    MetricValueDTO componentsMetric = new MetricValueDTO(components, null, METRIC_SOURCE_INDEX);

    // Organizations / Policies / Vulnerabilities: index-native totals (CLM-40927 cheap-tier). Policy custom/system
    // breakdown is not an indexed field; vulnerability severity uses decimal CVSS scores (not int-range buckets here).
    MetricValueDTO organizationsMetric = new MetricValueDTO(
        searchIndexClient.count(buildFilteredMetricQuery(ItemType.ORGANIZATION, filterContext)),
        null,
        METRIC_SOURCE_INDEX);
    MetricValueDTO policiesMetric = new MetricValueDTO(
        searchIndexClient.count(buildFilteredMetricQuery(ItemType.POLICY, filterContext)),
        null,
        METRIC_SOURCE_INDEX);
    MetricValueDTO vulnerabilitiesMetric = new MetricValueDTO(
        countDistinctSecurityVulnerabilities(filterContext),
        null,
        METRIC_SOURCE_INDEX);

    // Legal Obligations: stage-independent distinct keys align with the Scanned Components tile
    // ([applicationId, componentHash]) and de-dupe multi-stage re-indexes. Documents without a componentHash
    // field collapse to a single distinct bucket in countDistinct; real scan data always indexes a hash.
    MetricValueDTO legalMetric = countLegalObligations(filterContext);

    Set<String> accessibleOwnerIds = waiverScopeService.resolveAccessibleOwnerIds(request);
    long existingWaivers = policyWaiverDAO.selectCount(accessibleOwnerIds);
    long requestedWaivers = policyWaiverRequestDAO.selectCount(accessibleOwnerIds);
    MetricValueDTO waiversMetric = new MetricValueDTO(
        existingWaivers + requestedWaivers,
        Map.of("existing", existingWaivers, "requested", requestedWaivers),
        METRIC_SOURCE_SQL);

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
   * Counts each indexed CVE once per {@code (applicationId, componentHash, vulnerabilityId)} so multi-stage
   * evaluations do not inflate the tile (unlike raw {@link SearchIndexClient#count(String)} on
   * {@link ItemType#SECURITY_VULNERABILITY} docs, which are stage-tagged).
   */
  private long countDistinctSecurityVulnerabilities(MetricFilterContext filterContext) {
    return searchIndexClient.countDistinct(
        buildFilteredMetricQuery(ItemType.SECURITY_VULNERABILITY, filterContext),
        VULNERABILITY_KEY_FIELDS);
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
        buildOrganizationFilterClause(request.organizationIds),
        buildApplicationFilterClause(request.applicationIds));
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
