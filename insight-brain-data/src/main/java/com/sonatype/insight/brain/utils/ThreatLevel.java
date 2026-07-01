/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @since 1.33
 */
public enum ThreatLevel
{
  LOW("Low", 1),
  MODERATE("Moderate", 3),
  SEVERE("Severe", 7),
  CRITICAL("Critical", 10);

  private final String displayName;

  private final int maxInclusive;

  ThreatLevel(final String displayName, final int maxInclusive) {
    this.displayName = displayName;
    this.maxInclusive = maxInclusive;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getMaxInclusive() {
    return maxInclusive;
  }

  public int getMinInclusive() {
    int ordinal = ordinal();
    return ordinal == 0 ? 0 : values()[ordinal - 1].maxInclusive + 1;
  }

  public static ThreatLevel from(int threatLevel) {
    for (ThreatLevel level : values()) {
      if (threatLevel <= level.maxInclusive) {
        return level;
      }
    }
    return CRITICAL;
  }

  /**
   * Inclusive Lucene/OpenSearch numeric ranges for dashboard violation breakdown buckets.
   * Outer bands are open-ended so {@code total == sum(breakdown)} even for corrupt index values
   * outside the policy UI's 0–10 range. Keys are stable lowercase names ({@code critical}, …).
   * <p>
   * Returned band bounds are defensive copies; mutating an {@code int[]} from the map does not
   * affect subsequent calls or the canonical band definitions.
   */
  public static Map<String, int[]> searchAggregationBands() {
    Map<String, int[]> bands = new LinkedHashMap<>();
    CANONICAL_SEARCH_AGGREGATION_BANDS.forEach((key, bounds) -> bands.put(key, bounds.clone()));
    return Collections.unmodifiableMap(bands);
  }

  private static final Map<String, int[]> CANONICAL_SEARCH_AGGREGATION_BANDS = buildCanonicalSearchAggregationBands();

  private static Map<String, int[]> buildCanonicalSearchAggregationBands() {
    Map<String, int[]> bands = new LinkedHashMap<>();
    for (ThreatLevel level : values()) {
      bands.put(level.name().toLowerCase(Locale.ROOT), searchAggregationBand(level));
    }
    return Collections.unmodifiableMap(bands);
  }

  private static int[] searchAggregationBand(ThreatLevel level) {
    int min = level.getMinInclusive();
    int max = level.getMaxInclusive();
    if (level == LOW) {
      min = Integer.MIN_VALUE;
    }
    if (level == CRITICAL) {
      max = Integer.MAX_VALUE;
    }
    return new int[]{min, max};
  }
}
