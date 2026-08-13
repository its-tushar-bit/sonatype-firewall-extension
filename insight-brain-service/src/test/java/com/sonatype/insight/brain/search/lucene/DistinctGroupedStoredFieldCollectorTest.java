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
  public void groupKeysMatchedAndReturnedLowercased_forMixedCaseVulnerabilityId() throws Exception {
    // The vulnerabilityId keyword field carries a lowercase normalizer on OpenSearch, so this backend
    // must key its returned map lowercased too — otherwise the two backends disagree and, on OpenSearch,
    // the mixed-case requested value would never match the already-lowercased aggregation bucket key
    // (every vuln row read affectedApps 0). Requested values arrive uppercase from _source; the count
    // must still resolve and the map must come back keyed "cve-2021-44228".
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnDocument("CVE-2021-44228", "app-1"));
      writer.addDocument(vulnDocument("CVE-2021-44228", "app-2"));
      writer.addDocument(vulnDocument("CVE-2021-44228", "app-2"));
      writer.addDocument(vulnDocument("CVE-2021-44228", "app-3"));
      writer.commit();

      try (DirectoryReader reader = DirectoryReader.open(writer)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
            searcher.storedFields(),
            FieldIdentifier.VULNERABILITY_ID.label,
            FieldIdentifier.APPLICATION_ID.label,
            List.of("CVE-2021-44228"));

        searcher.search(new MatchAllDocsQuery(), collector);

        assertThat(collector.groupCounts()).containsExactly(Map.entry("cve-2021-44228", 3L));
      }
    }
  }

  @Test
  public void distinctValuesCollapsedCaseInsensitively_toMatchOpenSearchNormalizer() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(vulnDocument("CVE-1", "App-One"));
      writer.addDocument(vulnDocument("CVE-1", "app-one"));
      writer.addDocument(vulnDocument("CVE-1", "APP-ONE"));
      writer.commit();

      try (DirectoryReader reader = DirectoryReader.open(writer)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        DistinctGroupedStoredFieldCollector collector = new DistinctGroupedStoredFieldCollector(
            searcher.storedFields(),
            FieldIdentifier.VULNERABILITY_ID.label,
            FieldIdentifier.APPLICATION_ID.label,
            List.of("CVE-1"));

        searcher.search(new MatchAllDocsQuery(), collector);

        assertThat(collector.groupCounts()).containsExactly(Map.entry("cve-1", 1L));
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

  private static Document vulnDocument(final String vulnerabilityId, final String applicationId) {
    Document document = new Document();
    document.add(new StringField(FieldIdentifier.ITEM_TYPE.label, ItemType.SECURITY_VULNERABILITY.name(), YES));
    document.add(new StringField(FieldIdentifier.VULNERABILITY_ID.label, vulnerabilityId, YES));
    document.add(new StringField(FieldIdentifier.APPLICATION_ID.label, applicationId, YES));
    return document;
  }
}
