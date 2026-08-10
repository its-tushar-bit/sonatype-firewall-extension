/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.global.fieldmap.ComponentCoordinateQuery;
import com.sonatype.insight.brain.search.global.fieldmap.CompiledQuery;
import com.sonatype.insight.brain.search.global.fieldmap.FieldMap;
import com.sonatype.insight.brain.search.global.fieldmap.QueryCompiler;
import com.sonatype.insight.brain.search.global.parser.AstNode;
import com.sonatype.insight.brain.search.global.parser.ParsedQuery;
import com.sonatype.insight.brain.search.global.parser.QueryParser;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexSessionNumericSorts;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.global.Tab.APPLICATION;
import static com.sonatype.insight.brain.search.global.Tab.COMPONENT;
import static com.sonatype.insight.brain.search.global.Tab.VIOLATION;
import static com.sonatype.insight.brain.search.global.Tab.VULNERABILITY;
import static com.sonatype.insight.brain.search.global.Tab.WAIVER;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ITEM_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_EVALUATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_SEVERITY;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.index.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.SBOM_METADATA;

/**
 * Single-bool-query orchestrator for the IQ-local leg of Global Search. Entry point is
 * {@link #search(SearchInputs)}: parses the query ({@link QueryParser} + {@link QueryCompiler} +
 * {@link FieldMap}), wraps the permission filter, runs one search, and returns rows tagged with the
 * {@code source: "local"} marker (wrapped so {@link SearchResultItemDTO}, a legacy public API, is
 * untouched).
 *
 * <p>
 * Sort handling: the sort key is validated against {@link GlobalSearchSortAllowlist}, then mapped to
 * a physical {@link Sort} by {@link #sortFor(Tab, String)}. Numeric keys sort on a
 * {@code SortedNumericDocValues} twin (reverse LONG); string keys sort on a lower-cased keyword
 * {@code SortedDocValues} twin, matching the OpenSearch {@code keyword} mapping's lowercase
 * normalizer so both backends order identically. The validated key is echoed in
 * {@link IqLocalSearchResponse#sortKey}.
 */
@Named
@Singleton
public class IqLocalSearchService
{
  private static final Logger log = LoggerFactory.getLogger(IqLocalSearchService.class);

  public static final int DEFAULT_PER_TYPE_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Physical field sort is enabled: every field in {@link #SORTABLE_FIELD_BY_KEY} has a sortable
   * doc-values twin on both backends (numeric twin via {@code LuceneIndexingContext} / OpenSearch
   * numeric mapping; lower-cased keyword twin via {@code LuceneIndexingContext} / OpenSearch keyword
   * mapping). Retained as a constant so a regression can flip it off without a code rewrite.
   * <p>
   * The WAIVER {@code threat} and {@code expiration} sort keys already sort correctly on OpenSearch
   * (the {@code policyWaiverThreatLevel} integer and {@code policyWaiverExpiresAtEpochMs} long
   * mappings are natively sortable). On Lucene they require a {@code SortedNumericDocValues} twin
   * emitted in {@code LuceneIndexingContext.addDocuments} for POLICY_WAIVER docs (threat can be read
   * from the existing stored threat-level value; expiration needs a stored numeric twin added first,
   * since the expiry epoch point is not currently stored). That index-write-path change is owned by
   * the waiver-request indexing workstream and populated by a full reindex.
   */
  static final boolean SORT_BY_FIELD_ENABLED = true;

  /** Whether physical field sort is active; while {@code false}, every sort resolves to relevance. */
  public static boolean isFieldSortEnabled() {
    return SORT_BY_FIELD_ENABLED;
  }

  private final SearchIndexClient searchIndexClient;

  private final FieldMap fieldMap;

  @Inject
  public IqLocalSearchService(final SearchIndexClient searchIndexClient) {
    this(searchIndexClient, FieldMap.defaultMap());
  }

  IqLocalSearchService(final SearchIndexClient searchIndexClient, final FieldMap fieldMap) {
    this.searchIndexClient = Objects.requireNonNull(searchIndexClient, "searchIndexClient");
    this.fieldMap = Objects.requireNonNull(fieldMap, "fieldMap");
  }

  /**
   * Run an IQ-local search and return tagged rows.
   *
   * @throws IllegalArgumentException for unknown sort keys (defense-in-depth — controllers also
   *           validate at the request boundary).
   * @throws IllegalArgumentException when {@code itemTypes} is empty (callers must pick the
   *           per-tab set explicitly).
   * @throws IllegalStateException when the Global Search feature flag is disabled.
   */
  public IqLocalSearchResponse search(final SearchInputs inputs) {
    Objects.requireNonNull(inputs, "inputs");
    if (!searchIndexClient.isSearchPreviewEnabled()) {
      throw new IllegalStateException("Global Search is disabled");
    }
    if (inputs.itemTypes().isEmpty()) {
      throw new IllegalArgumentException("itemTypes must not be empty");
    }

    // Mirrors the v1 searchIndex() license/mode check so both read paths reject the same requests.
    searchIndexClient.checkGlobalSearchMode(inputs.isSbomManagerMode());

    int pageSize = clampPageSize(inputs.pageSize());
    String validatedSortKey = GlobalSearchSortAllowlist.requireAllowed(inputs.tab(), inputs.sortKey());

    // Decode the raw client cursor here (the service owns cursor internals), then re-validate its
    // token against the current preimage; any drift (reindex, allowlist change, backend switch,
    // tenant boundary) forces the client back to page 1. Malformed input is a 400, not a 500.
    GlobalSearchCursor cursor = null;
    if (!StringUtils.isBlank(inputs.cursor())) {
      try {
        cursor = GlobalSearchCursor.decodeUnvalidated(inputs.cursor());
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid pagination cursor.", e);
      }
      String expected = expectedGenerationToken(inputs.tab(), validatedSortKey, pageSize);
      if (!cursor.generationToken().equals(expected)) {
        throw new StaleCursorException("Cursor generation token mismatch");
      }
    }

    Set<ItemType> types = filterTypesForMode(inputs.itemTypes(), inputs.isSbomManagerMode());
    if (types.isEmpty()) {
      // Legitimate empty state: every requested type is excluded by the current mode. Return an
      // empty response with a warning rather than throwing (matches the no-results shape below).
      String warning = "No requested item types apply in "
          + (inputs.isSbomManagerMode() ? "SBOM Manager" : "default") + " mode.";
      return new IqLocalSearchResponse(List.of(), 0L, true, List.of(), validatedSortKey, List.of(warning), null);
    }

    ParsedQuery parsed = QueryParser.parse(inputs.query());
    ComposedQuery composed = composeBaseQuery(inputs.query(), parsed.ast(), types);
    List<String> warnings = mergeWarnings(parsed.warnings(), composed.warnings());

    Query finalQuery = searchIndexClient.buildPermittedQuery(composed.query());

    Sort sort = sortFor(inputs.tab(), validatedSortKey);

    List<String> searchAfter = cursor == null ? Collections.emptyList() : cursor.sortValues();
    GlobalSearchRequest request = new GlobalSearchRequest(finalQuery, sort, pageSize, searchAfter);

    GlobalSearchResult result = searchIndexClient.searchGlobal(request);

    List<IqLocalRow> tagged = new ArrayList<>(result.rows().size());
    for (SearchResultItemDTO row : result.rows()) {
      tagged.add(new IqLocalRow(SearchSource.LOCAL.value(), row));
    }

    long cappedTotal = AbstractSearchIndexClient.capTotalHitsForGlobalSearch(result.totalHits());
    // If capping lowered the total, the count is no longer exact regardless of the backend flag.
    boolean exactTotalHits = result.exactTotalHits() && cappedTotal == result.totalHits();

    return new IqLocalSearchResponse(tagged, cappedTotal, exactTotalHits, result.nextSearchAfter(),
        validatedSortKey, warnings, result.servingBackendId());
  }

  /**
   * Mint the next-page cursor, pinned to the token this service will validate on the follow-up
   * request; the mint and validate preimages MUST match. In particular the {@code pageSize} passed
   * here must be the same raw value later passed to {@link #search(SearchInputs)}, since both sides
   * apply {@link #clampPageSize(int)} independently and the clamped size feeds the token preimage.
   * Returns {@code null} when there is no further page.
   */
  public GlobalSearchCursor mintNextCursor(
      final Tab tab,
      final String sortKey,
      final int pageSize,
      final List<String> sortValues)
  {
    return mintNextCursor(tab, sortKey, pageSize, sortValues, null);
  }

  /**
   * As {@link #mintNextCursor(Tab, String, int, List)}, but pins the cursor to the backend that
   * actually served the page ({@link GlobalSearchResult#servingBackendId()}) rather than the
   * request's default backend. Under a Hybrid primary-failure fallback the secondary serves the
   * page; pinning to the secondary's id makes the primary reject the follow-up cursor as stale once
   * it recovers, instead of silently mis-paginating a secondary-format cursor. Pass {@code null} to
   * use the default backend.
   */
  public GlobalSearchCursor mintNextCursor(
      final Tab tab,
      final String sortKey,
      final int pageSize,
      final List<String> sortValues,
      final String servingBackendId)
  {
    if (sortValues == null || sortValues.isEmpty()) {
      return null;
    }
    String validatedSortKey = GlobalSearchSortAllowlist.requireAllowed(tab, sortKey);
    String token = servingBackendId == null
        ? expectedGenerationToken(tab, validatedSortKey, clampPageSize(pageSize))
        : computeGenerationToken(tab, validatedSortKey, clampPageSize(pageSize), servingBackendId);
    return GlobalSearchCursor.newCursor(token, sortValues);
  }

  private static List<String> mergeWarnings(final List<String> a, final List<String> b) {
    if (a.isEmpty() && b.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> merged = new LinkedHashSet<>(a);
    merged.addAll(b);
    return List.copyOf(merged);
  }

  static int clampPageSize(final int requested) {
    if (requested <= 0) {
      return DEFAULT_PER_TYPE_PAGE_SIZE;
    }
    return Math.min(requested, MAX_PAGE_SIZE);
  }

  /** Generation token expected for the current request; must match what {@link #mintNextCursor} minted. */
  String expectedGenerationToken(final Tab tab, final String sortKey, final int pageSize) {
    String backendId;
    try {
      backendId = searchIndexClient.backendId();
    }
    catch (UnsupportedOperationException e) {
      throw new IllegalStateException(
          "search backend does not implement backendId(); Global Search backend is misconfigured", e);
    }
    return computeGenerationToken(tab, sortKey, pageSize, backendId);
  }

  /** Generation token for an explicit backend id (null coalesces to empty), matching the mint preimage. */
  private String computeGenerationToken(
      final Tab tab,
      final String sortKey,
      final int pageSize,
      final String backendId)
  {
    Long lastIndexTime = searchIndexClient.getLastIndexTime();
    String indexGen = lastIndexTime == null ? "0" : Long.toString(lastIndexTime);
    String resolvedBackendId = backendId == null ? "" : backendId;
    return GlobalSearchCursor.computeGenerationToken(
        indexGen, tab.name(), sortKey, pageSize, resolvedBackendId, currentTenantId());
  }

  private static String currentTenantId() {
    try {
      Tenant t = TenantThreadLocal.getTenant();
      return t == null ? "" : t.tenantSlug;
    }
    catch (RuntimeException e) {
      // Non-MT / test environments may have no tenant wired; the empty tenant id still yields a
      // stable, self-consistent cursor token for the single-tenant case.
      log.warn(
          "TenantThreadLocal unavailable; falling back to empty tenant id for the generation token", e);
      return "";
    }
  }

  record ComposedQuery(Query query, List<String> warnings)
  {
  }

  ComposedQuery composeBaseQuery(final String rawQuery, final AstNode ast, final Set<ItemType> types) {
    LinkedHashSet<String> warnings = new LinkedHashSet<>();
    // A pasted Package URL is compiled to a targeted coordinate query on component-bearing types so
    // the exact component is retrieved into the small typeahead window instead of the generic parser
    // failing open to a match-all (which buries the target row past the fetch limit). Non-component
    // types get match-nothing for a coordinate query — a purl legitimately matches no application.
    Optional<Query> coordinateQuery = ComponentCoordinateQuery.compile(rawQuery);
    // Single-type fast path: skip the top-level SHOULD wrap (equivalent semantics) to save a
    // clause against the Lucene max-clause budget.
    if (types.size() == 1) {
      ItemType only = types.iterator().next();
      Query typeSubquery = subqueryForType(coordinateQuery, ast, only, warnings);
      return new ComposedQuery(wrapWithTypeFilter(only, typeSubquery), List.copyOf(warnings));
    }
    BooleanQuery.Builder top = new BooleanQuery.Builder();
    for (ItemType type : types) {
      Query typeSubquery = subqueryForType(coordinateQuery, ast, type, warnings);
      top.add(wrapWithTypeFilter(type, typeSubquery), Occur.SHOULD);
    }
    return new ComposedQuery(top.build(), List.copyOf(warnings));
  }

  /** Warning surfaced when a coordinate-shaped query is run against a non-component type. */
  static final String COORDINATE_ON_NON_COMPONENT_WARNING =
      "Query looks like a component coordinate; it only matches components. Try the Components tab.";

  /**
   * The coordinate query (when present) replaces the generic compilation for component-bearing
   * types and yields match-nothing for every other type; otherwise the generic per-type compilation
   * applies. Component-bearing types are exactly those whose coordinate fields the coordinate query
   * targets — identified here by the presence of a coordinate group/artifact/name field entry for
   * the type in the {@link FieldMap} (the same registry the generic compiler validates against).
   *
   * <p>
   * A coordinate query against a non-component type legitimately matches nothing, but returning a
   * bare {@link MatchNoDocsQuery} would look identical to "no results" on the results endpoint (the
   * warnings set flows through to the results {@code SectionResult}). Add a user-facing warning so
   * the empty section is non-silent; the {@link LinkedHashSet} dedups it to once per request.
   */
  private Query subqueryForType(
      final Optional<Query> coordinateQuery,
      final AstNode ast,
      final ItemType type,
      final LinkedHashSet<String> warnings)
  {
    if (coordinateQuery.isPresent()) {
      if (isComponentBearing(type)) {
        return coordinateQuery.get();
      }
      warnings.add(COORDINATE_ON_NON_COMPONENT_WARNING);
      return new MatchNoDocsQuery();
    }
    CompiledQuery compiled = QueryCompiler.compile(ast, type, fieldMap);
    warnings.addAll(compiled.warnings());
    return compiled.luceneQuery();
  }

  private boolean isComponentBearing(final ItemType type) {
    return fieldMap.lookup(FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID.label)
        .map(entry -> entry.allowedTypes().contains(type))
        .orElse(false);
  }

  /**
   * Filter requested {@link ItemType}s to those permitted in the current mode, mirroring the v1
   * exclusions in {@link AbstractSearchIndexClient#appendSbomFilteringToQuery}.
   */
  static Set<ItemType> filterTypesForMode(final Set<ItemType> requested, final boolean isSbomManagerMode) {
    Set<ItemType> out = new LinkedHashSet<>();
    for (ItemType t : requested) {
      if (isTypeAllowedInMode(t, isSbomManagerMode)) {
        out.add(t);
      }
    }
    return out;
  }

  private static boolean isTypeAllowedInMode(final ItemType type, final boolean isSbomManagerMode) {
    if (isSbomManagerMode) {
      return type != APPLICATION_CATEGORY
          && type != COMPONENT_LABEL
          && type != POLICY;
    }
    return type != SBOM_METADATA;
  }

  private static Query wrapWithTypeFilter(final ItemType type, final Query subquery) {
    Query typeClause = new TermQuery(new Term(ITEM_TYPE.label, type.searchFieldName()));
    return new BooleanQuery.Builder()
        .add(subquery, Occur.MUST)
        .add(typeClause, Occur.FILTER)
        .build();
  }

  /**
   * Map an allowlisted (tab, sortKey) to a Lucene {@link Sort}. Returns {@code null}
   * (relevance-only) for the relevance key or while {@link #SORT_BY_FIELD_ENABLED} is off. A null
   * index field for an allowlisted non-relevance key is an invariant violation — a missing
   * {@link #SORTABLE_FIELD_BY_KEY} entry — and falls back to relevance rather than throwing.
   */
  static Sort sortFor(final Tab tab, final String sortKey) {
    if (sortKey == null || GlobalSearchSortAllowlist.RELEVANCE.equals(sortKey)) {
      return null;
    }
    if (!SORT_BY_FIELD_ENABLED) {
      return null;
    }
    final boolean descendingPrefix = sortKey.startsWith("-");
    final String bareKey = descendingPrefix ? sortKey.substring(1) : sortKey;
    FieldIdentifier indexField = SORTABLE_FIELD_BY_KEY.getOrDefault(tab, Collections.emptyMap()).get(bareKey);
    if (indexField == null) {
      log.warn("Sort key '{}' is allowlisted for tab {} but has no IQ-local index field; "
          + "this is an invariant violation — falling back to relevance.", sortKey, tab);
      return null;
    }
    // "oldest" reuses the created-at epoch twin (a NUMERIC_DESC field) but ASCENDING, so it cannot go
    // through the field-based dispatch in buildSortField; force ascending here (missing create time last).
    if (GlobalSearchSortAllowlist.WAIVER_OLDEST.equals(bareKey)) {
      return ascendingNumericSort(indexField.label);
    }
    // Ana Waivers list keys honor an optional "-" descending prefix; without it they sort ascending.
    if (ANA_DIRECTIONAL_SORT_KEYS.contains(bareKey)) {
      return buildSortField(indexField, descendingPrefix);
    }
    return buildSortField(indexField);
  }

  /**
   * Missing-value sentinel for an ASCENDING LONG numeric sort by raw label (oldest created-at):
   * absent fields sort LAST. IntPoint missing sentinels live in {@link IndexSessionNumericSorts}.
   */
  private static final long NUMERIC_ASC_MISSING_LAST = Long.MAX_VALUE;

  /**
   * Build the {@link Sort} for a resolved sortable index field, choosing the numeric-vs-string
   * shape from the field itself rather than from a hardcoded key set. Descending numeric fields
   * (epoch-millis create time, threat level) sort on their {@code SortedNumericDocValues} twin
   * newest/highest first via a {@link SortedNumericSortField}. The comparator type is INT for
   * {@link #INT_POINT_SORT_FIELDS} (4-byte {@code IntPoint}) and LONG for epoch-millis fields
   * (8-byte {@code LongPoint}) — Lucene validates the sort's byte width against the points index.
   * Docs missing the numeric field sort last. Ascending numeric fields (expiry) sort soonest first
   * with a missing value (never-expires) placed last. String fields sort ascending on their
   * lower-cased keyword {@code SortedDocValues} twin.
   */
  static Sort buildSortField(final FieldIdentifier indexField) {
    if (FLOAT_SORT_FIELDS.contains(indexField)) {
      // CVSS severity: the doc-values twin is a float encoded via NumericUtils.floatToSortableInt
      // (LuceneIndexingContext.addFloatNumericSortDocValues), so sort as FLOAT (highest first) rather
      // than LONG (which would compare the raw sortable-int bits, not the score). OpenSearch sorts on
      // its native float mapping. A doc with no CVSS score sorts last under the reversed comparator.
      SortedNumericSortField sortField =
          new SortedNumericSortField(indexField.label, SortField.Type.FLOAT, true);
      sortField.setMissingValue(Float.NEGATIVE_INFINITY);
      return new Sort(sortField);
    }
    if (NUMERIC_DESC_SORT_FIELDS.contains(indexField)) {
      return numericSort(indexField, true);
    }
    if (NUMERIC_ASC_SORT_FIELDS.contains(indexField)) {
      // Ascending numeric (waiver expiry: soonest first; violation-state ordinal: Open=0 first).
      return ascendingNumericSort(indexField);
    }
    // Absent keyword sorts last under ascending order (STRING_LAST), so a never-set name/stage does
    // not lead the page.
    SortField sortField = new SortField(indexField.label, SortField.Type.STRING, false);
    sortField.setMissingValue(SortField.STRING_LAST);
    return new Sort(sortField);
  }

  /**
   * Directional overload for Ana sort keys that use an optional {@code -} descending prefix. Left-nav
   * keys continue to use {@link #buildSortField(FieldIdentifier)} (field-default direction).
   */
  static Sort buildSortField(final FieldIdentifier indexField, final boolean reverse) {
    if (FLOAT_SORT_FIELDS.contains(indexField)) {
      SortedNumericSortField sortField =
          new SortedNumericSortField(indexField.label, SortField.Type.FLOAT, reverse);
      sortField.setMissingValue(reverse ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY);
      return new Sort(sortField);
    }
    if (NUMERIC_DESC_SORT_FIELDS.contains(indexField) || NUMERIC_ASC_SORT_FIELDS.contains(indexField)) {
      return numericSort(indexField, reverse);
    }
    SortField sortField = new SortField(indexField.label, SortField.Type.STRING, reverse);
    sortField.setMissingValue(reverse ? SortField.STRING_FIRST : SortField.STRING_LAST);
    return new Sort(sortField);
  }

  /**
   * Ascending numeric sort; a doc with no value sorts LAST. Shared by the
   * {@code NUMERIC_ASC_SORT_FIELDS} dispatch. IntPoint fields go through
   * {@link IndexSessionNumericSorts#intField}; LongPoint via {@link IndexSessionNumericSorts#longField}.
   * The {@code oldest} created-at special case uses {@link #ascendingNumericSort(String)} by label.
   */
  private static Sort ascendingNumericSort(final FieldIdentifier indexField) {
    return numericSort(indexField, false);
  }

  /** Ascending LONG sort by field label (oldest created-at special case). */
  private static Sort ascendingNumericSort(final String label) {
    SortedNumericSortField sortField = new SortedNumericSortField(label, SortField.Type.LONG, false);
    sortField.setMissingValue(NUMERIC_ASC_MISSING_LAST);
    return new Sort(sortField);
  }

  private static Sort numericSort(final FieldIdentifier indexField, final boolean reverse) {
    if (INT_POINT_SORT_FIELDS.contains(indexField)) {
      return new Sort(IndexSessionNumericSorts.intField(indexField, reverse));
    }
    return new Sort(IndexSessionNumericSorts.longField(indexField, reverse));
  }

  /**
   * Sortable index fields backed by a numeric doc-values twin, sorted descending (newest/highest
   * first). Every field not in {@link #NUMERIC_ASC_SORT_FIELDS} or this set sorts ascending as a
   * lower-cased keyword string.
   */
  /** Ana Waivers list sort keys that honor an optional "-" descending prefix. */
  private static final Set<String> ANA_DIRECTIONAL_SORT_KEYS = Set.of(
      "policyWaiverCreatedAt",
      "policyWaiverThreatLevel");

  /**
   * Numeric sort fields whose {@code DocumentBuilder} point twin is an {@link
   * org.apache.lucene.document.IntPoint} (4 bytes) rather than a {@code LongPoint} (8 bytes). These
   * must sort as {@link SortField.Type#INT} so the comparator's byte width matches the indexed point
   * field: Lucene's numeric comparator reads the same-named points index to build a competitive
   * iterator, and {@code NumericComparator} rejects a width mismatch outright with "indexed with 4
   * bytes per dimension, but ... expected 8". The failure only surfaces once a segment actually holds
   * a value for the field, because an absent field yields no {@code PointValues} to validate against.
   * The doc-values twin itself is width-agnostic ({@code SortedNumericDocValuesField} always stores a
   * long), so sorting as INT reads the same values and yields the same ordering.
   *
   * <p>
   * Kept in sync with the {@code IntPoint} writes in {@code DocumentBuilder} by a drift-guard test that
   * indexes each sortable field and asserts the emitted point field's byte width matches this set; the
   * sort width is a property of how the field is indexed, not of the value range.
   */
  @VisibleForTesting
  static final Set<FieldIdentifier> INT_POINT_SORT_FIELDS = Set.of(
      POLICY_VIOLATION_THREAT_LEVEL,
      APPLICATION_MAX_POLICY_THREAT_LEVEL,
      POLICY_WAIVER_THREAT_LEVEL,
      COMPONENT_MAX_POLICY_THREAT_LEVEL,
      APPLICATION_VIOLATION_STATE_SORT_ORDINAL);

  private static final Set<FieldIdentifier> NUMERIC_DESC_SORT_FIELDS = Set.of(
      APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS,
      APPLICATION_MAX_POLICY_THREAT_LEVEL,
      POLICY_VIOLATION_THREAT_LEVEL,
      POLICY_WAIVER_CREATED_AT_EPOCH_MS,
      POLICY_WAIVER_THREAT_LEVEL,
      // Component max-threat sort (int doc-values twin); highest first.
      COMPONENT_MAX_POLICY_THREAT_LEVEL);

  /**
   * Sortable index fields backed by a numeric doc-values twin, sorted ASCENDING (soonest/lowest
   * first) with missing values placed last. WAIVER expiration sorts soonest-first (never-expires
   * last); the application violation-state ordinal sorts Open(0) before Waived(1) before Legacy(2),
   * with apps having no violation-state ordinal placed last.
   */
  private static final Set<FieldIdentifier> NUMERIC_ASC_SORT_FIELDS = Set.of(
      POLICY_WAIVER_EXPIRES_AT_EPOCH_MS,
      APPLICATION_VIOLATION_STATE_SORT_ORDINAL);

  /**
   * Sortable index fields backed by a <em>float</em> doc-values twin (encoded via
   * {@code NumericUtils.floatToSortableInt}), sorted descending (highest CVSS first). Distinct from
   * {@link #NUMERIC_DESC_SORT_FIELDS} because a float score must be compared as a float, not as the raw
   * long twin, to order values within an integer part correctly (7.5 before 7.1).
   */
  private static final Set<FieldIdentifier> FLOAT_SORT_FIELDS = Set.of(VULNERABILITY_SEVERITY);

  /**
   * Resolve the IQ-local index field backing an allowlisted (tab, sortKey), independent of
   * {@link #SORT_BY_FIELD_ENABLED}. Returns {@code null} when there is no mapping. Used by the
   * drift guard test to prove the allowlist and this map cannot diverge while the flag is off.
   */
  static FieldIdentifier sortableIndexFieldFor(final Tab tab, final String sortKey) {
    if (sortKey == null) {
      return null;
    }
    final String bareKey = sortKey.startsWith("-") ? sortKey.substring(1) : sortKey;
    return SORTABLE_FIELD_BY_KEY.getOrDefault(tab, Collections.emptyMap()).get(bareKey);
  }

  /**
   * Every index field reachable through a sortable (tab, sortKey) pair. Used by the drift-guard test that
   * checks each sortable field's indexed point width against {@link #INT_POINT_SORT_FIELDS}.
   */
  @VisibleForTesting
  static Set<FieldIdentifier> allSortableIndexFields() {
    Set<FieldIdentifier> fields = new LinkedHashSet<>();
    for (Map<String, FieldIdentifier> perTab : SORTABLE_FIELD_BY_KEY.values()) {
      fields.addAll(perTab.values());
    }
    return Collections.unmodifiableSet(fields);
  }

  private static final Map<Tab, Map<String, FieldIdentifier>> SORTABLE_FIELD_BY_KEY = buildSortableFieldMap();

  private static Map<Tab, Map<String, FieldIdentifier>> buildSortableFieldMap() {
    Map<Tab, Map<String, FieldIdentifier>> m = new EnumMap<>(Tab.class);
    m.put(APPLICATION, Map.of(
        "name", APPLICATION_NAME,
        "policyEvaluationStage", POLICY_EVALUATION_STAGE,
        // Default "latest evaluation" sort; numeric doc-values emitted by LuceneIndexingContext.
        "lastEvaluationTime", APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS,
        // A5: policy-threat-level desc (highest threat first), numeric max-threat twin.
        "policyThreatLevel", APPLICATION_MAX_POLICY_THREAT_LEVEL,
        // A6: violation-state asc (Open first), numeric worst-state-ordinal twin.
        "violationState", APPLICATION_VIOLATION_STATE_SORT_ORDINAL));
    // VIOLATION unions POLICY_VIOLATION + LEGAL_VIOLATION; both carry the policy-name field.
    // threat sorts POLICY_VIOLATION docs by threat level; LEGAL_VIOLATION docs carry no threat-level
    // doc-values, so they sort last under a threat sort (missing-value default).
    m.put(VIOLATION, Map.of(
        "name", POLICY_VIOLATION_POLICY_NAME,
        "threat", POLICY_VIOLATION_THREAT_LEVEL));
    // Component My-tab sorts. "name" -> component display name (keyword twin). "policyThreatLevel" ->
    // the denormalized max policy threat level on NON_VULNERABLE_COMPONENT docs (int numeric twin,
    // highest first). The prototype's other component sorts (trending/downloads/latest_release/dts)
    // are Catalog/federation attributes with no local doc field and are handled by the federation leg.
    m.put(COMPONENT, Map.of(
        "name", COMPONENT_NAME,
        "policyThreatLevel", COMPONENT_MAX_POLICY_THREAT_LEVEL));
    // Vulnerability My-tab sorts. "name" -> vulnerability id (keyword twin). "cvss" -> the CVSS
    // severity score, sorted on the float doc-values twin (highest first). The "published" sort has no
    // local backing field (local vuln docs carry no published date — see V2 gap), so it is not listed.
    m.put(VULNERABILITY, Map.of(
        "name", VULNERABILITY_ID,
        "cvss", VULNERABILITY_SEVERITY));
    // WAIVER default "created" (newest first) sorts on the created-at epoch-millis numeric twin;
    // "threat" (highest first) on the threat-level numeric twin; "expiration" (soonest first,
    // never-expires last) ASCENDING on the expires-at epoch-millis twin.
    m.put(WAIVER, Map.of(
        GlobalSearchSortAllowlist.WAIVER_CREATED, POLICY_WAIVER_CREATED_AT_EPOCH_MS,
        // "oldest" reuses the created-at twin but sorts ascending (see sortFor).
        GlobalSearchSortAllowlist.WAIVER_OLDEST, POLICY_WAIVER_CREATED_AT_EPOCH_MS,
        GlobalSearchSortAllowlist.WAIVER_THREAT, POLICY_WAIVER_THREAT_LEVEL,
        GlobalSearchSortAllowlist.WAIVER_EXPIRATION, POLICY_WAIVER_EXPIRES_AT_EPOCH_MS,
        // Ana Waivers list sort tokens (direction via optional "-" prefix in sortFor).
        "policyWaiverCreatedAt", POLICY_WAIVER_CREATED_AT_EPOCH_MS,
        "policyWaiverThreatLevel", POLICY_WAIVER_THREAT_LEVEL));
    return Collections.unmodifiableMap(m);
  }

  /**
   * Inputs for {@link #search(SearchInputs)}. {@code isSbomManagerMode} gates the license check and
   * item-type filtering, matching the v1 {@code searchIndex} API; controllers supply it from the
   * request's {@code ProductMode} or the tenant default.
   */
  public record SearchInputs(
      String query,
      Tab tab,
      Set<ItemType> itemTypes,
      int pageSize,
      String sortKey,
      String cursor,
      boolean isSbomManagerMode)
  {
    public SearchInputs {
      Objects.requireNonNull(tab, "tab");
      query = query == null ? "" : query;
      if (itemTypes == null) {
        itemTypes = Collections.emptySet();
      }
      else {
        if (itemTypes.stream().anyMatch(Objects::isNull)) {
          throw new IllegalArgumentException("itemTypes contains null elements");
        }
        itemTypes = Set.copyOf(itemTypes);
      }
    }

    /** Defaults {@code isSbomManagerMode} to {@code false}. */
    public SearchInputs(
        final String query,
        final Tab tab,
        final Set<ItemType> itemTypes,
        final int pageSize,
        final String sortKey,
        final String cursor)
    {
      this(query, tab, itemTypes, pageSize, sortKey, cursor, false);
    }
  }

  public record IqLocalRow(String source, SearchResultItemDTO row)
  {
  }

  /**
   * Response of {@link #search(SearchInputs)}. {@code exactTotalHits} is {@code false} when the
   * backend stopped counting at
   * {@link AbstractSearchIndexClient#GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP} (real total may be
   * higher — clients render "10000+"); {@code true} means {@code total} is exact.
   */
  public record IqLocalSearchResponse(
      List<IqLocalRow> rows,
      long total,
      boolean exactTotalHits,
      List<String> nextSearchAfter,
      String sortKey,
      List<String> warnings,
      String servingBackendId)
  {
    public IqLocalSearchResponse {
      rows = List.copyOf(rows);
      nextSearchAfter = nextSearchAfter == null ? List.of() : List.copyOf(nextSearchAfter);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    /** Without warnings or serving-backend id; retained for existing callers. */
    public IqLocalSearchResponse(
        final List<IqLocalRow> rows,
        final long total,
        final boolean exactTotalHits,
        final List<String> nextSearchAfter,
        final String sortKey)
    {
      this(rows, total, exactTotalHits, nextSearchAfter, sortKey, List.of(), null);
    }

    /** Without serving-backend id; retained for existing callers. */
    public IqLocalSearchResponse(
        final List<IqLocalRow> rows,
        final long total,
        final boolean exactTotalHits,
        final List<String> nextSearchAfter,
        final String sortKey,
        final List<String> warnings)
    {
      this(rows, total, exactTotalHits, nextSearchAfter, sortKey, warnings, null);
    }
  }
}
