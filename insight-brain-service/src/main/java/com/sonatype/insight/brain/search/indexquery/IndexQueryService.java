/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.search.RowFacetValues;
import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalRow;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.export.CsvExportLimits;
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
   * Facets are ordered cheapest-and-most-bounded FIRST, high-cardinality name facets LAST, within every
   * entity type. Buckets are counted in this order against one shared per-request budget (see
   * {@link #MAX_FACET_COUNT_QUERIES}), so this ordering is what guarantees the small always-relevant rail
   * sections — the fixed-vocabulary facets (states, waiverType, status, auto) and the bounded
   * stage/policyType/violationState/scope/threatLevel sets — are never starved by the per-page
   * organization/application/policy name facets, which can each saturate the per-field bucket cap on a
   * diverse page. An empty bounded facet therefore means "no matching values", never "ran out of budget".
   * <p>
   * The APPLICATION stages/policyTypes/violationStates facets bucket by the denormalized
   * applicationViolationStage/PolicyType/State keyword sets written on each application doc (the raw
   * indexed tokens: stage id, lowercased threat category, lowercased state), so a bucket value
   * round-trips through the matching filter on the same field.
   */
  private static final Map<IndexQueryType, List<Facet>> FACET_FIELDS = Map.of(
      // Bounded stages/policyTypes/violationStates first, high-cardinality org/app/category names last:
      // buckets are counted in this order against the one shared budget, so a page diverse enough to
      // saturate the name facets would otherwise return the bounded rail sections empty.
      IndexQueryType.APPLICATION, List.of(
          Facet.value("stages", "stages", "applicationViolationStage"),
          Facet.value("policyTypes", "policyTypes", "applicationViolationPolicyType"),
          Facet.value("violationStates", "violationStates", "applicationViolationState"),
          Facet.value("organizations", "organizationName", "organizationName"),
          Facet.value("applications", "applicationName", "applicationName"),
          Facet.value("applicationCategories", "applicationCategories", "applicationCategoryName")),
      IndexQueryType.VIOLATION, List.of(
          Facet.states("states"),
          Facet.waiverTypes("waiverType"),
          Facet.value("stages", "stage", "policyEvaluationStage"),
          Facet.value("policyTypes", "policyType", "policyViolationThreatCategory"),
          Facet.value("organizations", "organizationName", "organizationName"),
          Facet.value("applications", "applicationName", "applicationName"),
          Facet.value("applicationCategories", "applicationCategories", "applicationCategoryName")),
      IndexQueryType.POLICY, List.of(
          Facet.value("policyTypes", "policyType", "policyThreatCategory"),
          Facet.value("organizations", "organizationName", "organizationName")),
      // organizationName is the immediate org display name on waiver rows (app- and org-scoped); the
      // count resolves through the organizationName index field (rewritten to parentOrganizationName).
      // auto is the auto-vs-manual discriminator counted over the whole corpus
      // (see FacetMode.AUTO_WAIVER_TOGGLE). threatLevel is a numeric IntPoint, so it counts per
      // discrete value via an exact-value range [v TO v] rather than a phrase-quoted term (which does
      // not match a point field).
      // scope buckets by the indexed policyWaiverScope field: "application", "organization", or
      // "component" (when the waiver/request targets a specific component rather than all components in
      // the owner scope), so a page containing a component-targeted waiver surfaces a component bucket.
      // status is the fixed active/expiring/expired/auto-waived vocabulary derived from the expires-at
      // epoch point vs server-now and the auto discriminator (see FacetMode.WAIVER_STATUS).
      // Fixed / bounded facets first so the shared count budget never starves always-relevant rail
      // sections (status, auto, threatLevel, scope, policyType) when org/app/policyName hit the
      // per-field bucket cap on a dense page. Variable-name facets are last and may truncate.
      IndexQueryType.WAIVER, List.of(
          Facet.waiverStatus("status"),
          Facet.autoWaiverToggle("auto", "policyWaiverAuto"),
          Facet.numeric("threatLevel", "threatLevel", "policyWaiverThreatLevel"),
          Facet.value("scope", "scope", "policyWaiverScope"),
          // policyType buckets by the denormalized policyWaiverPolicyType keyword (SECURITY/LICENSE/
          // QUALITY/OTHER), seeded from each row's policyType field. Present on both waiver and request docs.
          Facet.value("policyType", "policyType", "policyWaiverPolicyType"),
          // policy buckets by the indexed policyWaiverPolicyName, seeded from each row's policyName
          // field, so a bucket round-trips through the policy filter (which matches policyWaiverPolicyName).
          Facet.value("policy", "policyName", "policyWaiverPolicyName"),
          Facet.value("organizationName", "organizationName", "organizationName"),
          // applications buckets by the indexed applicationName, written on app-scoped waivers via
          // setOwner(application); org-scoped waivers carry no applicationName and simply do not seed
          // this facet. The key matches the applications filter so a bucket round-trips as a filter.
          Facet.value("applications", "applicationName", "applicationName")));

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
   * Sized to admit a realistic worst-case page across all entity types while still guarding against a
   * pathological one, and kept below the sum of the per-field caps
   * ({@value #MAX_FACET_BUCKETS_PER_FIELD} x the facet count per entity type) so it is a live guard
   * rather than an unreachable ceiling. The densest WAIVER page — fixed status (4) + auto/manual (2) +
   * threatLevel (~11) + scope (≤3) + policyType (≤4) + organizationName / applications / policy
   * (up to {@value #MAX_FACET_BUCKETS_PER_FIELD} each) ≈ 84 counts — fits under this budget. Because
   * facets are processed in {@link #FACET_FIELDS} order (bounded facets first), any truncation is
   * deterministic and is surfaced as a warning rather than being silent.
   * <p>
   * A pathological page can still exceed the budget: APPLICATION has six per-page facets, so all six
   * saturating the per-field cap would want {@value #MAX_FACET_BUCKETS_PER_FIELD} x 6 = 120 counts. That
   * is deliberate — the budget is a live guard, not an unreachable ceiling. Real pages stay well under it
   * because stages/policyTypes/violationStates are small vocabularies, and because those bounded facets
   * are counted first only the trailing organization/application/category name buckets are ever cut,
   * always with the truncation warning naming the affected facet keys.
   * Fan-out stays O(1) in estate size (bounded Lucene {@code count()}s, not N-per-app).
   */
  static final int MAX_FACET_COUNT_QUERIES = 90;

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

  /**
   * Warning emitted when the per-request facet-count budget truncates the returned buckets. The affected
   * facet keys are appended by {@link #facetCountsTruncatedWarning(List)} so a client can tell a
   * truncated-empty facet from a legitimately-empty one.
   */
  static final String FACET_COUNTS_TRUNCATED =
      "some facet counts were omitted to stay within the per-request query budget";

  /** Truncation warning naming the facet keys whose buckets were omitted or cut short. */
  static String facetCountsTruncatedWarning(final List<String> truncatedFacetKeys) {
    return FACET_COUNTS_TRUNCATED + ": " + String.join(", ", truncatedFacetKeys);
  }

  private final IqLocalSearchService iqLocalSearchService;

  private final SearchIndexClient searchIndexClient;

  private final MeterRegistry meterRegistry;

  /**
   * Server clock the {@code expiry} filter and the WAIVER status facet resolve the active-vs-expired
   * boundary against. One clock is captured per request so the page query and its facet counts see a
   * single "now". Injectable for deterministic tests; production uses {@link Clock#systemUTC()}.
   */
  private final Clock clock;

  @Inject
  public IndexQueryService(
      final IqLocalSearchService iqLocalSearchService,
      final SearchIndexClient searchIndexClient,
      @Nullable final MeterRegistry meterRegistry)
  {
    this(iqLocalSearchService, searchIndexClient, meterRegistry, Clock.systemUTC());
  }

  IndexQueryService(
      final IqLocalSearchService iqLocalSearchService,
      final SearchIndexClient searchIndexClient,
      @Nullable final MeterRegistry meterRegistry,
      final Clock clock)
  {
    this.iqLocalSearchService = iqLocalSearchService;
    this.searchIndexClient = searchIndexClient;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
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
    AUTO_WAIVER_TOGGLE,
    /**
     * Fixed WAIVER active/expiring/expired/auto-waived buckets, derived from the expires-at epoch point
     * vs server-now (reusing the {@code expiry} filter's active/expired range shape) and the
     * {@code policyWaiverAuto} discriminator. Whole-corpus, not self-restricting to the user's expiry
     * or auto selection.
     */
    WAIVER_STATUS
  }

  /** WAIVER status facet bucket keys, matching the prototype STATUS_OPTIONS. */
  static final String STATUS_ACTIVE = "active";

  static final String STATUS_EXPIRING = "expiring";

  static final String STATUS_EXPIRED = "expired";

  static final String STATUS_AUTO_WAIVED = "auto-waived";

  /**
   * Expiring-soon window: a waiver is "expiring" when its expiry falls within this many days of now,
   * matching the prototype's {@code EXPIRY_THRESHOLD_DAYS}. "expiring" is a subset of "active".
   */
  static final int STATUS_EXPIRING_WINDOW_DAYS = 30;

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

    static Facet waiverStatus(final String key) {
      return new Facet(FacetMode.WAIVER_STATUS, key, null, null);
    }
  }

  public IndexQueryResponse query(final IndexQueryType queryType, final IndexQueryRequest request) {
    // Fix one "now" per request so the page query's expiry filter and the WAIVER status facet resolve
    // the active-vs-expired boundary against the same instant.
    final Clock requestClock = Clock.fixed(clock.instant(), clock.getZone());
    final CompiledQuery compiled =
        IndexQueryFilterCompiler.compileWithClauses(queryType, request.getFilters(), requestClock);
    final String q = compiled.q();
    final Tab tab = tabFor(queryType);
    final String sortKey = validateSort(queryType, tab, request.getSort());
    final int pageSize = pageSize(request.getPageSize());

    final String rawSearchAfter = StringUtils.isBlank(request.getSearchAfter()) ? null : request.getSearchAfter();
    final int page = validatePage(request.getPage(), rawSearchAfter);

    // Lifecycle-only endpoint: isSbomManagerMode is intentionally false (6-arg ctor uses the default).
    final SearchInputs inputs = new SearchInputs(q, tab, queryType.itemTypes(), pageSize, sortKey, rawSearchAfter);
    final IqLocalSearchResponse result = iqLocalSearchService.search(inputs);

    final List<IndexQueryRow> rows = toRows(queryType, result);
    final int dropped = result.rows().size() - rows.size();

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
        request.isIncludeFacets() ? computeFacets(queryType, compiled, rows, warnings, requestClock) : null;

    return buildResponse(queryType, page, pageSize, result, rows, facets, nextSearchAfter, warnings);
  }

  private static IndexQueryResponse buildResponse(
      final IndexQueryType queryType,
      final int page,
      final int pageSize,
      final IqLocalSearchResponse result,
      final List<IndexQueryRow> rows,
      final Map<String, List<IndexQueryFacetBucket>> facets,
      final String nextSearchAfter,
      final List<String> warnings)
  {
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

  /** Global-search entity {@link Tab}s that map to an {@link IndexQueryType} carrying a facet set. */
  private static IndexQueryType facetQueryTypeFor(final Tab tab) {
    return switch (tab) {
      case APPLICATION -> IndexQueryType.APPLICATION;
      case VIOLATION -> IndexQueryType.VIOLATION;
      case WAIVER -> IndexQueryType.WAIVER;
      // COMPONENT/VULNERABILITY carry no FACET_FIELDS entry today; ALL never reaches here (facets are
      // computed only for a single entity tab). Returning null yields a null facet map upstream.
      case COMPONENT, VULNERABILITY, ALL -> null;
    };
  }

  /**
   * Compute the per-tab facet map for a Global-Search {@code /results} request, reusing the identical
   * whole-corpus / RBAC-scoped / capped {@link #computeFacets} machinery the {@code /index-query}
   * endpoint uses.
   *
   * <p>
   * The {@code /results} filters live inside the free-text {@code q=} string rather than a structured
   * filter bag, so {@link ResultsFacetQueryBridge} re-parses {@code q} with the SAME query parser and
   * rebuilds the structured field chips as the Lucene clause strings the facet-count base expects
   * (same FieldMap field resolution as the index-query path).
   *
   * <p>
   * Facet VALUES are seeded from a dedicated page-1 / RELEVANCE / default-size query, independent of the
   * caller's sort, page and cursor. That is deliberate: the bucket COUNTS are whole-corpus (computed by
   * separate count queries, not derived from the seed rows), so the seed page affects only which candidate
   * values populate the rail, and pinning it keeps the filter rail stable as the user pages instead of
   * changing underneath them. The cost is one extra search per faceted request beyond the caller's page
   * search, which {@link #query} avoids by deriving both from a single call at the price of page-dependent
   * candidates.
   *
   * <p>
   * Returns {@code null} for a tab with no facet set (COMPONENT/VULNERABILITY today, and defensively
   * ALL). Returns a (possibly empty) map otherwise. A {@code q} carrying only free text (no
   * {@code field:value} chips) still yields the tab's whole-corpus facet buckets rather than erroring:
   * the rebuilt clause list is empty, so the counts fall back to the item-type base.
   *
   * <p>
   * VIOLATION unions POLICY_VIOLATION + LEGAL_VIOLATION inside {@link IndexQueryType#VIOLATION}, so the
   * violation facet counts already span both index item types with no extra work here.
   *
   * <p>
   * Facet-count warnings (e.g. {@code FACET_COUNTS_TRUNCATED} when the {@code MAX_FACET_COUNT_QUERIES}
   * budget is exhausted) are collected into {@code warnings}, so the caller can merge them into the
   * {@code /results} warnings channel instead of the frontend rendering truncated buckets with no signal.
   *
   * @param warnings non-null mutable sink for facet-count warnings; pass a fresh mutable list to ignore them
   */
  public Map<String, List<IndexQueryFacetBucket>> facetsForResults(
      final Tab tab,
      final String q,
      final List<String> warnings)
  {
    // Must be mutable and non-null: computeFacets appends the truncation warning to it. Checked up front
    // so a bad caller fails immediately rather than only on the queries that exhaust the count budget.
    Objects.requireNonNull(warnings, "warnings");
    final IndexQueryType queryType = facetQueryTypeFor(tab);
    if (queryType == null) {
      return null;
    }
    final Clock requestClock = Clock.fixed(clock.instant(), clock.getZone());
    final CompiledQuery compiled = ResultsFacetQueryBridge.compile(queryType, q);

    // Seed facet VALUES from a fresh page query (page 1, default size). The free-text refinement is
    // part of the page query (compiled.q() carries only the structured chips, so the free text is added
    // back here) so the seed rows reflect what the user searched.
    //
    // The seed query always sorts by RELEVANCE, never the per-tab default sort: facet VALUES are an
    // unordered set (the buckets are re-derived from row-field values and counted whole-corpus), so the
    // order of the seed page is irrelevant, and relevance is allowlisted on every tab. See the method
    // contract for why the seed is pinned rather than following the caller's sort/page.
    final String pageQuery = pageQueryFor(q, compiled);
    final Tab cursorTab = tabFor(queryType);
    final String sortKey = GlobalSearchSortAllowlist.RELEVANCE;
    final int pageSize = IqLocalSearchService.DEFAULT_PER_TYPE_PAGE_SIZE;
    final SearchInputs inputs =
        new SearchInputs(pageQuery, cursorTab, queryType.itemTypes(), pageSize, sortKey, null);
    final IqLocalSearchResponse result = iqLocalSearchService.search(inputs);

    final List<IndexQueryRow> rows = toRows(queryType, result);
    // Facet-count warnings (e.g. budget truncation) land in the caller-supplied list so /results can
    // merge them into its own warnings channel.
    return computeFacets(queryType, compiled, rows, warnings, requestClock);
  }

  /**
   * Maps the IQ-local rows to {@link IndexQueryRow}s, skipping any row the mapper rejects for missing its
   * identifying field. Callers that report drops derive the count by comparing sizes, so the drop count
   * stays observable without this helper owning the reporting policy.
   */
  private static List<IndexQueryRow> toRows(final IndexQueryType queryType, final IqLocalSearchResponse result) {
    final List<IndexQueryRow> rows = new ArrayList<>(result.rows().size());
    for (IqLocalRow tagged : result.rows()) {
      final IndexQueryRow row = IndexQueryRowMapper.toRow(queryType, tagged.row());
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  /**
   * The page query used to seed facet VALUES: the caller's raw {@code q} (free text + chips) so the
   * seed rows match what the user searched. Falls back to the rebuilt structured-only chip string when
   * {@code q} is blank so an empty query still returns a match-all page.
   */
  private static String pageQueryFor(final String q, final CompiledQuery compiled) {
    if (q != null && !q.isBlank()) {
      return q;
    }
    return compiled.q();
  }

  /**
   * Lazily streams EVERY row matching the request, for the CSV export. Same filters, same sort, same
   * RBAC scoping, same row mapping as {@link #query} — only pagination differs: the export walks the
   * whole result set by following the internal {@code searchAfter} cursor instead of returning one page.
   *
   * <p>
   * The query is compiled ONCE here, through the same
   * {@link IndexQueryFilterCompiler#compileWithClauses} call {@link #query} uses, so the export can
   * never diverge from what the page shows. The caller's {@code page}/{@code pageSize}/
   * {@code searchAfter} are ignored (an export is inherently unpaginated); the sort IS honoured and
   * validated by the same {@link #validateSort}, so the exported row order matches the page's.
   *
   * <p>
   * The returned iterator is lazy and holds at most one index page at a time, so a 100k-row export
   * never materialises the full result set. Rows the mapper rejects (incomplete index data) are
   * skipped, mirroring {@link #query}'s drop behaviour; the drop metric is recorded per page.
   */
  public Iterator<IndexQueryRow> streamForExport(
      final IndexQueryType queryType,
      final IndexQueryRequest request)
  {
    final Clock requestClock = Clock.fixed(clock.instant(), clock.getZone());
    final CompiledQuery compiled =
        IndexQueryFilterCompiler.compileWithClauses(queryType, request.getFilters(), requestClock);
    final Tab tab = tabFor(queryType);
    final String sortKey = validateSort(queryType, tab, request.getSort());
    return new ExportRowIterator(queryType, compiled.q(), tab, sortKey);
  }

  /**
   * Pull-based iterator over the whole result set, fetching one {@link CsvExportLimits#PAGE_SIZE} page
   * at a time and following the {@code searchAfter} sort values between pages.
   *
   * <p>
   * The walk terminates on either of two independent conditions — an empty {@code nextSearchAfter}, or
   * a page shorter than the requested size — so a backend that keeps handing back a cursor for an
   * empty tail cannot spin. The writer's row cap bounds it a third time.
   */
  private final class ExportRowIterator
      implements Iterator<IndexQueryRow>
  {
    private final IndexQueryType queryType;

    private final String query;

    private final Tab tab;

    private final String sortKey;

    private final Deque<IndexQueryRow> buffered = new ArrayDeque<>();

    private List<String> searchAfter = List.of();

    /**
     * The backend that served the previous page, pinned into the next page's cursor so a reindex or a
     * Hybrid failover between pages is rejected instead of replaying stale sort values against a changed
     * index. Null before the first page.
     */
    private String servingBackendId;

    private boolean exhausted;

    private ExportRowIterator(
        final IndexQueryType queryType,
        final String query,
        final Tab tab,
        final String sortKey)
    {
      this.queryType = queryType;
      this.query = query;
      this.tab = tab;
      this.sortKey = sortKey;
    }

    @Override
    public boolean hasNext() {
      // A page can be entirely dropped rows, so keep fetching until a row buffers or the walk ends.
      while (buffered.isEmpty() && !exhausted) {
        fetchNextPage();
      }
      return !buffered.isEmpty();
    }

    @Override
    public IndexQueryRow next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return buffered.removeFirst();
    }

    private void fetchNextPage() {
      final SearchInputs inputs = new SearchInputs(
          query, tab, queryType.itemTypes(), CsvExportLimits.PAGE_SIZE, sortKey, encodedSearchAfter());
      final IqLocalSearchResponse result = iqLocalSearchService.search(inputs);
      // Pin subsequent cursors to the backend that actually served this page. A generation change between
      // pages (reindex, Hybrid failover) then fails cursor validation on the next fetch rather than
      // silently skipping or duplicating rows into a wrong CSV returned at HTTP 200.
      servingBackendId = result.servingBackendId();
      final List<IndexQueryRow> pageRows = new ArrayList<>(result.rows().size());
      int dropped = 0;
      for (IqLocalRow tagged : result.rows()) {
        final IndexQueryRow row = IndexQueryRowMapper.toRow(queryType, tagged.row());
        if (row != null) {
          pageRows.add(row);
        }
        else {
          dropped++;
        }
      }
      if (dropped > 0) {
        log.warn("Omitted {} {} result(s) missing the identifying field from the CSV export", dropped, queryType);
        recordDropped(queryType, dropped);
      }
      // Identical enrichment to the list path, so an exported POLICY row carries the same Waiver Count the
      // page shows instead of an empty cell. Mutates pageRows in place, hence the mutable list above.
      if (queryType == IndexQueryType.POLICY) {
        enrichPolicyWaiverCounts(pageRows);
      }
      buffered.addAll(pageRows);
      final List<String> next = result.nextSearchAfter();
      if (next == null || next.isEmpty() || result.rows().size() < CsvExportLimits.PAGE_SIZE) {
        exhausted = true;
      }
      else {
        searchAfter = next;
      }
    }

    /**
     * Encodes the raw sort values into the cursor form {@link IqLocalSearchService#search} expects. The
     * token is minted for the same tab/sort/pageSize this walk uses AND pinned to the backend that served
     * the previous page, so an index generation change mid-walk is rejected rather than mis-paginated.
     * Returns {@code null} on the first page (no cursor).
     */
    private String encodedSearchAfter() {
      if (searchAfter.isEmpty()) {
        return null;
      }
      final GlobalSearchCursor cursor = iqLocalSearchService.mintNextCursor(
          tab, sortKey, CsvExportLimits.PAGE_SIZE, searchAfter, servingBackendId);
      return cursor == null ? null : cursor.encode();
    }
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
      final List<String> warnings,
      final Clock requestClock)
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
    // processed in their FACET_FIELDS order (bounded facets first, high-cardinality name facets last)
    // so truncation is deterministic and the always-relevant bounded facets are never starved by the
    // dynamic per-page name facets.
    final int[] budget = {MAX_FACET_COUNT_QUERIES};
    final boolean[] truncated = {false};
    final List<String> truncatedFacetKeys = new ArrayList<>();
    for (Facet facet : facetFields) {
      // Per-facet flag: every facet whose buckets were cut short is named, not just the first one to
      // exhaust the budget. A shared flag would stay set and mask each subsequent starved facet.
      final boolean[] facetTruncated = {false};
      final List<IndexQueryFacetBucket> buckets = switch (facet.mode()) {
        case VALUE -> valueBuckets(facet, baseQuery, rows, budget, facetTruncated);
        case NUMERIC -> numericBuckets(facet, baseQuery, rows, budget, facetTruncated);
        case STATES -> fixedBuckets(fixedFacetBaseQuery, budget, facetTruncated, stateClauses());
        case WAIVER_TYPES -> fixedBuckets(fixedFacetBaseQuery, budget, facetTruncated, waiverTypeClauses());
        // The auto/manual toggle reports true/false counts over the whole corpus regardless of a
        // manual-only view (from an explicit includeAutoWaivers:false), so it counts
        // against a base that drops the policyWaiverAuto:"false" restriction. Otherwise the base would
        // carry it and the "true" bucket would always count 0.
        case AUTO_WAIVER_TOGGLE -> autoWaiverToggleBuckets(
            facet, baseMetricQueryWithoutAutoRestriction(queryType, compiled), budget, facetTruncated);
        // Status counts over the whole corpus regardless of the user's own expiry/auto selection, so
        // the base drops both the expiry range clause and the manual-only auto restriction.
        case WAIVER_STATUS -> fixedBuckets(
            baseMetricQueryForStatus(queryType, compiled), budget, facetTruncated, statusClauses(requestClock));
      };
      out.put(facet.key(), buckets);
      if (facetTruncated[0]) {
        truncated[0] = true;
        truncatedFacetKeys.add(facet.key());
      }
    }
    if (truncated[0]) {
      warnings.add(facetCountsTruncatedWarning(truncatedFacetKeys));
    }
    return out;
  }

  /** Distinct string values of a (possibly multi-valued) page row-field, capped per field. */
  private static Set<String> distinctRowValues(final List<IndexQueryRow> rows, final String rowField) {
    return RowFacetValues.distinctRowValues(
        rows, rowField, IndexQueryRow::getFields, MAX_FACET_BUCKETS_PER_FIELD);
  }

  private List<IndexQueryFacetBucket> valueBuckets(
      final Facet facet,
      final String baseQuery,
      final List<IndexQueryRow> rows,
      final int[] budget,
      final boolean[] truncated)
  {
    final Set<String> values = distinctRowValues(rows, facet.valueRowField());
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
    final Set<String> values = distinctRowValues(rows, facet.valueRowField());
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

  /** Index field carrying the waiver expiry epoch-millis point (also the {@code expiry} filter field). */
  private static final String WAIVER_EXPIRES_AT_EPOCH_FIELD = "policyWaiverExpiresAtEpochMs";

  /** Index field carrying the auto-vs-manual discriminator ("true"/"false"). */
  private static final String WAIVER_AUTO_FIELD = "policyWaiverAuto";

  /** Item-type clause restricting a query to committed waivers (excludes waiver-request docs). */
  private static final String COMMITTED_WAIVER_TYPE_CLAUSE =
      "itemType:" + ItemType.POLICY_WAIVER.searchFieldName();

  /**
   * Fixed WAIVER status buckets (active / expiring / expired / auto-waived), derived from the
   * expires-at epoch point vs {@code now} and the auto discriminator. Status is a committed-waiver
   * lifecycle dimension, so every bucket is scoped to {@code itemType:policy_waiver} — waiver-request
   * docs (which also carry an expires-at point) are the separate {@code waiverStates} axis and must not
   * inflate the active/expiring/expired counts:
   * <ul>
   * <li>{@code expired} = expiry present AND at or before now ({@code field:[* TO now]}), matching
   * the {@code expiry} filter's expired shape;</li>
   * <li>{@code active} = NOT expired (never-expiring waivers carry no point and so are active),
   * matching the {@code expiry} filter's active shape;</li>
   * <li>{@code expiring} = expiry within the next {@value #STATUS_EXPIRING_WINDOW_DAYS} days
   * ({@code field:[now TO now+window]}); a subset of active, so its count can exceed neither the
   * active total nor overlap the expired bucket;</li>
   * <li>{@code auto-waived} = {@code policyWaiverAuto:"true"}, orthogonal to the expiry-derived
   * buckets (an auto-waiver can also be active/expiring/expired).</li>
   * </ul>
   */
  private static Map<String, String> statusClauses(final Clock clock) {
    final long now = clock.millis();
    final long windowEnd = now + Duration.ofDays(STATUS_EXPIRING_WINDOW_DAYS).toMillis();
    final String expiredClause = WAIVER_EXPIRES_AT_EPOCH_FIELD + ":[* TO " + now + "]";
    final Map<String, String> m = new LinkedHashMap<>();
    m.put(STATUS_ACTIVE, "(" + COMMITTED_WAIVER_TYPE_CLAUSE + " AND NOT " + expiredClause + ")");
    m.put(STATUS_EXPIRING, "(" + COMMITTED_WAIVER_TYPE_CLAUSE + " AND "
        + WAIVER_EXPIRES_AT_EPOCH_FIELD + ":[" + now + " TO " + windowEnd + "])");
    m.put(STATUS_EXPIRED, "(" + COMMITTED_WAIVER_TYPE_CLAUSE + " AND " + expiredClause + ")");
    m.put(STATUS_AUTO_WAIVED, WAIVER_AUTO_FIELD + ":" + quote("true"));
    return m;
  }

  /**
   * Facet-count base for the WAIVER status facet: drops the user's own expiry range clause(s) and the
   * manual-only auto restriction, so each status bucket counts independent of the user's current
   * expiry/auto selection. The base does NOT drop the user's {@code waiverStates} clauses, so status
   * counts remain scoped to the active waiverStates selection (e.g. with {@code waiverStates=[existing]}
   * the active/expiring/expired buckets reflect manual committed waivers only, and the auto-waived
   * bucket is 0). This is deliberate: status contextually narrows within the waiverStates dimension
   * rather than reporting the whole WAIVER union.
   */
  private static String baseMetricQueryForStatus(
      final IndexQueryType queryType,
      final CompiledQuery compiled)
  {
    final List<String> excluded = new ArrayList<>();
    final String autoRestriction = compiled.autoWaiverRestrictionClause();
    if (autoRestriction != null) {
      excluded.add(autoRestriction);
    }
    for (String clause : compiled.fieldClauses()) {
      if (clause.contains(WAIVER_EXPIRES_AT_EPOCH_FIELD)) {
        excluded.add(clause);
      }
    }
    return baseMetricQuery(queryType, compiled, excluded);
  }

  /**
   * Facet-count base for the auto/manual facet, dropping the manual-only {@code policyWaiverAuto:"false"}
   * restriction (from an explicit {@code includeAutoWaivers:false})
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

  /**
   * RBAC-scoped facet-count base: {@code itemType:<type(s)> AND <structured filter clauses>}, with
   * {@code excludedClauses} left out so a facet can count against a base that omits its own dimension
   * (e.g. the fixed states/waiverType facets drop the user's waiver-status filter so each fixed count is
   * whole-corpus rather than self-restricting). The RBAC filter is applied inside
   * {@link SearchIndexClient#count(String)} (fail-closed), so a caller with no readable contexts counts
   * 0 rather than an unscoped total.
   */
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
