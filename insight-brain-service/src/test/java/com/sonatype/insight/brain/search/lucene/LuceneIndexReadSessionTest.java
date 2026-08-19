/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.Test;

import com.sonatype.insight.brain.search.session.IndexPageRequest;
import com.sonatype.insight.brain.search.session.IndexPageResult;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;

public class LuceneIndexReadSessionTest
{
  @Test
  public void searchPage_collectsOneExtraHitForHasNextCursor() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("one"));
      writer.addDocument(document("two"));
      writer.addDocument(document("three"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          IndexPageResult firstPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), null, 2, List.of()));

          assertThat(firstPage.docs()).hasSize(2);
          assertThat(firstPage.hasNext()).isTrue();
          assertThat(firstPage.nextSearchAfter()).isNotEmpty();

          IndexPageResult secondPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), null, 2, firstPage.nextSearchAfter()));

          assertThat(secondPage.docs()).hasSize(1);
          assertThat(secondPage.hasNext()).isFalse();
          assertThat(secondPage.nextSearchAfter()).isEmpty();
        }
      }
    }
  }

  @Test
  public void searchPage_sortedSearchAfterPreservesDocIdTieBreaker() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("one", "alpha"));
      writer.addDocument(document("two", "alpha"));
      writer.addDocument(document("three", "alpha"));
      writer.addDocument(document("four", "beta"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Sort sort = new Sort(new SortField("sortKey", SortField.Type.STRING));
          IndexPageResult firstPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), sort, 2, List.of()));

          assertThat(values(firstPage.docs())).containsExactly("one", "two");
          assertThat(firstPage.hasNext()).isTrue();

          IndexPageResult secondPage = session.searchPage(
              new IndexPageRequest(new MatchAllDocsQuery(), sort, 2, firstPage.nextSearchAfter()));

          assertThat(values(secondPage.docs())).containsExactly("three", "four");
          assertThat(secondPage.hasNext()).isFalse();
          assertThat(secondPage.nextSearchAfter()).isEmpty();
        }
      }
    }
  }

  @Test
  public void termsAggregation_returnsTopBucketsByCountNotLexicographicOrder() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // "zebra" is lexicographically last but most frequent; "apple" is first but least frequent.
      writer.addDocument(documentWithSingleFacet("zebra"));
      writer.addDocument(documentWithSingleFacet("zebra"));
      writer.addDocument(documentWithSingleFacet("zebra"));
      writer.addDocument(documentWithSingleFacet("mango"));
      writer.addDocument(documentWithSingleFacet("mango"));
      writer.addDocument(documentWithSingleFacet("apple"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          List<IndexTermsBucket> buckets = session.termsAggregation(new MatchAllDocsQuery(), "facet_single", 2);

          assertThat(buckets).extracting(IndexTermsBucket::key).containsExactly("zebra", "mango");
          assertThat(buckets).extracting(IndexTermsBucket::count).containsExactly(3L, 2L);
        }
      }
    }
  }

  @Test
  public void termsAggregation_usesDocValuesForSingleValuedField() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // Index with SortedDocValuesField (single-valued facet field)
      writer.addDocument(documentWithSingleFacet("a"));
      writer.addDocument(documentWithSingleFacet("a"));
      writer.addDocument(documentWithSingleFacet("a"));
      writer.addDocument(documentWithSingleFacet("b"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          List<IndexTermsBucket> buckets = session.termsAggregation(new MatchAllDocsQuery(), "facet_single", 10);

          assertThat(buckets).hasSize(2);
          assertThat(buckets).extracting(IndexTermsBucket::key).containsExactly("a", "b");
          assertThat(buckets).extracting(IndexTermsBucket::count).containsExactly(3L, 1L);
        }
      }
    }
  }

  @Test
  public void termsAggregation_usesDocValuesForMultiValuedField() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // Index with SortedSetDocValuesField (multi-valued facet field)
      writer.addDocument(documentWithMultiFacet("x", "y"));
      writer.addDocument(documentWithMultiFacet("x", "z"));
      writer.addDocument(documentWithMultiFacet("y", "z"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          List<IndexTermsBucket> buckets = session.termsAggregation(new MatchAllDocsQuery(), "facet_multi", 10);

          // x appears in 2 docs, y in 2 docs, z in 2 docs
          assertThat(buckets).hasSize(3);
          assertThat(buckets).extracting(IndexTermsBucket::key).containsExactly("x", "y", "z");
          assertThat(buckets).extracting(IndexTermsBucket::count).containsExactly(2L, 2L, 2L);
        }
      }
    }
  }

  @Test
  public void termsAggregation_hierarchicalAncestorClosure_yieldsSubtreeCounts() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // Each "application" doc carries its full {self..root} org ancestor closure as a multi-valued
      // facet field, so aggregating that field yields hierarchical subtree counts: an ancestor's
      // count is the number of apps anywhere in its subtree. (Root exclusion is the facet builder's job.)
      writer.addDocument(documentWithMultiFacet("orgA", "parent", "grandparent", "ROOT_ORGANIZATION_ID"));
      writer.addDocument(documentWithMultiFacet("orgB", "grandparent", "ROOT_ORGANIZATION_ID"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          List<IndexTermsBucket> buckets = session.termsAggregation(new MatchAllDocsQuery(), "facet_multi", 10);

          java.util.Map<String, Long> counts = new java.util.HashMap<>();
          buckets.forEach(b -> counts.put(b.key(), b.count()));
          // grandparent & root see both apps (subtree = everything); parent & orgA only appA; orgB only appB.
          assertThat(counts).isEqualTo(java.util.Map.of(
              "orgA", 1L, "parent", 1L, "grandparent", 2L, "orgB", 1L, "ROOT_ORGANIZATION_ID", 2L));
        }
      }
    }
  }

  @Test
  public void termsAggregation_truncatesToMaxBuckets() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(documentWithSingleFacet("a"));
      writer.addDocument(documentWithSingleFacet("a"));
      writer.addDocument(documentWithSingleFacet("b"));
      writer.addDocument(documentWithSingleFacet("c"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          List<IndexTermsBucket> buckets = session.termsAggregation(new MatchAllDocsQuery(), "facet_single", 2);

          // Returns top 2 by count: "a" (2), then "b" and "c" (both 1) - ties broken by key
          assertThat(buckets).hasSize(2);
          assertThat(buckets.get(0).key()).isEqualTo("a");
          assertThat(buckets.get(0).count()).isEqualTo(2L);
        }
      }
    }
  }

  @Test
  public void termsAggregation_returnsEmptyForMaxBucketsZeroOrNegative() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(documentWithSingleFacet("a"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          assertThat(session.termsAggregation(new MatchAllDocsQuery(), "facet_single", 0)).isEmpty();
          assertThat(session.termsAggregation(new MatchAllDocsQuery(), "facet_single", -1)).isEmpty();
        }
      }
    }
  }

  @Test
  public void countDistinctGroupedBy_multiValuedGroupField_contributesDistinctKeyToEachAncestorBucket() throws Exception {
    // Multi-valued group field (e.g. parentOrganizationId ancestor closure):
    // a component counted under EVERY ancestor org in its closure.
    // A single doc contributes its distinct-key to MULTIPLE group buckets.
    // Deduplication: same distinct-key in same group counts once.
    //
    // PROVES: the implementation reads ALL values of the multi-valued group field via docValues,
    // not just the first value (stored-field document.get() only returns first value for multi-valued fields).
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // Doc A: orgs [orgA, parentA, grandparent], component hash = hash1
      // If implementation only reads first group value (orgA), grandparent would get 0 count.
      // If implementation reads all group values, grandparent gets count of 1.
      writer.addDocument(distinctGroupedDocumentMultiGroup(
          List.of("orgA", "parentA", "grandparent"), "hash1"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Long> counts = session.countDistinctGroupedBy(
              new MatchAllDocsQuery(),
              "group_multi",
              "distinct_key",
              List.of("orgA", "parentA", "grandparent"));

          // ALL three ancestors must see the distinct count of 1 - proves multi-value reading
          // Keys are returned lowercased (matching OpenSearch behavior)
          assertThat(counts).containsEntry("orga", 1L);
          assertThat(counts).containsEntry("parenta", 1L);
          assertThat(counts).containsEntry("grandparent", 1L);
          assertThat(counts).hasSize(3);
        }
      }
    }
  }

  @Test
  public void countDistinctGroupedBy_multiValuedGroupField_withDistinctDeduplication() throws Exception {
    // Proves that duplicate distinct-keys are deduplicated PER GROUP (not globally).
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      // Two docs with same group values but same distinct key
      writer.addDocument(distinctGroupedDocumentMultiGroup(
          List.of("orgA", "parentA"), "hash1"));
      writer.addDocument(distinctGroupedDocumentMultiGroup(
          List.of("orgA", "parentA"), "hash1"));
      // Third doc with different distinct key
      writer.addDocument(distinctGroupedDocumentMultiGroup(
          List.of("orgA", "parentA"), "hash2"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Long> counts = session.countDistinctGroupedBy(
              new MatchAllDocsQuery(),
              "group_multi",
              "distinct_key",
              List.of("orgA", "parentA"));

          // Each group sees 2 distinct hashes (hash1 and hash2), not 3 total docs
          // Keys are returned lowercased (matching OpenSearch behavior)
          assertThat(counts).containsEntry("orga", 2L);
          assertThat(counts).containsEntry("parenta", 2L);
        }
      }
    }
  }

  @Test
  public void countDistinctGroupedBy_singleValuedGroupField_maintainsExistingBehavior() throws Exception {
    // Single-valued group field (backward compatibility test).
    // The implementation must handle both single and multi-valued group fields.
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(distinctGroupedDocumentSingleGroup("stage-build", "app-1"));
      writer.addDocument(distinctGroupedDocumentSingleGroup("stage-build", "app-2"));
      writer.addDocument(distinctGroupedDocumentSingleGroup("stage-build", "app-1")); // duplicate
      writer.addDocument(distinctGroupedDocumentSingleGroup("stage-release", "app-3"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Long> counts = session.countDistinctGroupedBy(
              new MatchAllDocsQuery(),
              "group_single",
              "distinct_key",
              List.of("stage-build", "stage-release"));

          assertThat(counts).containsEntry("stage-build", 2L); // app-1, app-2 (app-1 duplicated)
          assertThat(counts).containsEntry("stage-release", 1L); // app-3
        }
      }
    }
  }

  private Document distinctGroupedDocumentMultiGroup(final List<String> groupValues, final String distinctKey) {
    Document document = new Document();
    for (String groupValue : groupValues) {
      document.add(new SortedSetDocValuesField("group_multi", new BytesRef(groupValue)));
    }
    document.add(new SortedDocValuesField("distinct_key", new BytesRef(distinctKey)));
    return document;
  }

  private Document distinctGroupedDocumentSingleGroup(final String groupValue, final String distinctKey) {
    Document document = new Document();
    document.add(new SortedDocValuesField("group_single", new BytesRef(groupValue)));
    document.add(new SortedDocValuesField("distinct_key", new BytesRef(distinctKey)));
    return document;
  }

  @Test
  public void termsAggregation_fieldPresentWithoutDocValues_returnsEmptyWithoutThrowing() throws Exception {
    // A field indexed only as a searchable StringField (no facet doc values) is the pre-reindex shape.
    // StringValueFacetCounts would throw IllegalStateException on it; the read path must degrade to no
    // buckets instead of 500ing the request.
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(documentWithTerm("a"));
      writer.addDocument(documentWithTerm("b"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          assertThat(session.termsAggregation(new MatchAllDocsQuery(), "term", 10)).isEmpty();
        }
      }
    }
  }

  private Document document(final String value) {
    Document document = new Document();
    document.add(new StringField("value", value, YES));
    return document;
  }

  private Document documentWithTerm(final String term) {
    Document document = new Document();
    document.add(new StringField("term", term, YES));
    return document;
  }

  private Document documentWithSingleFacet(final String value) {
    Document document = new Document();
    document.add(new SortedDocValuesField("facet_single", new BytesRef(value)));
    return document;
  }

  private Document documentWithMultiFacet(final String... values) {
    Document document = new Document();
    for (String value : values) {
      document.add(new SortedSetDocValuesField("facet_multi", new BytesRef(value)));
    }
    return document;
  }

  private Document document(final String value, final String sortKey) {
    Document document = document(value);
    document.add(new SortedDocValuesField("sortKey", new BytesRef(sortKey)));
    return document;
  }

  private List<String> values(final List<Document> documents) {
    return documents.stream()
        .map(document -> document.get("value"))
        .toList();
  }
}
