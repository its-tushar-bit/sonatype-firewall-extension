/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.results.SearchSuggestionResultDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ApiAdvancedSearchResourceTest
    extends AbstractResourceTest
{
  @Before
  @After
  public void beforeAndAfter() throws Exception {
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    FileUtils.deleteDirectory(insightWork.getSearchIndexDir());
    FileUtils.deleteDirectory(insightWork.getSearchSuggesterDir());
  }

  @Test
  public void testCreateSearchIndex() throws Exception {
    HttpResponse response = restRequest().path(ApiAdvancedSearchResource.INDEX_PATH).post();
    awaitIndexCompletion();

    assertResponseStatus(204, response);
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    assertIndexExists(insightWork.getSearchIndexDir());
    assertIndexExists(insightWork.getSearchSuggesterDir());
  }

  @Test
  public void testSearchIndex() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    restRequest().path(ApiAdvancedSearchResource.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response =
        restRequest().query("search", FieldIdentifier.APPLICATION_ID.label + ":" + application.getId()).get();

    assertResponseStatus(200, response);
    SearchResultDTO searchResultDTO = response.getBody(SearchResultDTO.class);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    GroupingByDTO groupingByDTO = searchResultDTO.groupingByDTOS.get(0);
    assertThat(groupingByDTO.searchResultItemDTOS).hasSize(1);
    SearchResultItemDTO searchResultItemDTO = groupingByDTO.searchResultItemDTOS.get(0);
    assertThat(searchResultItemDTO.applicationId).isEqualTo(application.getId());
  }

  @Test
  public void testAutoCompleteSearchQuery() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    restRequest().path(ApiAdvancedSearchResource.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest().path(ApiAdvancedSearchResource.SUGGESTER_PATH).query("search", "a").get();

    assertResponseStatus(200, response);
    SearchSuggestionResultDTO searchSuggestionResultDTO = response.getBody(SearchSuggestionResultDTO.class);
    assertThat(searchSuggestionResultDTO.searchResultItems)
        .containsOnlyOnce(FieldIdentifier.APPLICATION_ID.label + ":" + application.getId());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH);
  }

  private void assertIndexExists(File indexFile) throws Exception {
    try (FSDirectory fsDirectory = FSDirectory.open(indexFile.toPath())) {
      assertThat(DirectoryReader.indexExists(fsDirectory)).isTrue();
    }
  }

  private void awaitIndexCompletion() {
    await().atMost(10, TimeUnit.SECONDS).until(() -> !getCLMServer().getInstance(IndexService.class).isRunning());
  }
}
