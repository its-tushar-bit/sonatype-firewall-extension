/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

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
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Organization / application / stage facet counts for the Vulnerabilities My Scan Data list
 * (CLM-43211), following the {@code ComponentsListFacetsBuilder} pattern.
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

  /** Terms aggregation bucket cap for facet key discovery (doc-count order). */
  static final int MAX_FACET_TERM_BUCKETS = 100;

  private final IndexReadSessionFactory indexReadSessionFactory;

  private final ConversionHelper conversionHelper;

  private final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder;

  private final StageTypeService stageTypeService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  VulnerabilitiesListScopeFacetsBuilder(
      final IndexReadSessionFactory indexReadSessionFactory,
      final ConversionHelper conversionHelper,
      final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder,
      final StageTypeService stageTypeService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.indexReadSessionFactory = indexReadSessionFactory;
    this.conversionHelper = conversionHelper;
    this.indexQueryBuilder = indexQueryBuilder;
    this.stageTypeService = stageTypeService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
  }

  void attachScopeFacets(
      final VulnerabilitiesListFacetsDTO facets,
      final VulnerabilitiesListRequestDTO request)
  {
    String organizationQuery = indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.ORGANIZATION);
    String applicationQuery = indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.APPLICATION);
    String stageQuery = indexQueryBuilder.buildMyScanDataQuery(request, FacetDimension.STAGE);

    Query orgLucene = toScopedQuery(organizationQuery,
        indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.ORGANIZATION));
    Query appLucene = toScopedQuery(applicationQuery,
        indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.APPLICATION));
    Query stageLucene = toScopedQuery(stageQuery,
        indexQueryBuilder.buildScopeRestrictions(request, FacetDimension.STAGE));

    try (IndexReadSession session = indexReadSessionFactory.open()) {
      Set<String> organizationIds = capped(
          discoverKeys(session, orgLucene, FieldIdentifier.ORGANIZATION_ID.label));
      Set<String> applicationIds = capped(
          discoverKeys(session, appLucene, FieldIdentifier.APPLICATION_ID.label));

      facets.organizations = countDistinctVulnerabilities(
          session, orgLucene, FieldIdentifier.ORGANIZATION_ID.label, organizationIds);
      facets.applications = countDistinctVulnerabilities(
          session, appLucene, FieldIdentifier.APPLICATION_ID.label, applicationIds);
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
    Query base = conversionHelper.stringToQuery(query);
    Query filters = IdSetFilterQueries.combineLuceneFilters(restrictions);
    if (filters == null) {
      return base;
    }
    return new BooleanQuery.Builder()
        .add(base, Occur.MUST)
        .add(filters, Occur.FILTER)
        .build();
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

  private static Set<String> capped(final Set<String> keys) {
    if (keys.size() <= MAX_OWNER_FACET_ENTRIES) {
      return keys;
    }
    Set<String> limited = new LinkedHashSet<>();
    for (String key : keys) {
      if (limited.size() >= MAX_OWNER_FACET_ENTRIES) {
        break;
      }
      limited.add(key);
    }
    return limited;
  }
}
