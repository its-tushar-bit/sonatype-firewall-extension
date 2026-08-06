/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.search.index.FieldIdentifier;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LuceneRbacFilterQueryBuilderTest
{
  @Test
  public void build_globalContext_returnsMatchAllDocs() {
    assertThat(LuceneRbacFilterQueryBuilder.build(Optional.empty())).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void build_emptyReadContexts_returnsMatchNoDocs() {
    assertThat(LuceneRbacFilterQueryBuilder.build(Optional.of(Map.of()))).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void build_nonAppOrgOwnerTypesFailClosed() {
    Query filter = LuceneRbacFilterQueryBuilder.build(
        Optional.of(Map.of("repo-1", OwnerType.REPOSITORY_CONTAINER)));
    assertThat(filter).isInstanceOf(MatchNoDocsQuery.class);
  }

  /**
   * The RBAC keyword fields are indexed through {@link LowerCaseKeywordAnalyzer}, so
   * {@link LuceneRbacFilterQueryBuilder} lowercases context ids before building the
   * {@link org.apache.lucene.search.TermInSetQuery}. Getting the casing wrong on a security filter
   * means either a leak or missing results, so assert a mixed-case application id still matches the
   * lowercased indexed term while a non-permitted document is excluded.
   */
  @Test
  public void build_mixedCaseApplicationId_matchesLowercaseIndexedDocument() throws IOException {
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        Document permitted = new Document();
        permitted.add(new StoredField("doc_id", 1));
        permitted.add(new TextField(FieldIdentifier.APPLICATION_ID.label, "MyApp-ID", Field.Store.YES));
        writer.addDocument(permitted);

        Document forbidden = new Document();
        forbidden.add(new StoredField("doc_id", 2));
        forbidden.add(new TextField(FieldIdentifier.APPLICATION_ID.label, "OtherApp", Field.Store.YES));
        writer.addDocument(forbidden);
        writer.commit();
      }

      try (DirectoryReader reader = DirectoryReader.open(dir)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        // Caller supplies the id in different casing than it was indexed; the builder lowercases both.
        Query filter =
            LuceneRbacFilterQueryBuilder.build(Optional.of(Map.of("myAPP-id", OwnerType.APPLICATION)));

        TopDocs topDocs = searcher.search(filter, 10);
        Set<String> matched = new TreeSet<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
          matched.add(searcher.storedFields().document(scoreDoc.doc).get(FieldIdentifier.APPLICATION_ID.label));
        }
        assertThat(matched).containsExactly("MyApp-ID");
      }
    }
  }

  /**
   * CLM-33964 regression: a user whose readable-context count exceeds the advanced-search clause
   * budget must still be able to search. The RBAC permission filter is expressed as a
   * {@link org.apache.lucene.search.TermInSetQuery} (exempt from {@link IndexSearcher#getMaxClauseCount()}),
   * so the search neither throws {@link IndexSearcher.TooManyClauses} nor leaks documents outside the
   * permitted contexts. Enumerating the same contexts as one boolean clause each (the old behavior)
   * trips the budget, as the control assertion below demonstrates.
   */
  @Test
  public void build_contextsBeyondClauseBudget_doesNotThrowAndFiltersToPermittedContexts() throws IOException {
    List<String> permittedOrgIds = new ArrayList<>();
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        int docId = 0;
        for (int i = 0; i < 50; i++) {
          String orgId = "ctxorg-" + i;
          permittedOrgIds.add(orgId);
          Document doc = new Document();
          doc.add(new StoredField("doc_id", docId++));
          doc.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, orgId, Field.Store.YES));
          writer.addDocument(doc);
        }
        // A document in an org the user cannot read; must be filtered out.
        Document forbidden = new Document();
        forbidden.add(new StoredField("doc_id", docId));
        forbidden.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, "forbidden-org", Field.Store.YES));
        writer.addDocument(forbidden);
        writer.commit();
      }

      try (DirectoryReader reader = DirectoryReader.open(dir)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        int previousMaxClauseCount = IndexSearcher.getMaxClauseCount();
        try {
          // Clause budget deliberately far smaller than the number of permitted contexts.
          IndexSearcher.setMaxClauseCount(5);

          // Control: expressing the same permission set as one boolean clause per context (the old
          // behavior) trips the budget while the query is being built.
          assertThatThrownBy(() -> {
            BooleanQuery.Builder orClauses = new BooleanQuery.Builder();
            for (String orgId : permittedOrgIds) {
              orClauses.add(new TermQuery(new Term(FieldIdentifier.ORGANIZATION_ID.label, orgId)), Occur.SHOULD);
            }
            orClauses.build();
          }).isInstanceOf(IndexSearcher.TooManyClauses.class);

          // The fix: the TermInSetQuery-based permission filter does not trip the budget.
          Map<String, OwnerType> readableContexts = new LinkedHashMap<>();
          for (String orgId : permittedOrgIds) {
            readableContexts.put(orgId, OwnerType.ORGANIZATION);
          }
          Query permissionFilter = LuceneRbacFilterQueryBuilder.build(Optional.of(readableContexts));

          TopDocs topDocs = searcher.search(permissionFilter, 1000);

          Set<String> matchedOrgIds = new TreeSet<>();
          for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            matchedOrgIds.add(
                searcher.storedFields().document(scoreDoc.doc).get(FieldIdentifier.ORGANIZATION_ID.label));
          }
          assertThat(matchedOrgIds).containsExactlyInAnyOrderElementsOf(permittedOrgIds);
          assertThat(matchedOrgIds).doesNotContain("forbidden-org");
        }
        finally {
          IndexSearcher.setMaxClauseCount(previousMaxClauseCount);
        }
      }
    }
  }
}
