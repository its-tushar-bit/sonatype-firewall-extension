/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.OrdinalMap;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LongValues;

/**
 * Counts distinct values of a single-valued sorted doc-values field among matching documents by
 * setting the corresponding global ordinals.
 */
final class DistinctSortedDocValuesCollector
    extends SimpleCollector
{
  private final String field;

  private final OrdinalMap ordinalMap;

  private final FixedBitSet seenOrds;

  private SortedDocValues values;

  private LongValues segmentToGlobalOrd;

  DistinctSortedDocValuesCollector(
      final String field,
      final OrdinalMap ordinalMap,
      final FixedBitSet seenOrds)
  {
    this.field = field;
    this.ordinalMap = ordinalMap;
    this.seenOrds = seenOrds;
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) throws IOException {
    values = context.reader().getSortedDocValues(field);
    segmentToGlobalOrd = ordinalMap == null ? null : ordinalMap.getGlobalOrds(context.ord);
  }

  @Override
  public void collect(final int doc) throws IOException {
    if (values == null || !values.advanceExact(doc)) {
      return;
    }
    int segmentOrd = values.ordValue();
    int globalOrd = segmentToGlobalOrd == null ? segmentOrd : (int) segmentToGlobalOrd.get(segmentOrd);
    seenOrds.set(globalOrd);
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }
}
