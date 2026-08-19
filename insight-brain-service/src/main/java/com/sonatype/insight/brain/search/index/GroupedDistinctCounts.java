/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Map;

/**
 * Result of {@link SearchIndexClient#countDistinctGroupedBy} including whether the counts are exact.
 * Hybrid failover must report exactness from the backend that produced {@link #counts()}, not from
 * {@link SearchIndexClient#backendId()} (always the primary).
 */
public record GroupedDistinctCounts(Map<String, Long> counts, boolean exact)
{
  public static GroupedDistinctCounts empty() {
    return new GroupedDistinctCounts(Map.of(), true);
  }
}
