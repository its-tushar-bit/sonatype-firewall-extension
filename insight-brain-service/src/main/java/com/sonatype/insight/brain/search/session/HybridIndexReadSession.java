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
  public Map<String, Long> countDistinctGroupedBy(
      final Query query,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    return delegate.countDistinctGroupedBy(query, groupField, distinctField, groupValues);
  }

  @Override
  public void close() {
    delegate.close();
  }
}
