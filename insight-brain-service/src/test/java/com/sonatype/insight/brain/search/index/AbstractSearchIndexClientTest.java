/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import jakarta.inject.Inject;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class AbstractSearchIndexClientTest
    extends AbstractComponentTest
{
  @Inject
  private SearchIndexChangeDAO searchIndexChangeDAO;

  private TestSearchIndexClient client;

  @Before
  public void setup() throws Exception {
    client = spy(new TestSearchIndexClient());
  }

  @Test
  public void testProcessSearchIndexChanges_ParseExceptionIsSwallowed() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    doThrow(new IOException(new ParseException("Parse error"))).when(client).updateIndex(any(), any());

    assertThatCode(() ->
        client.processSearchIndexChanges(Collections.singletonList(change), null)
    ).doesNotThrowAnyException();
  }

  @Test
  public void testProcessSearchIndexChanges_WrappedParseExceptionIsSwallowed() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    IOException wrapped = new IOException("Wrapped", new ParseException("Parse error"));
    doThrow(wrapped).when(client).updateIndex(any(), any());

    assertThatCode(() ->
        client.processSearchIndexChanges(Collections.singletonList(change), null)
    ).doesNotThrowAnyException();
  }

  @Test
  public void testProcessSearchIndexChanges_NonParseExceptionIsRethrown() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    IOException nonParseException = new IOException("Other error");
    doThrow(nonParseException).when(client).updateIndex(any(), any());

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> client.processSearchIndexChanges(Collections.singletonList(change), null))
        .withMessage("Other error");
  }

  @Test
  public void testProcessSearchIndexChanges_IoExceptionIsRethrown() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    doThrow(new IOException("IO error")).when(client).updateIndex(any(), any());

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> client.processSearchIndexChanges(Collections.singletonList(change), null))
        .withMessage("IO error");
  }

  private class TestSearchIndexClient
      extends AbstractSearchIndexClient
  {
    public TestSearchIndexClient() {
      super(null, null, null, null, null, searchIndexChangeDAO, null, null, null, null, null, null, null, null, null,
          null, null, null);
    }

    @Override
    protected void updateMaxQueryClauseCount() {
    }

    @Override
    protected void updateIndex(SearchIndexChange change, IndexingContext indexingContext) throws IOException {
    }

    @Override
    public SearchResultDTO searchIndex(
        String searchQuery,
        int pageSize,
        int page,
        boolean allComponents,
        boolean isSbomManagerMode,
        List<String> searchAfter)
    {
      return null;
    }

    @Override
    public void populateIndex() {
    }

    @Override
    public void updateIndex() {
    }

    @Override
    public Long getLastIndexTime() {
      return null;
    }

    @Override
    public long getIndexSize() {
      return 0;
    }
  }
}
