/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SearchModuleServerStartTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  @ManualIqServerInit
  public void testSearchModule_defaultLuceneSearch() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/SearchModuleServerStartTest/config-with-default-search.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(LuceneSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_http() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/SearchModuleServerStartTest/config-with-opensearch-http.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(HybridSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_aws() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/SearchModuleServerStartTest/config-with-opensearch-aws.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(HybridSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_http_hybridMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-opensearch-http-hybrid-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(HybridSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_aws_hybridMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-opensearch-aws-hybrid-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(HybridSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_http_opensearchMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-opensearch-http-opensearch-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(OpenSearchSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_aws_opensearchMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-opensearch-aws-opensearch-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(OpenSearchSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_aws_luceneMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-opensearch-aws-lucene-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(LuceneSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_luceneSearchConfig_luceneMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-lucene-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(LuceneSearchIndexClient.class);
  }

  @Test
  @ManualIqServerInit
  public void testSearchModule_openSearchConfig_http_luceneMode() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/SearchModuleServerStartTest/config-with-opensearch-http-lucene-mode.yml")
            .getFile();
      }
    });
    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);
    assertThat(searchIndexClient).isInstanceOf(LuceneSearchIndexClient.class);
  }
}
