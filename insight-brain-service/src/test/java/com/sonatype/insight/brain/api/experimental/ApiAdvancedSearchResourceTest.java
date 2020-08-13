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
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
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
  public void before() throws Exception {
    cleanSearchIndexDir();
    TaskScheduler taskScheduler = getCLMServer().getInstance(TaskScheduler.class);
    taskScheduler.disableForTesting = false;
    taskScheduler.start();
    IndexService indexService = getCLMServer().getInstance(IndexService.class);
    indexService.disableForTesting = false;
    indexService.start();
  }

  @After
  public void after() throws Exception {
    cleanSearchIndexDir();
  }

  private void cleanSearchIndexDir() throws Exception {
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    FileUtils.deleteDirectory(insightWork.getSearchIndexDir());
  }

  @Test
  public void testCreateSearchIndex() throws Exception {
    HttpResponse response = restRequest().path(ApiAdvancedSearchResource.INDEX_PATH).post();
    awaitIndexCompletion();

    assertResponseStatus(204, response);
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    assertIndexExists(insightWork.getSearchIndexDir());
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
  public void testSearchIndex_Unauthenticated() throws Exception {
    HttpResponse response =
        restRequest().anon().query("search", FieldIdentifier.APPLICATION_ID.label + ":" + "i-am-anon").get();

    assertResponseStatus(401, response);
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
    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> !getCLMServer().getInstance(IndexService.class).isFullIndexRunning());
  }
}
