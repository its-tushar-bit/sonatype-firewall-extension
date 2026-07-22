/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;

/**
 * Builds Martha sidebar facet counts for the Violations list from RBAC-scoped index count queries.
 * <p>
 * Facet counts are computed against the same {@code violationQuery} as the list rows, so active sidebar
 * filters narrow both the result set and the facet counts (Martha V1 narrowing semantics; filter
 * interactions land in CLM-42258). The one exception is the single-select waiver-type facet, which is
 * counted against the query minus its own clause so the unselected option still shows a switchable count
 * (see {@link #buildFacets(String, String, long)}). Dimensions with a zero count are omitted.
 * <p>
 * On the PR-0 session path ({@code nexusOne.search.readPath.violations=new}), org/app facets use
 * {@link IndexReadSession#termsAggregation} against the shared session; small-vocabulary facets use
 * {@link IndexReadSession#count}. Legacy path keeps discovery + capped {@code SearchIndexClient.count}.
 * <p>
 * Session org/app buckets are exact {@code organizationId}/{@code applicationId} terms on the indexed
 * document (no parent→descendant rollup). The legacy path expands each discovered org via
 * {@link DashboardIndexDimensionQueryBuilder#buildOrganizationFilterClause}, so parent buckets can
 * include child-org violations. Session path prefers exact ownership for stable, non-overlapping
 * facet keys; list filtering still uses the dimension query builder (with descendant expansion) when
 * the user selects an organization.
 */
@Named
@Singleton
final class ViolationsListFacetsBuilder
{
  static final int MAX_FACET_DISCOVERY_HITS =
      Integer.getInteger("nexusOne.violations.facets.maxDiscoveryHits", 200);

  /**
   * Cap on organization facet keys returned (legacy: max count-query fan-out; session: maxBuckets).
   * Tunable via {@code nexusOne.violations.facets.maxOrganizationCountQueries} for continuity with
   * existing deployments that already set that property.
   */
  static final int MAX_ORGANIZATION_FACETS =
      Integer.getInteger("nexusOne.violations.facets.maxOrganizationCountQueries", 15);

  /**
   * Cap on application facet keys returned (legacy: max count-query fan-out; session: maxBuckets).
   * Tunable via {@code nexusOne.violations.facets.maxApplicationCountQueries} for continuity with
   * existing deployments that already set that property.
   */
  static final int MAX_APPLICATION_FACETS =
      Integer.getInteger("nexusOne.violations.facets.maxApplicationCountQueries", 15);

  /** Waiver-type facet keys (CLM-42261); mirrored by the frontend radio labels. */
  static final String WAIVER_TYPE_AUTO = "AUTO";

  static final String WAIVER_TYPE_MANUAL = "MANUAL";

  private final SearchIndexClient searchIndexClient;

  private final StageTypeService stageTypeService;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final ConversionHelper conversionHelper;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  ViolationsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService,
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      final ConversionHelper conversionHelper,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.conversionHelper = conversionHelper;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
  }

  /**
   * Convenience overload for callers with no active waiver-type filter, where the waiver-type facet is
   * counted against the same query as every other facet.
   */
  ViolationsListFacetsDTO buildFacets(final String violationQuery, final long totalViolations) {
    return buildFacets(violationQuery, violationQuery, totalViolations);
  }

  /**
   * @param violationQuery the fully-narrowed list query (all active filters) — used for every facet
   *          except waiver-type, matching the V1 narrowing semantics above.
   * @param waiverFacetQuery the same query with the waiver-type clause removed
   *          ({@link ViolationsListIndexQueryBuilder#buildViolationQueryExcludingWaiverType}).
   *          The waiver-type facet is a single-select radio, so counting it against the
   *          fully-narrowed query would zero out and hide the unselected option once one
   *          is picked; counting against this query instead keeps both AUTO and MANUAL
   *          showing the switchable count (still narrowed by every other active filter).
   *          When no waiver-type filter is active the two queries are identical.
   */
  ViolationsListFacetsDTO buildFacets(
      final String violationQuery,
      final String waiverFacetQuery,
      final long totalViolations)
  {
    ViolationsListFacetsDTO facets = new ViolationsListFacetsDTO();
    facets.totalViolations = totalViolations;
    if (totalViolations == 0) {
      return facets;
    }

    ToLongFunction<String> counter = searchIndexClient::count;
    facets.states = countStates(violationQuery, counter);
    facets.waiverTypes = countWaiverTypes(waiverFacetQuery, counter);
    facets.threatCategories = countThreatCategories(violationQuery, counter);
    facets.stages = countLicensedStages(violationQuery, counter);

    LinkedHashMap<String, SearchResultItemDTO> discovered = discoverViolationItems(violationQuery);
    if (!discovered.isEmpty()) {
      facets.organizations = countOrganizations(violationQuery, discovered);
      facets.applications = countApplications(violationQuery, discovered);
    }
    attachOwnerLabels(facets, discovered);
    return facets;
  }

  /**
   * Session-path facets: share the caller's {@link IndexReadSession} (one RBAC compile + one snapshot).
   * String queries are the same Martha query strings as the legacy path; conversion happens here so
   * we never round-trip {@link Query#toString()}.
   */
  ViolationsListFacetsDTO buildFacets(
      final IndexReadSession session,
      final String violationQuery,
      final String waiverFacetQuery,
      final long totalViolations)
  {
    ViolationsListFacetsDTO facets = new ViolationsListFacetsDTO();
    facets.totalViolations = totalViolations;
    if (totalViolations == 0) {
      return facets;
    }

    Query sessionViolationQuery = conversionHelper.stringToQuery(violationQuery);
    ToLongFunction<String> counter = query -> sessionCount(session, query);

    facets.states = countStates(violationQuery, counter);
    facets.waiverTypes = countWaiverTypes(waiverFacetQuery, counter);
    facets.threatCategories = countThreatCategories(violationQuery, counter);
    facets.stages = countLicensedStages(violationQuery, counter);
    facets.organizations = countOrganizations(session, sessionViolationQuery);
    facets.applications = countApplications(session, sessionViolationQuery);
    attachOwnerLabels(facets);
    return facets;
  }

  /** Session path: no discovery hits; resolve names via DAO for facet keys. */
  private void attachOwnerLabels(final ViolationsListFacetsDTO facets) {
    attachOwnerLabels(facets, null);
  }

  /**
   * Resolve friendly org/app display names for facet keys so the Martha rail never has to show raw
   * internal ids. Prefers names already present on discovery hits (legacy path), then fills gaps with
   * a batched DAO lookup for the facet key set.
   */
  private void attachOwnerLabels(
      final ViolationsListFacetsDTO facets,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    Map<String, String> organizationNames = new LinkedHashMap<>();
    Map<String, String> applicationNames = new LinkedHashMap<>();

    if (discovered != null) {
      for (SearchResultItemDTO item : discovered.values()) {
        if (StringUtils.isNotBlank(item.organizationId) && StringUtils.isNotBlank(item.organizationName)) {
          organizationNames.putIfAbsent(item.organizationId, item.organizationName);
        }
        if (StringUtils.isNotBlank(item.applicationId) && StringUtils.isNotBlank(item.applicationName)) {
          applicationNames.putIfAbsent(item.applicationId, item.applicationName);
        }
      }
    }

    Set<String> missingOrganizationIds = missingLabelIds(facets.organizations, organizationNames);
    if (!missingOrganizationIds.isEmpty()) {
      for (Organization organization : organizationDAO.getByIds(missingOrganizationIds)) {
        if (organization != null && StringUtils.isNotBlank(organization.getId())
            && StringUtils.isNotBlank(organization.getName()))
        {
          organizationNames.putIfAbsent(organization.getId(), organization.getName());
        }
      }
    }

    Set<String> missingApplicationIds = missingLabelIds(facets.applications, applicationNames);
    if (!missingApplicationIds.isEmpty()) {
      for (Application application : applicationDAO.getByIds(missingApplicationIds)) {
        if (application != null && StringUtils.isNotBlank(application.getId())
            && StringUtils.isNotBlank(application.getName()))
        {
          applicationNames.putIfAbsent(application.getId(), application.getName());
        }
      }
    }

    facets.organizationNames = organizationNames.isEmpty() ? null : organizationNames;
    facets.applicationNames = applicationNames.isEmpty() ? null : applicationNames;
  }

  private static Set<String> missingLabelIds(
      final Map<String, Long> counts,
      final Map<String, String> knownNames)
  {
    if (counts == null || counts.isEmpty()) {
      return Set.of();
    }
    Set<String> missing = new HashSet<>();
    for (String id : counts.keySet()) {
      if (StringUtils.isNotBlank(id) && !knownNames.containsKey(id)) {
        missing.add(id);
      }
    }
    return missing;
  }

  private Map<String, Long> countStates(final String violationQuery, final ToLongFunction<String> counter) {
    Map<String, Long> counts = new LinkedHashMap<>();
    String waivedClause = waivedClause();
    long open = counter.applyAsLong(violationQuery + " AND NOT (" + waivedClause + ")");
    if (open > 0) {
      counts.put(PolicyViolationState.OPEN.name(), open);
    }
    long waived = counter.applyAsLong(violationQuery + " AND " + waivedClause);
    if (waived > 0) {
      counts.put(PolicyViolationState.WAIVED.name(), waived);
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countWaiverTypes(final String waiverFacetQuery, final ToLongFunction<String> counter) {
    Map<String, Long> counts = new LinkedHashMap<>();
    long autoWaived = counter.applyAsLong(waiverFacetQuery + " AND "
        + ViolationsListIndexQueryBuilder.waiverStatusClause(ViolationWaiverStatus.AUTO_WAIVED));
    if (autoWaived > 0) {
      counts.put(WAIVER_TYPE_AUTO, autoWaived);
    }
    long manualWaived = counter.applyAsLong(waiverFacetQuery + " AND "
        + ViolationsListIndexQueryBuilder.waiverStatusClause(ViolationWaiverStatus.WAIVED));
    if (manualWaived > 0) {
      counts.put(WAIVER_TYPE_MANUAL, manualWaived);
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countThreatCategories(
      final String violationQuery,
      final ToLongFunction<String> counter)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      String clause = FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(category.getName()) + ")";
      long count = counter.applyAsLong(violationQuery + " AND " + clause);
      if (count > 0) {
        counts.put(category.getName(), count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countLicensedStages(
      final String violationQuery,
      final ToLongFunction<String> counter)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      String stageId = stageType.getId();
      String clause = FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId) + ")";
      long count = counter.applyAsLong(violationQuery + " AND " + clause);
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

    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String organizationId : organizationIds) {
      if (queries >= MAX_ORGANIZATION_FACETS) {
        break;
      }
      String orgClause = dimensionQueryBuilder.buildOrganizationFilterClause(Set.of(organizationId));
      if (orgClause == null) {
        continue;
      }
      counts.put(organizationId, searchIndexClient.count(violationQuery + " AND " + orgClause));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * Exact-match org buckets from the index (see class Javadoc). Caps at {@link #MAX_ORGANIZATION_FACETS}.
   */
  private Map<String, Long> countOrganizations(final IndexReadSession session, final Query violationQuery) {
    List<IndexTermsBucket> buckets = session.termsAggregation(
        violationQuery,
        FieldIdentifier.ORGANIZATION_ID.label,
        MAX_ORGANIZATION_FACETS);
    return bucketsToCounts(buckets);
  }

  private Map<String, Long> countApplications(
      final String violationQuery,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String applicationId : discovered.keySet()) {
      if (queries >= MAX_APPLICATION_FACETS) {
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

  private Map<String, Long> countApplications(final IndexReadSession session, final Query violationQuery) {
    List<IndexTermsBucket> buckets = session.termsAggregation(
        violationQuery,
        FieldIdentifier.APPLICATION_ID.label,
        MAX_APPLICATION_FACETS);
    return bucketsToCounts(buckets);
  }

  private static Map<String, Long> bucketsToCounts(final List<IndexTermsBucket> buckets) {
    if (buckets == null || buckets.isEmpty()) {
      return null;
    }
    Map<String, Long> counts = new LinkedHashMap<>();
    for (IndexTermsBucket bucket : buckets) {
      if (bucket.count() > 0 && StringUtils.isNotBlank(bucket.key())) {
        counts.put(bucket.key(), bucket.count());
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private long sessionCount(final IndexReadSession session, final String query) {
    return session.count(conversionHelper.stringToQuery(query));
  }

  private static String waivedClause() {
    return FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":("
        + ViolationWaiverStatus.WAIVED + " " + ViolationWaiverStatus.AUTO_WAIVED + ")";
  }
}
