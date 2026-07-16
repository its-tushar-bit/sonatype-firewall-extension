/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Index-backed Martha V1 Violations list.
 * <p>
 * Rows are mapped directly from {@code POLICY_VIOLATION} search-index hits — there is no per-page SQL
 * enrichment pass (unlike the Applications list), because violation volume is much higher and every
 * card field the index carries is sufficient for V1.
 * <p>
 * Request defaults: {@code page=0}, {@code pageSize=50}, {@code includeFacets=true} when omitted.
 * <p>
 * <b>Sort caveat:</b> {@code orderBy=-policyThreatLevel} (the default) orders rows <em>within</em> each
 * index page after retrieval — {@link SearchIndexClient#searchIndex} exposes no sort-field parameter,
 * so the ordering cannot be pushed into the index for this V1. Consequently the highest-threat rows are
 * ordered correctly on each returned page, but global "highest threat first" ordering is <em>not</em>
 * guaranteed across page boundaries: a threat-10 row can land on page 2 while page 1 shows lower
 * threats, because index pagination order (not threat level) decides which rows fall on which page.
 * Index-level sort is tracked under CLM-42262; {@code ViolationsListResourceTest} pins this per-page
 * behaviour with a {@code total > pageSize} case so the limitation stays visible in the suite. (Unlike
 * the Applications list, which rejects {@code orderBy} outright, we keep the threat-level default
 * because it is the product-specified ordering and is correct for the common single-page view.)
 */
@Named
@Singleton
public class ViolationsListService
{
  public static final int DEFAULT_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  private final SearchIndexClient searchIndexClient;

  private final ViolationsListIndexQueryBuilder indexQueryBuilder;

  private final ViolationsListRequestValidator requestValidator;

  private final ViolationsListFacetsBuilder facetsBuilder;

  @Inject
  public ViolationsListService(
      final SearchIndexClient searchIndexClient,
      final ViolationsListIndexQueryBuilder indexQueryBuilder,
      final ViolationsListRequestValidator requestValidator,
      final ViolationsListFacetsBuilder facetsBuilder)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.facetsBuilder = facetsBuilder;
  }

  public ViolationsListResponseDTO listViolations(final ViolationsListRequestDTO request) {
    int page = request == null || request.page == null ? 0 : request.page;
    int pageSize = request == null || request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    String search = request == null ? null : request.search;
    boolean includeFacets = request == null || request.includeFacets == null || request.includeFacets;

    validatePagination(page, pageSize);
    validateSearch(search);
    requestValidator.validate(request);

    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? ViolationsListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;

    String query = indexQueryBuilder.buildViolationQuery(request);
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(query, pageSize, toSearchIndexPage(page), false, false, List.of());
    LinkedHashMap<String, SearchResultItemDTO> pageItems = extractViolationPageItems(searchResult);

    List<ViolationRowDTO> rows = new ArrayList<>(pageItems.size());
    for (SearchResultItemDTO item : pageItems.values()) {
      rows.add(toRow(item));
    }
    rows.sort(comparator(orderBy));

    ViolationsListResponseDTO response = new ViolationsListResponseDTO();
    response.violations = rows;
    // total is the raw index hit count. Violation docs are 1:1 with policyViolationId, so it matches
    // the deduplicated row count; extractViolationPageItems only dedups defensively against a
    // hypothetical duplicate doc, in which case total would over-report by the duplicate count.
    response.total = searchResult.totalNumberOfHits;
    response.page = page;
    response.pageSize = pageSize;
    long consumed = (long) page * pageSize + pageItems.size();
    response.hasNextPage = consumed < searchResult.totalNumberOfHits;
    response.source = ViolationsListResponseDTO.SOURCE_INDEX;
    if (includeFacets) {
      // The waiver-type facet is single-select, so it is counted against the query minus its own clause
      // (identical to `query` when no waiver-type filter is active) — see ViolationsListFacetsBuilder.
      String waiverFacetQuery = indexQueryBuilder.buildViolationQueryExcludingWaiverType(request);
      response.facets =
          facetsBuilder.buildFacets(query, waiverFacetQuery, searchResult.totalNumberOfHits);
    }
    return response;
  }

  /**
   * {@link SearchIndexClient#searchIndex} uses {@code page=0} as a first-page sentinel and 1-based
   * pages thereafter — same contract as the Applications list and Advanced Search. Index pages 0 and
   * 1 both return the first result window, so client page {@code 1} maps to index page {@code 2}.
   * <p>
   * The client {@code pageSize} is passed straight through as the index page size (see the
   * {@code searchIndex} call), so every index page holds exactly {@code pageSize} rows and the mapping
   * is gap-free: client page 0 → index rows 0..pageSize-1, client page 1 (index page 2) →
   * rows pageSize..2*pageSize-1, and so on. A {@code pageSize=50, total=51} request therefore returns
   * the 50th–51st rows on client page 1 with no dropped item — the {@code pageSize=2, total=3}
   * resource tests pin this boundary.
   */
  static int toSearchIndexPage(final int zeroBasedPage) {
    return zeroBasedPage == 0 ? 0 : zeroBasedPage + 1;
  }

  static Comparator<ViolationRowDTO> comparator(final String orderBy) {
    boolean ascending = "policyThreatLevel".equals(orderBy);
    Comparator<Integer> threatOrder = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();
    // Wrap nullsLast around the directional order (not the other way around) so that rows with an
    // absent threat level always sort last, for both ascending and the default descending request.
    // (byThreat.reversed() would flip the null placement to the front for descending.)
    return Comparator.comparing(row -> row.threatLevel, Comparator.nullsLast(threatOrder));
  }

  private static ViolationRowDTO toRow(final SearchResultItemDTO item) {
    ViolationRowDTO row = new ViolationRowDTO();
    row.policyViolationId = item.policyViolationId;
    row.threatLevel = item.policyViolationThreatLevel;
    if (item.policyViolationThreatLevel != null) {
      row.severity = ThreatLevel.from(item.policyViolationThreatLevel).name().toLowerCase(Locale.ROOT);
    }
    row.threatCategory = item.policyViolationThreatCategory;
    row.policyId = item.policyViolationPolicyId;
    row.policyName = item.policyViolationPolicyName;
    row.organizationId = item.organizationId;
    row.organizationName = item.organizationName;
    row.applicationId = item.applicationId;
    row.applicationPublicId = item.applicationPublicId;
    row.applicationName = item.applicationName;
    row.componentName = item.componentName;
    row.componentIdentifier = item.componentIdentifier;
    row.componentVersion = extractComponentVersion(item.componentIdentifier);
    row.stage = item.policyEvaluationStage;
    // A null/absent waiver status intentionally maps to OPEN via toState's fall-through — do not add
    // a null guard here or make toState throw; OPEN is the correct default for an unwaived violation.
    row.state = ViolationWaiverStatus.toState(item.policyViolationWaiverStatus).name();
    row.waivedWithAutoWaiver = ViolationWaiverStatus.isAutoWaived(item.policyViolationWaiverStatus);
    row.constraintName = item.policyViolationConstraintName;
    return row;
  }

  private static String extractComponentVersion(final ApiComponentIdentifierDTOV2 componentIdentifier) {
    // The source is a deserialized index DTO, so guard the coordinates map too — do not assume the
    // TreeMap default survives deserialization.
    if (componentIdentifier == null || componentIdentifier.getCoordinates() == null) {
      return null;
    }
    return componentIdentifier.getCoordinates().get("version");
  }

  private static LinkedHashMap<String, SearchResultItemDTO> extractViolationPageItems(
      final SearchResultDTO searchResult)
  {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (searchResult.groupingByDTOS != null) {
      for (var group : searchResult.groupingByDTOS) {
        if (group.searchResultItemDTOS == null) {
          continue;
        }
        for (SearchResultItemDTO item : group.searchResultItemDTOS) {
          if (!ItemType.POLICY_VIOLATION.name().equals(item.itemType)
              || StringUtils.isBlank(item.policyViolationId))
          {
            continue;
          }
          items.putIfAbsent(item.policyViolationId, item);
        }
      }
    }
    return items;
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
