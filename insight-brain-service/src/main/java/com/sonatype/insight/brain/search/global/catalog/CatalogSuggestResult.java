/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.List;

import com.sonatype.insight.brain.search.global.SuggestRow;

/**
 * Outcome of a catalog suggest call. Distinguishes "catalog returned no rows" from "catalog failed /
 * timed out / was unentitled". Both cases produce an empty {@link #rows()} list, but only the former
 * sets {@link #available()} to {@code true}.
 *
 * <p>
 * The suggest service uses this to drive the {@code catalogAvailable} field on the response. The four
 * states collapse onto this two-state record:
 *
 * <ul>
 * <li>HTTP 200 with hits &rarr; {@code (rows = tagged hits, available = true)}.</li>
 * <li>HTTP 200 empty &rarr; {@code (rows = [], available = true)}.</li>
 * <li>HTTP 5xx / 429 / timeout / unentitled &rarr; {@code (rows = [], available = false)}.</li>
 * </ul>
 */
public record CatalogSuggestResult(List<SuggestRow> rows, boolean available)
{
  public CatalogSuggestResult {
    rows = rows == null ? List.of() : List.copyOf(rows);
    if (!available && !rows.isEmpty()) {
      throw new IllegalArgumentException("unavailable CatalogSuggestResult must carry no rows");
    }
  }

  public static CatalogSuggestResult unavailable() {
    return new CatalogSuggestResult(List.of(), false);
  }

  public static CatalogSuggestResult available(final List<SuggestRow> rows) {
    return new CatalogSuggestResult(rows, true);
  }
}
