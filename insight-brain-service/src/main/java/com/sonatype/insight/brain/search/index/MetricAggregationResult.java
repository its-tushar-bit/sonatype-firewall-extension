/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Map;

/** Result of a bucketed count aggregation over the search index. */
public class MetricAggregationResult
{
  /** Total documents matching the (RBAC-scoped) query. */
  public final long total;

  /** Bucket label -> count (e.g. threat-level band -> count). Empty when only a total was requested. */
  public final Map<String, Long> buckets;

  public MetricAggregationResult(long total, Map<String, Long> buckets) {
    this.total = total;
    this.buckets = buckets;
  }
}
