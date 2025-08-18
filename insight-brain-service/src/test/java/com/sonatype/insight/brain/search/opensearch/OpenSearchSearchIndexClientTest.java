/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@OpenSearchHttpTest
public class OpenSearchSearchIndexClientTest
    extends AbstractBrainServiceIntegrationTest
{
  private OpenSearchSearchIndexClient openSearchSearchIndexClient; //under test

  private IndexConfigProvider indexConfigProvider;

  private IndexConfig indexConfig;

  @Before
  public void setUp() throws Exception {
    startIqTestServer(config -> config.setSearchConfig(searchIndexRule.getSearchConfig()));
    openSearchSearchIndexClient = getCLMServer().getInstance(OpenSearchSearchIndexClient.class);
    indexConfigProvider = getCLMServer().getInstance(SingleTenantIndexConfigProvider.class);
    indexConfig = indexConfigProvider.getIndexConfig();
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIndex() throws Exception {
    openSearchSearchIndexClient.populateIndex();

    GetIndexRequest getIndexRequest = new GetIndexRequest.Builder()
        .index(indexConfig.getIndexName())
        .build();
    GetIndexResponse getIndexResponse = openSearchSearchIndexClient.getClient()
        .indices()
        .get(getIndexRequest);

    Map<String, IndexState> result = getIndexResponse.result();
    assertThat(result).hasSize(1);
    Entry<String, IndexState> entry = result.entrySet().iterator().next();
    assertThat(entry.getKey()).startsWith(indexConfig.getIndexName());
    String expectedMappingsJson = JsonUtils.writeUnformatted(indexConfig.getIndexMapping().getMappings());
    String actualMappingsJson = JsonUtils.writeUnformatted(entry.getValue().mappings().properties());
    assertThatJson(actualMappingsJson).isEqualTo(expectedMappingsJson);
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIndex_IndexAlreadyExists() throws IOException {
    // This call to get the client will internally create the index itself.
    OpenSearchClient openSearchClient = spy(openSearchSearchIndexClient.getClient());
    OpenSearchIndicesClient openSearchIndicesClient = spy(openSearchClient.indices());

    // This call to create the index should then be a no-op
    openSearchSearchIndexClient.populateIndex();

    verify(openSearchIndicesClient, never()).create(any(CreateIndexRequest.class));
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIndex_BadOpenSearchConfiguration() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
        // no-op
      }

      @Override
      public String getConfigFilePath() {
        return Objects.requireNonNull(
                InsightBrainService.class.getResource("/OpenSearchSearchIndexClientTest/bad-opensearch-config.yml"))
            .getFile();
      }
    });
    openSearchSearchIndexClient = getCLMServer().getInstance(OpenSearchSearchIndexClient.class);
    indexConfigProvider = getCLMServer().getInstance(SingleTenantIndexConfigProvider.class);
    indexConfig = indexConfigProvider.getIndexConfig();

    assertThrows("Error creating OpenSearch index: " + indexConfig.getIndexName(), RuntimeException.class, () -> {
      openSearchSearchIndexClient.populateIndex();
    });
  }
}
