/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalRow;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterCompiler.CompiledQuery;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;

/**
 * Service backing {@code POST /rest/search/index-query}. Delegates query construction, permission
 * filtering, cursor minting, and the total-hits cap to {@link IqLocalSearchService}.
 */
@Named
@Singleton
public class IndexQueryService
{
  private static final Logger log = LoggerFactory.getLogger(IndexQueryService.class);

  /**
   * Facet spec per entity type. Each {@link Facet} maps a {@link IndexQueryRow#getFields()} key (whose
   * page values seed the bucket list) to the IQ index field the whole-corpus count queries. No category
   * facet: it would be fed by applicationCategoryName, which is only indexed on APPLICATION_CATEGORY docs,
   * so it is always empty on APPLICATION/VIOLATION rows. Kept ordered so the returned facet map is stable.
   */
  private static final Map<IndexQueryType, List<Facet>> FACET_FIELDS = Map.of(
      IndexQueryType.APPLICATION, List.of(
          new Facet("organizationName", "organizationName"),
          new Facet("policyEvaluationStage", "policyEvaluationStage")),
      IndexQueryType.VIOLATION, List.of(
          new Facet("organizationName", "organizationName"),
          new Facet("policyType", "policyViolationThreatCategory")),
      IndexQueryType.POLICY, List.of(
          new Facet("policyType", "policyThreatCategory"),
          new Facet("organizationName", "organizationName")));

  /**
   * Cap on distinct values counted per facet field. Facet values come from the current page's rows, so
   * this is bounded by page size in practice; the cap is a hard ceiling on {@code count()} calls per
   * field to keep per-field fan-out well below page size (see {@link #MAX_FACET_COUNT_QUERIES}).
   */
  static final int MAX_FACET_BUCKETS_PER_FIELD = 20;

  /**
   * Overall ceiling on whole-corpus facet {@code count()} calls issued per request across all facet
   * fields. Each bucket count is one RBAC-scoped index query; bounding total fan-out keeps a single
   * {@code includeFacets} request from firing dozens of counts under load (p95 &lt; 300ms target). Once
   * the budget is exhausted the remaining buckets are omitted and a truncation warning is added.
   * <p>
   * Kept below the sum of the per-field caps ({@value #MAX_FACET_BUCKETS_PER_FIELD} x the two facet
   * fields per entity type) so it is a live guard rather than an unreachable ceiling.
   */
  static final int MAX_FACET_COUNT_QUERIES = 30;

  /** Metric name for rows dropped due to incomplete index data. Tagged with {@code entityType}. */
  static final String DROPPED_METRIC_NAME = "insight_brain_index_query_dropped_rows_total";

  /** Warning emitted when the per-request facet-count budget truncates the returned buckets. */
  static final String FACET_COUNTS_TRUNCATED =
      "some facet counts were omitted to stay within the per-request query budget";

  private final IqLocalSearchService iqLocalSearchService;

  private final SearchIndexClient searchIndexClient;

  private final MeterRegistry meterRegistry;

  @Inject
  public IndexQueryService(
      final IqLocalSearchService iqLocalSearchService,
      final SearchIndexClient searchIndexClient,
      @Nullable final MeterRegistry meterRegistry)
  {
    this.iqLocalSearchService = iqLocalSearchService;
    this.searchIndexClient = searchIndexClient;
    this.meterRegistry = meterRegistry;
  }

  /** Maps a page row-field key (seeds bucket values) to the IQ index field the whole-corpus count queries. */
  private record Facet(String rowField, String indexField)
  {
  }

  public IndexQueryResponse query(final IndexQueryType queryType, final IndexQueryRequest request) {
    final CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(queryType, request.getFilters());
    final String q = compiled.q();
    final Tab tab = tabFor(queryType);
    final String sortKey = validateSort(queryType, tab, request.getSort());
    final int pageSize = pageSize(request.getPageSize());

    final String rawSearchAfter = StringUtils.isBlank(request.getSearchAfter()) ? null : request.getSearchAfter();
    final int page = validatePage(request.getPage(), rawSearchAfter);

    // Lifecycle-only endpoint: isSbomManagerMode is intentionally false (6-arg ctor uses the default).
    final SearchInputs inputs = new SearchInputs(q, tab, queryType.itemTypes(), pageSize, sortKey, rawSearchAfter);
    final IqLocalSearchResponse result = iqLocalSearchService.search(inputs);

    final List<IndexQueryRow> rows = new ArrayList<>(result.rows().size());
    int dropped = 0;
    for (IqLocalRow tagged : result.rows()) {
      final IndexQueryRow row = IndexQueryRowMapper.toRow(queryType, tagged.row());
      if (row != null) {
        rows.add(row);
      }
      else {
        dropped++;
      }
    }

    final List<String> warnings = new ArrayList<>(result.warnings());
    if (dropped > 0) {
      log.warn("Omitted {} {} result(s) missing the identifying field from the index-query response",
          dropped, queryType);
      warnings.add(dropped + " result(s) omitted due to incomplete index data.");
      recordDropped(queryType, dropped);
    }

    final GlobalSearchCursor next = iqLocalSearchService.mintNextCursor(
        tab, sortKey, pageSize, result.nextSearchAfter(), result.servingBackendId());
    final String nextSearchAfter = next == null ? null : next.encode();

    // Facet VALUES come from the returned page, but each bucket COUNT is a whole-corpus, RBAC-scoped
    // count over the same active structured filters + item type (not page-only).
    final Map<String, List<IndexQueryFacetBucket>> facets =
        request.isIncludeFacets() ? computeFacets(queryType, compiled, rows, warnings) : null;

    return new IndexQueryResponse(
        queryType.name(),
        page,
        pageSize,
        result.total(),
        result.exactTotalHits(),
        rows,
        facets,
        false,
        nextSearchAfter,
        warnings);
  }

  private static Tab tabFor(final IndexQueryType queryType) {
    // POLICY has no tab; carry APPLICATION as a neutral sort/cursor tab (POLICY allows relevance sort only).
    // FIXME(CLM-41642): if field sorting is ever enabled, POLICY must not use APPLICATION's sort fields.
    return queryType.tab() != null ? queryType.tab() : Tab.APPLICATION;
  }

  private static String validateSort(final IndexQueryType queryType, final Tab tab, final String requestedSort) {
    if (StringUtils.isBlank(requestedSort)) {
      return GlobalSearchSortAllowlist.RELEVANCE;
    }
    if (queryType == IndexQueryType.POLICY && !GlobalSearchSortAllowlist.RELEVANCE.equals(requestedSort)) {
      throw sortNotAllowed(requestedSort, queryType);
    }
    if (!GlobalSearchSortAllowlist.isAllowed(tab, requestedSort)) {
      throw sortNotAllowed(requestedSort, queryType);
    }
    return requestedSort;
  }

  private void recordDropped(final IndexQueryType queryType, final int dropped) {
    if (meterRegistry != null) {
      meterRegistry.counter(DROPPED_METRIC_NAME, "entityType", queryType.name()).increment(dropped);
    }
  }

  private static int validatePage(final Integer requested, final String searchAfter) {
    if (requested != null && requested < 1) {
      throw new FilterValidationException(FilterValidationException.Code.INVALID_FILTER, "page must be >= 1");
    }
    // Run the searchAfter/page consistency checks against the RAW requested page (null-aware) before
    // defaulting null to 1, so a cursor sent with no page is rejected rather than paginating as page 1.
    final boolean firstPage = requested == null || requested <= 1;
    // A first-page request must not carry a cursor: a stale/mismatched searchAfter from a different
    // search would otherwise be silently accepted and paginate from the wrong position.
    if (firstPage && searchAfter != null) {
      throw new FilterValidationException(FilterValidationException.Code.INVALID_FILTER,
          "page 1 must not carry a searchAfter cursor");
    }
    // Pagination is cursor-based: a page beyond the first is only reachable via a searchAfter cursor,
    // otherwise the client would mislabel page-1 data as a later page.
    if (!firstPage && searchAfter == null) {
      throw new FilterValidationException(FilterValidationException.Code.DEEP_PAGINATION_NOT_SUPPORTED,
          "page > 1 requires a searchAfter cursor");
    }
    return requested == null ? 1 : requested;
  }

  private static int pageSize(final Integer requested) {
    if (requested == null) {
      return IqLocalSearchService.DEFAULT_PER_TYPE_PAGE_SIZE;
    }
    if (requested < 1 || requested > IqLocalSearchService.MAX_PAGE_SIZE) {
      throw new FilterValidationException(FilterValidationException.Code.INVALID_FILTER,
          "pageSize must be in [1, " + IqLocalSearchService.MAX_PAGE_SIZE + "]");
    }
    return requested;
  }

  private static FilterValidationException sortNotAllowed(final String requestedSort, final IndexQueryType queryType) {
    return new FilterValidationException(FilterValidationException.Code.SORT_NOT_ALLOWED,
        "sort '" + requestedSort + "' is not allowed for entityType " + queryType);
  }

  /**
   * Whole-corpus facets. Values are discovered from the current page's rows (capped at
   * {@link #MAX_FACET_BUCKETS_PER_FIELD} per field); each bucket count is a fresh RBAC-scoped,
   * fail-closed {@link SearchIndexClient#count(String)} over the same active structured filters + item
   * type AND {@code indexField=value}, so it reflects the full filtered result set rather than the page.
   * <p>
   * The count base intentionally omits the free-text {@code query} refinement; facet counts therefore
   * reflect the structured filters + item type. Full free-text-consistent aggregate facets are a follow-up.
   */
  private Map<String, List<IndexQueryFacetBucket>> computeFacets(
      final IndexQueryType queryType,
      final CompiledQuery compiled,
      final List<IndexQueryRow> rows,
      final List<String> warnings)
  {
    final List<Facet> facetFields = FACET_FIELDS.getOrDefault(queryType, List.of());
    if (facetFields.isEmpty()) {
      return new LinkedHashMap<>();
    }
    final String baseQuery = baseMetricQuery(queryType, compiled);
    final Map<String, List<IndexQueryFacetBucket>> out = new LinkedHashMap<>();
    // Bound total count() fan-out per request: each bucket is one RBAC-scoped index query, and the
    // p95 < 300ms target cannot absorb dozens of them under concurrency. Fields and values are
    // processed in their existing stable order so truncation is deterministic.
    int budget = MAX_FACET_COUNT_QUERIES;
    boolean truncated = false;
    for (Facet facet : facetFields) {
      final Set<String> values = new LinkedHashSet<>();
      for (IndexQueryRow row : rows) {
        final Object value = row.getFields().get(facet.rowField());
        if (value != null && values.size() < MAX_FACET_BUCKETS_PER_FIELD) {
          values.add(String.valueOf(value));
        }
      }
      final List<IndexQueryFacetBucket> buckets = new ArrayList<>(values.size());
      for (String value : values) {
        if (budget <= 0) {
          truncated = true;
          break;
        }
        final String query = baseQuery + " AND " + facet.indexField() + ":" + quote(value);
        buckets.add(new IndexQueryFacetBucket(value, searchIndexClient.count(query)));
        budget--;
      }
      out.put(facet.rowField(), buckets);
    }
    if (truncated) {
      warnings.add(FACET_COUNTS_TRUNCATED);
    }
    return out;
  }

  /**
   * RBAC-scoped facet-count base: {@code itemType:<type(s)> AND <structured filter clauses>}. The RBAC
   * filter is applied inside {@link SearchIndexClient#count(String)} (fail-closed), so a caller with no
   * readable contexts counts 0 rather than an unscoped total.
   */
  private static String baseMetricQuery(final IndexQueryType queryType, final CompiledQuery compiled) {
    final StringBuilder q = new StringBuilder();
    final Set<ItemType> types = queryType.itemTypes();
    final List<String> typeClauses = new ArrayList<>(types.size());
    for (ItemType type : types) {
      typeClauses.add("itemType:" + type.searchFieldName());
    }
    q.append(typeClauses.size() == 1 ? typeClauses.get(0) : "(" + String.join(" OR ", typeClauses) + ")");
    for (String clause : compiled.fieldClauses()) {
      q.append(" AND ").append(clause);
    }
    return q.toString();
  }

  private static String quote(final String value) {
    // Keyword fields match a single token; escape embedded quotes/backslashes so a value cannot break out
    // of the phrase. Not injection defense (server-built), just query shaping.
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
