/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

/**
 * Result payload of {@link IqLocalSearchService#search(IqLocalSearchService.SearchInputs)}: row DTOs,
 * capped total-hits, and the next-page {@code searchAfter} tuple.
 *
 * <p>
 * A class, not a record, because record-generated {@code equals}/{@code hashCode} are not meaningful
 * for the Lucene-document-backed {@link SearchResultItemDTO} rows.
 */
public final class GlobalSearchResult
{
  private final List<SearchResultItemDTO> rows;

  private final long totalHits;

  private final List<String> nextSearchAfter;

  private final boolean exactTotalHits;

  private final String servingBackendId;

  /**
   * Convenience constructor that defaults {@code exactTotalHits} to {@code true}. Any backend whose
   * count stops at a cap (so the real total may be higher) MUST use the four-argument constructor
   * with {@code exactTotalHits=false} instead, or callers will render the capped total as exact.
   */
  public GlobalSearchResult(
      final List<SearchResultItemDTO> rows,
      final long totalHits,
      final List<String> nextSearchAfter)
  {
    this(rows, totalHits, nextSearchAfter, true);
  }

  public GlobalSearchResult(
      final List<SearchResultItemDTO> rows,
      final long totalHits,
      final List<String> nextSearchAfter,
      final boolean exactTotalHits)
  {
    this(rows, totalHits, nextSearchAfter, exactTotalHits, null);
  }

  public GlobalSearchResult(
      final List<SearchResultItemDTO> rows,
      final long totalHits,
      final List<String> nextSearchAfter,
      final boolean exactTotalHits,
      final String servingBackendId)
  {
    this.rows = Collections.unmodifiableList(Objects.requireNonNull(rows, "rows"));
    if (totalHits < 0) {
      throw new IllegalArgumentException("totalHits must be >= 0");
    }
    this.totalHits = totalHits;
    this.nextSearchAfter = nextSearchAfter == null ? Collections.emptyList() : List.copyOf(nextSearchAfter);
    this.exactTotalHits = exactTotalHits;
    this.servingBackendId = servingBackendId;
  }

  public List<SearchResultItemDTO> rows() {
    return rows;
  }

  /** Capped at {@code GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP}. */
  public long totalHits() {
    return totalHits;
  }

  /** Empty when there is no further page. */
  public List<String> nextSearchAfter() {
    return nextSearchAfter;
  }

  /** {@code false} when the backend stopped counting at the cap and the real total may be higher. */
  public boolean exactTotalHits() {
    return exactTotalHits;
  }

  /**
   * Backend that actually produced this page (its {@code backendId()}), or {@code null} when the
   * serving backend is the request's default. Used to pin the next-page cursor to the serving
   * backend so a cross-backend switch on the follow-up request is rejected as a stale cursor rather
   * than silently mis-paginated. Set by {@code HybridSearchIndexClient} when it falls back to the
   * secondary; single-backend clients leave it {@code null}.
   */
  public String servingBackendId() {
    return servingBackendId;
  }
}
