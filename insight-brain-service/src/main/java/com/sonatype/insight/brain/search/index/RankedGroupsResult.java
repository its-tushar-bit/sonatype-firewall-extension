/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of ranking distinct groups by a per-group maximum metric.
 *
 * @param groups ranked groups, at most the requested limit, best first
 * @param distinctGroupCount distinct groups matching the query, ignoring the limit
 * @param distinctGroupCountExact false when the backend reports an estimate rather than a count
 * @param bandCounts distinct groups per requested band, zero-filled, in request order
 * @param unbandedGroupCount distinct groups with no metric, or whose metric fell in no band
 */
public record RankedGroupsResult(
    List<RankedGroup> groups,
    long distinctGroupCount,
    boolean distinctGroupCountExact,
    Map<String, Long> bandCounts,
    long unbandedGroupCount)
{
  public static RankedGroupsResult empty(final Map<String, float[]> requestedBands) {
    Map<String, Long> zeroed = new LinkedHashMap<>();
    if (requestedBands != null) {
      requestedBands.keySet().forEach(band -> zeroed.put(band, 0L));
    }
    return new RankedGroupsResult(List.of(), 0L, true, zeroed, 0L);
  }
}
