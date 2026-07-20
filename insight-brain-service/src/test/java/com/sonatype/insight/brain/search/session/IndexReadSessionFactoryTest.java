/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.lucene.LuceneSearcherManagerHolder;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_ID;
import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexReadSessionFactoryTest
{
  @Test
  public void open_injectsFailClosedFilterForRestrictedUser() throws Exception {
    UserPrincipal principal = new UserPrincipal("restricted", "Restricted User", "default", Set.of());
    CurrentUser currentUser = mock(CurrentUser.class);
    ReadableContextAuthzCache authzCache = mock(ReadableContextAuthzCache.class);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    when(authzCache.compiledRbacFilter(principal)).thenReturn(new MatchNoDocsQuery());

    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      writer.addDocument(document("visible-app", "visible-org"));
      writer.commit();

      try (LuceneSearcherManagerHolder holder = new LuceneSearcherManagerHolder(writer, (MeterRegistry) null)) {
        IndexReadSessionFactory factory = IndexReadSessionFactory.forTest(holder, currentUser, authzCache);

        try (IndexReadSession session = factory.open()) {
          assertThat(session.count(new MatchAllDocsQuery())).isZero();
        }
      }
    }
  }

  private Document document(final String applicationId, final String organizationId) {
    Document document = new Document();
    document.add(new StringField(APPLICATION_ID.label, applicationId, YES));
    document.add(new StringField(ORGANIZATION_ID.label, organizationId, YES));
    return document;
  }
}
