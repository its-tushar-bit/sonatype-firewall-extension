/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import com.sonatype.insight.brain.search.index.FieldIdentifier;

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;

/**
 * Shared Lucene numeric {@link SortField} constructors for Martha {@link IndexReadSession} sorts.
 * <p>
 * Threat / ordinal fields are indexed as {@code IntPoint}; epochs as {@code LongPoint}. Lucene's
 * {@code NumericComparator} requires the sort type width to match the point width
 * ({@code Type.INT} → 4 bytes, {@code Type.LONG} → 8 bytes). Missing values always sort last.
 */
public final class IndexSessionNumericSorts
{
  private IndexSessionNumericSorts() {
  }

  /** Sort over an {@code IntPoint}-backed field; missing values sort last. */
  public static SortedNumericSortField intField(final FieldIdentifier fieldIdentifier, final boolean reverse) {
    SortedNumericSortField sortField = new SortedNumericSortField(
        fieldIdentifier.label,
        SortField.Type.INT,
        reverse);
    sortField.setMissingValue(reverse ? Integer.MIN_VALUE : Integer.MAX_VALUE);
    return sortField;
  }

  /** Sort over a {@code LongPoint}-backed field; missing values sort last. */
  public static SortedNumericSortField longField(final FieldIdentifier fieldIdentifier, final boolean reverse) {
    SortedNumericSortField sortField = new SortedNumericSortField(
        fieldIdentifier.label,
        SortField.Type.LONG,
        reverse);
    // Ascending: MAX_VALUE is last. Descending: MIN_VALUE is last (after positive timestamps).
    sortField.setMissingValue(reverse ? Long.MIN_VALUE : Long.MAX_VALUE);
    return sortField;
  }

  public static SortField documentKeyAscending() {
    return new SortField(FieldIdentifier.DOCUMENT_KEY.label, SortField.Type.STRING);
  }
}
