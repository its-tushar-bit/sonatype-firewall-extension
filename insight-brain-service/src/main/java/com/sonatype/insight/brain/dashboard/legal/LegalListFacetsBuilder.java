/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IdSetFilterQueries;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds Nexus One Legal sidebar facet counts from RBAC-scoped index count queries.
 * <p>
 * Organization / application / LTG count-query fan-out is capped (same contract as Violations)
 * so estate-scale discovery cannot unbounded-query the index. Owner and LTG discovery share a
 * single capped search so facet discovery stays O(1) searches per list request.
 */
@Named
@Singleton
final class LegalListFacetsBuilder
{
  static final int MAX_FACET_DISCOVERY_HITS =
      Integer.getInteger("nexusOne.legal.facets.maxDiscoveryHits", 200);

  static final int MAX_ORGANIZATION_FACETS =
      Integer.getInteger("nexusOne.legal.facets.maxOrganizationCountQueries", 15);

  static final int MAX_APPLICATION_FACETS =
      Integer.getInteger("nexusOne.legal.facets.maxApplicationCountQueries", 15);

  static final int MAX_LICENSE_THREAT_GROUP_FACETS =
      Integer.getInteger("nexusOne.legal.facets.maxLicenseThreatGroupCountQueries", 15);

  private final SearchIndexClient searchIndexClient;

  private final StageTypeService stageTypeService;

  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  LegalListFacetsBuilder(
      final SearchIndexClient searchIndexClient,
      final StageTypeService stageTypeService,
      final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.searchIndexClient = searchIndexClient;
    this.stageTypeService = stageTypeService;
    this.dimensionQueryBuilder = dimensionQueryBuilder;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
  }

  LegalListFacetsDTO buildFacets(
      final String legalQuery,
      final long totalFindings,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    LegalListFacetsDTO facets = new LegalListFacetsDTO();
    facets.totalFindings = totalFindings;
    if (totalFindings == 0) {
      return facets;
    }

    List<IndexFilterRestriction> restrictions = scopeRestrictions == null ? List.of() : scopeRestrictions;
    ToLongFunction<String> counter = query -> searchIndexClient.count(query, restrictions);
    facets.stages = countLicensedStages(legalQuery, counter);

    SearchResultDTO discovery =
        searchIndexClient.searchIndex(legalQuery, MAX_FACET_DISCOVERY_HITS, 0, false, false, List.of(),
            restrictions);
    LinkedHashMap<String, SearchResultItemDTO> discoveredOwners = ownersByApplicationId(discovery);
    Set<String> ltgNames = licenseThreatGroupNames(discovery);

    if (!discoveredOwners.isEmpty()) {
      facets.organizations = countOrganizations(legalQuery, discoveredOwners, restrictions);
      facets.applications = countApplications(legalQuery, discoveredOwners, restrictions);
    }
    facets.licenseThreatGroups = countLicenseThreatGroups(legalQuery, ltgNames, counter);
    attachOwnerLabels(facets, discoveredOwners);
    return facets;
  }

  private Map<String, Long> countLicensedStages(
      final String legalQuery,
      final ToLongFunction<String> counter)
  {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
      String stageId = stageType.getId();
      String clause = FieldIdentifier.POLICY_EVALUATION_STAGE.label + ":("
          + DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(stageId) + ")";
      long count = counter.applyAsLong(legalQuery + " AND " + clause);
      if (count > 0) {
        counts.put(stageId, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  /** One representative hit per application for org/app facet keys and owner labels. */
  private static LinkedHashMap<String, SearchResultItemDTO> ownersByApplicationId(final SearchResultDTO discovery) {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    LegalListIndexItems.legalHits(discovery).forEach(item -> {
      if (StringUtils.isNotBlank(item.applicationId)) {
        items.putIfAbsent(item.applicationId, item);
      }
    });
    return items;
  }

  /** All distinct LTG display names in the discovery window (not collapsed by application). */
  private static Set<String> licenseThreatGroupNames(final SearchResultDTO discovery) {
    Set<String> names = new LinkedHashSet<>();
    LegalListIndexItems.legalHits(discovery).forEach(item -> {
      if (StringUtils.isNotBlank(item.componentLicenseThreatGroupName)) {
        names.add(item.componentLicenseThreatGroupName);
      }
    });
    return names;
  }

  private Map<String, Long> countOrganizations(
      final String legalQuery,
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

    Map<String, Set<String>> expandedById = dimensionQueryBuilder.expandOrganizationFilterIdsById(organizationIds);
    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String organizationId : organizationIds) {
      if (queries >= MAX_ORGANIZATION_FACETS) {
        break;
      }
      Set<String> expandedOrgIds = expandedById.get(organizationId);
      if (expandedOrgIds == null) {
        continue;
      }
      List<IndexFilterRestriction> combined = IdSetFilterQueries.combine(scopeRestrictions,
          IndexTermSetRestriction.singleton(FieldIdentifier.ORGANIZATION_ID.label, expandedOrgIds));
      counts.put(organizationId, searchIndexClient.count(legalQuery, combined));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countApplications(
      final String legalQuery,
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
      counts.put(applicationId, searchIndexClient.count(legalQuery, combined));
      queries++;
    }
    return counts.isEmpty() ? null : counts;
  }

  private Map<String, Long> countLicenseThreatGroups(
      final String legalQuery,
      final Set<String> ltgNames,
      final ToLongFunction<String> counter)
  {
    if (ltgNames == null || ltgNames.isEmpty()) {
      return null;
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    int queries = 0;
    for (String ltgName : ltgNames) {
      // Cap on populated facet entries so zero-count discoveries do not starve later LTGs of slots.
      if (counts.size() >= MAX_LICENSE_THREAT_GROUP_FACETS) {
        break;
      }
      // Hard bound on index round-trips even when early names return zero (stale/renamed LTGs).
      if (queries >= MAX_LICENSE_THREAT_GROUP_FACETS) {
        break;
      }
      String clause = FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label + ":("
          + LegalListIndexQueryBuilder.quotedPhrase(ltgName) + ")";
      queries++;
      long count = counter.applyAsLong(legalQuery + " AND " + clause);
      if (count > 0) {
        counts.put(ltgName, count);
      }
    }
    return counts.isEmpty() ? null : counts;
  }

  private void attachOwnerLabels(
      final LegalListFacetsDTO facets,
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

}
