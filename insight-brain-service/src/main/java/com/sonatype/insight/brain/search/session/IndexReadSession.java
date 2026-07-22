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

public interface IndexReadSession
    extends AutoCloseable
{
  String backendId();

  Instant lastUpdatedAt();

  /** Diagnostic only — not a cross-request consistency token. */
  String snapshotHandle();

  IndexPageResult searchPage(IndexPageRequest request);

  long count(Query query);

  List<IndexTermsBucket> termsAggregation(Query query, String field, int maxBuckets);

  /**
   * Counts distinct values of {@code distinctField} grouped by {@code groupField}, restricted to
   * {@code groupValues}. Lucene implements this via stored-field collection until Track B
   * docValues cardinality lands.
   */
  default Map<String, Long> countDistinctGroupedBy(
      final Query query,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    throw new UnsupportedOperationException(
        "IndexReadSession.countDistinctGroupedBy is implemented by Lucene until Track B docValues cardinality");
  }

  @Override
  void close();
}
