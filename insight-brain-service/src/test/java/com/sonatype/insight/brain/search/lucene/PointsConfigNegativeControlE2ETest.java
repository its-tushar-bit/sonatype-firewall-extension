/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.sonatype.insight.brain.search.index.FieldIdentifier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Negative-control harness for the componentMaxPolicyThreatLevel PointsConfig fix. Indexes a real
 * IntPoint component doc (mirroring DocumentBuilder.setComponentMaxPolicyThreatLevel) into a real
 * Lucene index, then runs the Components-tab range fragment componentMaxPolicyThreatLevel:[8 TO 10]
 * through the REAL production LuceneComponents parser wiring twice:
 * 1. WITH the PointsConfig entry (the fix) -> the in-range doc IS returned (non-zero);
 * 2. WITHOUT the PointsConfig entry (the pre-fix state) -> the same query returns ZERO.
 * This is the zero -> fix -> non-zero negative control at the exact code layer where the bug lives.
 */
public class PointsConfigNegativeControlE2ETest
{
  private static Directory buildIndex() throws Exception {
    Directory directory = new ByteBuffersDirectory();
    Map<String, Analyzer> perField = new HashMap<>();
    PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), perField);
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      writer.addDocument(componentDoc("threat-fixture-high", 9));
      writer.addDocument(componentDoc("threat-fixture-low", 3));
      writer.commit();
    }
    return directory;
  }

  private static Document componentDoc(final String name, final int threatLevel) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, "non_vulnerable_component", Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_NAME.label, name, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label, threatLevel));
    return doc;
  }

  /** Reproduces the pre-fix parser: identical wiring MINUS the componentMaxPolicyThreatLevel entry. */
  private static Function<String, Query> parserWithoutTheFix() {
    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ROOT);
    Map<String, PointsConfig> pointsConfigsByFieldName = new HashMap<>();
    pointsConfigsByFieldName.put(FieldIdentifier.POLICY_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    pointsConfigsByFieldName.put(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label,
        new PointsConfig(numberFormat, Integer.class));
    // Intentionally NO COMPONENT_MAX_POLICY_THREAT_LEVEL entry -> the pre-fix state.
    StandardQueryParser queryParser = new StandardQueryParser(
        new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), new HashMap<>()));
    queryParser.setPointsConfigMap(pointsConfigsByFieldName);
    queryParser.setAllowLeadingWildcard(true);
    return searchString -> {
      try {
        return queryParser.parse(searchString, FieldIdentifier.VULNERABILITY_ID.label);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  private static int hits(Directory dir, Query query) throws Exception {
    try (IndexReader reader = DirectoryReader.open(dir)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      TopDocs top = searcher.search(query, 25);
      return (int) top.totalHits.value;
    }
  }

  @Test
  public void negativeControl_zeroWithoutFix_nonZeroWithFix() throws Exception {
    final String rangeFragment = FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label + ":[8 TO 10]";

    try (Directory dir = buildIndex()) {
      // Pre-fix state: no PointsConfig for componentMaxPolicyThreatLevel -> range not parsed as an
      // IntPoint range -> zero hits (the silent Lucene-only defect).
      Query preFix = parserWithoutTheFix().apply(rangeFragment);
      int preFixHits = hits(dir, preFix);
      assertThat(preFixHits).as("without the PointsConfig fix the range returns zero").isZero();

      // With-fix state: the REAL production LuceneComponents parser now carries the entry.
      Function<String, Query> production = new LuceneComponents(null).newQueryParser();
      Query postFix = production.apply(rangeFragment);
      int postFixHits = hits(dir, postFix);
      // The in-range component (max threat 9) is returned; the below-range one (3) is excluded.
      assertThat(postFixHits).as("with the PointsConfig fix the range returns the matching component")
          .isEqualTo(1);
    }
  }
}
