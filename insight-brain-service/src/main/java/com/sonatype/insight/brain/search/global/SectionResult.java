/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.Objects;

/**
 * Internal wrapper carrying a single section's rows plus the metadata needed to assemble a
 * {@link ResultsResponse}.
 *
 * <p>
 * For per-tab requests this represents the entire tab's slice. For {@link Tab#ALL} requests one section
 * is produced per entity type and {@link AllTabPacker} stitches them together in the fixed presentation
 * order.
 *
 * @param tab the (sub-)tab this slice belongs to
 * @param rows the rows in this slice, already sorted in the engine's natural order
 * @param totalEstimate total hits estimate for this tab, capped at the 10k track_total_hits cap
 * @param nextSearchAfter opaque cursor for the next page of this tab, or {@code null} if this is the last
 *          page
 * @param catalogAvailable whether the catalog source served this slice. {@code false} marks a degraded
 *          catalog section (off, 5xx, 429, timeout); surfaced on {@link ResultsResponse#isCatalogAvailable()}
 *          (single-tab) and OR-reduced across catalog sections for the ALL tab. Always {@code true} for
 *          IQ-local tabs.
 */
public record SectionResult(
    Tab tab,
    List<ResultRow> rows,
    long totalEstimate,
    String nextSearchAfter,
    boolean catalogAvailable,
    List<String> warnings)
{
  public SectionResult {
    Objects.requireNonNull(tab, "tab");
    rows = rows == null ? List.of() : List.copyOf(rows);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  /** Back-compat constructor without warnings, defaults to empty list. */
  public SectionResult(
      Tab tab,
      List<ResultRow> rows,
      long totalEstimate,
      String nextSearchAfter,
      boolean catalogAvailable)
  {
    this(tab, rows, totalEstimate, nextSearchAfter, catalogAvailable, List.of());
  }

  /**
   * Empty section with {@code catalogAvailable=true}. Use {@link #empty(Tab, boolean)} when the caller
   * needs to surface a catalog-unavailable empty section.
   */
  public static SectionResult empty(Tab tab) {
    return empty(tab, true);
  }

  /**
   * Empty section with the supplied {@code catalogAvailable} flag. Catalog callers MUST use this
   * overload when the catalog source is unavailable so the dispatcher can flip the response-level flag.
   */
  public static SectionResult empty(Tab tab, boolean catalogAvailable) {
    return new SectionResult(tab, List.of(), 0L, null, catalogAvailable);
  }
}
