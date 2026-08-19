/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Martha V1 My Scan Data blast-radius CSV export (CLM-42216).
 * <p>
 * Unlike the list API (one row per distinct {@code vulnerabilityId}), export emits one row per
 * index hit (vulnerability × scan target / component occurrence).
 */
@Named
@Singleton
public class VulnerabilitiesExportService
{
  private static final Logger log = LoggerFactory.getLogger(VulnerabilitiesExportService.class);

  /** Soft cap to avoid unbounded memory materialization for estate-scale exports. */
  static final int MAX_EXPORT_ROWS = 50_000;

  private static final int INDEX_FETCH_PAGE_SIZE = 100;

  private static final int MAX_INDEX_PAGES = 500;

  private final SearchIndexClient searchIndexClient;

  private final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder;

  private final VulnerabilitiesListRequestValidator requestValidator;

  @Inject
  public VulnerabilitiesExportService(
      final SearchIndexClient searchIndexClient,
      final VulnerabilitiesListIndexQueryBuilder indexQueryBuilder,
      final VulnerabilitiesListRequestValidator requestValidator)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
  }

  public List<VulnerabilitiesBlastRadiusRowDTO> exportBlastRadius(final VulnerabilitiesListRequestDTO request) {
    String search = request == null ? null : request.search;
    validateSearch(search);
    requestValidator.validate(request);

    String tab = VulnerabilitiesListRequestValidator.normalizeTab(request == null ? null : request.tab);
    if (VulnerabilitiesListRequestValidator.TAB_CATALOG.equals(tab)) {
      throw new BadRequestException(
          "Catalog tab export is not supported. Export is available for My Scan Data only.");
    }

    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? VulnerabilitiesListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;

    String query = indexQueryBuilder.buildMyScanDataQuery(request);
    List<IndexFilterRestriction> scopeRestrictions = indexQueryBuilder.buildScopeRestrictions(request);
    List<SearchResultItemDTO> hits = collectHits(query, scopeRestrictions);
    if (hits.size() >= MAX_EXPORT_ROWS) {
      log.warn(
          "Vulnerabilities blast-radius export truncated at {} rows; results may be incomplete",
          MAX_EXPORT_ROWS);
    }
    hits.sort(exportComparator(orderBy));

    List<VulnerabilitiesBlastRadiusRowDTO> rows = new ArrayList<>(hits.size());
    for (SearchResultItemDTO item : hits) {
      rows.add(VulnerabilitiesBlastRadiusRowDTO.fromIndexItem(item));
    }
    return rows;
  }

  private List<SearchResultItemDTO> collectHits(
      final String query,
      final List<IndexFilterRestriction> scopeRestrictions)
  {
    List<SearchResultItemDTO> hits = new ArrayList<>();
    for (int indexPage = 0; indexPage < MAX_INDEX_PAGES && hits.size() < MAX_EXPORT_ROWS; indexPage++) {
      SearchResultDTO searchResult = searchIndexClient.searchIndex(
          query,
          INDEX_FETCH_PAGE_SIZE,
          VulnerabilitiesListService.toSearchIndexPage(indexPage),
          false,
          false,
          List.of(),
          scopeRestrictions);
      appendVulnerabilityHits(searchResult, hits);
      boolean exhaustedPage = searchResult.groupingByDTOS == null
          || searchResult.groupingByDTOS.isEmpty()
          || countItems(searchResult) < INDEX_FETCH_PAGE_SIZE;
      if (exhaustedPage) {
        break;
      }
    }
    return hits;
  }

  private static void appendVulnerabilityHits(
      final SearchResultDTO searchResult,
      final List<SearchResultItemDTO> hits)
  {
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (!ItemType.SECURITY_VULNERABILITY.name().equals(item.itemType)
            || StringUtils.isBlank(item.vulnerabilityId))
        {
          continue;
        }
        if (hits.size() >= MAX_EXPORT_ROWS) {
          return;
        }
        hits.add(item);
      }
    }
  }

  private static int countItems(final SearchResultDTO searchResult) {
    int count = 0;
    if (searchResult.groupingByDTOS == null) {
      return 0;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group.searchResultItemDTOS != null) {
        count += group.searchResultItemDTOS.size();
      }
    }
    return count;
  }

  static Comparator<SearchResultItemDTO> exportComparator(final String orderBy) {
    boolean ascending = "cvssScore".equals(orderBy);
    Comparator<Float> scoreOrder = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();
    return Comparator
        .comparing((SearchResultItemDTO item) -> item.vulnerabilitySeverity, Comparator.nullsLast(scoreOrder))
        .thenComparing(
            (SearchResultItemDTO item) -> item.vulnerabilityId,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(
            (SearchResultItemDTO item) -> item.organizationName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(
            (SearchResultItemDTO item) -> item.applicationName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(
            (SearchResultItemDTO item) -> item.componentName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
  }

  private static void validateSearch(final String search) {
    if (search != null && search.length() > VulnerabilitiesListService.MAX_SEARCH_LENGTH) {
      throw new BadRequestException(
          "Search query exceeds maximum length of " + VulnerabilitiesListService.MAX_SEARCH_LENGTH
              + " characters.");
    }
  }
}
