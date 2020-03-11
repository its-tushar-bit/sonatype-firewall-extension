/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.nio.file.Files;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.ConflictException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SearchServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SearchService searchService;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testSearchIndex_NoSearchIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testSearchIndex_EmptySearchIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchIndexDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testAutoCompleteSearchQuery_NoSearchSuggesterIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.autoCompleteSearchQuery("query"))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testAutoCompleteSearchQuery_EmptySearchSuggesterIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchSuggesterDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.autoCompleteSearchQuery("query"))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testFindStartOfLastClause() {
    assertThat(SearchService.findStartOfLastClause("")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("foo")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("foo:bar")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("foo:bar ")).isEqualTo(8);
    assertThat(SearchService.findStartOfLastClause("foo:\"bar ")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("foo:\\\"bar ")).isEqualTo(10);
    assertThat(SearchService.findStartOfLastClause("foo bar")).isEqualTo(4);
    assertThat(SearchService.findStartOfLastClause("+b")).isEqualTo(1);
    assertThat(SearchService.findStartOfLastClause("foo +b")).isEqualTo(5);
    assertThat(SearchService.findStartOfLastClause("foo+b")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("-b")).isEqualTo(1);
    assertThat(SearchService.findStartOfLastClause("foo -b")).isEqualTo(5);
    assertThat(SearchService.findStartOfLastClause("foo-b")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("!b")).isEqualTo(1);
    assertThat(SearchService.findStartOfLastClause("foo !b")).isEqualTo(5);
    assertThat(SearchService.findStartOfLastClause("foo!b")).isEqualTo(0);
    assertThat(SearchService.findStartOfLastClause("foo (b")).isEqualTo(5);
  }
}
