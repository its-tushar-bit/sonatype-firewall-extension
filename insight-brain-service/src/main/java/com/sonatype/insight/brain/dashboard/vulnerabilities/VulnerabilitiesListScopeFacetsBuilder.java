/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.IndexGroupedCountKeys;
import com.sonatype.insight.brain.dashboard.vulnerabilities.VulnerabilitiesListIndexQueryBuilder.FacetDimension;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IdSetFilterQueries;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Organization / application / stage facet counts for the Vulnerabilities My Scan Data list,
 * following the {@code ComponentsListFacetsBuilder} pattern.
 * <p>
 * These three dimensions differ from severity and ecosystem: severity and ecosystem are properties
 * of the vulnerability itself, so the list can bucket them from its collapsed page. Organization,
 * application, and stage vary across the <em>uncollapsed</em> SECURITY_VULNERABILITY docs for a
 * single vulnerability — one doc per (application, stage, component, vulnerability) — so bucketing
 * the collapsed page would credit a CVE only to whichever hit happened to represent it.
 * <p>
 * <b>Keys come from a terms aggregation, not from the list's hits.</b> Discovering keys by walking
 * the page would break the rail as soon as a filter is applied: selecting one organization narrows
 * the walk to that organization, so every sibling would disappear and the filter could never be
 * changed, only cleared. Each dimension is therefore both discovered and counted against the query
 * with its own clause omitted (see {@link FacetDimension}), which keeps unselected siblings
 * visible and switchable while every other active filter still applies.
 * <p>
 * <b>Hierarchical owner facets via owner-removed base.</b> Organization and application
 * are ONE owner dimension. Both facets aggregate over a query with the whole owner group
 * (org OR app) removed, so selecting an org or app does NOT collapse the org/app rails.
 * The org facet uses PARENT_ORGANIZATION_ID (the {self..root} closure) for hierarchical aggregation,
 * with root org excluded before the display cap. Counts are distinct {@code vulnerabilityId}s.
 * <p>
 * <b>Round trips are constant</b> with respect to estate size: two {@code termsAggregation} calls
 * for key discovery plus one grouped distinct count per dimension, all on one shared session.
 * Counts are distinct {@code vulnerabilityId}s, matching the units of the list's {@code total}.
 * Backends without grouped distinct counting degrade to omitting the facet rather than to a
 * per-key count loop, since that loop would scale with the number of organizations.
 */
@Named
@Singleton
final class VulnerabilitiesListScopeFacetsBuilder
{
  private static final Logger log = LoggerFactory.getLogger(VulnerabilitiesListScopeFacetsBuilder.class);

  /** Caps the facet entries returned to the rail (response size), not the number of queries. */
  static final int MAX_OWNER_FACET_ENTRIES = 25;

  /**
   * Candidate buckets requested for the hierarchical organization facet, before root exclusion, the read
   * gate and the display cap. Wide because buckets are ranked by document count and ancestor-closure counts
   * accumulate toward the root, so a caller scoped to a low-count leaf would otherwise find the window
   * filled by higher-count ancestors the read gate then drops.
   */
  static final int MAX_ORGANIZATION_FACET_CANDIDATES = 500;

  /**
   * Upper bound on the groups handed to the distinct-count collector, which holds a set of vulnerability ids
   * per group and, for an ancestor, spans its whole subtree. Carries headroom over the display cap so
   * re-ranking by distinct count can still promote an organization document count ordered lower.
   */
  static final int MAX_DISTINCT_GROUP_KEYS = MAX_OWNER_FACET_ENTRIES * 2;

  /** Terms aggregation bucket cap for facet key discovery (doc-count order). */
  static final int MAX_FACET_TERM_BUCKETS = 100;

  private final IndexReadSessionFactory indexReadSessionFactory;

  private final ConversionHelper conversionHelper;

  private final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder;

  private final StageTypeService stageTypeService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationSummaryService organizationSummaryService;

  @Inject
  VulnerabilitiesListScopeFacetsBuilder(
      final IndexReadSessionFactory indexReadSessionFactory,
      final ConversionHelper conversionHelper,
      final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder,
      final StageTypeService stageTypeService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationSummaryService organizationSummaryService)
  {
    this.indexReadSessionFactory = indexReadSessionFactory;
    this.conversionHelper = conversionHelper;
    this.indexQueryBuilder = indexQueryBuilder;
    this.stageTypeService = stageTypeService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.organizationSummaryService = organizationSummaryService;
  }

  /**
   * Session-path facets with owner-removed base for hierarchical owner facets.
   * <p>
   * Org facet aggregates on PARENT_ORGANIZATION_ID (ancestor closure) over the owner-removed base,
   * so parent/grandparent orgs appear with subtree counts. Root org is excluded before the display cap.
   * App facet aggregates over the owner-removed base to prevent collapse when an org is selected.
   * <p>
   *
   * @param facets the DTO to attach facets to
   * @param ownerRemovedBase the query with owner-dimension (org OR app) clauses removed
   * @param request the requestDTO (needed for stage facet which uses the legacy per-dimension query)
   */
  void attachScopeFacets(
      final VulnerabilitiesListFacetsDTO facets,
      final Query ownerRemovedBase,
      final VulnerabilitiesListRequestDTO request)
  {
    // Org and app are ONE owner dimension, so the owner-facet base withholds BOTH term sets
    // (FacetDimension.OWNER_GROUP) and selecting an org/app does not collapse either rail.
    Query scopedOwnerRemovedBase = IdSetFilterQueries.toScopedQuery(
        ownerRemovedBase, indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.OWNER_GROUP));
    String stageQuery = indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.STAGE);
    Query stageLucene = toScopedQuery(stageQuery,
        indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.STAGE));

    try (IndexReadSession session = indexReadSessionFactory.open()) {
      facets.organizations = countOrganizations(session, scopedOwnerRemovedBase);
      facets.applications = countApplications(session, scopedOwnerRemovedBase);

      facets.stages = countDistinctVulnerabilities(
          session, stageLucene, FieldIdentifier.POLICY_EVALUATION_STAGE.label, licensedStageIds());

      facets.organizationNames = resolveOrganizationNames(keysOf(facets.organizations));
      facets.applicationNames = resolveApplicationNames(keysOf(facets.applications));
      facets.stageNames = resolveStageNames(keysOf(facets.stages));
    }
    catch (RuntimeException e) {
      log.warn("Vulnerabilities scope facets unavailable; returning the list without them", e);
    }
  }

  private Query toScopedQuery(final String query, final List<IndexFilterRestriction> restrictions) {
    return IdSetFilterQueries.toScopedQuery(conversionHelper.stringToQuery(query), restrictions);
  }

  private Set<String> discoverKeys(
      final IndexReadSession session,
      final Query query,
      final String field)
  {
    List<IndexTermsBucket> buckets;
    try {
      buckets = session.termsAggregation(query, field, MAX_FACET_TERM_BUCKETS);
    }
    catch (RuntimeException e) {
      log.warn("Vulnerabilities list facet key discovery failed for field {}", field, e);
      return Set.of();
    }
    Set<String> keys = new LinkedHashSet<>();
    if (buckets == null) {
      return keys;
    }
    for (IndexTermsBucket bucket : buckets) {
      if (bucket != null && StringUtils.isNotBlank(bucket.key())) {
        keys.add(bucket.key());
      }
    }
    return keys;
  }

  private Map<String, Long> countDistinctVulnerabilities(
      final IndexReadSession session,
      final Query query,
      final String groupField,
      final Set<String> groupValues)
  {
    if (groupValues.isEmpty()) {
      return null;
    }
    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          query,
          groupField,
          FieldIdentifier.VULNERABILITY_ID.label,
          groupValues);
    }
    catch (RuntimeException e) {
      log.warn(
          "Grouped distinct counting failed for {} session; omitting the {} facet",
          session.backendId(),
          groupField,
          e);
      return null;
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      long count = grouped == null
          ? 0L
          : grouped.getOrDefault(IndexGroupedCountKeys.lookupKey(groupValue), 0L);
      if (count > 0) {
        counts.put(groupValue, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private Set<String> licensedStageIds() {
    Set<String> stageIds = new LinkedHashSet<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      if (stageType != null && StringUtils.isNotBlank(stageType.getId())) {
        stageIds.add(stageType.getId());
      }
    }
    return stageIds;
  }

  private Map<String, String> resolveOrganizationNames(final Set<String> organizationIds) {
    if (organizationIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (Organization organization : organizationDAO.getByIds(organizationIds)) {
      if (organization == null || StringUtils.isBlank(organization.getId())) {
        continue;
      }
      names.put(organization.getId(), displayName(organization.getName(), organization.getId()));
    }
    return names.isEmpty() ? null : names;
  }

  private Map<String, String> resolveApplicationNames(final Set<String> applicationIds) {
    if (applicationIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (Application application : applicationDAO.getByIds(applicationIds)) {
      if (application == null || StringUtils.isBlank(application.getId())) {
        continue;
      }
      names.put(application.getId(), displayName(application.getName(), application.getId()));
    }
    return names.isEmpty() ? null : names;
  }

  private Map<String, String> resolveStageNames(final Set<String> stageIds) {
    if (stageIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      if (stageType != null && stageIds.contains(stageType.getId())) {
        names.put(stageType.getId(), displayName(stageType.getName(), stageType.getId()));
      }
    }
    return names.isEmpty() ? null : names;
  }

  private static String displayName(final String name, final String id) {
    String trimmed = StringUtils.trimToNull(name);
    return trimmed != null ? trimmed : id;
  }

  private static Set<String> keysOf(final Map<String, Long> counts) {
    return counts == null ? Set.of() : counts.keySet();
  }

  /**
   * Hierarchical org facets via ancestor closure.
   * <p>
   * Aggregates on PARENT_ORGANIZATION_ID (the {self..root} closure) over the owner-removed base,
   * so parent/grandparent orgs appear with subtree counts. The root org is excluded before the
   * display cap is applied (root is in every doc's closure and would consume the top bucket).
   * <p>
   * Uses distinct-count (vulnerabilityId) because a vulnerability spans many docs.
   */
  private static Set<String> boundGroupKeys(
      final List<IndexTermsBucket> bucketsByDocCount,
      final Set<String> readableOrgIds)
  {
    Set<String> groupValues = new LinkedHashSet<>();
    for (IndexTermsBucket bucket : bucketsByDocCount) {
      if (groupValues.size() >= MAX_DISTINCT_GROUP_KEYS) {
        break;
      }
      if (readableOrgIds.contains(bucket.key())) {
        groupValues.add(bucket.key());
      }
    }
    return groupValues;
  }

  private Map<String, Long> countOrganizations(
      final IndexReadSession session,
      final Query ownerRemovedBase)
  {
    // Discover ancestor org ids (groupValues) from PARENT_ORGANIZATION_ID multi-valued field
    List<IndexTermsBucket> buckets = session.termsAggregation(
        ownerRemovedBase,
        FieldIdentifier.PARENT_ORGANIZATION_ID.label,
        MAX_ORGANIZATION_FACET_CANDIDATES);
    if (buckets.isEmpty()) {
      return null;
    }

    // Collect org ids, then apply the read gate: the ancestor closure surfaces parent orgs above the
    // caller's scope, so only readable orgs are counted, resolved, or emitted.
    Set<String> orgIds = new HashSet<>();
    for (IndexTermsBucket bucket : buckets) {
      if (StringUtils.isNotBlank(bucket.key())) {
        orgIds.add(bucket.key());
      }
    }
    List<Organization> readableOrganizations = organizationSummaryService.getOrganizationsForRead(orgIds);
    Set<String> readableOrgIds = readableOrganizations.stream()
        .map(Organization::getId)
        .collect(Collectors.toSet());
    // Root is in every doc's ancestor closure, so it must never become a group: its set would accumulate
    // the whole estate. The read gate already excludes it (see OrganizationSummaryService), which is what
    // keeps it out of the group set here.
    // Bounded so the collector's per-group id sets track the rail rather than the candidate window; see the
    // constant. Nothing readable means nothing to count, so skip the pass entirely.
    Set<String> groupValues = boundGroupKeys(buckets, readableOrgIds);
    if (groupValues.isEmpty()) {
      return null;
    }

    // Hierarchical distinct-count: each ancestor org gets the count of DISTINCT vulnerabilities in its subtree
    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          ownerRemovedBase,
          FieldIdentifier.PARENT_ORGANIZATION_ID.label,
          FieldIdentifier.VULNERABILITY_ID.label,
          groupValues);
    }
    catch (RuntimeException e) {
      // Includes UnsupportedOperationException on backends without grouped distinct counting.
      log.warn(
          "Grouped distinct counting failed for {} session; omitting the org facet",
          session.backendId(),
          e);
      return null;
    }

    // Names come from the rows the read gate already returned, covering ancestors without a second load.
    Map<String, String> organizationNames = new LinkedHashMap<>();
    for (Organization organization : readableOrganizations) {
      if (StringUtils.isNotBlank(organization.getName())) {
        organizationNames.putIfAbsent(organization.getId(), organization.getName());
      }
    }

    // Exclude root org, zero-count buckets, and orgs outside the caller's read scope; sort by name
    List<IndexTermsBucket> nonZeroBuckets = new ArrayList<>();
    for (IndexTermsBucket bucket : buckets) {
      long count = grouped == null ? 0L : grouped.getOrDefault(IndexGroupedCountKeys.lookupKey(bucket.key()), 0L);
      if (count > 0
          && StringUtils.isNotBlank(bucket.key())
          && !Organization.ROOT_ORGANIZATION_ID.equals(bucket.key())
          && readableOrgIds.contains(bucket.key()))
      {
        nonZeroBuckets.add(new IndexTermsBucket(bucket.key(), count));
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
      if (entries >= MAX_OWNER_FACET_ENTRIES) {
        break;
      }
      counts.put(bucket.key(), bucket.count());
      entries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * App facet via distinct-count over owner-removed base.
   * <p>
   * Uses distinct-count (vulnerabilityId) because a vulnerability spans many docs.
   * Aggregates on APPLICATION_ID over the owner-removed base so the app rail stays fully populated
   * when an org is selected (no collapse).
   */
  private Map<String, Long> countApplications(
      final IndexReadSession session,
      final Query ownerRemovedBase)
  {
    // Discover app ids via termsAggregation
    List<IndexTermsBucket> buckets = session.termsAggregation(
        ownerRemovedBase,
        FieldIdentifier.APPLICATION_ID.label,
        MAX_OWNER_FACET_ENTRIES);
    if (buckets.isEmpty()) {
      return null;
    }

    // Collect app ids for distinct-count aggregation
    Set<String> appIds = new LinkedHashSet<>();
    for (IndexTermsBucket bucket : buckets) {
      if (StringUtils.isNotBlank(bucket.key())) {
        appIds.add(bucket.key());
      }
    }

    // Distinct-count: each app gets the count of DISTINCT vulnerabilities
    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          ownerRemovedBase,
          FieldIdentifier.APPLICATION_ID.label,
          FieldIdentifier.VULNERABILITY_ID.label,
          appIds);
    }
    catch (RuntimeException e) {
      // Includes UnsupportedOperationException on backends without grouped distinct counting.
      log.warn(
          "Grouped distinct counting failed for {} session; omitting the app facet",
          session.backendId(),
          e);
      return null;
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String appId : appIds) {
      long count = grouped == null ? 0L : grouped.getOrDefault(IndexGroupedCountKeys.lookupKey(appId), 0L);
      if (count > 0) {
        counts.put(appId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }
}
