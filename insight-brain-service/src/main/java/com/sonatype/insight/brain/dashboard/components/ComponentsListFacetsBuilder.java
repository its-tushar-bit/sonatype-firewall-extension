/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import org.apache.commons.lang3.StringUtils;
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
 * Round trips are constant with respect to estate size: two {@code termsAggregation} calls for
 * key discovery plus one {@link IndexReadSession#countDistinctGroupedBy} call per facet group
 * (organizations, applications, stages) on a single shared session. The per-key
 * {@code countDistinct} loop is only a fallback for backends that do not implement grouped
 * distinct counting, and stays bounded by the entry caps below.
 */
@Named
@Singleton
final class ComponentsListFacetsBuilder
{
  private static final Logger log = LoggerFactory.getLogger(ComponentsListFacetsBuilder.class);

  /** Caps the facet entries returned to the rail (response size), not the number of queries. */
  static final int MAX_ORGANIZATION_FACET_ENTRIES = 25;

  static final int MAX_APPLICATION_FACET_ENTRIES = 25;

  /** Terms aggregation bucket cap for facet key discovery (doc-count order). */
  static final int MAX_FACET_TERM_BUCKETS = 100;

  private final SearchIndexClient searchIndexClient;

  private final IndexReadSessionFactory indexReadSessionFactory;

  private final ConversionHelper conversionHelper;

  private final StageTypeService stageTypeService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  ComponentsListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final IndexReadSessionFactory indexReadSessionFactory,
      final ConversionHelper conversionHelper,
      final StageTypeService stageTypeService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexReadSessionFactory = indexReadSessionFactory;
    this.conversionHelper = conversionHelper;
    this.stageTypeService = stageTypeService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
  }

  ComponentsListFacetsDTO buildFacets(final String componentQuery, final long totalComponents) {
    ComponentsListFacetsDTO facets = new ComponentsListFacetsDTO();
    facets.totalComponents = totalComponents;

    try (IndexReadSession session = indexReadSessionFactory.open()) {
      Set<String> organizationIds = discoverFacetKeys(session, componentQuery,
          FieldIdentifier.ORGANIZATION_ID.label);
      Set<String> applicationIds = discoverFacetKeys(session, componentQuery,
          FieldIdentifier.APPLICATION_ID.label);

      if (!organizationIds.isEmpty() || !applicationIds.isEmpty()) {
        facets.organizations = countByDimension(session, componentQuery,
            FieldIdentifier.ORGANIZATION_ID.label, capped(organizationIds, MAX_ORGANIZATION_FACET_ENTRIES));
        facets.applications = countByDimension(session, componentQuery,
            FieldIdentifier.APPLICATION_ID.label, capped(applicationIds, MAX_APPLICATION_FACET_ENTRIES));
        facets.organizationNames = resolveOrganizationNames(
            facets.organizations == null ? Set.of() : facets.organizations.keySet());
        facets.applicationNames = resolveApplicationNames(
            facets.applications == null ? Set.of() : facets.applications.keySet());
      }
      facets.stages = countLicensedStages(session, componentQuery);
      return facets;
    }
  }

  private Set<String> discoverFacetKeys(
      final IndexReadSession session,
      final String componentQuery,
      final String field)
  {
    try {
      List<IndexTermsBucket> buckets = session.termsAggregation(
          conversionHelper.stringToQuery(componentQuery),
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
   * One grouped distinct-count pass for the whole dimension. Falls back to a bounded per-key
   * {@code countDistinct} loop only when the session backend lacks grouped distinct counting.
   */
  private Map<String, Long> countByDimension(
      final IndexReadSession session,
      final String componentQuery,
      final String groupField,
      final Set<String> groupValues)
  {
    if (groupValues.isEmpty()) {
      return null;
    }

    Map<String, Long> grouped;
    try {
      grouped = session.countDistinctGroupedBy(
          conversionHelper.stringToQuery(componentQuery),
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
      return countByDimensionPerKey(componentQuery, groupField, groupValues);
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      counts.put(groupValue, grouped == null ? 0L : grouped.getOrDefault(groupValue, 0L));
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countByDimensionPerKey(
      final String componentQuery,
      final String groupField,
      final Set<String> groupValues)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      String clause = groupField + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(groupValue) + ")";
      counts.put(groupValue, searchIndexClient.countDistinct(
          componentQuery + " AND " + clause,
          List.of(FieldIdentifier.COMPONENT_HASH.label)));
    }
    return counts.isEmpty() ? null : counts;
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
    if (organizationIds == null || organizationIds.isEmpty()) {
      return null;
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (Organization organization : organizationDAO.getByIds(organizationIds)) {
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
      final String componentQuery)
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
          conversionHelper.stringToQuery(violationQuery),
          FieldIdentifier.POLICY_EVALUATION_STAGE.label,
          FieldIdentifier.COMPONENT_HASH.label,
          licensedStageIds);
    }
    catch (UnsupportedOperationException e) {
      log.warn(
          "Grouped distinct counting unavailable for {} session; falling back to {} per-stage facet queries",
          session.backendId(),
          licensedStageIds.size());
      grouped = countStagesPerKey(violationQuery, licensedStageIds);
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    for (String stageId : licensedStageIds) {
      long count = grouped == null ? 0L : grouped.getOrDefault(stageId, 0L);
      if (count > 0) {
        counts.put(stageId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countStagesPerKey(final String violationQuery, final List<String> stageIds) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String stageId : stageIds) {
      String stageClause = ComponentsListViolationQuerySupport.buildStageFilterClause(Set.of(stageId));
      counts.put(stageId, searchIndexClient.countDistinct(
          violationQuery + " AND " + stageClause,
          List.of(FieldIdentifier.COMPONENT_HASH.label)));
    }
    return counts;
  }
}
