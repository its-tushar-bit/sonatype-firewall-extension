/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Index-backed Nexus One Legal list.
 * <p>
 * Rows are mapped directly from {@code LEGAL_VIOLATION} search-index hits — the same index
 * document type the Dashboard Legal Obligations card queries. Row identity is deliberately
 * finer than the card: the card uses {@code countDistinct} on
 * {@code (applicationId, componentHash, componentEffectiveLicenseId)} (collapsing stage and
 * license threat group), while this list returns one triage row per
 * {@code (application, hash, license, LTG, stage)} so operators can filter and drill by stage
 * and LTG. {@code total} therefore counts displayable index docs and is not expected to equal
 * the card metric.
 * <p>
 * Request defaults: {@code page=0}, {@code pageSize=50}, {@code includeFacets=true} when omitted.
 * <p>
 * <b>Sort caveat:</b> {@code orderBy=-licenseThreatLevel} (the default) orders rows within each
 * index page after retrieval — {@code SearchIndexClient} has no sort parameter — so each returned
 * page is sorted highest-threat-first among the rows it contains, but global order across page
 * boundaries is not guaranteed (same limitation as Violations; index-level sort tracked under
 * CLM-42262).
 */
@Named
@Singleton
public class LegalListService
{
  public static final int DEFAULT_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  /**
   * Soft ceiling on 0-based page index so {@link #toSearchIndexPage} cannot overflow
   * ({@code Integer.MAX_VALUE + 1}) and deep links cannot request absurd offsets.
   * Overridable via {@code nexusOne.legal.maxWalkablePage} (same convention as Violations).
   */
  public static final int MAX_WALKABLE_PAGE =
      Integer.getInteger("nexusOne.legal.maxWalkablePage", 200);

  private final SearchIndexClient searchIndexClient;

  private final LegalListIndexQueryBuilder indexQueryBuilder;

  private final LegalListRequestValidator requestValidator;

  private final LegalListFacetsBuilder facetsBuilder;

  @Inject
  public LegalListService(
      final SearchIndexClient searchIndexClient,
      final LegalListIndexQueryBuilder indexQueryBuilder,
      final LegalListRequestValidator requestValidator,
      final LegalListFacetsBuilder facetsBuilder)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.facetsBuilder = facetsBuilder;
  }

  public LegalListResponseDTO listLegalFindings(final LegalListRequestDTO request) {
    int page = request == null || request.page == null ? 0 : request.page;
    int pageSize = request == null || request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    String search = request == null ? null : request.search;
    boolean includeFacets = request == null || request.includeFacets == null || request.includeFacets;

    validatePagination(page, pageSize);
    validateSearch(search);
    requestValidator.validate(request);

    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? LegalListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;

    String query = indexQueryBuilder.buildLegalQuery(request);
    List<IndexFilterRestriction> scopeRestrictions = indexQueryBuilder.buildScopeRestrictions(request);
    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(query, pageSize, toSearchIndexPage(page), false, false, List.of(),
            scopeRestrictions);
    LinkedHashMap<String, SearchResultItemDTO> pageItems = LegalListIndexItems.extractLegalItems(searchResult);

    List<LegalRowDTO> rows = new ArrayList<>(pageItems.size());
    for (SearchResultItemDTO item : pageItems.values()) {
      rows.add(toRow(item));
    }
    rows.sort(comparator(orderBy));

    LegalListResponseDTO response = new LegalListResponseDTO();
    response.findings = rows;
    response.total = searchResult.totalNumberOfHits;
    response.page = page;
    response.pageSize = pageSize;
    long consumed = (long) page * pageSize + pageItems.size();
    response.hasNextPage = consumed < searchResult.totalNumberOfHits && page < MAX_WALKABLE_PAGE;
    response.source = LegalListResponseDTO.SOURCE_INDEX;
    if (includeFacets) {
      response.facets = facetsBuilder.buildFacets(query, searchResult.totalNumberOfHits, scopeRestrictions);
    }
    return response;
  }

  /**
   * {@link SearchIndexClient#searchIndex} uses {@code page=0} as a first-page sentinel and 1-based
   * pages thereafter — same contract as the Violations list.
   */
  static int toSearchIndexPage(final int zeroBasedPage) {
    return zeroBasedPage == 0 ? 0 : zeroBasedPage + 1;
  }

  static Comparator<LegalRowDTO> comparator(final String orderBy) {
    boolean ascending = "licenseThreatLevel".equals(orderBy);
    Comparator<Integer> threatOrder = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();
    return Comparator.comparing(row -> row.threatLevel, Comparator.nullsLast(threatOrder));
  }

  private static LegalRowDTO toRow(final SearchResultItemDTO item) {
    LegalRowDTO row = new LegalRowDTO();
    row.legalFindingId = LegalListIndexItems.compositeLegalFindingId(item);
    row.threatLevel = item.componentLicenseThreatLevel;
    if (item.componentLicenseThreatLevel != null) {
      row.severity = ThreatLevel.from(item.componentLicenseThreatLevel).name().toLowerCase(Locale.ROOT);
    }
    row.licenseId = item.componentEffectiveLicenseId;
    row.licenseName = item.componentEffectiveLicenseName;
    row.licenseThreatGroupName = item.componentLicenseThreatGroupName;
    row.organizationId = item.organizationId;
    row.organizationName = item.organizationName;
    row.applicationId = item.applicationId;
    row.applicationPublicId = item.applicationPublicId;
    row.applicationName = item.applicationName;
    row.componentName = item.componentName;
    row.componentIdentifier = item.componentIdentifier;
    row.componentVersion = extractComponentVersion(item.componentIdentifier);
    row.componentHash = item.componentHash;
    row.stage = item.policyEvaluationStage;
    row.reportId = item.reportId;
    return row;
  }

  private static String extractComponentVersion(final ApiComponentIdentifierDTOV2 componentIdentifier) {
    if (componentIdentifier == null || componentIdentifier.getCoordinates() == null) {
      return null;
    }
    return componentIdentifier.getCoordinates().get("version");
  }

  private static void validatePagination(final int page, final int pageSize) {
    if (page < 0) {
      throw new BadRequestException("Invalid page: " + page + ". Page must be >= 0.");
    }
    if (page > MAX_WALKABLE_PAGE) {
      throw new BadRequestException(
          "Invalid page: " + page + ". Page must be <= " + MAX_WALKABLE_PAGE + ".");
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
