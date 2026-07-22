/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;

public class DistinctGroupedStoredFieldCollectorTest
{
  @Test
  public void countsDistinctValuesPerGroupInOnePass() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(violationDocument("app-1", "build"));
      writer.addDocument(violationDocument("app-1", "build"));
      writer.addDocument(violationDocument("app-2", "build"));
      writer.addDocument(violationDocument("app-1", "stage-release"));
      writer.addDocument(violationDocument("app-3", "unlicensed"));
      writer.commit();

      try (DirectoryReader reader = DirectoryReader.open(writer)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
            searcher.storedFields(),
            FieldIdentifier.POLICY_EVALUATION_STAGE.label,
            FieldIdentifier.APPLICATION_ID.label,
            List.of("build", "stage-release"));

        searcher.search(new MatchAllDocsQuery(), collector);

        assertThat(collector.groupCounts()).containsExactly(
            Map.entry("build", 2L),
            Map.entry("stage-release", 1L));
        assertThat(collector.matchedDocuments()).isEqualTo(4L);
      }
    }
  }

  @Test
  public void omitsGroupsWithoutDistinctValuesAndSkipsBlankFields() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(violationDocument("", "build"));
      writer.addDocument(violationDocument("app-1", ""));
      writer.commit();

      try (DirectoryReader reader = DirectoryReader.open(writer)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
            searcher.storedFields(),
            FieldIdentifier.POLICY_EVALUATION_STAGE.label,
            FieldIdentifier.APPLICATION_ID.label,
            List.of("build", "release"));

        searcher.search(new MatchAllDocsQuery(), collector);

        assertThat(collector.groupCounts()).isEmpty();
        assertThat(collector.matchedDocuments()).isZero();
      }
    }
  }

  private static Document violationDocument(final String applicationId, final String stageId) {
    Document document = new Document();
    document.add(new StringField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_VIOLATION.name(), YES));
    document.add(new StringField(FieldIdentifier.APPLICATION_ID.label, applicationId, YES));
    document.add(new StringField(FieldIdentifier.POLICY_EVALUATION_STAGE.label, stageId, YES));
    return document;
  }
}
