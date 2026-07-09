/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.apache.commons.lang3.StringUtils;

/**
 * Index-backed Martha V1 Applications list.
 * <p>
 * Pagination and RBAC discovery use the search index; evaluation card payload
 * (stage threats + scanId) is enriched per page via {@link ApplicationRiskService}
 * so only the visible page pays SQL/policy-violation cost.
 * <p>
 * Request defaults: {@code page=0}, {@code pageSize=50}, {@code includeFacets=true} when omitted.
 */
@Named
@Singleton
public class ApplicationsListService
{
  public static final int DEFAULT_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  private final SearchIndexClient searchIndexClient;

  private final ApplicationRiskService applicationRiskService;

  private final ApplicationsListIndexQueryBuilder indexQueryBuilder;

  private final ApplicationsListRequestValidator requestValidator;

  @Inject
  public ApplicationsListService(
      final SearchIndexClient searchIndexClient,
      final ApplicationRiskService applicationRiskService,
      final ApplicationsListIndexQueryBuilder indexQueryBuilder,
      final ApplicationsListRequestValidator requestValidator)
  {
    this.searchIndexClient = searchIndexClient;
    this.applicationRiskService = applicationRiskService;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
  }

  public ApplicationsListResponseDTO listApplications(final ApplicationsListRequestDTO request) {
    int page = request == null || request.page == null ? 0 : request.page;
    int pageSize = request == null || request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    String search = request == null ? null : request.search;
    boolean includeFacets = request == null || request.includeFacets == null || request.includeFacets;

    validatePagination(page, pageSize);
    validateSearch(search);
    requestValidator.validate(request);

    String query = indexQueryBuilder.buildApplicationQuery(request);
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(query, pageSize, toSearchIndexPage(page), false, false, List.of());
    LinkedHashMap<String, SearchResultItemDTO> pageItems = extractApplicationPageItems(searchResult);
    Set<String> pageApplicationIds = pageItems.keySet();

    List<ApplicationRiskScoreDTO> cards = List.of();
    if (!pageApplicationIds.isEmpty()) {
      List<ApplicationRiskScoreDTO> enriched = List.of();
      try {
        DashboardResultsDTO<ApplicationRiskScoreDTO> risks = applicationRiskService.getApplicationRiskCards(
            null,
            pageApplicationIds,
            request == null ? null : request.stageIds,
            null,
            request == null ? null : request.policyThreatCategories,
            request == null ? null : request.policyThreatLevelRange,
            request == null ? null : request.policyViolationStates);
        enriched = risks.dashboardResults == null ? List.of() : risks.dashboardResults;
      }
      catch (ConflictException e) {
        // Only Classic Dashboard licence checks throw ConflictException here; real enrichment
        // failures (DB, policy load) propagate as 500 so operators see actionable errors.
      }
      cards = mergeIndexPageWithEnrichment(pageItems, enriched);
    }

    ApplicationsListResponseDTO response = new ApplicationsListResponseDTO();
    response.applications = new ArrayList<>(cards);
    response.total = searchResult.totalNumberOfHits;
    response.page = page;
    response.pageSize = pageSize;
    long consumed = (long) page * pageSize + pageApplicationIds.size();
    response.hasNextPage = consumed < searchResult.totalNumberOfHits;
    response.source = ApplicationsListResponseDTO.SOURCE_INDEX;
    if (includeFacets) {
      ApplicationsListFacetsDTO facets = new ApplicationsListFacetsDTO();
      facets.totalApplications = searchResult.totalNumberOfHits;
      response.facets = facets;
    }
    return response;
  }

  /**
   * {@link SearchIndexClient#searchIndex} uses {@code page=0} as a first-page sentinel and
   * 1-based pages thereafter — same contract as Advanced Search ({@code ApiAdvancedSearchResourceV2}).
   * <p>
   * Index pages 0 and 1 both return the first result window, so client page {@code 1} maps to
   * index page {@code 2} (not {@code 1}).
   */
  static int toSearchIndexPage(final int zeroBasedPage) {
    return zeroBasedPage == 0 ? 0 : zeroBasedPage + 1;
  }

  static String escapeLuceneTerm(final String input) {
    return DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(input);
  }

  private static LinkedHashMap<String, SearchResultItemDTO> extractApplicationPageItems(
      final SearchResultDTO searchResult)
  {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (searchResult.groupingByDTOS != null) {
      for (var group : searchResult.groupingByDTOS) {
        if (group.searchResultItemDTOS == null) {
          continue;
        }
        for (SearchResultItemDTO item : group.searchResultItemDTOS) {
          if (!ItemType.APPLICATION.name().equals(item.itemType) || StringUtils.isBlank(item.applicationId)) {
            continue;
          }
          items.putIfAbsent(item.applicationId, item);
        }
      }
    }
    return items;
  }

  private static List<ApplicationRiskScoreDTO> mergeIndexPageWithEnrichment(
      final LinkedHashMap<String, SearchResultItemDTO> pageItems,
      final List<ApplicationRiskScoreDTO> enriched)
  {
    Map<String, ApplicationRiskScoreDTO> enrichedByInternalId = enriched == null
        ? Map.of()
        : enriched.stream()
            .filter(card -> card.id != null)
            .collect(Collectors.toMap(card -> card.id, Function.identity(), (left, right) -> left));

    List<ApplicationRiskScoreDTO> cards = new ArrayList<>(pageItems.size());
    for (Map.Entry<String, SearchResultItemDTO> entry : pageItems.entrySet()) {
      ApplicationRiskScoreDTO card = enrichedByInternalId.get(entry.getKey());
      if (card != null) {
        cards.add(card);
      }
      else {
        SearchResultItemDTO item = entry.getValue();
        cards.add(new ApplicationRiskScoreDTO(
            item.organizationName,
            item.organizationId,
            item.applicationName,
            item.applicationPublicId,
            item.applicationId));
      }
    }
    return cards;
  }

  private static void validatePagination(final int page, final int pageSize) {
    if (page < 0) {
      throw new BadRequestException("Invalid page: " + page + ". Page must be >= 0.");
    }
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "Invalid page size: " + pageSize + ". Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
    }
  }

  private static void validateSearch(final String search) {
    if (search != null && search.length() > MAX_SEARCH_LENGTH) {
      throw new BadRequestException(
          "Search query exceeds maximum length of " + MAX_SEARCH_LENGTH + " characters.");
    }
  }
}
