/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

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

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.IndexGroupedCountKeys;
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
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds Martha sidebar facet counts from RBAC-scoped index queries for Components.
 * <p>
 * Org/app bucket <em>keys</em> come from {@link IndexReadSession#termsAggregation} over the full
 * filtered result set (not the first N raw hits). Bucket <em>values</em> remain
 * {@code countDistinct(componentHash)} so counts stay in distinct-component units. Friendly
 * display names for those keys are resolved from ODS owner tables for the filter rail.
 * <p>
 * On backends that implement {@link IndexReadSession#countDistinctGroupedBy}, round trips are
 * constant with respect to estate size: two {@code termsAggregation} calls for key discovery plus
 * one grouped call per facet group (organizations, applications, stages) on a single shared
 * session.
 * <p>
 * A backend without grouped distinct counting degrades to the document counts the discovery aggregation
 * already returned. Those overstate the rail's units - a component with several vulnerabilities contributes
 * one document per vulnerability per application - but keep the values selectable, which an empty rail would
 * not.
 * <p>
 * Grouped results are keyed by the lowercased group value on every backend (see
 * {@link IndexGroupedCountKeys}), so counts are read back with that key rather than the verbatim id.
 */
@Named
@Singleton
final class ComponentsListFacetsBuilder
{
  private static final Logger log = LoggerFactory.getLogger(ComponentsListFacetsBuilder.class);

  /** Caps the facet entries returned to the rail (response size), not the number of queries. */
  static final int MAX_ORGANIZATION_FACET_ENTRIES = 25;

  /**
   * Candidate buckets requested for the hierarchical organization facet, before the read gate, root
   * exclusion and {@link #boundGroupKeys}. Wide because buckets are ranked by document count and
   * ancestor-closure counts accumulate toward the root, so a caller scoped to a low-count leaf would
   * otherwise find the window filled by higher-count ancestors the read gate then drops.
   */
  static final int MAX_ORGANIZATION_FACET_CANDIDATES = 500;

  /**
   * Upper bound on the groups handed to the distinct-count collector, which holds a set of component hashes
   * per group. Set above {@link #MAX_ORGANIZATION_FACET_ENTRIES} so re-ranking by distinct count can still
   * promote an organization that document count ordered lower, but bounded so peak memory tracks the rail
   * rather than the candidate window.
   */
  static final int MAX_DISTINCT_GROUP_KEYS = MAX_ORGANIZATION_FACET_ENTRIES * 2;

  static final int MAX_APPLICATION_FACET_ENTRIES = 25;

  /** Terms aggregation bucket cap for facet key discovery (doc-count order). */
  static final int MAX_FACET_TERM_BUCKETS = 100;

  private final SearchIndexClient searchIndexClient;

  private final IndexReadSessionFactory indexReadSessionFactory;

  private final ConversionHelper conversionHelper;

  private final StageTypeService stageTypeService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationSummaryService organizationSummaryService;

  @Inject
  ComponentsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final IndexReadSessionFactory indexReadSessionFactory,
      final ConversionHelper conversionHelper,
      final StageTypeService stageTypeService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationSummaryService organizationSummaryService)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexReadSessionFactory = indexReadSessionFactory;
    this.conversionHelper = conversionHelper;
    this.stageTypeService = stageTypeService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.organizationSummaryService = organizationSummaryService;
  }

  ComponentsListFacetsDTO buildFacets(final String componentQuery, final long totalComponents) {
    return buildFacets(componentQuery, List.of(), totalComponents);
  }

  ComponentsListFacetsDTO buildFacets(
      final String componentQuery,
      final List<? extends IndexFilterRestriction> termSets,
      final long totalComponents)
  {
    ComponentsListFacetsDTO facets = new ComponentsListFacetsDTO();
    facets.totalComponents = totalComponents;
    List<? extends IndexFilterRestriction> restrictions = termSets == null ? List.of() : termSets;

    try (IndexReadSession session = indexReadSessionFactory.open()) {
      Set<String> organizationIds = discoverFacetKeys(session, componentQuery, restrictions,
          FieldIdentifier.ORGANIZATION_ID.label);
      Set<String> applicationIds = discoverFacetKeys(session, componentQuery, restrictions,
          FieldIdentifier.APPLICATION_ID.label);

      if (!organizationIds.isEmpty() || !applicationIds.isEmpty()) {
        facets.organizations = countByDimension(session, componentQuery, restrictions,
            FieldIdentifier.ORGANIZATION_ID.label, capped(organizationIds, MAX_ORGANIZATION_FACET_ENTRIES));
        facets.applications = countByDimension(session, componentQuery, restrictions,
            FieldIdentifier.APPLICATION_ID.label, capped(applicationIds, MAX_APPLICATION_FACET_ENTRIES));
        facets.organizationNames = resolveOrganizationNames(
            facets.organizations == null ? Set.of() : facets.organizations.keySet());
        facets.applicationNames = resolveApplicationNames(
            facets.applications == null ? Set.of() : facets.applications.keySet());
      }
      facets.stages = countLicensedStages(session, componentQuery, restrictions);
      facets.stageNames = resolveStageNames(facets.stages == null ? Set.of() : facets.stages.keySet());
      return facets;
    }
  }

  /**
   * Session-path facets with per-facet bases for the hierarchical owner rails.
   * <p>
   * The organization facet aggregates on PARENT_ORGANIZATION_ID (the {@code {self..root}} ancestor
   * closure) over a base whose organization/application term sets are withheld, so parent and
   * grandparent organizations appear with subtree counts and selecting an org/app does not collapse the
   * org/app rails. Root is excluded before the display cap. Stages aggregate over a base with the stage
   * filter removed, so selecting a stage does not collapse the stages rail. Each base carries its own
   * term sets, since the violation-scope hash resolution differs with the excluded dimension.
   */
  ComponentsListFacetsDTO buildFacets(
      final Query ownerRemovedBase,
      final List<? extends IndexFilterRestriction> ownerRemovedTermSets,
      final String stageRemovedBase,
      final List<? extends IndexFilterRestriction> stageRemovedTermSets,
      final long totalComponents)
  {
    ComponentsListFacetsDTO facets = new ComponentsListFacetsDTO();
    facets.totalComponents = totalComponents;

    try (IndexReadSession session = indexReadSessionFactory.open()) {
      Query scopedOwnerRemovedBase = IdSetFilterQueries.toScopedQuery(
          ownerRemovedBase, ownerRemovedTermSets == null ? List.of() : ownerRemovedTermSets);
      // Org and app are ONE owner dimension. Both aggregate over the owner-removed base so selecting an
      // org/app does NOT collapse the org/app rails.
      Map<String, String> resolvedOrganizationNames = new LinkedHashMap<>();
      facets.organizations = countOrganizations(session, scopedOwnerRemovedBase, resolvedOrganizationNames);
      facets.applications = countApplications(session, scopedOwnerRemovedBase);
      facets.organizationNames = resolveOrganizationNames(
          facets.organizations == null ? Set.of() : facets.organizations.keySet(), resolvedOrganizationNames);
      facets.applicationNames = resolveApplicationNames(
          facets.applications == null ? Set.of() : facets.applications.keySet());
      facets.stages = countLicensedStages(
          session, stageRemovedBase, stageRemovedTermSets == null ? List.of() : stageRemovedTermSets);
      facets.stageNames = resolveStageNames(facets.stages == null ? Set.of() : facets.stages.keySet());
      return facets;
    }
  }

  private Set<String> discoverFacetKeys(
      final IndexReadSession session,
      final String componentQuery,
      final List<? extends IndexFilterRestriction> termSets,
      final String field)
  {
    try {
      List<IndexTermsBucket> buckets = session.termsAggregation(
          toScopedQuery(componentQuery, termSets),
          field,
          MAX_FACET_TERM_BUCKETS);
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
    catch (RuntimeException e) {
      log.warn("Components list facet key discovery failed for field {}", field, e);
      return Set.of();
    }
  }

  /**
   * @param alreadyResolved names the organization facet resolved from the read-gate rows; ids present here
   *          are not loaded again
   */
  private Map<String, Long> countByDimension(
      final IndexReadSession session,
      final String componentQuery,
      final List<? extends IndexFilterRestriction> termSets,
      final String groupField,
      final Set<String> groupValues)
  {
    if (groupValues.isEmpty()) {
      return null;
    }

    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          toScopedQuery(componentQuery, termSets),
          groupField,
          FieldIdentifier.COMPONENT_HASH.label,
          groupValues);
    }
    catch (UnsupportedOperationException e) {
      log.warn(
          "Grouped distinct counting unavailable for {} session; falling back to {} per-key {} facet queries",
          session.backendId(),
          groupValues.size(),
          groupField);
      return countByDimensionPerKey(componentQuery, termSets, groupField, groupValues);
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      counts.put(groupValue,
          grouped == null ? 0L : grouped.getOrDefault(IndexGroupedCountKeys.lookupKey(groupValue), 0L));
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countByDimensionPerKey(
      final String componentQuery,
      final List<? extends IndexFilterRestriction> termSets,
      final String groupField,
      final Set<String> groupValues)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      String clause = groupField + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(groupValue) + ")";
      counts.put(groupValue, searchIndexClient.countDistinct(
          componentQuery + " AND " + clause,
          List.of(FieldIdentifier.COMPONENT_HASH.label),
          termSets));
    }
    return counts.isEmpty() ? null : counts;
  }

  private Query toScopedQuery(final String componentQuery, final List<? extends IndexFilterRestriction> termSets) {
    return IdSetFilterQueries.toScopedQuery(conversionHelper.stringToQuery(componentQuery), termSets);
  }

  private static Set<String> capped(final Set<String> keys, final int maxEntries) {
    if (keys.size() <= maxEntries) {
      return keys;
    }
    Set<String> limited = new LinkedHashSet<>();
    for (String key : keys) {
      if (limited.size() >= maxEntries) {
        break;
      }
      limited.add(key);
    }
    return limited;
  }

  private Map<String, String> resolveOrganizationNames(final Set<String> organizationIds) {
    return resolveOrganizationNames(organizationIds, Map.of());
  }

  private Map<String, String> resolveOrganizationNames(
      final Set<String> organizationIds,
      final Map<String, String> alreadyResolved)
  {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>(alreadyResolved);
    Set<String> missing = new LinkedHashSet<>();
    for (String organizationId : organizationIds) {
      if (!names.containsKey(organizationId)) {
        missing.add(organizationId);
      }
    }
    if (missing.isEmpty()) {
      return names;
    }
    for (Organization organization : organizationDAO.getByIds(missing)) {
      if (organization == null || StringUtils.isBlank(organization.getId())) {
        continue;
      }
      String name = StringUtils.trimToNull(organization.getName());
      names.put(organization.getId(), name != null ? name : organization.getId());
    }
    return names.isEmpty() ? null : names;
  }

  private Map<String, String> resolveApplicationNames(final Set<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (Application application : applicationDAO.getByIds(applicationIds)) {
      if (application == null || StringUtils.isBlank(application.getId())) {
        continue;
      }
      String name = StringUtils.trimToNull(application.getName());
      names.put(application.getId(), name != null ? name : application.getId());
    }
    return names.isEmpty() ? null : names;
  }

  private Map<String, Long> countLicensedStages(
      final IndexReadSession session,
      final String componentQuery,
      final List<? extends IndexFilterRestriction> termSets)
  {
    List<String> licensedStageIds = stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)
        .stream()
        .map(StageType::getId)
        .toList();
    if (licensedStageIds.isEmpty()) {
      return null;
    }

    String violationQuery = ComponentsListViolationQuerySupport.toViolationQuery(componentQuery);
    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          toScopedQuery(violationQuery, termSets),
          FieldIdentifier.POLICY_EVALUATION_STAGE.label,
          FieldIdentifier.COMPONENT_HASH.label,
          licensedStageIds);
    }
    catch (UnsupportedOperationException e) {
      log.warn(
          "Grouped distinct counting unavailable for {} session; falling back to {} per-stage facet queries",
          session.backendId(),
          licensedStageIds.size());
      grouped = countStagesPerKey(violationQuery, termSets, licensedStageIds);
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String stageId : licensedStageIds) {
      long count = grouped == null ? 0L : grouped.getOrDefault(IndexGroupedCountKeys.lookupKey(stageId), 0L);
      if (count > 0) {
        counts.put(stageId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, String> resolveStageNames(final Set<String> stageIds) {
    if (stageIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      if (stageType != null && stageIds.contains(stageType.getId())) {
        String name = StringUtils.trimToNull(stageType.getName());
        names.put(stageType.getId(), name != null ? name : stageType.getId());
      }
    }
    return names.isEmpty() ? null : names;
  }

  /** Keyed like {@code countDistinctGroupedBy} so the caller reads both paths with one lookup key. */
  private Map<String, Long> countStagesPerKey(
      final String violationQuery,
      final List<? extends IndexFilterRestriction> termSets,
      final List<String> stageIds)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String stageId : stageIds) {
      String stageClause = ComponentsListViolationQuerySupport.buildStageFilterClause(Set.of(stageId));
      counts.put(IndexGroupedCountKeys.lookupKey(stageId), searchIndexClient.countDistinct(
          violationQuery + " AND " + stageClause,
          List.of(FieldIdentifier.COMPONENT_HASH.label),
          termSets));
    }
    return counts;
  }

  /**
   * The readable, non-root candidate ids in document-count order, capped at {@link #MAX_DISTINCT_GROUP_KEYS}.
   * <p>
   * Document count is a proxy for distinct-component count, not the same ordering, so the cap carries
   * headroom over the display cap. An organization beyond the bound is not displayed, which is the same
   * outcome the display cap produces for it.
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

  /**
   * Hierarchical org facets via ancestor closure.
   * <p>
   * Aggregates on PARENT_ORGANIZATION_ID (the {self..root} closure) over the owner-removed base, so
   * parent/grandparent orgs appear with subtree counts. The root org is excluded before the display cap is
   * applied (root is in every doc's closure and would consume the top bucket).
   * <p>
   * Uses distinct-count (componentHash) because a component spans many docs.
   */
  private Map<String, Long> countOrganizations(
      final IndexReadSession session,
      final Query ownerRemovedBase,
      final Map<String, String> resolvedNamesOut)
  {
    // Candidate ancestor org ids (groupValues) from the multi-valued PARENT_ORGANIZATION_ID closure. The
    // window is wide because buckets are ranked by document count and ancestor counts accumulate toward the
    // root, so a caller scoped to a low-count leaf must not have its organizations pushed out of it.
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
    // Unlike the doc-count rails, the group set here drives a distinct-count collector that holds a set of
    // component hashes PER GROUP, and an ancestor's set spans its whole subtree. Handing it the entire
    // candidate window would make peak memory scale with the window rather than with the rail, so the groups
    // are bounded to the buckets that can plausibly be displayed, taken in document-count order.
    Set<String> groupValues = boundGroupKeys(buckets, readableOrgIds);
    if (groupValues.isEmpty()) {
      // Nothing readable to group by, so skip the pass rather than scan the estate for an empty result.
      return null;
    }

    // Hierarchical distinct-count: each ancestor org gets the count of DISTINCT components in its subtree
    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          ownerRemovedBase,
          FieldIdentifier.PARENT_ORGANIZATION_ID.label,
          FieldIdentifier.COMPONENT_HASH.label,
          groupValues);
    }
    catch (UnsupportedOperationException e) {
      // Document counts, not distinct components: a component with several vulnerabilities contributes one
      // document per vulnerability per application, so these counts read high. Preferred over an empty rail
      // because the values are still selectable and the ordering is still meaningful.
      log.warn(
          "Grouped distinct counting unavailable for {} session; the organization rail falls back to "
              + "document counts, which overstate distinct components",
          session.backendId());
      return fallbackCountOrganizationsByDocCount(buckets, readableOrgIds);
    }
    catch (RuntimeException e) {
      // An unexpected failure costs this rail only. Letting it propagate would reach the service's catch and
      // drop every component facet, including the ones already computed.
      log.warn("Organization facet unavailable for {} session; returning the other facets", session.backendId(), e);
      return null;
    }

    // Names come from the rows the read gate already returned, so ancestors surfaced by the closure sort by
    // name like everything else without a second load.
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
      if (entries >= MAX_ORGANIZATION_FACET_ENTRIES) {
        break;
      }
      counts.put(bucket.key(), bucket.count());
      entries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * Fallback for backends without grouped distinct counting: uses doc-count from termsAggregation.
   * This inflates counts (one doc per CVE×app) but keeps the facet working. {@code readableOrgIds} is
   * the read-gated org set, so non-readable ancestor buckets are dropped here too.
   */
  private Map<String, Long> fallbackCountOrganizationsByDocCount(
      final List<IndexTermsBucket> buckets,
      final Set<String> readableOrgIds)
  {
    Map<String, String> organizationNames = new LinkedHashMap<>();
    for (Organization organization : organizationDAO.getByIds(readableOrgIds)) {
      if (organization != null && StringUtils.isNotBlank(organization.getId())
          && StringUtils.isNotBlank(organization.getName()))
      {
        organizationNames.putIfAbsent(organization.getId(), organization.getName());
      }
    }

    List<IndexTermsBucket> nonZeroBuckets = new ArrayList<>();
    for (IndexTermsBucket bucket : buckets) {
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

    Map<String, Long> counts = new LinkedHashMap<>();
    int entries = 0;
    for (IndexTermsBucket bucket : nonZeroBuckets) {
      if (entries >= MAX_ORGANIZATION_FACET_ENTRIES) {
        break;
      }
      counts.put(bucket.key(), bucket.count());
      entries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  /**
   * App facet via hierarchical distinct-count over owner-removed base.
   * <p>
   * Uses distinct-count (componentHash) because a component spans many docs.
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
        MAX_APPLICATION_FACET_ENTRIES);
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

    // Distinct-count: each app gets the count of DISTINCT components
    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          ownerRemovedBase,
          FieldIdentifier.APPLICATION_ID.label,
          FieldIdentifier.COMPONENT_HASH.label,
          appIds);
    }
    catch (UnsupportedOperationException e) {
      log.warn(
          "Grouped distinct counting unavailable for {} session; falling back to {} per-app facet queries",
          session.backendId(),
          appIds.size());
      return fallbackCountApplicationsByDocCount(buckets);
    }
    catch (RuntimeException e) {
      log.warn("Application facet unavailable for {} session; returning the other facets", session.backendId(), e);
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

  /**
   * Fallback for backends without grouped distinct counting: uses doc-count from termsAggregation.
   * This inflates counts (one doc per CVE×app×component) but keeps the facet working.
   */
  private Map<String, Long> fallbackCountApplicationsByDocCount(final List<IndexTermsBucket> buckets) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (IndexTermsBucket bucket : buckets) {
      if (bucket.count() > 0 && StringUtils.isNotBlank(bucket.key())) {
        counts.put(bucket.key(), bucket.count());
      }
    }
    return counts.isEmpty() ? null : counts;
  }
}
