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
   * page values seed the bucket list) to the IQ index field the whole-corpus count queries.
   * applicationCategoryName is now denormalized onto APPLICATION and violation docs, so the categories
   * facet is populated. Kept ordered so the returned facet map is stable.
   * <p>
   * The org/app facets bucket by the display NAME (value == displayName == the indexed name field),
   * so the emitted bucket value round-trips directly back through the matching organizations/applications
   * filter (which matches organizationName/applicationName). Bucketing by id instead would emit a value
   * the name-matching filter can never resolve.
   * <p>
   * The fixed-vocabulary facets (states, waiverType) are ordered FIRST: they are a tiny, always-relevant
   * set (four cheap counts total) and must never be starved by the dynamic per-page facets under the
   * shared per-request count budget (see {@link #MAX_FACET_COUNT_QUERIES}).
   * <p>
   * APPLICATION has no stages facet/filter: an application doc is not stage-scoped (policyEvaluationStage
   * is written only on violation/vuln docs), so a stages facet on APPLICATION would always be empty.
   */
  private static final Map<IndexQueryType, List<Facet>> FACET_FIELDS = Map.of(
      IndexQueryType.APPLICATION, List.of(
          Facet.value("organizations", "organizationName", "organizationName"),
          Facet.value("applications", "applicationName", "applicationName"),
          Facet.value("applicationCategories", "applicationCategories", "applicationCategoryName")),
      IndexQueryType.VIOLATION, List.of(
          Facet.states("states"),
          Facet.waiverTypes("waiverType"),
          Facet.value("organizations", "organizationName", "organizationName"),
          Facet.value("applications", "applicationName", "applicationName"),
          Facet.value("applicationCategories", "applicationCategories", "applicationCategoryName"),
          Facet.value("stages", "stage", "policyEvaluationStage"),
          Facet.value("policyTypes", "policyType", "policyViolationThreatCategory")),
      IndexQueryType.POLICY, List.of(
          Facet.value("policyTypes", "policyType", "policyThreatCategory"),
          Facet.value("organizations", "organizationName", "organizationName")),
      // organizationName is the org owner display name surfaced on org-scoped waiver rows; the count
      // resolves through the organizationName index field (rewritten to parentOrganizationName). auto is
      // the auto-vs-manual discriminator counted over the whole corpus (see FacetMode.AUTO_WAIVER_TOGGLE).
      // threatLevel is a numeric IntPoint, so it counts per discrete value via an exact-value range
      // [v TO v] rather than a phrase-quoted term (which does not match a point field).
      IndexQueryType.WAIVER, List.of(
          Facet.value("organizationName", "organizationName", "organizationName"),
          Facet.autoWaiverToggle("auto", "policyWaiverAuto"),
          Facet.numeric("threatLevel", "threatLevel", "policyWaiverThreatLevel")));

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
   * the budget is exhausted the remaining buckets are omitted and a truncation warning is added — so
   * truncation is never silent.
   * <p>
   * Sized to admit a realistic worst-case page across all entity types without truncating while still
   * guarding against a pathological one, and kept below the sum of the per-field caps
   * ({@value #MAX_FACET_BUCKETS_PER_FIELD} x the facet count per entity type) so it is a live guard
   * rather than an unreachable ceiling. The densest WAIVER page — organizationName (up to
   * {@value #MAX_FACET_BUCKETS_PER_FIELD}), the auto/manual toggle (2), and threatLevel (numeric, at
   * most the ~11 discrete IQ threat levels), ~33 counts — fits comfortably under this budget too.
   */
  static final int MAX_FACET_COUNT_QUERIES = 60;

  /** Metric name for rows dropped due to incomplete index data. Tagged with {@code entityType}. */
  static final String DROPPED_METRIC_NAME = "insight_brain_index_query_dropped_rows_total";

  /**
   * Hard ceiling on the per-page {@code waiverCount} aggregation: one RBAC-scoped {@code count()} of
   * POLICY_WAIVER docs per POLICY row. Bounded by page size in practice; this cap keeps a pathological
   * page from firing more counts than a facet request would (see {@link #MAX_FACET_COUNT_QUERIES}).
   */
  static final int MAX_POLICY_WAIVER_COUNT_QUERIES = 60;

  /** Row field carrying the number of manual waivers referencing a POLICY row's policy id. */
  static final String WAIVER_COUNT_FIELD = "waiverCount";

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

  /** How a facet's buckets are derived + counted. */
  private enum FacetMode
  {
    /** Buckets seeded from a single page row-field's distinct values; count {@code indexField:value}. */
    VALUE,
    /**
     * Numeric-point buckets seeded from a page row-field's distinct values; each value counts via an
     * exact-value range {@code indexField:[v TO v]} because a phrase-quoted term does not match a point.
     */
    NUMERIC,
    /** Fixed OPEN/WAIVED buckets counted from the waiver-status field (OPEN = not waived). */
    STATES,
    /** Fixed AUTO/MANUAL buckets counted from the waiver-status field. */
    WAIVER_TYPES,
    /**
     * Whole-corpus auto-vs-manual toggle: fixed true/false buckets counted against a base that drops the
     * manual-only {@code policyWaiverAuto:"false"} restriction, so both buckets reflect the full corpus
     * regardless of the current include-toggle view.
     */
    AUTO_WAIVER_TOGGLE
  }

  /**
   * One facet.
   *
   * @param key stable facet name; aligned to the corresponding filter key so the rail round-trips.
   * @param valueRowField page row-field whose distinct values seed VALUE/NUMERIC buckets
   *          (null for fixed-vocabulary facets).
   * @param indexField IQ index field the whole-corpus count queries (null for fixed-vocabulary facets).
   */
  private record Facet(FacetMode mode, String key, String valueRowField, String indexField)
  {
    static Facet value(final String key, final String valueRowField, final String indexField) {
      return new Facet(FacetMode.VALUE, key, valueRowField, indexField);
    }

    static Facet numeric(final String key, final String valueRowField, final String indexField) {
      return new Facet(FacetMode.NUMERIC, key, valueRowField, indexField);
    }

    static Facet states(final String key) {
      return new Facet(FacetMode.STATES, key, null, null);
    }

    static Facet waiverTypes(final String key) {
      return new Facet(FacetMode.WAIVER_TYPES, key, null, null);
    }

    static Facet autoWaiverToggle(final String key, final String indexField) {
      return new Facet(FacetMode.AUTO_WAIVER_TOGGLE, key, null, indexField);
    }
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

    // POLICY rows carry a per-policy waiverCount: for each policy on the page, an RBAC-scoped count of
    // the POLICY_WAIVER docs whose policyWaiverPolicyId equals that policy's id (bounded per page).
    if (queryType == IndexQueryType.POLICY) {
      enrichPolicyWaiverCounts(rows);
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

  /**
   * Adds a {@code waiverCount} to each POLICY row: the number of POLICY_WAIVER docs whose
   * {@code policyWaiverPolicyId} equals the policy's stable id, counted whole-corpus and RBAC-scoped
   * (fail-closed) via {@link SearchIndexClient#count(String)}. One count per row, so fan-out is bounded
   * by page size and further capped at {@link #MAX_POLICY_WAIVER_COUNT_QUERIES}. Rows past the cap keep
   * no waiverCount rather than a wrong 0. The link key is the stable policy id present on both the
   * POLICY doc ({@code policyId}) and the manual-waiver doc ({@code policyWaiverPolicyId}); auto-waivers
   * carry no policy id and so never contribute, matching the manual-waiver-only product semantics.
   */
  private void enrichPolicyWaiverCounts(final List<IndexQueryRow> rows) {
    final String waiverType = "itemType:" + ItemType.POLICY_WAIVER.searchFieldName();
    int budget = MAX_POLICY_WAIVER_COUNT_QUERIES;
    for (int i = 0; i < rows.size(); i++) {
      if (budget <= 0) {
        break;
      }
      final IndexQueryRow row = rows.get(i);
      final String policyId = row.getId();
      if (StringUtils.isBlank(policyId)) {
        continue;
      }
      final String query = waiverType + " AND policyWaiverPolicyId:" + quote(policyId);
      final long count = searchIndexClient.count(query);
      budget--;
      rows.set(i, row.toBuilder().field(WAIVER_COUNT_FIELD, count).build());
    }
  }

  private static Tab tabFor(final IndexQueryType queryType) {
    // POLICY has no tab; carry APPLICATION as a neutral cursor tab. validateSort forces POLICY to
    // relevance regardless, so APPLICATION's sort fields are never applied to a POLICY query.
    return queryType.tab() != null ? queryType.tab() : Tab.APPLICATION;
  }

  /**
   * Resolve the effective sort key. A blank request sort resolves to the per-entity default
   * ({@link GlobalSearchSortAllowlist#defaultSortFor}): Applications = latest evaluation, Violations
   * = threat, Waivers = created, everything else = relevance. POLICY has no tab and is relevance-only
   * (a blank sort yields relevance; a non-relevance request is rejected). A supplied non-relevance
   * key must be allowlisted for the tab.
   */
  private static String validateSort(final IndexQueryType queryType, final Tab tab, final String requestedSort) {
    if (queryType == IndexQueryType.POLICY) {
      if (StringUtils.isBlank(requestedSort) || GlobalSearchSortAllowlist.RELEVANCE.equals(requestedSort)) {
        return GlobalSearchSortAllowlist.RELEVANCE;
      }
      throw sortNotAllowed(requestedSort, queryType);
    }
    if (StringUtils.isBlank(requestedSort)) {
      return GlobalSearchSortAllowlist.defaultSortFor(tab);
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
    final String baseQuery = baseMetricQuery(queryType, compiled, List.of());
    // Fixed states/waiverType facets count against a base that omits the user's OWN waiver-status
    // filter, so a user who has selected states=OPEN still sees the true WAIVED count (and vice versa)
    // rather than a self-restricted 0 they cannot reason about.
    final String fixedFacetBaseQuery = baseMetricQuery(queryType, compiled, compiled.waiverStatusClauses());
    final Map<String, List<IndexQueryFacetBucket>> out = new LinkedHashMap<>();
    // Bound total count() fan-out per request: each bucket is one RBAC-scoped index query, and the
    // p95 < 300ms target cannot absorb dozens of them under concurrency. Facets and values are
    // processed in their FACET_FIELDS order (fixed facets first) so truncation is deterministic and
    // the always-relevant fixed facets are never starved by the dynamic per-page facets.
    final int[] budget = {MAX_FACET_COUNT_QUERIES};
    final boolean[] truncated = {false};
    for (Facet facet : facetFields) {
      final List<IndexQueryFacetBucket> buckets = switch (facet.mode()) {
        case VALUE -> valueBuckets(facet, baseQuery, rows, budget, truncated);
        case NUMERIC -> numericBuckets(facet, baseQuery, rows, budget, truncated);
        case STATES -> fixedBuckets(fixedFacetBaseQuery, budget, truncated, stateClauses());
        case WAIVER_TYPES -> fixedBuckets(fixedFacetBaseQuery, budget, truncated, waiverTypeClauses());
        // The auto/manual toggle reports true/false counts over the whole corpus regardless of a
        // manual-only view (from the default OR an explicit includeAutoWaivers:false), so it counts
        // against a base that drops the policyWaiverAuto:"false" restriction. Otherwise the base would
        // carry it and the "true" bucket would always count 0.
        case AUTO_WAIVER_TOGGLE -> autoWaiverToggleBuckets(
            facet, baseMetricQueryWithoutAutoRestriction(queryType, compiled), budget, truncated);
      };
      out.put(facet.key(), buckets);
    }
    if (truncated[0]) {
      warnings.add(FACET_COUNTS_TRUNCATED);
    }
    return out;
  }

  /** Distinct string values of a (possibly multi-valued) page row-field, capped per field. */
  private static Set<String> distinctRowValues(final String rowField, final List<IndexQueryRow> rows) {
    final Set<String> values = new LinkedHashSet<>();
    for (IndexQueryRow row : rows) {
      final Object value = row.getFields().get(rowField);
      if (value == null) {
        continue;
      }
      if (value instanceof Iterable<?> many) {
        for (Object element : many) {
          if (element != null && values.size() < MAX_FACET_BUCKETS_PER_FIELD) {
            values.add(String.valueOf(element));
          }
        }
      }
      else if (values.size() < MAX_FACET_BUCKETS_PER_FIELD) {
        values.add(String.valueOf(value));
      }
    }
    return values;
  }

  private List<IndexQueryFacetBucket> valueBuckets(
      final Facet facet,
      final String baseQuery,
      final List<IndexQueryRow> rows,
      final int[] budget,
      final boolean[] truncated)
  {
    final Set<String> values = distinctRowValues(facet.valueRowField(), rows);
    final List<IndexQueryFacetBucket> buckets = new ArrayList<>(values.size());
    for (String value : values) {
      if (budget[0] <= 0) {
        truncated[0] = true;
        break;
      }
      final String query = baseQuery + " AND " + facet.indexField() + ":" + quote(value);
      buckets.add(new IndexQueryFacetBucket(value, searchIndexClient.count(query)));
      budget[0]--;
    }
    return buckets;
  }

  /**
   * Numeric-point buckets: values seeded from the page row-field, each counted via an exact-value range
   * {@code indexField:[v TO v]} (a phrase-quoted term does not match a point field). A value that is not
   * an integer is skipped rather than counted against a malformed query.
   */
  private List<IndexQueryFacetBucket> numericBuckets(
      final Facet facet,
      final String baseQuery,
      final List<IndexQueryRow> rows,
      final int[] budget,
      final boolean[] truncated)
  {
    final Set<String> values = distinctRowValues(facet.valueRowField(), rows);
    final List<IndexQueryFacetBucket> buckets = new ArrayList<>(values.size());
    for (String value : values) {
      if (budget[0] <= 0) {
        truncated[0] = true;
        break;
      }
      final int parsed;
      try {
        parsed = Integer.parseInt(value.trim());
      }
      catch (NumberFormatException e) {
        continue;
      }
      final String query = baseQuery + " AND " + facet.indexField() + ":[" + parsed + " TO " + parsed + "]";
      buckets.add(new IndexQueryFacetBucket(value, searchIndexClient.count(query)));
      budget[0]--;
    }
    return buckets;
  }

  /**
   * Whole-corpus auto-vs-manual toggle: fixed true/false buckets counted against a base that already
   * drops the manual-only restriction, so both buckets reflect what the user would see if they flipped
   * the include toggle.
   */
  private List<IndexQueryFacetBucket> autoWaiverToggleBuckets(
      final Facet facet,
      final String baseQuery,
      final int[] budget,
      final boolean[] truncated)
  {
    final List<IndexQueryFacetBucket> buckets = new ArrayList<>(2);
    for (String value : List.of("true", "false")) {
      if (budget[0] <= 0) {
        truncated[0] = true;
        break;
      }
      final String query = baseQuery + " AND " + facet.indexField() + ":" + quote(value);
      buckets.add(new IndexQueryFacetBucket(value, searchIndexClient.count(query)));
      budget[0]--;
    }
    return buckets;
  }

  /** Fixed-vocabulary buckets (states, waiver types): one count per predefined key + Lucene clause. */
  private List<IndexQueryFacetBucket> fixedBuckets(
      final String baseQuery,
      final int[] budget,
      final boolean[] truncated,
      final Map<String, String> keyToClause)
  {
    final List<IndexQueryFacetBucket> buckets = new ArrayList<>(keyToClause.size());
    for (Map.Entry<String, String> entry : keyToClause.entrySet()) {
      if (budget[0] <= 0) {
        truncated[0] = true;
        break;
      }
      buckets.add(new IndexQueryFacetBucket(
          entry.getKey(), searchIndexClient.count(baseQuery + " AND " + entry.getValue())));
      budget[0]--;
    }
    return buckets;
  }

  private static Map<String, String> stateClauses() {
    final String waived = IndexQueryWaiverStatus.waivedClause("policyViolationWaiverStatus");
    final Map<String, String> m = new LinkedHashMap<>();
    m.put(IndexQueryWaiverStatus.STATE_OPEN, "NOT " + waived);
    m.put(IndexQueryWaiverStatus.STATE_WAIVED, waived);
    return m;
  }

  private static Map<String, String> waiverTypeClauses() {
    final Map<String, String> m = new LinkedHashMap<>();
    m.put(IndexQueryWaiverStatus.WAIVER_TYPE_AUTO,
        "policyViolationWaiverStatus:" + quote(IndexQueryWaiverStatus.AUTO_WAIVED));
    m.put(IndexQueryWaiverStatus.WAIVER_TYPE_MANUAL,
        "policyViolationWaiverStatus:" + quote(IndexQueryWaiverStatus.WAIVED));
    return m;
  }

  /**
   * RBAC-scoped facet-count base: {@code itemType:<type(s)> AND <structured filter clauses>}, with
   * {@code excludedClauses} left out so a facet can count against a base that omits its own dimension
   * (e.g. the fixed states/waiverType facets drop the user's waiver-status filter so each fixed count is
   * whole-corpus rather than self-restricting). The RBAC filter is applied inside
   * {@link SearchIndexClient#count(String)} (fail-closed), so a caller with no readable contexts counts
   * 0 rather than an unscoped total.
   */
  /**
   * Facet-count base for the auto/manual facet, dropping the manual-only {@code policyWaiverAuto:"false"}
   * restriction (whether it came from the absent/null default OR an explicit {@code includeAutoWaivers:false})
   * so both true and false buckets count over the whole corpus -- the toggle facet tells the user what they
   * would see if they flipped the include toggle. An explicit {@code true} adds no restriction, so this is a
   * no-op difference in that case.
   */
  private static String baseMetricQueryWithoutAutoRestriction(
      final IndexQueryType queryType,
      final CompiledQuery compiled)
  {
    final String autoRestriction = compiled.autoWaiverRestrictionClause();
    return baseMetricQuery(
        queryType, compiled, autoRestriction == null ? List.of() : List.of(autoRestriction));
  }

  private static String baseMetricQuery(
      final IndexQueryType queryType,
      final CompiledQuery compiled,
      final List<String> excludedClauses)
  {
    final StringBuilder q = new StringBuilder();
    final Set<ItemType> types = queryType.itemTypes();
    final List<String> typeClauses = new ArrayList<>(types.size());
    for (ItemType type : types) {
      typeClauses.add("itemType:" + type.searchFieldName());
    }
    q.append(typeClauses.size() == 1 ? typeClauses.get(0) : "(" + String.join(" OR ", typeClauses) + ")");
    for (String clause : compiled.fieldClauses()) {
      if (!excludedClauses.contains(clause)) {
        q.append(" AND ").append(clause);
      }
    }
    return q.toString();
  }

  private static String quote(final String value) {
    // Keyword fields match a single token; escape embedded quotes/backslashes so a value cannot break out
    // of the phrase. Not injection defense (server-built), just query shaping.
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
