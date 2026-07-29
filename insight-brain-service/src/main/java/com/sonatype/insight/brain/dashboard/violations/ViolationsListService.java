/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
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
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

/**
 * Index-backed Martha V1 Violations list.
 * <p>
 * Rows are discovered from {@code POLICY_VIOLATION} search-index hits. Card identity fields come from
 * the index; {@code firstOccurredTime} is enriched per page via {@link PolicyViolationDAO#getByIds}
 * ({@code PolicyViolation.openTime}) — same hybrid pattern as the Applications list risk enrich.
 * <p>
 * Request defaults: {@code page=0}, {@code pageSize=50}, {@code includeFacets=true} when omitted.
 * <p>
 * <b>Sort caveat:</b> {@code orderBy=-policyThreatLevel} (the default) orders rows <em>within</em> each
 * index page after retrieval — neither {@link SearchIndexClient#searchIndex} nor the session walk
 * expose threat-level sort until Track B docValues, so the ordering cannot be pushed into the index
 * for this V1. Consequently the highest-threat rows are ordered correctly on each returned page, but
 * global "highest threat first" ordering is <em>not</em> guaranteed across page boundaries.
 * Index-level sort is tracked under CLM-42262.
 * <p>
 * When {@code nexusOne.search.readPath.violations=new}, list + facets share one
 * {@link IndexReadSession} opened via {@link IndexReadSessionFactory} (PR-0 / CLM-42705). Default
 * remains the legacy {@link SearchIndexClient} path.
 */
@Named
@Singleton
public class ViolationsListService
{
  public static final int DEFAULT_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  public static final int MAX_SEARCH_LENGTH = 200;

  /**
   * Highest zero-based page the session {@code searchAfter} walk will materialize. Tunable via
   * {@code nexusOne.violations.maxWalkablePage} (same convention as the facet discovery caps).
   */
  public static final int MAX_WALKABLE_PAGE =
      Integer.getInteger("nexusOne.violations.maxWalkablePage", 200);

  private final SearchIndexClient searchIndexClient;

  private final ViolationsListIndexQueryBuilder indexQueryBuilder;

  private final ViolationsListRequestValidator requestValidator;

  private final ViolationsListFacetsBuilder facetsBuilder;

  private final IndexReadSessionFactory sessionFactory;

  private final ConversionHelper conversionHelper;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public ViolationsListService(
      final SearchIndexClient searchIndexClient,
      final ViolationsListIndexQueryBuilder indexQueryBuilder,
      final ViolationsListRequestValidator requestValidator,
      final ViolationsListFacetsBuilder facetsBuilder,
      final IndexReadSessionFactory sessionFactory,
      final ConversionHelper conversionHelper,
      final PolicyViolationDAO policyViolationDAO)
  {
    this.searchIndexClient = searchIndexClient;
    this.indexQueryBuilder = indexQueryBuilder;
    this.requestValidator = requestValidator;
    this.facetsBuilder = facetsBuilder;
    this.sessionFactory = sessionFactory;
    this.conversionHelper = conversionHelper;
    this.policyViolationDAO = policyViolationDAO;
  }

  public ViolationsListResponseDTO listViolations(final ViolationsListRequestDTO request) {
    int page = request == null || request.page == null ? 0 : request.page;
    int pageSize = request == null || request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    String search = request == null ? null : request.search;
    boolean includeFacets = request == null || request.includeFacets == null || request.includeFacets;

    validatePagination(page, pageSize);
    validateSearch(search);
    validateSearch(request == null ? null : request.organizationFacetSearch, "Organization facet search");
    validateSearch(request == null ? null : request.applicationFacetSearch, "Application facet search");
    requestValidator.validate(request);

    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? ViolationsListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;

    String query = indexQueryBuilder.buildViolationQuery(request);
    if (SearchReadPathFlags.forSurface(SearchReadPathSurface.VIOLATIONS) == SearchReadPath.NEW) {
      return listViolationsWithSession(request, page, pageSize, includeFacets, orderBy, query);
    }

    SearchResultDTO searchResult =
        searchIndexClient.searchIndex(query, pageSize, toSearchIndexPage(page), false, false, List.of());
    LinkedHashMap<String, SearchResultItemDTO> pageItems =
        ViolationsListIndexItems.extractViolationItems(searchResult);

    List<ViolationRowDTO> rows = new ArrayList<>(pageItems.size());
    for (SearchResultItemDTO item : pageItems.values()) {
      rows.add(toRow(item));
    }
    enrichFirstOccurredTimes(rows);
    rows.sort(comparator(orderBy));

    ViolationsListResponseDTO response = new ViolationsListResponseDTO();
    response.violations = rows;
    // total is the raw index hit count. Violation docs are 1:1 with policyViolationId, so it matches
    // the deduplicated row count; extractViolationItems only dedups defensively against a
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
      response.facets = facetsBuilder.buildFacets(
          query,
          waiverFacetQuery,
          searchResult.totalNumberOfHits,
          request == null ? null : request.organizationFacetSearch,
          request == null ? null : request.applicationFacetSearch);
    }
    return response;
  }

  private ViolationsListResponseDTO listViolationsWithSession(
      final ViolationsListRequestDTO request,
      final int page,
      final int pageSize,
      final boolean includeFacets,
      final String orderBy,
      final String query)
  {
    Query sessionQuery = conversionHelper.stringToQuery(query);
    try (IndexReadSession session = sessionFactory.open()) {
      long total = session.count(sessionQuery);
      // Past-total over-cap pages return empty (soft). Only reject when the requested page still has hits.
      boolean targetPageHasHits = (long) page * pageSize < total;
      if (targetPageHasHits && page > MAX_WALKABLE_PAGE) {
        throw new BadRequestException(
            "Invalid page: " + page + ". Page must be <= " + MAX_WALKABLE_PAGE + ".");
      }

      LinkedHashMap<String, SearchResultItemDTO> pageItems = new LinkedHashMap<>();
      if (targetPageHasHits) {
        IndexPageResult result = null;
        List<Object> searchAfter = List.of();
        Sort sort = stableSessionSort();
        for (int currentPage = 0; currentPage <= page; currentPage++) {
          result = session.searchPage(new IndexPageRequest(sessionQuery, sort, pageSize, searchAfter));
          searchAfter = result.nextSearchAfter();
        }
        pageItems = ViolationsListIndexItems.extractViolationItems(result == null ? List.of() : result.docs());
      }

      List<ViolationRowDTO> rows = new ArrayList<>(pageItems.size());
      for (SearchResultItemDTO item : pageItems.values()) {
        rows.add(toRow(item));
      }
      enrichFirstOccurredTimes(rows);
      rows.sort(comparator(orderBy));

      ViolationsListResponseDTO response = new ViolationsListResponseDTO();
      response.violations = rows;
      response.total = total;
      response.page = page;
      response.pageSize = pageSize;
      long consumed = (long) page * pageSize + pageItems.size();
      // Do not advertise a next page that the walkable-page guard would reject with 400.
      response.hasNextPage = consumed < total && page < MAX_WALKABLE_PAGE;
      response.source = ViolationsListResponseDTO.SOURCE_INDEX;
      if (includeFacets) {
        // Same failure mode as the legacy path: facet errors propagate (no silent page-without-facets).
        String waiverFacetQuery = indexQueryBuilder.buildViolationQueryExcludingWaiverType(request);
        response.facets = facetsBuilder.buildFacets(
            session,
            query,
            waiverFacetQuery,
            total,
            request == null ? null : request.organizationFacetSearch,
            request == null ? null : request.applicationFacetSearch);
      }
      return response;
    }
  }

  /**
   * Stable total order for session {@code searchAfter} walks.
   * <p>
   * Lucene today only indexes {@link FieldIdentifier#DOCUMENT_KEY} as SortedDocValuesField for a
   * unique stable order without schema change. Threat-level sort awaits Track B docValues (CLM-42262).
   */
  static Sort stableSessionSort() {
    return new Sort(new SortField(FieldIdentifier.DOCUMENT_KEY.label, SortField.Type.STRING));
  }

  /**
   * {@link SearchIndexClient#searchIndex} uses {@code page=0} as a first-page sentinel and 1-based
   * pages thereafter — same contract as the Applications list and Advanced Search. Index pages 0 and
   * 1 both return the first result window, so client page {@code 1} maps to index page {@code 2}.
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

  /**
   * Page-scoped SQL enrich: attach {@code PolicyViolation.openTime} as epoch millis. Missing ids or
   * null open times leave {@link ViolationRowDTO#firstOccurredTime} unset (FE omits the line).
   */
  void enrichFirstOccurredTimes(final List<ViolationRowDTO> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    Set<String> ids = new LinkedHashSet<>();
    for (ViolationRowDTO row : rows) {
      if (row != null && StringUtils.isNotBlank(row.policyViolationId)) {
        ids.add(row.policyViolationId);
      }
    }
    if (ids.isEmpty()) {
      return;
    }

    Map<String, Long> openTimesById = new HashMap<>();
    for (PolicyViolation violation : policyViolationDAO.getByIds(ids)) {
      if (violation == null || StringUtils.isBlank(violation.getId()) || violation.getOpenTime() == null) {
        continue;
      }
      openTimesById.put(violation.getId(), violation.getOpenTime().getTime());
    }

    for (ViolationRowDTO row : rows) {
      if (row == null || StringUtils.isBlank(row.policyViolationId)) {
        continue;
      }
      Long firstOccurredTime = openTimesById.get(row.policyViolationId);
      if (firstOccurredTime != null) {
        row.firstOccurredTime = firstOccurredTime;
      }
    }
  }

  private static String extractComponentVersion(final ApiComponentIdentifierDTOV2 componentIdentifier) {
    // The source is a deserialized index DTO, so guard the coordinates map too — do not assume the
    // TreeMap default survives deserialization.
    if (componentIdentifier == null || componentIdentifier.getCoordinates() == null) {
      return null;
    }
    return componentIdentifier.getCoordinates().get("version");
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
    validateSearch(search, "Search query");
  }

  private static void validateSearch(final String search, final String fieldLabel) {
    if (search != null && search.length() > MAX_SEARCH_LENGTH) {
      throw new BadRequestException(
          fieldLabel + " exceeds maximum length of " + MAX_SEARCH_LENGTH + " characters.");
    }
  }
}
