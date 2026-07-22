/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.SearchReadPath;
import com.sonatype.insight.brain.search.session.SearchReadPathFlags;
import com.sonatype.insight.brain.search.session.SearchReadPathSurface;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.commons.lang3.StringUtils;

/**
 * Resolves internal application ids matching violation-scoped filters (stage, threat level)
 * under the same RBAC-scoped base query as the Martha applications list.
 * <p>
 * Discovery is capped at {@link Configuration#getMaxAdvancedSearchClauseCount()} unique ids.
 * On the legacy read path ({@code nexusOne.search.readPath.applications=old}), discovery pages via
 * {@link SearchIndexClient#searchIndex} using {@link ApplicationsListService#toSearchIndexPage} so
 * index pages 0 and 1 (duplicate first-window sentinel) are not double-counted.
 * On the session read path ({@code =new}), discovery uses {@link IndexReadSession#searchPage} with
 * {@link ApplicationsListService#stableSessionSort()} and page size {@value #VIOLATION_DISCOVERY_PAGE_SIZE}.
 * <p>
 * Violation index hits are one document per violation, so paging counts raw violation docs.
 * Termination is driven by distinct {@code applicationId} progress: paging stops on a short page,
 * when the distinct-id cap is hit on a full page (400 — more matches likely exist), or after
 * {@link #MAX_CONSECUTIVE_VIOLATION_PAGES_WITHOUT_NEW_APPLICATION_IDS} consecutive full pages that
 * add no new application ids (graceful partial result, not a 400). Hitting exactly {@code maxIds}
 * on a short final page returns successfully; a full page at the cap means truncation risk so we 400.
 */
@Named
@Singleton
final class ApplicationsListViolationScopeResolver
{
  private static final int VIOLATION_DISCOVERY_PAGE_SIZE = 500;

  /**
   * Consecutive full violation pages that add zero new application ids before discovery stops.
   * Bounds work when many violation docs map to the same application without 400ing on raw doc volume.
   */
  static final int MAX_CONSECUTIVE_VIOLATION_PAGES_WITHOUT_NEW_APPLICATION_IDS = 10;

  private static final int DEFAULT_MAX_DISCOVERY_IDS = 2048;

  private final SearchIndexClient searchIndexClient;

  private final Configuration configuration;

  private final IndexReadSessionFactory sessionFactory;

  private final ConversionHelper conversionHelper;

  @Inject
  ApplicationsListViolationScopeResolver(
      final SearchIndexClient searchIndexClient,
      final Configuration configuration,
      final IndexReadSessionFactory sessionFactory,
      final ConversionHelper conversionHelper)
  {
    this.searchIndexClient = searchIndexClient;
    this.configuration = configuration;
    this.sessionFactory = sessionFactory;
    this.conversionHelper = conversionHelper;
  }

  /**
   * Test helper for the legacy ({@code old}) read path only — leaves session dependencies null.
   * Callers that exercise {@code readPath.applications=new} must use the 4-arg constructor.
   */
  ApplicationsListViolationScopeResolver(
      final SearchIndexClient searchIndexClient,
      final Configuration configuration)
  {
    this(searchIndexClient, configuration, null, null);
  }

  Set<String> resolveApplicationIds(
      final String baseApplicationQuery,
      final Set<String> stageIds,
      final List<PolicyThreatLevelFilter> threatFilters)
  {
    String violationQuery = buildScopedViolationQuery(baseApplicationQuery, stageIds, threatFilters);
    int maxIds = configuration.getMaxAdvancedSearchClauseCount();
    if (maxIds <= 0) {
      // Misconfigured or zero clause cap — fall back to the product default.
      maxIds = DEFAULT_MAX_DISCOVERY_IDS;
    }
    if (SearchReadPathFlags.forSurface(SearchReadPathSurface.APPLICATIONS) == SearchReadPath.NEW) {
      return resolveApplicationIdsWithSession(violationQuery, maxIds);
    }
    return resolveApplicationIdsWithSearchIndex(violationQuery, maxIds);
  }

  private Set<String> resolveApplicationIdsWithSearchIndex(final String violationQuery, final int maxIds) {
    LinkedHashSet<String> applicationIds = new LinkedHashSet<>();
    int page = 0;
    int consecutivePagesWithoutNewIds = 0;
    while (applicationIds.size() < maxIds) {
      int idsBeforePage = applicationIds.size();
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          violationQuery,
          VIOLATION_DISCOVERY_PAGE_SIZE,
          ApplicationsListService.toSearchIndexPage(page),
          false,
          false,
          List.of());
      if (searchResult == null || searchResult.groupingByDTOS == null || searchResult.groupingByDTOS.isEmpty()) {
        break;
      }
      int rawHitsThisPage = countRawViolationHits(searchResult);
      boolean discoveryCapped = false;
      for (var group : searchResult.groupingByDTOS) {
        if (group == null || group.searchResultItemDTOS == null) {
          continue;
        }
        for (SearchResultItemDTO item : group.searchResultItemDTOS) {
          if (item == null) {
            continue;
          }
          // Violation hits without applicationId are skipped; pagination assumes remaining pages
          // still yield distinct ids when present (blank ids do not advance the unique-id cap).
          if (StringUtils.isNotBlank(item.applicationId)) {
            applicationIds.add(item.applicationId);
            if (applicationIds.size() >= maxIds) {
              discoveryCapped = true;
              break;
            }
          }
        }
        if (discoveryCapped) {
          break;
        }
      }
      if (discoveryCapped) {
        if (rawHitsThisPage >= VIOLATION_DISCOVERY_PAGE_SIZE) {
          throw new BadRequestException(
              "Violation-scoped application discovery matched too many applications (max " + maxIds + ").");
        }
        break;
      }
      if (rawHitsThisPage < VIOLATION_DISCOVERY_PAGE_SIZE) {
        break;
      }
      int idsAddedThisPage = applicationIds.size() - idsBeforePage;
      if (idsAddedThisPage == 0) {
        consecutivePagesWithoutNewIds++;
        if (consecutivePagesWithoutNewIds >= MAX_CONSECUTIVE_VIOLATION_PAGES_WITHOUT_NEW_APPLICATION_IDS) {
          break;
        }
      }
      else {
        consecutivePagesWithoutNewIds = 0;
      }
      page++;
    }
    return applicationIds;
  }

  private Set<String> resolveApplicationIdsWithSession(final String violationQuery, final int maxIds) {
    if (conversionHelper == null || sessionFactory == null) {
      throw new IllegalStateException(
          "ApplicationsListViolationScopeResolver session dependencies are required when "
              + "nexusOne.search.readPath.applications=new (use the 4-arg constructor).");
    }
    LinkedHashSet<String> applicationIds = new LinkedHashSet<>();
    int consecutivePagesWithoutNewIds = 0;
    Query query = conversionHelper.stringToQuery(ApplicationsListService.toSessionQueryString(violationQuery));
    List<Object> searchAfter = List.of();
    try (IndexReadSession session = sessionFactory.open()) {
      while (applicationIds.size() < maxIds) {
        int idsBeforePage = applicationIds.size();
        IndexPageResult result = session.searchPage(new IndexPageRequest(
            query, ApplicationsListService.stableSessionSort(), VIOLATION_DISCOVERY_PAGE_SIZE, searchAfter));
        List<Document> docs = result == null ? List.of() : result.docs();
        if (docs.isEmpty()) {
          break;
        }

        boolean discoveryCapped = false;
        for (Document doc : docs) {
          String applicationId = doc.get(FieldIdentifier.APPLICATION_ID.label);
          if (StringUtils.isNotBlank(applicationId)) {
            applicationIds.add(applicationId);
            if (applicationIds.size() >= maxIds) {
              discoveryCapped = true;
              break;
            }
          }
        }
        boolean fullPage = docs.size() >= VIOLATION_DISCOVERY_PAGE_SIZE;
        if (discoveryCapped) {
          if (fullPage) {
            throw new BadRequestException(
                "Violation-scoped application discovery matched too many applications (max " + maxIds + ").");
          }
          break;
        }
        if (!fullPage || !result.hasNext()) {
          break;
        }
        int idsAddedThisPage = applicationIds.size() - idsBeforePage;
        if (idsAddedThisPage == 0) {
          consecutivePagesWithoutNewIds++;
          if (consecutivePagesWithoutNewIds >= MAX_CONSECUTIVE_VIOLATION_PAGES_WITHOUT_NEW_APPLICATION_IDS) {
            break;
          }
        }
        else {
          consecutivePagesWithoutNewIds = 0;
        }
        searchAfter = result.nextSearchAfter();
      }
    }
    return applicationIds;
  }

  private String buildScopedViolationQuery(
      final String baseApplicationQuery,
      final Set<String> stageIds,
      final List<PolicyThreatLevelFilter> threatFilters)
  {
    String violationQuery = ApplicationsListViolationQuerySupport.toViolationQuery(baseApplicationQuery);
    List<String> scopedClauses = new ArrayList<>(2);
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (maxClauseCount <= 0) {
      maxClauseCount = DEFAULT_MAX_DISCOVERY_IDS;
    }
    String stageClause = ApplicationsListViolationQuerySupport.buildStageFilterClause(stageIds, maxClauseCount);
    if (stageClause != null) {
      scopedClauses.add(stageClause);
    }
    String threatClause = ApplicationsListViolationQuerySupport.buildThreatFilterClause(threatFilters);
    if (threatClause != null) {
      scopedClauses.add(threatClause);
    }
    return ApplicationsListViolationQuerySupport.appendClauses(violationQuery, scopedClauses);
  }

  private static int countRawViolationHits(final SearchResultDTO searchResult) {
    int rawHits = 0;
    for (var group : searchResult.groupingByDTOS) {
      if (group != null && group.searchResultItemDTOS != null) {
        rawHits += group.searchResultItemDTOS.size();
      }
    }
    return rawHits;
  }
}
