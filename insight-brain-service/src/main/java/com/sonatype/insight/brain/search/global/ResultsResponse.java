/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
 */
@JsonPropertyOrder({"tab", "page", "pageSize", "totalEstimate", "results", "nextSearchAfter", "warnings",
  "catalogAvailable"})
@JsonInclude(Include.NON_NULL)
public final class ResultsResponse
{
  private final Tab tab;

  private final int page;

  private final int pageSize;

  private final long totalEstimate;

  private final List<ResultRow> results;

  private final String nextSearchAfter;

  private final List<String> warnings;

  private final boolean catalogAvailable;

  public ResultsResponse(
      Tab tab,
      int page,
      int pageSize,
      long totalEstimate,
      List<ResultRow> results,
      String nextSearchAfter)
  {
    this(tab, page, pageSize, totalEstimate, results, nextSearchAfter, List.of(), true);
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
    this(tab, page, pageSize, totalEstimate, results, nextSearchAfter, warnings, true);
  }

  public ResultsResponse(
      Tab tab,
      int page,
      int pageSize,
      long totalEstimate,
      List<ResultRow> results,
      String nextSearchAfter,
      List<String> warnings,
      boolean catalogAvailable)
  {
    this.tab = Objects.requireNonNull(tab, "tab");
    this.page = page;
    this.pageSize = pageSize;
    this.totalEstimate = totalEstimate;
    this.results = results == null ? List.of() : List.copyOf(results);
    this.nextSearchAfter = nextSearchAfter;
    this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    this.catalogAvailable = catalogAvailable;
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
}
