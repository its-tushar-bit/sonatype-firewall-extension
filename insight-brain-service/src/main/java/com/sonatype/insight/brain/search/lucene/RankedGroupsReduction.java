/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.FixedBitSet;

final class RankedGroupsReduction
{
  private RankedGroupsReduction() {
  }

  /**
   * Walks the seen ordinals once to produce the distinct count, the band counts and a bounded top-N.
   * Ordinal order is lower-cased term order, so an ascending ordinal is the case-insensitive
   * tie-break the ranked contract promises, and only surviving ordinals are resolved to terms.
   */
  static RankedGroupsResult reduceRankedGroups(
      final SortedDocValues globalGroups,
      final float[] maxByOrd,
      final FixedBitSet seen,
      final int ordCount,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands) throws IOException
  {
    Map<String, Long> bandCounts = new LinkedHashMap<>();
    metricBands.keySet().forEach(band -> bandCounts.put(band, 0L));

    Comparator<Integer> worstFirst = rankedOrdComparator(maxByOrd, ascending).reversed();
    PriorityQueue<Integer> heap = new PriorityQueue<>(worstFirst);

    long distinct = 0;
    long unbanded = 0;
    // The bit set spans the whole term dictionary, of which a filtered read typically matches a small
    // part. Jumping set bit to set bit skips the gaps a word at a time, so the walk costs what the
    // query matched rather than what the estate has ever recorded.
    for (int ord = nextSeenOrd(seen, 0, ordCount); ord < ordCount; ord = nextSeenOrd(seen, ord + 1, ordCount)) {
      distinct++;
      String band = bandFor(metricBands, maxByOrd[ord]);
      if (band == null) {
        unbanded++;
      }
      else {
        bandCounts.merge(band, 1L, Long::sum);
      }
      heap.add(ord);
      if (heap.size() > limit) {
        heap.poll();
      }
    }

    List<Integer> ordered = new ArrayList<>(heap);
    ordered.sort(rankedOrdComparator(maxByOrd, ascending));
    List<RankedGroup> groups = new ArrayList<>(ordered.size());
    for (int ord : ordered) {
      float value = maxByOrd[ord];
      groups.add(new RankedGroup(
          globalGroups.lookupOrd(ord).utf8ToString(),
          Float.isNaN(value) ? null : value));
    }
    return new RankedGroupsResult(groups, distinct, true, bandCounts, unbanded);
  }

  /**
   * Next matched ordinal at or after {@code from}, or {@code ordCount} once none remain. The bit set is
   * allocated with at least one bit even for an empty term dictionary, so {@code from} is range-checked
   * here rather than handed to a lookup that rejects it. Lucene {@link FixedBitSet#nextSetBit(int)}
   * returns {@link org.apache.lucene.search.DocIdSetIterator#NO_MORE_DOCS} ({@code Integer.MAX_VALUE})
   * when exhausted — normalize that sentinel to {@code ordCount} so the caller's {@code ord < ordCount}
   * loop terminates without relying on the numeric magnitude of {@code NO_MORE_DOCS}.
   */
  private static int nextSeenOrd(final FixedBitSet seen, final int from, final int ordCount) {
    if (from >= ordCount) {
      return ordCount;
    }
    int next = seen.nextSetBit(from);
    return next >= ordCount ? ordCount : next;
  }

  /** Best-first: metric-less groups always last, then metric by direction, then ordinal ascending. */
  private static Comparator<Integer> rankedOrdComparator(final float[] maxByOrd, final boolean ascending) {
    return (left, right) -> {
      int leftOrd = left;
      int rightOrd = right;
      float leftValue = maxByOrd[leftOrd];
      float rightValue = maxByOrd[rightOrd];
      boolean leftMissing = Float.isNaN(leftValue);
      boolean rightMissing = Float.isNaN(rightValue);
      if (leftMissing || rightMissing) {
        if (leftMissing && rightMissing) {
          return Integer.compare(leftOrd, rightOrd);
        }
        return leftMissing ? 1 : -1;
      }
      int byMetric = ascending
          ? Float.compare(leftValue, rightValue)
          : Float.compare(rightValue, leftValue);
      return byMetric != 0 ? byMetric : Integer.compare(leftOrd, rightOrd);
    };
  }

  /** Half-open {@code [min, max)} band containing {@code value}, or null when unscored / out of range. */
  private static String bandFor(final Map<String, float[]> metricBands, final float value) {
    if (Float.isNaN(value)) {
      return null;
    }
    for (Map.Entry<String, float[]> band : metricBands.entrySet()) {
      float[] bounds = band.getValue();
      if (value >= bounds[0] && value < bounds[1]) {
        return band.getKey();
      }
    }
    return null;
  }
}
