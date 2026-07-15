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
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.global.Tab.APPLICATION;
import static com.sonatype.insight.brain.search.global.Tab.COMPONENT;
import static com.sonatype.insight.brain.search.global.Tab.VIOLATION;
import static com.sonatype.insight.brain.search.global.Tab.VULNERABILITY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ITEM_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_EVALUATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_ID;
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
 * Sort handling: the sort key is validated against {@link GlobalSearchSortAllowlist}, but physical
 * sort needs doc-values that {@link com.sonatype.insight.brain.search.lucene.DocumentBuilder} does
 * not yet emit; until then {@link #sortFor(Tab, String)} resolves every sort to relevance. The
 * validated key is still echoed in {@link IqLocalSearchResponse#sortKey}.
 */
@Named
@Singleton
public class IqLocalSearchService
{
  private static final Logger log = LoggerFactory.getLogger(IqLocalSearchService.class);

  public static final int DEFAULT_PER_TYPE_PAGE_SIZE = 50;

  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Flip once {@code DocumentBuilder} emits doc-values for every field in {@link #SORTABLE_FIELD_BY_KEY}.
   * TODO(CLM-41642): enable field sort and remove the relevance-only short-circuit in {@link #sortFor}.
   */
  static final boolean SORT_BY_FIELD_ENABLED = false;

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
    if (!searchIndexClient.isGlobalSearchEnabled()) {
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
      return new IqLocalSearchResponse(List.of(), 0L, true, List.of(), validatedSortKey, List.of(warning));
    }

    ParsedQuery parsed = QueryParser.parse(inputs.query());
    ComposedQuery composed = composeBaseQuery(parsed.ast(), types);
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
        validatedSortKey, warnings);
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

  ComposedQuery composeBaseQuery(final AstNode ast, final Set<ItemType> types) {
    LinkedHashSet<String> warnings = new LinkedHashSet<>();
    // Single-type fast path: skip the top-level SHOULD wrap (equivalent semantics) to save a
    // clause against the Lucene max-clause budget.
    if (types.size() == 1) {
      ItemType only = types.iterator().next();
      CompiledQuery compiled = QueryCompiler.compile(ast, only, fieldMap);
      warnings.addAll(compiled.warnings());
      return new ComposedQuery(wrapWithTypeFilter(only, compiled.luceneQuery()), List.copyOf(warnings));
    }
    BooleanQuery.Builder top = new BooleanQuery.Builder();
    for (ItemType type : types) {
      CompiledQuery compiled = QueryCompiler.compile(ast, type, fieldMap);
      warnings.addAll(compiled.warnings());
      top.add(wrapWithTypeFilter(type, compiled.luceneQuery()), Occur.SHOULD);
    }
    return new ComposedQuery(top.build(), List.copyOf(warnings));
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
   * (relevance-only) while {@link #SORT_BY_FIELD_ENABLED} is off. Once enabled, a null index field
   * for an allowlisted key is an invariant violation — a missing {@link #SORTABLE_FIELD_BY_KEY} entry.
   */
  static Sort sortFor(final Tab tab, final String sortKey) {
    if (sortKey == null || GlobalSearchSortAllowlist.RELEVANCE.equals(sortKey)) {
      return null;
    }
    if (!SORT_BY_FIELD_ENABLED) {
      return null;
    }
    FieldIdentifier indexField = SORTABLE_FIELD_BY_KEY.getOrDefault(tab, Collections.emptyMap()).get(sortKey);
    if (indexField == null) {
      log.warn("Sort key '{}' is allowlisted for tab {} but has no IQ-local index field; "
          + "this is an invariant violation — falling back to relevance.", sortKey, tab);
      return null;
    }
    return new Sort(new SortField(indexField.label, SortField.Type.STRING));
  }

  /**
   * Resolve the IQ-local index field backing an allowlisted (tab, sortKey), independent of
   * {@link #SORT_BY_FIELD_ENABLED}. Returns {@code null} when there is no mapping. Used by the
   * drift guard test to prove the allowlist and this map cannot diverge while the flag is off.
   */
  static FieldIdentifier sortableIndexFieldFor(final Tab tab, final String sortKey) {
    return SORTABLE_FIELD_BY_KEY.getOrDefault(tab, Collections.emptyMap()).get(sortKey);
  }

  private static final Map<Tab, Map<String, FieldIdentifier>> SORTABLE_FIELD_BY_KEY = buildSortableFieldMap();

  private static Map<Tab, Map<String, FieldIdentifier>> buildSortableFieldMap() {
    Map<Tab, Map<String, FieldIdentifier>> m = new EnumMap<>(Tab.class);
    m.put(APPLICATION, Map.of(
        "name", APPLICATION_NAME,
        "policyEvaluationStage", POLICY_EVALUATION_STAGE));
    // VIOLATION unions POLICY_VIOLATION + LEGAL_VIOLATION; both carry the policy-name field.
    m.put(VIOLATION, Map.of("name", POLICY_VIOLATION_POLICY_NAME));
    m.put(COMPONENT, Map.of("name", COMPONENT_NAME));
    m.put(VULNERABILITY, Map.of("name", VULNERABILITY_ID));
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
      List<String> warnings)
  {
    public IqLocalSearchResponse {
      rows = List.copyOf(rows);
      nextSearchAfter = nextSearchAfter == null ? List.of() : List.copyOf(nextSearchAfter);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    /** Without warnings; retained for existing callers. */
    public IqLocalSearchResponse(
        final List<IqLocalRow> rows,
        final long total,
        final boolean exactTotalHits,
        final List<String> nextSearchAfter,
        final String sortKey)
    {
      this(rows, total, exactTotalHits, nextSearchAfter, sortKey, List.of());
    }
  }
}
