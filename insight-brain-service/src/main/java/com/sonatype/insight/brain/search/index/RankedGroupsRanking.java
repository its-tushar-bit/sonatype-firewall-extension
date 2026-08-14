/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * Shared ranking / banding rules for {@link RankedGroupsResult} so Lucene and OpenSearch stay aligned.
 * <p>
 * Tie-break compares UTF-8 bytes of the group key (Lucene term ordinals are BytesRef / UTF-8 order).
 * Metrics are assumed non-negative for production fields; {@link Float#NaN} means unscored and sorts last.
 */
public final class RankedGroupsRanking
{
  private RankedGroupsRanking() {
  }

  /** Half-open {@code [min, max)} band containing {@code value}, or null when unscored / out of range. */
  public static String bandFor(final Map<String, float[]> metricBands, final float value) {
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

  /**
   * Unscored ({@link Float#NaN}) always last regardless of direction, then metric by direction.
   * Tie-breaks are caller-specific (UTF-8 key vs Lucene ordinal).
   *
   * @return 0 when both metrics are present and equal, or both unscored — caller applies tie-break
   */
  public static int compareMetric(
      final float leftMetric,
      final float rightMetric,
      final boolean ascending)
  {
    boolean leftMissing = Float.isNaN(leftMetric);
    boolean rightMissing = Float.isNaN(rightMetric);
    if (leftMissing || rightMissing) {
      if (leftMissing && rightMissing) {
        return 0;
      }
      return leftMissing ? 1 : -1;
    }
    return ascending
        ? Float.compare(leftMetric, rightMetric)
        : Float.compare(rightMetric, leftMetric);
  }

  /**
   * Best-first compare: unscored ({@link Float#NaN}) always last, then metric by direction, then
   * UTF-8 group-key ascending.
   */
  public static int compareMetricThenKey(
      final float leftMetric,
      final String leftKey,
      final float rightMetric,
      final String rightKey,
      final boolean ascending)
  {
    int byMetric = compareMetric(leftMetric, rightMetric, ascending);
    return byMetric != 0 ? byMetric : compareUtf8(leftKey, rightKey);
  }

  /**
   * UTF-8 byte order, matching Lucene {@code BytesRef} / term-ordinal ordering for the same string.
   * Uses unsigned byte comparison — {@link Arrays#compare(byte[], byte[])} is signed and would
   * diverge from Lucene for any byte {@code >= 0x80}.
   */
  public static int compareUtf8(final String left, final String right) {
    return Arrays.compareUnsigned(
        left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8));
  }
}
