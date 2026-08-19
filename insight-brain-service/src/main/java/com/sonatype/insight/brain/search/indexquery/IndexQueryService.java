/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalRow;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.export.CsvExportLimits;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;
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
   * Facet spec per entity type. Each {@link Facet} names the IQ index field it aggregates over the whole
   * (RBAC-scoped) corpus in a single pass ({@code termsAggregation} for VALUE facets,
   * {@code aggregateCountByField} for NUMERIC).
   * applicationCategoryName is denormalized onto APPLICATION and violation docs, so the categories
   * facet is populated. Kept ordered so the returned facet map is stable.
   * <p>
   * The organizations/applications/applicationCategories/policy entity facets aggregate on the raw
   * opaque-ID docValues field ({@code parentOrganizationId}, {@code applicationId},
   * {@code applicationCategoryId}, {@code policyWaiverPolicyId}) instead of the display-name field. The
   * name fields are backed by a case-FOLDED sort docValues twin (see {@code LuceneIndexingContext}), so
   * aggregating on them silently lowercases every display name; aggregating on the id field avoids that
   * entirely, and the human-readable name is resolved separately, in one batched DAO lookup per facet,
   * by {@link #valueBucketsFromAggregation}. The emitted bucket {@code value} is therefore the id (so it
   * round-trips through the id-keyed structured filters -- see {@code IndexQueryFilterSchema}), and
   * {@code displayName} carries the resolved name for rendering. The legacy name-keyed filters
   * (organizations/applications) remain supported as deprecated aliases.
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
  // Package-private so IndexQueryFilterSchemaTest can assert the round-trip invariant across every
  // facet: an id-aggregated facet needs a filter key compiling to that same field.
  static final Map<IndexQueryType, List<Facet>> FACET_FIELDS = Map.of(
      // Facet order is the response order of the rail sections; each facet is its own aggregation pass, so
      // no facet can starve another.
      IndexQueryType.APPLICATION, List.of(
          Facet.value("stages", "applicationViolationStage"),
          Facet.value("policyTypes", "applicationViolationPolicyType"),
          Facet.value("violationStates", "applicationViolationState"),
          Facet.value("organizations", "parentOrganizationId"),
          Facet.value("applications", "applicationId"),
          Facet.value("applicationCategories", "applicationCategoryId")),
      IndexQueryType.VIOLATION, List.of(
          Facet.states("states"),
          Facet.waiverTypes("waiverType"),
          Facet.value("stages", "policyEvaluationStage"),
          Facet.value("policyTypes", "policyViolationThreatCategory"),
          Facet.value("organizations", "parentOrganizationId"),
          Facet.value("applications", "applicationId"),
          Facet.value("applicationCategories", "applicationCategoryId")),
      IndexQueryType.POLICY, List.of(
          Facet.value("policyTypes", "policyThreatCategory"),
          Facet.value("organizations", "parentOrganizationId")),
      // auto is the auto-vs-manual discriminator counted over the whole corpus
      // (see FacetMode.AUTO_WAIVER_TOGGLE). threatLevel is a numeric IntPoint, so it buckets per
      // discrete value via an exact-value range [v TO v] rather than a phrase-quoted term (which does
      // not match a point field).
      // scope buckets by the indexed policyWaiverScope field: "application", "organization", or
      // "component" (when the waiver/request targets a specific component rather than all components in
      // the owner scope), so a page containing a component-targeted waiver surfaces a component bucket.
      // status is the fixed active/expiring/expired/auto-waived vocabulary derived from the expires-at
      // epoch point vs server-now and the auto discriminator (see FacetMode.WAIVER_STATUS).
      IndexQueryType.WAIVER, List.of(
          Facet.waiverStatus("status"),
          Facet.autoWaiverToggle("auto", "policyWaiverAuto"),
          // Threat level is an IQ 0-10 integer scale.
          Facet.numeric("threatLevel", "policyWaiverThreatLevel", 0, 10),
          Facet.value("scope", "policyWaiverScope"),
          // policyType buckets by the denormalized policyWaiverPolicyType keyword (SECURITY/LICENSE/
          // QUALITY/OTHER). Present on both waiver and request docs.
          Facet.value("policyType", "policyWaiverPolicyType"),
          // policy buckets by policyWaiverPolicyId (display name resolved separately); a bucket
          // round-trips through the policyIds filter (deprecated alias: policy -> policyWaiverPolicyName).
          Facet.value("policy", "policyWaiverPolicyId"),
          Facet.value("organizations", "parentOrganizationId"),
          // applications buckets by the indexed applicationId, written on app-scoped waivers via
          // setOwner(application); org-scoped waivers carry no applicationId and simply do not
          // contribute to this facet. The bucket value is the id; the applications (name) filter is kept as a
          // deprecated alias, and applicationIds is the id-keyed structured filter it round-trips through.
          Facet.value("applications", "applicationId")));

  /**
   * Buckets a VALUE or NUMERIC facet returns to the caller: the top-N of a single whole-corpus
   * aggregation, ranked by document count. It bounds the rail's length, not any query fan-out - those
   * facets cost one aggregation each regardless of how many values match, so this never affects the
   * per-request budget in {@link #MAX_FACET_COUNT_QUERIES}. When more values match than the rail shows,
   * {@link #FACET_VALUES_CAPPED} names the affected facets so the truncation is not silent.
   * <p>
   * The hierarchical organization facet requests a wider candidate window than this and reduces to it
   * only after ROOT exclusion and the read gate - see {@link #MAX_ORGANIZATION_FACET_CANDIDATES}.
   */
  static final int MAX_FACET_BUCKETS_PER_FIELD = 20;

  /**
   * Candidate buckets requested for the hierarchical organization facet before ROOT exclusion, the
   * read gate and the display cap are applied.
   * <p>
   * Sized well above {@link #MAX_FACET_BUCKETS_PER_FIELD} because the aggregation returns top-N by
   * document count and ancestor-closure counts accumulate toward the root: every ancestor outranks the
   * leaves beneath it. A caller who can read only a low-count leaf organization would otherwise see the
   * candidate window filled entirely by higher-count ancestors it cannot read, and the read gate can
   * only filter what the aggregation returned - it cannot recover an organization that never appeared.
   * Matches {@code ApplicationsListFacetsBuilder.MAX_ORGANIZATION_FACET_ENTRIES}.
   */
  static final int MAX_ORGANIZATION_FACET_CANDIDATES = 500;

  /**
   * Overall ceiling on whole-corpus facet {@code count()} calls issued per request. Each such count is one
   * RBAC-scoped index query, so bounding the fan-out keeps a single {@code includeFacets} request from
   * firing dozens of counts under load (p95 &lt; 300ms target). When the budget runs out the remaining
   * buckets are omitted and {@link #FACET_COUNTS_TRUNCATED} names the affected facets, so truncation is
   * never silent.
   * <p>
   * Only the clause-counted facet modes draw on it: states, waiver types, the auto/manual toggle and
   * waiver status, each of which counts one query per candidate value. VALUE and NUMERIC facets do not -
   * they are a single aggregation pass each, bounded by {@link #MAX_FACET_BUCKETS_PER_FIELD} and reported
   * via {@link #FACET_VALUES_CAPPED} when more values match than the rail shows.
   * <p>
   * Sized so a realistic page never truncates while a pathological one still cannot fan out without
   * bound: the densest WAIVER page draws status (4) + auto/manual (2) + threatLevel (~11) + scope (≤3) +
   * policyType (≤4), comfortably inside the budget. Facets are processed in {@link #FACET_FIELDS} order,
   * bounded ones first, so any truncation is deterministic. Fan-out stays O(1) in estate size - bounded
   * counts, not one per application.
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

  /**
   * Warning emitted when the facet build failed and the response carries an empty facet map. Distinguishes
   * "facets requested but unavailable" from "facets not requested", which a null map alone cannot.
   */
  static final String FACETS_UNAVAILABLE = "facets could not be computed for this request";

  /**
   * Warning emitted when a VALUE facet had more matching values than {@link #MAX_FACET_BUCKETS_PER_FIELD}.
   * Distinct from {@link #FACET_COUNTS_TRUNCATED}, which is about the shared count budget: this one means
   * the aggregation found more values than the rail displays, so a client can tell "these are all the
   * values" from "these are the top values".
   */
  static final String FACET_VALUES_CAPPED =
      "some facet values were omitted because more values matched than the per-facet display limit";

  /**
   * Separator between {@link #FACET_COUNTS_TRUNCATED} and the keys of the facets that actually lost
   * buckets. Facets are processed in {@link #FACET_FIELDS} order, so the variable-name facets declared
   * last are the ones a dense multi-filter request truncates; naming them makes the warning actionable
   * instead of leaving a caller to guess which rail section is short.
   */
  static final String TRUNCATED_FACETS_SEPARATOR = ": ";

  /** Truncation warning naming the facet keys whose buckets were omitted or cut short. */
  static String facetCountsTruncatedWarning(final List<String> truncatedFacetKeys) {
    return FACET_COUNTS_TRUNCATED + TRUNCATED_FACETS_SEPARATOR + String.join(", ", truncatedFacetKeys);
  }

  static String facetValuesCappedWarning(final List<String> cappedFacetKeys) {
    return FACET_VALUES_CAPPED + TRUNCATED_FACETS_SEPARATOR + String.join(", ", cappedFacetKeys);
  }

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  /** Resolves applicationCategoryId -> name (application categories are backed by {@link Tag}). */
  private final TagDAO tagDAO;

  private final PolicyDAO policyDAO;

  private final IqLocalSearchService iqLocalSearchService;

  private final SearchIndexClient searchIndexClient;

  private final IndexReadSessionFactory sessionFactory;

  private final ConversionHelper conversionHelper;

  private final OrganizationSummaryService organizationSummaryService;

  private final MeterRegistry meterRegistry;

  /**
   * Server clock the {@code expiry} filter and the WAIVER status facet resolve the active-vs-expired
   * boundary against. One clock is captured per request so the page query and its facet counts see a
   * single "now". Injectable for deterministic tests; production uses {@link Clock#systemUTC()}.
   */
  private final Clock clock;

  @Inject
  public IndexQueryService(
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final TagDAO tagDAO,
      final PolicyDAO policyDAO,
      final IqLocalSearchService iqLocalSearchService,
      final SearchIndexClient searchIndexClient,
      final IndexReadSessionFactory sessionFactory,
      final ConversionHelper conversionHelper,
      final OrganizationSummaryService organizationSummaryService,
      @Nullable final MeterRegistry meterRegistry)
  {
    this(organizationDAO, applicationDAO, tagDAO, policyDAO, iqLocalSearchService, searchIndexClient, sessionFactory,
        conversionHelper, organizationSummaryService, meterRegistry, Clock.systemUTC());
  }

  IndexQueryService(
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final TagDAO tagDAO,
      final PolicyDAO policyDAO,
      final IqLocalSearchService iqLocalSearchService,
      final SearchIndexClient searchIndexClient,
      final IndexReadSessionFactory sessionFactory,
      final ConversionHelper conversionHelper,
      final OrganizationSummaryService organizationSummaryService,
      @Nullable final MeterRegistry meterRegistry,
      final Clock clock)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.tagDAO = tagDAO;
    this.policyDAO = policyDAO;
    this.iqLocalSearchService = iqLocalSearchService;
    this.searchIndexClient = searchIndexClient;
    this.sessionFactory = sessionFactory;
    this.conversionHelper = conversionHelper;
    this.organizationSummaryService = organizationSummaryService;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  /** How a facet's buckets are derived + counted. */
  private enum FacetMode
  {
    /**
     * Buckets and counts come from one whole-corpus aggregation pass over {@code indexField}, so values
     * that fall outside the caller's page are still offered.
     */
    VALUE,
    /**
     * Numeric-point buckets from one whole-corpus aggregation over {@code indexField}, bucketed by value
     * range because a phrase-quoted term does not match a point field.
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
  static final int STATUS_EXPIRING_WINDOW_DAYS = 7;

  /**
   * One facet.
   *
   * @param key stable facet name; aligned to the corresponding filter key so the rail round-trips.
   * @param indexField IQ index field this facet aggregates over (null for fixed-vocabulary facets, whose
   *          buckets come from counting a clause per vocabulary value).
   * @param numericDomain inclusive integer domain a {@link FacetMode#NUMERIC} facet buckets over, one
   *          bucket per value; null for every other mode. Declared per facet because the domain belongs to
   *          the field: a facet over a differently-scaled field must state its own bounds rather than
   *          inherit another's.
   */
  record Facet(FacetMode mode, String key, String indexField, NumericDomain numericDomain)
  {
    /** Inclusive integer bounds of a NUMERIC facet's vocabulary. */
    record NumericDomain(int minInclusive, int maxInclusive)
    {
      NumericDomain {
        if (minInclusive > maxInclusive) {
          throw new IllegalArgumentException(
              "numeric facet domain min " + minInclusive + " exceeds max " + maxInclusive);
        }
      }
    }

    static Facet value(final String key, final String indexField) {
      return new Facet(FacetMode.VALUE, key, indexField, null);
    }

    static Facet numeric(final String key, final String indexField, final int minInclusive, final int maxInclusive) {
      return new Facet(FacetMode.NUMERIC, key, indexField, new NumericDomain(minInclusive, maxInclusive));
    }

    static Facet states(final String key) {
      return new Facet(FacetMode.STATES, key, null, null);
    }

    static Facet waiverTypes(final String key) {
      return new Facet(FacetMode.WAIVER_TYPES, key, null, null);
    }

    static Facet autoWaiverToggle(final String key, final String indexField) {
      return new Facet(FacetMode.AUTO_WAIVER_TOGGLE, key, indexField, null);
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

    // Facet values and counts are whole-corpus and RBAC-scoped, over the same active structured filters
    // and item type as the page - never limited to what this page happens to contain.
    Map<String, List<IndexQueryFacetBucket>> facets = null;
    if (request.isIncludeFacets()) {
      try {
        facets = computeFacets(queryType, compiled, warnings, requestClock);
      }
      catch (RuntimeException e) {
        log.warn("Index-query facet build failed; returning page without facets", e);
        // An empty map plus a warning, not null: a caller that asked for facets can then tell a failed
        // build from a request that never asked, and still render the page.
        facets = Map.of();
        warnings.add(FACETS_UNAVAILABLE);
      }
    }

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
   * Facet values and counts are both whole-corpus, from one aggregation pass per facet that is independent
   * of the caller's sort, page and cursor. The rail therefore stays stable as the user pages, and offers
   * values no page contains. The cost is one aggregation per facet per faceted request beyond the caller's
   * page search, the same shape {@link #query} uses.
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
    // Facet counts come from single-pass aggregations over the RBAC-scoped index, so no page of rows is
    // needed here. Facet-count warnings (e.g. budget truncation) land in the caller-supplied list so
    // /results can merge them into its own warnings channel.
    try {
      return computeFacets(queryType, compiled, warnings, requestClock);
    }
    catch (RuntimeException e) {
      // The filter rail is an enrichment of the results page. Failing it must not take the page's rows
      // with it, so this degrades the same way the paged query path does and reports it in the warnings
      // the caller merges into the response.
      log.warn("Results facet build failed for tab {}; returning the page without facets", tab, e);
      warnings.add(FACETS_UNAVAILABLE);
      return Map.of();
    }
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
   * Whole-corpus facets computed over the RBAC-scoped index, not the current page.
   * <p>
   * VALUE facets are built by a single-pass {@code termsAggregation} on the facet's index field, capped
   * at {@link #MAX_FACET_BUCKETS_PER_FIELD} buckets per field; NUMERIC facets by a single bucketed
   * count. A facet whose own field the request filters is aggregated with that field's clauses removed
   * from the base query, so the user still sees every value they could switch to or add alongside the
   * current selection. The fixed states/waiverType facets count against a base that omits the user's own
   * waiver-status filter for the same reason.
   * <p>
   * On the {@code /index-query} path the count base includes the compiled free-text {@code query} chip
   * (it is part of {@link CompiledQuery#fieldClauses()}), so facet counts reflect the free text plus the
   * structured filters and item type. The {@code /results} bridge ({@link ResultsFacetQueryBridge})
   * deliberately does not lift bare free-text terms into the base, so that path's facet counts reflect
   * the structured filters and item type only.
   * <p>
   * Total {@code count()} fan-out is bounded at {@value #MAX_FACET_COUNT_QUERIES} per request; facets are
   * processed in {@link #FACET_FIELDS} order (bounded facets first) so truncation is deterministic and
   * adds {@link #FACET_COUNTS_TRUNCATED} to the warnings rather than failing.
   */
  private Map<String, List<IndexQueryFacetBucket>> computeFacets(
      final IndexQueryType queryType,
      final CompiledQuery compiled,
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
    // Keys of the facets that actually lost buckets, in processing order, so the truncation warning can
    // name them instead of leaving a caller to work out which rail section is short.
    final List<String> truncatedFacetKeys = new ArrayList<>();
    // Keys of the VALUE facets that had more matching values than the display cap allows.
    final List<String> cappedFacetKeys = new ArrayList<>();
    try (IndexReadSession session = sessionFactory.open()) {
      for (Facet facet : facetFields) {
        // Per-facet flag: every facet whose buckets were cut short is named, not just the first one to
        // exhaust the budget. A shared flag would stay set and mask each subsequent starved facet.
        final boolean[] facetTruncated = {false};
        // Distinct from facetTruncated: a VALUE facet is cut by the per-facet display cap, not by the
        // shared count budget, so it is reported with its own warning naming the cause.
        final boolean[] facetValuesCapped = {false};
        // A facet must not be narrowed by its own selection, or its candidate values collapse to the value
        // the user just picked and the section becomes single-use. Facets whose field the request filters
        // are therefore counted against a base with that field's clauses removed; unfiltered facets use
        // the baseQuery directly.
        final List<String> ownClauses = facet.indexField() == null
            ? List.of()
            : compiled.clausesByField().getOrDefault(facet.indexField(), List.of());
        final boolean selfFiltered = !ownClauses.isEmpty();
        final String facetBaseQuery = selfFiltered ? baseMetricQuery(queryType, compiled, ownClauses) : baseQuery;
        final List<IndexQueryFacetBucket> buckets = switch (facet.mode()) {
          case VALUE -> valueBucketsFromAggregation(session, facet, facetBaseQuery, facetValuesCapped);
          case NUMERIC -> numericBucketsFromAggregation(facet, facetBaseQuery);
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
        if (facetValuesCapped[0]) {
          cappedFacetKeys.add(facet.key());
        }
      }
    }
    if (truncated[0]) {
      warnings.add(facetCountsTruncatedWarning(truncatedFacetKeys));
    }
    if (!cappedFacetKeys.isEmpty()) {
      warnings.add(facetValuesCappedWarning(cappedFacetKeys));
    }
    return out;
  }

  /** Index field the hierarchical organizations facet aggregates on (the ancestor-closure id). */
  private static final String PARENT_ORGANIZATION_ID_FIELD = "parentOrganizationId";

  /** Facet keys whose bucket value is an opaque entity id needing a resolved {@code displayName}. */
  private static final Set<String> ID_KEYED_FACET_KEYS =
      Set.of("organizations", "applications", "applicationCategories", "policy");

  /**
   * VALUE-mode facet buckets via a single-pass termsAggregation.
   * <p>
   * The aggregation is executed over the RBAC-scoped session with the facet's own-field clauses
   * removed from the base query (if applicable), so sibling values are still offered. This mirrors
   * the ApplicationsListFacetsBuilder pattern.
   * <p>
   * The organizations/applications/applicationCategories/policy facets aggregate on the opaque-id
   * field (see {@link #FACET_FIELDS}), so their bucket {@code value} is an id; the human-readable
   * {@code displayName} is resolved via one batched DAO lookup per facet (all bucket ids in a single
   * {@code getByIds} call), not per-bucket, mirroring {@code ApplicationsListFacetsBuilder}.
   */
  private List<IndexQueryFacetBucket> valueBucketsFromAggregation(
      final IndexReadSession session,
      final Facet facet,
      final String baseQuery,
      final boolean[] valuesCapped)
  {
    final Query query = conversionHelper.stringToQuery(baseQuery);
    // The hierarchical org facet aggregates on the ancestor closure, so every doc contributes to every
    // ancestor bucket including ROOT. ROOT and any ancestor outside the caller's read scope are excluded
    // BEFORE the display cap, so a dropped bucket never costs a legitimate org its slot, mirroring
    // ApplicationsListFacetsBuilder#countOrganizations.
    final boolean isOrgFacet = PARENT_ORGANIZATION_ID_FIELD.equals(facet.indexField());
    // Org facet over-fetches: root and any ancestor above the caller's read scope are dropped before the
    // display cap, and ancestors always outrank leaves by count, so the candidate window must be wide
    // enough that a leaf-scoped caller's organizations are still in it (see the constant).
    final int requestCap = isOrgFacet ? MAX_ORGANIZATION_FACET_CANDIDATES : MAX_FACET_BUCKETS_PER_FIELD;
    final List<IndexTermsBucket> buckets = session.termsAggregation(query, facet.indexField(), requestCap);

    // The org facet's bucket keys are the ancestor closure, which can include parent/grandparent orgs
    // above the caller's read scope. Gate on the readable set BEFORE the cap so a non-readable ancestor
    // never consumes a capped slot that a legitimately-visible org could otherwise fill.
    // The read gate loads the Organization rows, so their names are reused for the display map below
    // rather than fetched again by id.
    final Map<String, String> readableOrgNames = new LinkedHashMap<>();
    if (isOrgFacet) {
      organizationSummaryService.getOrganizationsForRead(
          buckets.stream().map(IndexTermsBucket::key).collect(Collectors.toSet()))
          .forEach(org -> readableOrgNames.put(org.getId(), org.getName()));
    }
    final Set<String> readableOrgIds = readableOrgNames.keySet();

    final List<IndexTermsBucket> nonZero = new ArrayList<>(buckets.size());
    for (IndexTermsBucket bucket : buckets) {
      if (bucket.count() <= 0 || StringUtils.isBlank(bucket.key())) {
        continue;
      }
      if (isOrgFacet && Organization.ROOT_ORGANIZATION_ID.equals(bucket.key())) {
        continue;
      }
      if (isOrgFacet && !readableOrgIds.contains(bucket.key())) {
        continue;
      }
      nonZero.add(bucket);
    }

    final boolean overDisplayCap = nonZero.size() > MAX_FACET_BUCKETS_PER_FIELD;
    if (overDisplayCap) {
      // Tell the caller the rail was cut: an exactly-full facet is otherwise indistinguishable from one
      // where only that many values exist.
      valuesCapped[0] = true;
    }
    final List<IndexTermsBucket> capped = overDisplayCap
        ? nonZero.subList(0, MAX_FACET_BUCKETS_PER_FIELD)
        : nonZero;

    final Map<String, String> displayNames;
    if (isOrgFacet) {
      displayNames = readableOrgNames;
    }
    else if (ID_KEYED_FACET_KEYS.contains(facet.key())) {
      displayNames = resolveDisplayNames(facet.key(), capped);
    }
    else {
      displayNames = Map.of();
    }

    final List<IndexQueryFacetBucket> out = new ArrayList<>(capped.size());
    for (IndexTermsBucket bucket : capped) {
      final String displayName = displayNames.get(bucket.key());
      out.add(displayName != null
          ? new IndexQueryFacetBucket(bucket.key(), displayName, bucket.count())
          : new IndexQueryFacetBucket(bucket.key(), bucket.count()));
    }
    return out;
  }

  /**
   * Batched id -> displayName resolution for one facet's buckets: a single {@code getByIds} call
   * against the DAO backing the facet's entity (never one lookup per bucket). Unresolvable ids
   * (deleted/inaccessible entity, or a DAO failure) are simply absent from the returned map, so the
   * caller falls back to the id as the display value rather than failing the whole facet.
   * <p>
   * These lookups are intentionally NOT permission-filtered, and the organization facet must not be
   * changed to match them. Every id here came from an aggregation over the RBAC-scoped session, so a
   * bucket only exists because the caller can already read documents carrying that id - resolving its
   * name discloses nothing further. The organization facet is the exception because its buckets are the
   * ancestor closure: a readable document contributes buckets for organizations above the caller's
   * scope, which it may not be allowed to see, so that facet gates on
   * {@code getOrganizationsForRead} and takes its names from that result instead of this method.
   */
  private Map<String, String> resolveDisplayNames(final String facetKey, final List<IndexTermsBucket> buckets) {
    if (buckets.isEmpty()) {
      return Map.of();
    }
    final List<String> ids = new ArrayList<>(buckets.size());
    for (IndexTermsBucket bucket : buckets) {
      ids.add(bucket.key());
    }
    try {
      return switch (facetKey) {
        case "organizations" -> namesFrom(organizationDAO.getByIds(ids), Organization::getId, Organization::getName);
        case "applications" -> namesFrom(applicationDAO.getByIds(new LinkedHashSet<>(ids)), Application::getId,
            Application::getName);
        case "applicationCategories" -> namesFrom(tagDAO.getByIds(ids), Tag::getId, Tag::getName);
        case "policy" -> namesFrom(policyDAO.getByIds(ids), Policy::getId, Policy::getName);
        default -> Map.of();
      };
    }
    catch (Exception e) {
      log.error("Failed to resolve display names for the '{}' facet; buckets will fall back to raw ids", facetKey, e);
      return Map.of();
    }
  }

  /** Builds an id -> name map from a batch DAO load, skipping any entity missing an id or a name. */
  private static <T> Map<String, String> namesFrom(
      final List<T> entities,
      final Function<T, String> idFn,
      final Function<T, String> nameFn)
  {
    final Map<String, String> names = new LinkedHashMap<>();
    for (T entity : entities) {
      if (entity == null) {
        continue;
      }
      final String id = idFn.apply(entity);
      final String name = nameFn.apply(entity);
      if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(name)) {
        names.put(id, name);
      }
    }
    return names;
  }

  /**
   * NUMERIC-mode facet buckets via a single-pass aggregateCountByField.
   * <p>
   * The aggregation is executed over the RBAC-scoped client with the facet's own-field clauses
   * removed from the base query (if applicable), so sibling values are still offered. The vocabulary is the
   * facet's declared integer domain, one point range {@code [v, v]} per value, so a facet over a field with
   * different bounds buckets over its own range rather than this one. The single aggregation sees the whole
   * corpus, including off-page values, rather than fanning out one count() call per value.
   */
  private List<IndexQueryFacetBucket> numericBucketsFromAggregation(
      final Facet facet,
      final String baseQuery)
  {
    final Facet.NumericDomain domain = facet.numericDomain();
    if (domain == null) {
      throw new IllegalStateException("NUMERIC facet '" + facet.key() + "' declares no numeric domain");
    }
    // One point range [v, v] per value in the declared domain; the key is the value's string form.
    final Map<String, int[]> ranges = new LinkedHashMap<>();
    for (int v = domain.minInclusive(); v <= domain.maxInclusive(); v++) {
      ranges.put(String.valueOf(v), new int[]{v, v});
    }

    final MetricAggregationResult result =
        searchIndexClient.aggregateCountByField(baseQuery, facet.indexField(), ranges);

    // Map aggregation result to facet buckets, preserving order and dropping zero-count buckets.
    final List<IndexQueryFacetBucket> buckets = new ArrayList<>();
    for (Map.Entry<String, Long> entry : result.buckets.entrySet()) {
      if (entry.getValue() > 0) {
        buckets.add(new IndexQueryFacetBucket(entry.getKey(), entry.getValue()));
      }
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
   * <li>{@code expiring} = expiry after now and on or before the classic {@code IN_7_DAYS}
   * upper bound (start of the current UTC day + {@value #STATUS_EXPIRING_WINDOW_DAYS} days
   * + 1 day); a subset of active, so its count can exceed neither the active total nor overlap
   * the expired bucket;</li>
   * <li>{@code auto-waived} = {@code policyWaiverAuto:"true"}, orthogonal to the expiry-derived
   * buckets (an auto-waiver can also be active/expiring/expired).</li>
   * </ul>
   */
  static Map<String, String> statusClauses(final Clock clock) {
    final Instant requestTime = clock.instant();
    final long now = requestTime.toEpochMilli();
    final long windowEnd = requestTime
        .truncatedTo(ChronoUnit.DAYS)
        .plus(STATUS_EXPIRING_WINDOW_DAYS, ChronoUnit.DAYS)
        .plus(1, ChronoUnit.DAYS)
        .toEpochMilli();
    final String expiredClause = WAIVER_EXPIRES_AT_EPOCH_FIELD + ":[* TO " + now + "]";
    final Map<String, String> m = new LinkedHashMap<>();
    m.put(STATUS_ACTIVE, "(" + COMMITTED_WAIVER_TYPE_CLAUSE + " AND NOT " + expiredClause + ")");
    m.put(STATUS_EXPIRING, "(" + COMMITTED_WAIVER_TYPE_CLAUSE + " AND "
        + WAIVER_EXPIRES_AT_EPOCH_FIELD + ":{" + now + " TO " + windowEnd + "])");
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
    excluded.addAll(compiled.lifecycleStatusClauses());
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
