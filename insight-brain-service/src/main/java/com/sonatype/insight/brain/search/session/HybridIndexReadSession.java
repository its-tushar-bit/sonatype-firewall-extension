/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;

import org.apache.lucene.search.Query;

public class HybridIndexReadSession
    implements IndexReadSession
{
  private final IndexReadSession delegate;

  public HybridIndexReadSession(final IndexReadSession delegate) {
    this.delegate = delegate;
  }

  @Override
  public String backendId() {
    return delegate.backendId();
  }

  @Override
  public Instant lastUpdatedAt() {
    return delegate.lastUpdatedAt();
  }

  @Override
  public String snapshotHandle() {
    return delegate.snapshotHandle();
  }

  @Override
  public IndexPageResult searchPage(final IndexPageRequest request) {
    IndexSessionCursors.decode(backendId(), request.searchAfter());
    return delegate.searchPage(request);
  }

  @Override
  public long count(final Query query) {
    return delegate.count(query);
  }

  @Override
  public List<IndexTermsBucket> termsAggregation(final Query query, final String field, final int maxBuckets) {
    return delegate.termsAggregation(query, field, maxBuckets);
  }

  @Override
  public RankedGroupsResult rankGroupsByMaxMetric(
      final Query query,
      final String groupField,
      final String metricField,
      final int limit,
      final boolean ascending,
      final Map<String, float[]> metricBands)
  {
    return delegate.rankGroupsByMaxMetric(query, groupField, metricField, limit, ascending, metricBands);
  }

  @Override
  public MetricAggregationResult aggregateCountByField(
      final Query query,
      final String bucketField,
      final Map<String, int[]> ranges)
  {
    return delegate.aggregateCountByField(query, bucketField, ranges);
  }

  @Override
  public MetricAggregationResult aggregateCountByFloatField(
      final Query query,
      final String bucketField,
      final Map<String, float[]> ranges,
      final String distinctField)
  {
    return delegate.aggregateCountByFloatField(query, bucketField, ranges, distinctField);
  }

  @Override
  public Map<String, Long> countDistinctGroupedBy(
      final Query query,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    return delegate.countDistinctGroupedBy(query, groupField, distinctField, groupValues);
  }

  @Override
  public Map<String, Map<String, Long>> countDistinctGroupedByBands(
      final Query query,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues,
      final String bandField,
      final Map<String, int[]> bands)
  {
    return delegate.countDistinctGroupedByBands(
        query, groupField, distinctField, groupValues, bandField, bands);
  }

  @Override
  public Map<String, Long> sumGroupedBy(
      final Query query,
      final String groupField,
      final String sumField,
      final Collection<String> groupValues)
  {
    return delegate.sumGroupedBy(query, groupField, sumField, groupValues);
  }

  @Override
  public Map<String, Map<String, Long>> sumGroupedByBands(
      final Query query,
      final String groupField,
      final String sumField,
      final Collection<String> groupValues,
      final String bandField,
      final Map<String, int[]> bands)
  {
    return delegate.sumGroupedByBands(query, groupField, sumField, groupValues, bandField, bands);
  }

  @Override
  public void close() {
    delegate.close();
  }
}
