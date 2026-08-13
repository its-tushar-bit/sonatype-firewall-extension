/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.junit.Test;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LuceneIndexReadSessionAggregationTest
{
  @Test
  public void sumGroupedBy_sumsThreatLevelPerComponentHash() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", 10));
      writer.addDocument(threatDocument("hashA", 5));
      writer.addDocument(threatDocument("hashB", 1));
      writer.addDocument(threatDocument("hashC", 99));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Long> sums = session.sumGroupedBy(
              new MatchAllDocsQuery(),
              "componentHash",
              "policyViolationThreatLevel",
              List.of("hashA", "hashB"));

          assertThat(sums).containsExactly(
              Map.entry("hasha", 15L),
              Map.entry("hashb", 1L));
        }
      }
    }
  }

  @Test
  public void sumGroupedBy_negativeSum_isPreserved() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", -5));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Long> sums = session.sumGroupedBy(
              new MatchAllDocsQuery(),
              "componentHash",
              "policyViolationThreatLevel",
              List.of("hashA"));

          // negative sums must survive the `sum != 0` filter, matching the OpenSearch backend
          // for signed sum fields.
          assertThat(sums).containsExactly(Map.entry("hasha", -5L));
        }
      }
    }
  }

  @Test
  public void sumGroupedBy_emptyGroupValues_returnsEmpty() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", 10));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          assertThat(session.sumGroupedBy(
              new MatchAllDocsQuery(),
              "componentHash",
              "policyViolationThreatLevel",
              List.of())).isEmpty();
          assertThat(session.sumGroupedBy(
              new MatchAllDocsQuery(),
              "componentHash",
              "policyViolationThreatLevel",
              null)).isEmpty();
        }
      }
    }
  }

  @Test
  public void sumGroupedByBands_splitsInclusiveIntBands() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", 10));
      writer.addDocument(threatDocument("hashA", 5));
      writer.addDocument(threatDocument("hashB", 1));
      writer.commit();

      Map<String, int[]> bands = new LinkedHashMap<>();
      bands.put("critical", new int[]{8, 10});
      bands.put("low", new int[]{0, 1});

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Map<String, Long>> byGroup = session.sumGroupedByBands(
              new MatchAllDocsQuery(),
              "componentHash",
              "policyViolationThreatLevel",
              List.of("hashA", "hashB"),
              "policyViolationThreatLevel",
              bands);

          assertThat(byGroup).containsExactly(
              Map.entry("hasha", Map.of("critical", 10L)),
              Map.entry("hashb", Map.of("low", 1L)));
        }
      }
    }
  }

  @Test
  public void aggregateCountByField_countsCriticalBandDocuments() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", 10));
      writer.addDocument(threatDocument("hashB", 9));
      writer.addDocument(threatDocument("hashC", 7));
      writer.commit();

      Map<String, int[]> bands = new LinkedHashMap<>();
      bands.put("critical", new int[]{8, 10});
      bands.put("nonCritical", new int[]{0, 7});

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          MetricAggregationResult result = session.aggregateCountByField(
              new MatchAllDocsQuery(),
              "policyViolationThreatLevel",
              bands);

          assertThat(result.total).isEqualTo(3);
          assertThat(result.buckets).containsExactly(
              Map.entry("critical", 2L),
              Map.entry("nonCritical", 1L));
        }
      }
    }
  }

  @Test
  public void aggregateCountByFloatField_usesHalfOpenCvssBands() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 6.9f));
      writer.addDocument(vulnerabilityDocument("hashB", "CVE-2", 7.0f));
      writer.addDocument(vulnerabilityDocument("hashC", "CVE-3", 8.9f));
      writer.commit();

      Map<String, float[]> bands = cvssBands();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          MetricAggregationResult result = session.aggregateCountByFloatField(
              new MatchAllDocsQuery(),
              "vulnerabilitySeverity",
              bands);

          assertThat(result.total).isEqualTo(3);
          assertThat(result.buckets).containsExactly(
              Map.entry("medium", 1L),
              Map.entry("high", 2L),
              Map.entry("critical", 0L));
        }
      }
    }
  }

  @Test
  public void aggregateCountByFloatField_withDistinctFieldCollapsesDuplicateCves() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 7.0f));
      writer.addDocument(vulnerabilityDocument("hashB", "CVE-1", 7.5f));
      writer.addDocument(vulnerabilityDocument("hashC", "CVE-2", 9.1f));
      writer.commit();

      Map<String, float[]> bands = cvssBands();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          MetricAggregationResult result = session.aggregateCountByFloatField(
              new MatchAllDocsQuery(),
              "vulnerabilitySeverity",
              bands,
              "cve");

          assertThat(result.total).isEqualTo(3);
          assertThat(result.buckets).containsExactly(
              Map.entry("medium", 0L),
              Map.entry("high", 1L),
              Map.entry("critical", 1L));
        }
      }
    }
  }

  @Test
  public void aggregateCountByFloatField_withDistinctField_skipsDocsMissingDistinctValue() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 7.0f));
      // Same band, no CVE stored field — must not inflate distinct count as "".
      Document missingCve = new Document();
      missingCve.add(new StringField("componentHash", "hashB", YES));
      missingCve.add(new SortedDocValuesField("componentHash", new BytesRef("hashb")));
      missingCve.add(new FloatPoint("vulnerabilitySeverity", 7.5f));
      missingCve.add(new StoredField("vulnerabilitySeverity", 7.5f));
      missingCve.add(new SortedNumericDocValuesField(
          "vulnerabilitySeverity", NumericUtils.floatToSortableInt(7.5f)));
      writer.addDocument(missingCve);
      writer.commit();

      Map<String, float[]> bands = cvssBands();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          MetricAggregationResult result = session.aggregateCountByFloatField(
              new MatchAllDocsQuery(),
              "vulnerabilitySeverity",
              bands,
              "cve");

          assertThat(result.total).isEqualTo(2);
          assertThat(result.buckets).containsExactly(
              Map.entry("medium", 0L),
              Map.entry("high", 1L),
              Map.entry("critical", 0L));
        }
      }
    }
  }

  @Test
  public void countDistinctGroupedByBands_countsDistinctCvesPerHashSeverityBand() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 9));
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 10));
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-2", 5));
      writer.addDocument(vulnerabilityDocument("hashB", "CVE-3", 8));
      writer.addDocument(vulnerabilityDocument("hashC", "CVE-4", 10));
      writer.commit();

      Map<String, int[]> bands = new LinkedHashMap<>();
      bands.put("critical", new int[]{8, 10});
      bands.put("medium", new int[]{4, 7});

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Map<String, Long>> result = session.countDistinctGroupedByBands(
              new MatchAllDocsQuery(),
              "componentHash",
              "cve",
              List.of("hashA", "hashB"),
              "policyViolationThreatLevel",
              bands);

          assertThat(result).containsExactly(
              Map.entry("hasha", Map.of("critical", 1L, "medium", 1L)),
              Map.entry("hashb", Map.of("critical", 1L)));
        }
      }
    }
  }

  @Test
  public void rankGroupsByMaxMetric_ordersByMaxCvssAndZeroFillsBands() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 4.0f));
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-2", 8.0f));
      writer.addDocument(vulnerabilityDocument("hashB", "CVE-3", 9.5f));
      writer.addDocument(vulnerabilityDocument("hashC", "CVE-4", 7.0f));
      writer.commit();

      Map<String, float[]> bands = cvssBands();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          RankedGroupsResult result = session.rankGroupsByMaxMetric(
              new MatchAllDocsQuery(),
              "componentHash",
              "vulnerabilitySeverity",
              2,
              false,
              bands);

          assertThat(result.groups()).containsExactly(
              new RankedGroup("hashb", 9.5f),
              new RankedGroup("hasha", 8.0f));
          assertThat(result.distinctGroupCount()).isEqualTo(3);
          assertThat(result.distinctGroupCountExact()).isTrue();
          assertThat(result.bandCounts()).containsExactly(
              Map.entry("medium", 0L),
              Map.entry("high", 2L),
              Map.entry("critical", 1L));
          assertThat(result.unbandedGroupCount()).isZero();
        }
      }
    }
  }

  @Test
  public void rankGroupsByMaxMetric_ascending_ordersLowestMetricFirst() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 8.0f));
      writer.addDocument(vulnerabilityDocument("hashB", "CVE-2", 4.0f));
      writer.addDocument(vulnerabilityDocument("hashC", "CVE-3", 9.5f));
      writer.commit();

      Map<String, float[]> bands = cvssBands();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          RankedGroupsResult result = session.rankGroupsByMaxMetric(
              new MatchAllDocsQuery(),
              "componentHash",
              "vulnerabilitySeverity",
              2,
              true,
              bands);

          assertThat(result.groups()).containsExactly(
              new RankedGroup("hashb", 4.0f),
              new RankedGroup("hasha", 8.0f));
          assertThat(result.distinctGroupCount()).isEqualTo(3);
          assertThat(result.bandCounts()).containsExactly(
              Map.entry("medium", 1L),
              Map.entry("high", 1L),
              Map.entry("critical", 1L));
        }
      }
    }
  }

  @Test
  public void sumGroupedBy_closedSession_throwsIllegalState() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", 10));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder);
        session.close();

        assertThatThrownBy(() -> session.sumGroupedBy(
            new MatchAllDocsQuery(),
            "componentHash",
            "policyViolationThreatLevel",
            List.of("hashA")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
      }
    }
  }

  @Test
  public void sumGroupedBy_rejectsFloatSumField() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 7.0f));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          assertThatThrownBy(() -> session.sumGroupedBy(
              new MatchAllDocsQuery(),
              "componentHash",
              FieldIdentifier.VULNERABILITY_SEVERITY.label,
              List.of("hashA")))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("vulnerabilitySeverity")
                  .hasMessageContaining("integral");
        }
      }
    }
  }

  @Test
  public void rankGroupsByMaxMetric_noMatchQuery_returnsEmptyGroups() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 8.0f));
      writer.addDocument(vulnerabilityDocument("hashB", "CVE-2", 5.0f));
      writer.commit();

      Map<String, float[]> bands = cvssBands();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          RankedGroupsResult result = session.rankGroupsByMaxMetric(
              new TermQuery(new Term("componentHash", "nonexistent")),
              "componentHash",
              "vulnerabilitySeverity",
              10,
              false,
              bands);

          assertThat(result.groups()).isEmpty();
          assertThat(result.distinctGroupCount()).isZero();
          assertThat(result.bandCounts().values()).allSatisfy(count -> assertThat(count).isZero());
        }
      }
    }
  }

  @Test
  public void countDistinctGroupedByBands_noMatchQuery_returnsEmpty() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnerabilityDocument("hashA", "CVE-1", 9));
      writer.commit();

      Map<String, int[]> bands = new LinkedHashMap<>();
      bands.put("critical", new int[]{8, 10});

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Map<String, Long>> result = session.countDistinctGroupedByBands(
              new TermQuery(new Term("componentHash", "nonexistent")),
              "componentHash",
              "cve",
              List.of("hashA"),
              "policyViolationThreatLevel",
              bands);

          assertThat(result).isEmpty();
        }
      }
    }
  }

  @Test
  public void sumGroupedBy_noMatchQuery_returnsEmpty() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(threatDocument("hashA", 10));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexSearcher searcher = holder.acquire();
        try (LuceneIndexReadSession session = new LuceneIndexReadSession(searcher, new MatchAllDocsQuery(), holder)) {
          Map<String, Long> sums = session.sumGroupedBy(
              new TermQuery(new Term("componentHash", "nonexistent")),
              "componentHash",
              "policyViolationThreatLevel",
              List.of("hashA"));

          assertThat(sums).isEmpty();
        }
      }
    }
  }

  private static Document threatDocument(final String hash, final int threat) {
    Document doc = new Document();
    doc.add(new StringField("componentHash", hash, YES));
    doc.add(new SortedDocValuesField("componentHash", new BytesRef(hash.toLowerCase(Locale.ROOT))));
    doc.add(new IntPoint("policyViolationThreatLevel", threat));
    doc.add(new StoredField("policyViolationThreatLevel", threat));
    doc.add(new SortedNumericDocValuesField("policyViolationThreatLevel", threat));
    return doc;
  }

  private static Document vulnerabilityDocument(final String hash, final String cve, final int threat) {
    Document doc = vulnerabilityDocument(hash, cve, (float) threat);
    doc.add(new IntPoint("policyViolationThreatLevel", threat));
    doc.add(new StoredField("policyViolationThreatLevel", threat));
    return doc;
  }

  private static Document vulnerabilityDocument(final String hash, final String cve, final float severity) {
    Document doc = new Document();
    doc.add(new StringField("componentHash", hash, YES));
    doc.add(new SortedDocValuesField("componentHash", new BytesRef(hash.toLowerCase(Locale.ROOT))));
    doc.add(new StringField("cve", cve, YES));
    doc.add(new FloatPoint("vulnerabilitySeverity", severity));
    doc.add(new StoredField("vulnerabilitySeverity", severity));
    doc.add(new SortedNumericDocValuesField("vulnerabilitySeverity", NumericUtils.floatToSortableInt(severity)));
    return doc;
  }

  private static Map<String, float[]> cvssBands() {
    Map<String, float[]> bands = new LinkedHashMap<>();
    bands.put("medium", new float[]{4.0f, 7.0f});
    bands.put("high", new float[]{7.0f, 9.0f});
    bands.put("critical", new float[]{9.0f, 10.1f});
    return bands;
  }
}
