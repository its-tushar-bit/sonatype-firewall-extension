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

import com.sonatype.insight.brain.dashboard.IndexGroupedCountKeys;
import com.sonatype.insight.brain.dashboard.PolicyViolationIndexClauses;
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
import com.sonatype.insight.brain.search.index.IdSetFilterQueries;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.search.index.ItemType;
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

  private final ConversionHelper conversionHelper;

  @Inject
  ApplicationsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final ConversionHelper conversionHelper)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.conversionHelper = conversionHelper;
  }

  ApplicationsListFacetsDTO buildFacets(
      final String applicationQuery,
      final List<IndexFilterRestriction> scopeRestrictions,
      final long totalApplications)
  {
    List<IndexFilterRestriction> restrictions = scopeRestrictions == null ? List.of() : scopeRestrictions;
    ApplicationsListFacetsDTO facets = new ApplicationsListFacetsDTO();
    facets.totalApplications = totalApplications;

    LinkedHashMap<String, SearchResultItemDTO> discovered =
        discoverApplicationItems(applicationQuery, restrictions);
    if (discovered.isEmpty()) {
      return facets;
    }

    String violationFacetQuery = toViolationFacetQuery(applicationQuery);
    facets.organizations = countOrganizations(applicationQuery, discovered, restrictions);
    facets.applications = applicationFacetCountsFromDiscovery(discovered);
    facets.stages = countLicensedStages(applicationQuery, restrictions);
    facets.policyTypes = countPolicyTypes(
        categoryNames -> searchIndexClient.countDistinctGroupedBy(
            violationFacetQuery,
            FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label,
            FieldIdentifier.APPLICATION_ID.label,
            categoryNames,
            restrictions));
    facets.violationStates = countViolationStates(
        stateClause -> searchIndexClient.countDistinct(
            violationFacetQuery + " AND " + stateClause,
            List.of(FieldIdentifier.APPLICATION_ID.label),
            restrictions));
    attachDisplayNames(facets, discovered);
    return facets;
  }

  ApplicationsListFacetsDTO buildFacets(
      final IndexReadSession session,
      final Query applicationQuery,
      final Query violationQuery,
      final List<IndexFilterRestriction> scopeRestrictions,
      final String violationFacetQuery,
      final long totalApplications)
  {
    ApplicationsListFacetsDTO facets = new ApplicationsListFacetsDTO();
    facets.totalApplications = totalApplications;

    LinkedHashMap<String, SearchResultItemDTO> discovered = discoverApplicationItems(session, applicationQuery);
    if (discovered.isEmpty()) {
      return facets;
    }

    List<IndexFilterRestriction> restrictions = scopeRestrictions == null ? List.of() : scopeRestrictions;
    facets.organizations = countOrganizations(session, applicationQuery, discovered);
    facets.applications = applicationFacetCountsFromDiscovery(discovered);
    facets.stages = countLicensedStages(session, violationQuery);
    facets.policyTypes = countPolicyTypes(
        categoryNames -> session.countDistinctGroupedBy(
            violationQuery,
            FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label,
            FieldIdentifier.APPLICATION_ID.label,
            categoryNames));
    facets.violationStates = countViolationStates(
        stateClause -> countDistinctApplications(session, violationFacetQuery, stateClause, restrictions));
    // See legacy overload: display-name maps are required for both read paths.
    attachDisplayNames(facets, discovered);
    return facets;
  }

  /**
   * Policy-type buckets (CLM-43211) from one grouped pass over {@code policyViolationThreatCategory}.
   * The field is single-valued per violation doc, so each bucket is already an exact
   * distinct-application count. Like stage facets, this degrades to no facet on backends without
   * grouped distinct counts rather than falling back to a per-category scan.
   */
  private Map<String, Long> countPolicyTypes(final GroupedDistinctCounter counter) {
    List<String> categoryNames = new ArrayList<>();
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      categoryNames.add(category.getName());
    }

    Map<String, Long> grouped;
    try {
      grouped = counter.countDistinctGroupedBy(categoryNames);
    }
    catch (UnsupportedOperationException e) {
      log.warn("Applications list policy-type facets unavailable on this index backend; omitting them", e);
      return null;
    }
    if (grouped == null || grouped.isEmpty()) {
      return null;
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String categoryName : categoryNames) {
      long count = grouped.getOrDefault(categoryName, 0L);
      if (count > 0) {
        counts.put(categoryName, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * Violation-state buckets (CLM-43211): one distinct-application count per state, so three constant
   * round trips regardless of estate size.
   * <p>
   * These cannot be folded into a single grouped pass over {@code policyViolationWaiverStatus}. WAIVED
   * spans two indexed statuses (Waived and AutoWaived) and distinct-application counts from separate
   * buckets cannot be summed - an application holding one manually-waived and one auto-waived violation
   * appears in both buckets and would be double-counted. OPEN is the complement of the excluded
   * statuses, so it has no bucket of its own at all.
   */
  private Map<String, Long> countViolationStates(final StateDistinctCounter counter) {
    Map<String, Long> counts = new LinkedHashMap<>();
    try {
      // The violation query is the positive anchor for the OPEN negation, so the non-anchored clause is
      // correct here - matching ViolationsListFacetsBuilder.countStates.
      putWhenPositive(counts, PolicyViolationState.OPEN.name(),
          counter.countDistinctApplications(PolicyViolationIndexClauses.openClause(false)));
      putWhenPositive(counts, PolicyViolationState.WAIVED.name(),
          counter.countDistinctApplications(PolicyViolationIndexClauses.waivedClause()));
      // Pure-legacy only: a violation that is both waived and legacy indexes as Waived by precedence and
      // is counted under WAIVED above (see ViolationWaiverStatus).
      putWhenPositive(counts, PolicyViolationState.LEGACY_VIOLATION.name(),
          counter.countDistinctApplications(PolicyViolationIndexClauses.legacyClause()));
    }
    catch (UnsupportedOperationException e) {
      log.warn("Applications list violation-state facets unavailable on this index backend; omitting them", e);
      return null;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * Distinct applications matching one violation state on the session path.
   * <p>
   * {@link IndexReadSession} has no plain distinct count, so this groups by {@code itemType}: the query
   * is already scoped to POLICY_VIOLATION docs, so every match lands in that single bucket and the
   * bucket value is the distinct-application count for the state.
   */
  private long countDistinctApplications(
      final IndexReadSession session,
      final String violationFacetQuery,
      final String stateClause,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    Query stateQuery = conversionHelper.stringToQuery(
        ApplicationsListService.toSessionQueryString(violationFacetQuery + " AND " + stateClause));
    stateQuery = IdSetFilterQueries.toScopedQuery(stateQuery, scopeRestrictions);
    // Both backends key the grouped map by the lowercased group value - see IndexGroupedCountKeys.
    String policyViolationBucket = IndexGroupedCountKeys.lookupKey(ItemType.POLICY_VIOLATION.name());
    Map<String, Long> grouped = session.countDistinctGroupedBy(
        stateQuery,
        FieldIdentifier.ITEM_TYPE.label,
        FieldIdentifier.APPLICATION_ID.label,
        List.of(policyViolationBucket));
    return grouped == null ? 0L : grouped.getOrDefault(policyViolationBucket, 0L);
  }

  private static void putWhenPositive(final Map<String, Long> counts, final String key, final long count) {
    if (count > 0) {
      counts.put(key, count);
    }
  }

  /** Read-path-specific grouped distinct count, so facet shaping is written once for both paths. */
  @FunctionalInterface
  private interface GroupedDistinctCounter
  {
    Map<String, Long> countDistinctGroupedBy(List<String> groupValues);
  }

  /** Read-path-specific distinct-application count for one violation-state clause. */
  @FunctionalInterface
  private interface StateDistinctCounter
  {
    long countDistinctApplications(String stateClause);
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

  private LinkedHashMap<String, SearchResultItemDTO> discoverApplicationItems(
      final String applicationQuery,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(applicationQuery, MAX_FACET_DISCOVERY_HITS, 0, false, false, List.of(),
            scopeRestrictions);
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
      final LinkedHashMap<String, SearchResultItemDTO> discovered,
      final List<IndexFilterRestriction> scopeRestrictions)
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
      // APPLICATION documents store only the direct-parent organizationId (not ancestor ids). Match
      // that field exactly — unlike Violations/Legal facets, which expand to descendants because
      // violation docs can sit under child orgs while still belonging to the selected parent.
      List<IndexFilterRestriction> combined = IdSetFilterQueries.combine(scopeRestrictions,
          IndexTermSetRestriction.singleton(FieldIdentifier.ORGANIZATION_ID.label, Set.of(organizationId)));
      counts.put(organizationId, searchIndexClient.count(applicationQuery, combined));
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
      // sessions omit stages (org/app facets still return) - accepted V1 degradation.
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

  private Map<String, Long> countLicensedStages(
      final String applicationQuery,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    String violationQuery = toViolationFacetQuery(applicationQuery);
    Map<String, Long> counts = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      String stageId = stageType.getId();
      String stageClause = ApplicationsListViolationQuerySupport.buildStageFilterClause(Set.of(stageId));
      long count = searchIndexClient.countDistinct(
          violationQuery + " AND " + stageClause,
          List.of(FieldIdentifier.APPLICATION_ID.label),
          scopeRestrictions);
      if (count > 0) {
        counts.put(stageId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private static String toViolationFacetQuery(final String applicationQuery) {
    return ApplicationsListViolationQuerySupport.toViolationQuery(applicationQuery);
  }
}
