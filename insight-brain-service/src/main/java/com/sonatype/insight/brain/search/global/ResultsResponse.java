/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;

/**
 * Response body for {@code GET /rest/search/results}.
 *
 * <p>
 * {@code totalEstimate} is capped at the value of
 * {@code AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP} (10000): exact below the cap,
 * literally {@code 10000} above it (the frontend renders that as {@code "10,000+"}).
 *
 * <p>
 * For {@link Tab#ALL} responses, {@code totalEstimate} is the sum of per-section capped totals re-clamped
 * to 10000. Each per-section total is itself capped at 10000 by the index client. As a consequence the
 * displayed "10,000+" can hide a much larger cross-section total — e.g. two sections each with 8,000 real
 * hits yield 16,000 raw, sum-capped to 10000.
 *
 * <p>
 * {@code nextSearchAfter} is the opaque cursor to use for the next page; {@code null} when the current
 * page is the last one for this tab.
 *
 * <p>
 * {@code warnings} is a list of human-readable warnings from the parser + compiler pipeline (e.g.
 * unknown filter keys that were skipped). The same list is mirrored in the {@code X-Search-Warnings}
 * response header for clients that prefer to read it out-of-band; the body copy exists because some HTTP
 * clients drop non-standard headers.
 *
 * <p>
 * {@code catalogAvailable} is {@code false} when the catalog source was degraded for this response (off,
 * 5xx, 429, timeout). It lets the frontend distinguish "catalog returned no rows" from "catalog was
 * unavailable" without string-matching {@code warnings}. Always {@code true} for IQ-local-only responses.
 *
 * <p>
 * {@code tabCounts} carries the per-tab capped total for each tab (keyed by {@link Tab}, serialized as
 * the uppercase enum name to match the {@code tab} field), so the results page can render a count badge
 * on each tab from a single request regardless of which tab was asked for. Each value is capped at 10000
 * exactly like {@code totalEstimate}. Responses built by {@code ResultsService} always carry the field;
 * the shorter convenience constructors leave it null and it is then omitted from the JSON entirely, so a
 * consumer must tolerate an absent {@code tabCounts} as well as absent individual keys. An individual
 * entry is omitted when that tab's count is unavailable, so a consumer renders a placeholder for a
 * missing key rather than treating it as {@code 0} (a present {@code 0} means "genuinely no hits").
 *
 * <p>
 * The ALL badge must read {@code tabCounts[ALL]} and render a placeholder when it is absent — do not
 * fall back to {@code totalEstimate}. The two answer different questions: {@code totalEstimate} is
 * always emitted and on an {@link Tab#ALL} response sums whatever sections responded, so falling back
 * to it would display a silent undercount. On a single-tab response {@code totalEstimate} equals
 * {@code tabCounts[tab]}, not {@code tabCounts[ALL]}.
 *
 * <p>
 * {@code facets} is the per-tab facet map (facet key -&gt; ordered buckets), reusing the index-query
 * {@link IndexQueryFacetBucket} shape (value/displayName/count). It is populated ONLY for a single
 * IQ-local entity-tab request made with {@code includeFacets=true}; it is {@code null} for the
 * {@link Tab#ALL} tab, for count-only probes, and for catalog-source responses (the catalog leg is
 * HDS-backed and does not emit IQ-local facets). Whole-corpus, RBAC-scoped bucket counts round-trip
 * through the same filters the rail renders, matching the index-query endpoint.
 *
 * @see ResultsService the authoritative capping and omission rules live on its {@code cappedTabCounts}
 *      builder
 */
@JsonPropertyOrder({"tab", "page", "pageSize", "totalEstimate", "tabCounts", "results", "nextSearchAfter",
  "warnings", "catalogAvailable", "facets"})
@JsonInclude(Include.NON_NULL)
public final class ResultsResponse
{
  private final Tab tab;

  private final int page;

  private final int pageSize;

  private final long totalEstimate;

  private final Map<Tab, Long> tabCounts;

  private final List<ResultRow> results;

  private final String nextSearchAfter;

  private final List<String> warnings;

  private final boolean catalogAvailable;

  private final Map<String, List<IndexQueryFacetBucket>> facets;

  public ResultsResponse(
      Tab tab,
      int page,
      int pageSize,
      long totalEstimate,
      List<ResultRow> results,
      String nextSearchAfter)
  {
    this(tab, page, pageSize, totalEstimate, null, results, nextSearchAfter, List.of(), true);
  }

  public ResultsResponse(
      Tab tab,
      int page,
      int pageSize,
      long totalEstimate,
      List<ResultRow> results,
      String nextSearchAfter,
      List<String> warnings)
  {
    this(tab, page, pageSize, totalEstimate, null, results, nextSearchAfter, warnings, true);
  }

  public ResultsResponse(
      Tab tab,
      int page,
      int pageSize,
      long totalEstimate,
      Map<Tab, Long> tabCounts,
      List<ResultRow> results,
      String nextSearchAfter,
      List<String> warnings,
      boolean catalogAvailable)
  {
    this(tab, page, pageSize, totalEstimate, tabCounts, results, nextSearchAfter, warnings, catalogAvailable,
        null);
  }

  public ResultsResponse(
      Tab tab,
      int page,
      int pageSize,
      long totalEstimate,
      Map<Tab, Long> tabCounts,
      List<ResultRow> results,
      String nextSearchAfter,
      List<String> warnings,
      boolean catalogAvailable,
      Map<String, List<IndexQueryFacetBucket>> facets)
  {
    this.tab = Objects.requireNonNull(tab, "tab");
    this.page = page;
    this.pageSize = pageSize;
    this.totalEstimate = totalEstimate;
    // EnumMap keeps the badge keys in Tab declaration order so the serialized JSON is stable across
    // requests, matching the deterministic ordering the warnings list gets from its LinkedHashSet.
    // Map.copyOf would drop that order. Built key-by-key because EnumMap's copy constructor rejects an
    // empty non-EnumMap source.
    this.tabCounts = tabCounts == null ? null : unmodifiableTabCounts(tabCounts);
    this.results = results == null ? List.of() : List.copyOf(results);
    this.nextSearchAfter = nextSearchAfter;
    this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    this.catalogAvailable = catalogAvailable;
    // Null means "facets not requested / not applicable" (ALL tab, catalog source, count-only probe);
    // it serializes away under NON_NULL so existing clients see no new field. An empty map means
    // "requested, but this tab has no facet set".
    // LinkedHashMap preserves the FACET_FIELDS declaration order computeFacets builds, so the serialized
    // facet keys are stable across requests and a rail rendered in response order does not reshuffle.
    // Map.copyOf would drop that order, the same reason tabCounts uses an EnumMap above.
    this.facets = facets == null ? null : unmodifiableFacets(facets);
  }

  /**
   * Defends the bucket lists as well as the outer key set: {@code unmodifiableMap} leaves the value
   * instances mutable, so a caller holding the response could still add buckets to the same lists the
   * facet computation returned. Each value is copied with {@code List.copyOf}, matching how
   * {@code results} and {@code warnings} are defended, while the {@link LinkedHashMap} keeps the
   * declaration order a rebuild via {@code Map.copyOf} would lose.
   */
  private static Map<String, List<IndexQueryFacetBucket>> unmodifiableFacets(
      final Map<String, List<IndexQueryFacetBucket>> source)
  {
    final Map<String, List<IndexQueryFacetBucket>> ordered = new LinkedHashMap<>();
    for (Map.Entry<String, List<IndexQueryFacetBucket>> entry : source.entrySet()) {
      final List<IndexQueryFacetBucket> buckets = entry.getValue();
      ordered.put(entry.getKey(), buckets == null ? List.of() : List.copyOf(buckets));
    }
    return Collections.unmodifiableMap(ordered);
  }

  private static Map<Tab, Long> unmodifiableTabCounts(final Map<Tab, Long> source) {
    final Map<Tab, Long> ordered = new EnumMap<>(Tab.class);
    ordered.putAll(source);
    return Collections.unmodifiableMap(ordered);
  }

  public Tab getTab() {
    return tab;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public long getTotalEstimate() {
    return totalEstimate;
  }

  @JsonProperty("tabCounts")
  public Map<Tab, Long> getTabCounts() {
    return tabCounts;
  }

  public List<ResultRow> getResults() {
    return results;
  }

  public String getNextSearchAfter() {
    return nextSearchAfter;
  }

  @JsonProperty("warnings")
  public List<String> getWarnings() {
    return warnings;
  }

  @JsonProperty("catalogAvailable")
  public boolean isCatalogAvailable() {
    return catalogAvailable;
  }

  @JsonProperty("facets")
  public Map<String, List<IndexQueryFacetBucket>> getFacets() {
    return facets;
  }
}
