/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.ViolationWaiverStatus;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.integration.ApplicationSummaryService;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.IdSetFilterQueries;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
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
 * Legacy path ({@code nexusOne.search.readPath.violations=old}): every facet — {@code states},
 * {@code threatCategories}, {@code stages}, {@code organizations}, {@code applications} — is counted
 * against the same {@code violationQuery} as the list rows, so active sidebar filters narrow both the
 * result set and the facet counts. {@code waiverTypes} is the one exception on this path, counted
 * against {@code waiverFacetQuery} (the query minus its own clause) instead (see
 * {@link #buildFacets(String, String, long)}).
 * <p>
 * Session path ({@code =new}): every facet is counted against a base query with only that facet's own
 * clause removed — owner (org/app), waiver-type, state, threat-category, and stage each aggregate over
 * a base with every OTHER active filter still applied but their own clause omitted — so selecting a
 * value in one facet never collapses the other values in that same facet to zero (see
 * {@link #buildFacets(IndexReadSession, String, String, long, OwnerFacetBase, FixedFacetBases)}).
 * Dimensions with a zero count are omitted.
 * <p>
 * On the session path the owner facets are one {@link IndexReadSession#termsAggregation} each and the
 * small-vocabulary facets are {@link IndexReadSession#count} calls, all on the shared session. Organization
 * buckets are the {@code parentOrganizationId} ancestor closure, so a parent's bucket counts its whole
 * subtree and matches what selecting that organization as a list filter returns; the root organization and
 * any organization outside the caller's read scope are dropped before the display cap. When the rail is
 * being name-searched, the matching organizations are counted via
 * {@link DashboardIndexDimensionQueryBuilder#buildOrganizationFilterClausesById} against the same
 * owner-removed base, so searching while a selection is active still shows the other matches' real counts.
 * <p>
 * The legacy path discovers owners from capped list hits and counts each with
 * {@code SearchIndexClient.count}, likewise one {@code parentOrganizationId} ancestor-match term per
 * organization, so there is no descendant expansion or clause cap.
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
   * Candidate buckets requested for the hierarchical organization facet, before root exclusion, the read
   * gate and the {@link #MAX_ORGANIZATION_FACETS} display cap.
   * <p>
   * Sized well above the display cap rather than as a small multiple of it: the aggregation returns top-N by
   * document count and ancestor-closure counts accumulate toward the root, so every ancestor outranks the
   * leaves beneath it. A caller who can read only a low-count leaf would otherwise find the candidate window
   * filled by higher-count ancestors it cannot read, and the read gate can only filter what the aggregation
   * returned. Matches the applications rail and the index-query facets.
   */
  static final int MAX_ORGANIZATION_FACET_CANDIDATES = 500;

  /**
   * Cap on application facet keys returned (legacy: max count-query fan-out; session: maxBuckets).
   * Tunable via {@code nexusOne.violations.facets.maxApplicationCountQueries} for continuity with
   * existing deployments that already set that property.
   */
  static final int MAX_APPLICATION_FACETS =
      Integer.getInteger("nexusOne.violations.facets.maxApplicationCountQueries", 15);

  /**
   * Max name-search candidates fetched before the {@code count > 0} gate. Larger than the display cap
   * so alphabetically-early zero-count owners do not hide later owners that have violations.
   */
  static final int MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES =
      Integer.getInteger(
          "nexusOne.violations.facets.maxOrganizationNameSearchCandidates",
          MAX_ORGANIZATION_FACETS * 10);

  static final int MAX_APPLICATION_FACET_SEARCH_CANDIDATES =
      Integer.getInteger(
          "nexusOne.violations.facets.maxApplicationNameSearchCandidates",
          MAX_APPLICATION_FACETS * 10);

  /** Waiver-type facet keys; mirrored by the frontend radio labels. */
  static final String WAIVER_TYPE_AUTO = "AUTO";

  static final String WAIVER_TYPE_MANUAL = "MANUAL";

  private final SearchIndexClient searchIndexClient;

  private final StageTypeService stageTypeService;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final ConversionHelper conversionHelper;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationSummaryService organizationSummaryService;

  private final ApplicationSummaryService applicationSummaryService;

  @Inject
  ViolationsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService,
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      final ConversionHelper conversionHelper,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationSummaryService organizationSummaryService,
      final ApplicationSummaryService applicationSummaryService)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.conversionHelper = conversionHelper;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.organizationSummaryService = organizationSummaryService;
    this.applicationSummaryService = applicationSummaryService;
  }

  /**
   * Convenience overload for callers with no active waiver-type filter, where the waiver-type facet is
   * counted against the same query as every other facet.
   */
  ViolationsListFacetsDTO buildFacets(final String violationQuery, final long totalViolations) {
    return buildFacets(violationQuery, violationQuery, totalViolations, null, null, List.of());
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
    return buildFacets(violationQuery, waiverFacetQuery, totalViolations, null, null, List.of());
  }

  /**
   * Legacy-path facets with optional org/app name search.
   * <p>
   * When {@code organizationFacetSearch} / {@code applicationFacetSearch} is non-blank, that owner
   * facet map is replaced with DAO name-substring matches that still have a positive count under
   * {@code violationQuery} (same cap as the uncapped top-N map). Blank keeps top-by-count discovery.
   */
  ViolationsListFacetsDTO buildFacets(
      final String violationQuery,
      final String waiverFacetQuery,
      final long totalViolations,
      final String organizationFacetSearch,
      final String applicationFacetSearch,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    ViolationsListFacetsDTO facets = new ViolationsListFacetsDTO();
    facets.totalViolations = totalViolations;

    List<IndexFilterRestriction> restrictions = scopeRestrictions == null ? List.of() : scopeRestrictions;
    ToLongFunction<String> counter = query -> searchIndexClient.count(query, restrictions);
    facets.states = countStates(violationQuery, counter);
    facets.waiverTypes = countWaiverTypes(waiverFacetQuery, counter);
    facets.threatCategories = countThreatCategories(violationQuery, counter);
    facets.stages = countLicensedStages(violationQuery, counter);

    boolean organizationSearch = StringUtils.isNotBlank(organizationFacetSearch);
    boolean applicationSearch = StringUtils.isNotBlank(applicationFacetSearch);
    LinkedHashMap<String, SearchResultItemDTO> discovered =
        organizationSearch && applicationSearch
            ? new LinkedHashMap<>()
            : discoverViolationItems(violationQuery, restrictions);
    BiFunction<String, List<IndexFilterRestriction>, Long> scopedCounter =
        (query, combined) -> searchIndexClient.count(query, combined);
    if (organizationSearch) {
      applyOrganizationFacetSearch(
          facets,
          countOrganizationsMatchingName(violationQuery, organizationFacetSearch, restrictions, scopedCounter));
    }
    else if (!discovered.isEmpty()) {
      facets.organizations = countOrganizations(violationQuery, discovered, restrictions);
    }
    if (applicationSearch) {
      applyApplicationFacetSearch(
          facets,
          countApplicationsMatchingName(violationQuery, applicationFacetSearch, restrictions, scopedCounter));
    }
    else if (!discovered.isEmpty()) {
      facets.applications = countApplications(violationQuery, discovered, restrictions);
    }
    attachOwnerLabels(facets, discovered);
    return facets;
  }

  /**
   * Per-dimension facet base queries for the fixed/small-vocabulary Violations facets on the session
   * path, each with only that facet's own clause removed from the fully-narrowed query (every other
   * active filter still applied) — see
   * {@link ViolationsListIndexQueryBuilder#buildViolationQueryExcludingState},
   * {@link ViolationsListIndexQueryBuilder#buildViolationQueryExcludingThreatCategory}, and
   * {@link ViolationsListIndexQueryBuilder#buildViolationQueryExcludingStage}.
   */
  record FixedFacetBases(
      String stateRemovedBase,
      String threatCategoryRemovedBase,
      String stageRemovedBase)
  {
  }

  /**
   * The owner-dimension facet base, in both forms the counting paths need, plus the rail's name-search
   * terms.
   * <p>
   * Organization and application are one owner dimension, so both count against a base with the whole
   * dimension removed. Name search counts the same way: a user typing in the organization rail while an
   * organization is selected must still see the other matches, not just the selection.
   *
   * @param query owner-removed base for the doc-values aggregations
   * @param queryString the same base as a query string, for the name-search clause counting
   * @param organizationSearch organization rail name-search term, blank when the rail is not being searched
   * @param applicationSearch application rail name-search term, blank when the rail is not being searched
   */
  record OwnerFacetBase(Query query, String queryString, String organizationSearch, String applicationSearch)
  {
    boolean hasOrganizationSearch() {
      return StringUtils.isNotBlank(organizationSearch);
    }

    boolean hasApplicationSearch() {
      return StringUtils.isNotBlank(applicationSearch);
    }
  }

  /**
   * Session-path facets with owner-removed base.
   * <p>
   * Owner facets (org + app) aggregate over the owner-removed base so selecting an org/app
   * does NOT collapse the org/app rails. Org facet uses PARENT_ORGANIZATION_ID (hierarchical
   * closure) and excludes root org before the display cap. The {@code states}, {@code threatCategories},
   * and {@code stages} facets each aggregate over their own clause-removed base from
   * {@code fixedFacetBases} for the same no-collapse reason.
   *
   * The fully-narrowed list query is deliberately absent: no facet counts against it, since every dimension
   * counts against a base with its own clause removed.
   *
   * @param session the index read session
   * @param waiverFacetQuery the query with waiver-type clause removed
   * @param totalViolations total violation count
   * @param ownerFacetBase the owner-removed base plus the rail's name-search terms
   * @param fixedFacetBases per-dimension base queries for {@code states}/{@code threatCategories}/{@code stages}
   */
  ViolationsListFacetsDTO buildFacets(
      final IndexReadSession session,
      final String waiverFacetQuery,
      final long totalViolations,
      final OwnerFacetBase ownerFacetBase,
      final FixedFacetBases fixedFacetBases,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    ViolationsListFacetsDTO facets = new ViolationsListFacetsDTO();
    facets.totalViolations = totalViolations;

    // Fixed-vocabulary facets keep the caller's organization/application scope; each already counts
    // against a base with only its own clause removed.
    List<IndexFilterRestriction> restrictions = scopeRestrictions == null ? List.of() : scopeRestrictions;
    ToLongFunction<String> counter = query -> sessionCount(session, query, restrictions);

    facets.states = countStates(fixedFacetBases.stateRemovedBase(), counter);
    facets.waiverTypes = countWaiverTypes(waiverFacetQuery, counter);
    facets.threatCategories = countThreatCategories(fixedFacetBases.threatCategoryRemovedBase(), counter);
    facets.stages = countLicensedStages(fixedFacetBases.stageRemovedBase(), counter);
    BiFunction<String, List<IndexFilterRestriction>, Long> scopedCounter =
        (query, combined) -> sessionCount(session, query, combined);
    // Org and app are ONE owner dimension. Both count with the organization/application term-set
    // restrictions withheld, so selecting an org/app does NOT collapse the org/app rails. A searched
    // rail narrows to name matches instead of aggregating the whole dimension, and withholds the same
    // restrictions for the same reason.
    if (ownerFacetBase.hasOrganizationSearch()) {
      applyOrganizationFacetSearch(facets, countOrganizationsMatchingName(
          ownerFacetBase.queryString(), ownerFacetBase.organizationSearch(), List.of(), scopedCounter));
    }
    else {
      facets.organizations = countOrganizations(session, ownerFacetBase.query(), facets);
    }
    if (ownerFacetBase.hasApplicationSearch()) {
      applyApplicationFacetSearch(facets, countApplicationsMatchingName(
          ownerFacetBase.queryString(), ownerFacetBase.applicationSearch(), List.of(), scopedCounter));
    }
    else {
      facets.applications = countApplications(session, ownerFacetBase.query());
    }
    attachOwnerLabels(facets);
    return facets;
  }

  /** Session path: no discovery hits; resolve names via DAO for facet keys. */
  private void attachOwnerLabels(final ViolationsListFacetsDTO facets) {
    attachOwnerLabels(facets, null);
  }

  /**
   * Resolve friendly org/app display names for facet keys so the Martha rail never has to show raw
   * internal ids. Prefers names already seeded on the facets DTO (facet-search path), then discovery
   * hits (legacy path), then fills gaps with a batched DAO lookup for the facet key set.
   */
  private void attachOwnerLabels(
      final ViolationsListFacetsDTO facets,
      final LinkedHashMap<String, SearchResultItemDTO> discovered)
  {
    Map<String, String> organizationNames = new LinkedHashMap<>();
    Map<String, String> applicationNames = new LinkedHashMap<>();
    if (facets.organizationNames != null) {
      organizationNames.putAll(facets.organizationNames);
    }
    if (facets.applicationNames != null) {
      applicationNames.putAll(facets.applicationNames);
    }

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
    // OPEN is the complement of the shared excluded set (Waived AutoWaived Legacy) so the facet count
    // mirrors the state filter (ViolationsListIndexQueryBuilder.buildStateClause) and the row-state
    // derivation (ViolationWaiverStatus.toState) exactly. OPEN must exclude Legacy or Legacy leaks in.
    // The violationQuery is the positive anchor for the OPEN negation, so the non-anchored clause form
    // is used here.
    long open = counter.applyAsLong(violationQuery + " AND " + ViolationsListIndexQueryBuilder.openClause(false));
    if (open > 0) {
      counts.put(PolicyViolationState.OPEN.name(), open);
    }
    long waived = counter.applyAsLong(violationQuery + " AND " + ViolationsListIndexQueryBuilder.waivedClause());
    if (waived > 0) {
      counts.put(PolicyViolationState.WAIVED.name(), waived);
    }
    // Legacy count is the pure-legacy population only (waived+legacy indexes as Waived by precedence and
    // is counted under WAIVED above). Omitted when zero.
    long legacy = counter.applyAsLong(violationQuery + " AND " + ViolationsListIndexQueryBuilder.legacyClause());
    if (legacy > 0) {
      counts.put(PolicyViolationState.LEGACY_VIOLATION.name(), legacy);
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

  private LinkedHashMap<String, SearchResultItemDTO> discoverViolationItems(
      final String violationQuery,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(violationQuery, MAX_FACET_DISCOVERY_HITS, 0, false, false, List.of(),
            scopeRestrictions);
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

  private static void applyOrganizationFacetSearch(
      final ViolationsListFacetsDTO facets,
      final OwnerFacetSearchResult result)
  {
    if (result == null) {
      facets.organizations = null;
      return;
    }
    facets.organizations = result.counts;
    facets.organizationNames = result.names;
  }

  private static void applyApplicationFacetSearch(
      final ViolationsListFacetsDTO facets,
      final OwnerFacetSearchResult result)
  {
    if (result == null) {
      facets.applications = null;
      return;
    }
    facets.applications = result.counts;
    facets.applicationNames = result.names;
  }

  /**
   * Name-matched organization facet keys (estate-scale findability).
   * <p>
   * Pipeline: over-fetch name matches (up to {@link #MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES}) →
   * intersect with {@link OrganizationSummaryService#getOrganizationsForRead(Set)} (id-scoped) so
   * parent orgs the caller cannot see are never emitted even when a visible child has violations →
   * build per-org {@code parentOrganizationId} ancestor-match clauses → count against the RBAC-scoped
   * {@code violationQuery} → keep the first {@link #MAX_ORGANIZATION_FACETS} positive-count owners.
   */
  private OwnerFacetSearchResult countOrganizationsMatchingName(
      final String violationQuery,
      final String organizationFacetSearch,
      final List<IndexFilterRestriction> scopeRestrictions,
      final BiFunction<String, List<IndexFilterRestriction>, Long> scopedCounter)
  {
    List<Organization> candidates =
        organizationDAO.searchByNameSubstring(organizationFacetSearch, MAX_ORGANIZATION_FACET_SEARCH_CANDIDATES);
    if (candidates.isEmpty()) {
      return null;
    }
    Set<String> candidateIds = new LinkedHashSet<>();
    for (Organization organization : candidates) {
      if (organization != null && StringUtils.isNotBlank(organization.getId())) {
        candidateIds.add(organization.getId());
      }
    }
    Set<String> readableOrgIds = organizationSummaryService.getOrganizationsForRead(candidateIds)
        .stream()
        .map(Organization::getId)
        .collect(Collectors.toSet());
    List<Organization> matches = candidates.stream()
        .filter(org -> org != null && StringUtils.isNotBlank(org.getId()) && readableOrgIds.contains(org.getId()))
        .toList();
    if (matches.isEmpty()) {
      return null;
    }
    Set<String> matchedIds = new LinkedHashSet<>();
    for (Organization organization : matches) {
      matchedIds.add(organization.getId());
    }
    Map<String, Set<String>> expandedById = dimensionQueryBuilder.organizationFilterIdsById(matchedIds);
    Map<String, Long> counts = new LinkedHashMap<>();
    Map<String, String> names = new LinkedHashMap<>();
    for (Organization organization : matches) {
      if (counts.size() >= MAX_ORGANIZATION_FACETS) {
        break;
      }
      Set<String> orgIds = expandedById.get(organization.getId());
      if (orgIds == null) {
        // No entry for this organization: the builder omits root and blank ids.
        continue;
      }
      List<IndexFilterRestriction> combined = IdSetFilterQueries.combine(scopeRestrictions,
          IndexTermSetRestriction.singleton(FieldIdentifier.PARENT_ORGANIZATION_ID.label, orgIds));
      long count = scopedCounter.apply(violationQuery, combined);
      if (count > 0) {
        counts.put(organization.getId(), count);
        if (StringUtils.isNotBlank(organization.getName())) {
          names.put(organization.getId(), organization.getName());
        }
      }
    }
    return counts.isEmpty() ? null : new OwnerFacetSearchResult(counts, names);
  }

  /**
   * Name-matched application facet keys. Over-fetches candidates, intersects with
   * {@link ApplicationSummaryService#getApplicationsForRead}, then keeps the first
   * {@link #MAX_APPLICATION_FACETS} positive-count apps.
   */
  private OwnerFacetSearchResult countApplicationsMatchingName(
      final String violationQuery,
      final String applicationFacetSearch,
      final List<IndexFilterRestriction> scopeRestrictions,
      final BiFunction<String, List<IndexFilterRestriction>, Long> scopedCounter)
  {
    List<Application> candidates =
        applicationDAO.searchByNameSubstring(applicationFacetSearch, MAX_APPLICATION_FACET_SEARCH_CANDIDATES);
    if (candidates.isEmpty()) {
      return null;
    }
    Set<String> hitPublicIds = candidates.stream()
        .filter(app -> app != null && StringUtils.isNotBlank(app.getPublicId()))
        .map(Application::getPublicId)
        .collect(Collectors.toSet());
    if (hitPublicIds.isEmpty()) {
      return null;
    }
    Set<String> readableAppIds = applicationSummaryService.getApplicationsForRead(null, hitPublicIds)
        .stream()
        .map(Application::getId)
        .collect(Collectors.toSet());
    Map<String, Long> counts = new LinkedHashMap<>();
    Map<String, String> names = new LinkedHashMap<>();
    for (Application application : candidates) {
      if (counts.size() >= MAX_APPLICATION_FACETS) {
        break;
      }
      if (application == null || StringUtils.isBlank(application.getId())
          || !readableAppIds.contains(application.getId()))
      {
        continue;
      }
      List<IndexFilterRestriction> combined = IdSetFilterQueries.combine(scopeRestrictions,
          IndexTermSetRestriction.singleton(FieldIdentifier.APPLICATION_ID.label, Set.of(application.getId())));
      long count = scopedCounter.apply(violationQuery, combined);
      if (count > 0) {
        counts.put(application.getId(), count);
        if (StringUtils.isNotBlank(application.getName())) {
          names.put(application.getId(), application.getName());
        }
      }
    }
    return counts.isEmpty() ? null : new OwnerFacetSearchResult(counts, names);
  }

  private record OwnerFacetSearchResult(Map<String, Long> counts, Map<String, String> names)
  {
  }

  private Map<String, Long> countOrganizations(
      final String violationQuery,
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

    Map<String, Set<String>> expandedById = dimensionQueryBuilder.organizationFilterIdsById(organizationIds);
    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String organizationId : organizationIds) {
      if (queries >= MAX_ORGANIZATION_FACETS) {
        break;
      }
      Set<String> orgIds = expandedById.get(organizationId);
      if (orgIds == null) {
        continue;
      }
      List<IndexFilterRestriction> combined = IdSetFilterQueries.combine(scopeRestrictions,
          IndexTermSetRestriction.singleton(FieldIdentifier.PARENT_ORGANIZATION_ID.label, orgIds));
      counts.put(organizationId, searchIndexClient.count(violationQuery, combined));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * Hierarchical org facets via ancestor closure.
   * <p>
   * Aggregates on PARENT_ORGANIZATION_ID (the {self..root} closure) over the owner-removed base,
   * so parent/grandparent orgs appear with subtree counts. The root org is excluded before the
   * display cap is applied (root is in every doc's closure and would consume the top bucket).
   */
  private Map<String, Long> countOrganizations(
      final IndexReadSession session,
      final Query ownerRemovedBase,
      final ViolationsListFacetsDTO facets)
  {
    List<IndexTermsBucket> buckets = session.termsAggregation(
        ownerRemovedBase,
        FieldIdentifier.PARENT_ORGANIZATION_ID.label,
        MAX_ORGANIZATION_FACET_CANDIDATES);
    if (buckets.isEmpty()) {
      return null;
    }

    Set<String> orgIds = new HashSet<>();
    for (IndexTermsBucket bucket : buckets) {
      if (StringUtils.isNotBlank(bucket.key())) {
        orgIds.add(bucket.key());
      }
    }
    // Read gate: the ancestor closure surfaces parent/grandparent orgs above the caller's scope, so
    // intersect the bucket ids with the readable set before resolving names or emitting buckets — a
    // caller must never see or filter by an org they cannot read.
    // The read gate returns the rows themselves, so their names are reused for sorting rather than fetched
    // again by id.
    Set<String> readableOrgIds = new HashSet<>();
    Map<String, String> organizationNames = new LinkedHashMap<>();
    for (Organization organization : organizationSummaryService.getOrganizationsForRead(orgIds)) {
      readableOrgIds.add(organization.getId());
      if (StringUtils.isNotBlank(organization.getName())) {
        organizationNames.putIfAbsent(organization.getId(), organization.getName());
      }
    }

    List<IndexTermsBucket> nonZeroBuckets = new ArrayList<>();
    for (IndexTermsBucket bucket : buckets) {
      // Exclude root org (it's in every doc's ancestor closure and would dominate the facet) and any
      // org outside the caller's read scope.
      if (bucket.count() > 0
          && StringUtils.isNotBlank(bucket.key())
          && !Organization.ROOT_ORGANIZATION_ID.equals(bucket.key())
          && readableOrgIds.contains(bucket.key()))
      {
        nonZeroBuckets.add(bucket);
      }
    }
    nonZeroBuckets.sort(
        Comparator
            .comparing((IndexTermsBucket bucket) -> organizationNames.getOrDefault(bucket.key(), bucket.key()))
            .thenComparing(IndexTermsBucket::key));

    // Apply display cap after root exclusion
    Map<String, Long> counts = new LinkedHashMap<>();
    int entries = 0;
    for (IndexTermsBucket bucket : nonZeroBuckets) {
      if (entries >= MAX_ORGANIZATION_FACETS) {
        break;
      }
      counts.put(bucket.key(), bucket.count());
      entries++;
    }
    if (counts.isEmpty()) {
      return null;
    }
    // Publish the names resolved above for the emitted buckets, so the label pass has nothing left to look
    // up: it fetches only ids it has no name for.
    Map<String, String> emittedNames = new LinkedHashMap<>();
    for (String organizationId : counts.keySet()) {
      String name = organizationNames.get(organizationId);
      if (name != null) {
        emittedNames.put(organizationId, name);
      }
    }
    if (facets.organizationNames == null) {
      facets.organizationNames = emittedNames;
    }
    else {
      emittedNames.forEach(facets.organizationNames::putIfAbsent);
    }
    return counts;
  }

  private Map<String, Long> countApplications(
      final String violationQuery,
      final LinkedHashMap<String, SearchResultItemDTO> discovered,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String applicationId : discovered.keySet()) {
      if (queries >= MAX_APPLICATION_FACETS) {
        break;
      }
      if (StringUtils.isBlank(applicationId)) {
        continue;
      }
      List<IndexFilterRestriction> combined = IdSetFilterQueries.combine(scopeRestrictions,
          IndexTermSetRestriction.singleton(FieldIdentifier.APPLICATION_ID.label, Set.of(applicationId)));
      counts.put(applicationId, searchIndexClient.count(violationQuery, combined));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * App facet via real aggregation over owner-removed base.
   * <p>
   * Aggregating over the owner-removed base lists ALL matching apps (unnarrowed by the owner
   * selection), which prevents the app rail from collapsing when an org is selected.
   */
  private Map<String, Long> countApplications(
      final IndexReadSession session,
      final Query ownerRemovedBase)
  {
    List<IndexTermsBucket> buckets = session.termsAggregation(
        ownerRemovedBase,
        FieldIdentifier.APPLICATION_ID.label,
        MAX_APPLICATION_FACETS);
    if (buckets.isEmpty()) {
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

  private long sessionCount(
      final IndexReadSession session,
      final String query,
      final List<IndexFilterRestriction> restrictions)
  {
    List<IndexFilterRestriction> normalized = restrictions == null ? List.of() : restrictions;
    return session.count(toScopedQuery(query, normalized));
  }

  private Query toScopedQuery(final String query, final List<IndexFilterRestriction> restrictions) {
    return IdSetFilterQueries.toScopedQuery(conversionHelper.stringToQuery(query), restrictions);
  }

}
