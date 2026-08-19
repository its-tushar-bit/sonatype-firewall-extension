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
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LongValues;
import org.apache.lucene.util.NumericUtils;

/**
 * Reduces matching documents to one maximum metric value per distinct group, reading only doc-values.
 * <p>
 * {@code maxByOrd} is indexed by global group ordinal and MUST arrive filled with {@link Float#NaN}.
 * {@code NaN} is the sentinel for "no document in this group carried the metric", and it has to be a
 * sentinel that no real metric can take: a zero-filled array would be indistinguishable from a genuine
 * CVSS score of {@code 0.0}, which is a valid score in the {@code none} band. Because every comparison
 * against {@code NaN} is false, the maximum is updated through an explicit {@link Float#isNaN} check
 * rather than {@code Math.max}.
 * <p>
 * {@code seen} marks every group ordinal encountered, including groups with no metric value, and is
 * therefore the exact distinct group count for the query.
 */
final class MaxMetricByGroupCollector
    extends SimpleCollector
{
  private final String groupField;

  private final String metricField;

  private final OrdinalMap ordinalMap;

  private final float[] maxByOrd;

  private final FixedBitSet seen;

  private SortedDocValues groupValues;

  private SortedNumericDocValues metricValues;

  private LongValues segmentToGlobalOrd;

  MaxMetricByGroupCollector(
      final String groupField,
      final String metricField,
      final OrdinalMap ordinalMap,
      final float[] maxByOrd,
      final FixedBitSet seen)
  {
    this.groupField = groupField;
    this.metricField = metricField;
    this.ordinalMap = ordinalMap;
    this.maxByOrd = maxByOrd;
    this.seen = seen;
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) throws IOException {
    groupValues = context.reader().getSortedDocValues(groupField);
    metricValues = context.reader().getSortedNumericDocValues(metricField);
    // A single-segment reader needs no mapping: its ordinals are already global.
    segmentToGlobalOrd = ordinalMap == null ? null : ordinalMap.getGlobalOrds(context.ord);
  }

  @Override
  public void collect(final int doc) throws IOException {
    if (groupValues == null || !groupValues.advanceExact(doc)) {
      return;
    }
    int segmentOrd = groupValues.ordValue();
    int globalOrd = segmentToGlobalOrd == null ? segmentOrd : (int) segmentToGlobalOrd.get(segmentOrd);
    seen.set(globalOrd);

    if (metricValues == null || !metricValues.advanceExact(doc)) {
      return;
    }
    for (int i = 0, count = metricValues.docValueCount(); i < count; i++) {
      float value = NumericUtils.sortableIntToFloat((int) metricValues.nextValue());
      float current = maxByOrd[globalOrd];
      if (Float.isNaN(current) || value > current) {
        maxByOrd[globalOrd] = value;
      }
    }
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }
}
