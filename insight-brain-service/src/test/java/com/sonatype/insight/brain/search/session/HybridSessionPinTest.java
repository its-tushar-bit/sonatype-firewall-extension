/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.time.Instant;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HybridSessionPinTest
{
  @Test
  public void delegatesSuccessfulReadsToPinnedBackend() {
    IndexReadSession pinnedLucene = new StubSession("lucene");
    IndexReadSession session = new HybridIndexReadSession(pinnedLucene);

    IndexPageResult page = session.searchPage(new IndexPageRequest(new MatchAllDocsQuery(), null, 10, List.of()));

    assertThat(session.backendId()).isEqualTo("lucene");
    assertThat(page.docs()).extracting(document -> document.get("value")).containsExactly("one");
    assertThat(session.count(new MatchAllDocsQuery())).isEqualTo(1L);
    assertThat(session.termsAggregation(new MatchAllDocsQuery(), "category", 10))
        .containsExactly(new IndexTermsBucket("alpha", 1L));
  }

  @Test
  public void searchPage_rejectsCursorFromOtherBackend() {
    IndexReadSession pinnedOpenSearch = new StubSession("opensearch");
    IndexReadSession session = new HybridIndexReadSession(pinnedOpenSearch);

    IndexPageRequest request = new IndexPageRequest(
        new MatchAllDocsQuery(),
        null,
        10,
        IndexSessionCursors.encode("lucene", List.of(1, 1.0F)));

    assertThatThrownBy(() -> session.searchPage(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("lucene")
        .hasMessageContaining("opensearch");
  }

  private static class StubSession
      implements IndexReadSession
  {
    private final String backendId;

    StubSession(final String backendId) {
      this.backendId = backendId;
    }

    @Override
    public String backendId() {
      return backendId;
    }

    @Override
    public Instant lastUpdatedAt() {
      return Instant.EPOCH;
    }

    @Override
    public String snapshotHandle() {
      return "stub";
    }

    @Override
    public IndexPageResult searchPage(final IndexPageRequest request) {
      return new IndexPageResult(List.of(document()), List.of(), false);
    }

    @Override
    public long count(final Query query) {
      return 1;
    }

    @Override
    public List<IndexTermsBucket> termsAggregation(final Query query, final String field, final int maxBuckets) {
      return List.of(new IndexTermsBucket("alpha", 1L));
    }

    @Override
    public void close() {
    }

    private Document document() {
      Document document = new Document();
      document.add(new StringField("value", "one", YES));
      document.add(new StringField("category", "alpha", YES));
      return document;
    }
  }
}
