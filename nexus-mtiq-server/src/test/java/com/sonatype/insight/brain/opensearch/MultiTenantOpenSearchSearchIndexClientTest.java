/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.opensearch;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.IndexConfig;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Ignore("CLM-38327: Broken by reuseForks=true from CLM-39038; static initial* fields in TemporaryEntity leak across tenant contexts")
@OpenSearchHttpTest
@Category(SlowTest.class)
public class MultiTenantOpenSearchSearchIndexClientTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private OpenSearchSearchIndexClient openSearchSearchIndexClient; // under test

  private IndexConfigProvider indexConfigProvider;

  private IndexConfig indexConfig;

  @Before
  public void setUp() throws Exception {
    Auth0Config auth0Config = new Auth0Config();
    auth0Config.setDomain("local/");
    startIqTestServer(config -> {
      MultiTenantInsightConfig multiTenantConfig = (MultiTenantInsightConfig) config;
      multiTenantConfig.setAuth0Config(auth0Config);
      multiTenantConfig.setSearchConfig(searchIndexRule.getSearchConfig());
    });

    SearchIndexClient searchIndexClient = getCLMServer().getInstance(SearchIndexClient.class);

    // Handle HybridSearchIndexClient - extract the primary (OpenSearch) client
    if (searchIndexClient instanceof HybridSearchIndexClient) {
      HybridSearchIndexClient hybridClient = (HybridSearchIndexClient) searchIndexClient;
      openSearchSearchIndexClient = (OpenSearchSearchIndexClient) hybridClient.getPrimaryClient();
    }
    else if (searchIndexClient instanceof OpenSearchSearchIndexClient) {
      openSearchSearchIndexClient = (OpenSearchSearchIndexClient) searchIndexClient;
    }
    else {
      throw new IllegalStateException(
          "Expected HybridSearchIndexClient or OpenSearchSearchIndexClient, but got: "
              + searchIndexClient.getClass().getName());
    }

    indexConfigProvider = getCLMServer().getInstance(MultiTenantIndexConfigProvider.class);
    indexConfig = indexConfigProvider.getIndexConfig();
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIndex_onTenantProvisioning() throws Exception {
    // Tenant is already provisioned by the AbstractMultiTenantBaseIntegrationTest, including the index creation
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
  public void testPopulateIndex_onSecondTenantProvisioning() throws Exception {
    provisionTenant("second-tenant");
    String indexName = "second-tenant-mtiq-index";

    GetIndexRequest getIndexRequest = new GetIndexRequest.Builder()
        .index(indexName)
        .build();
    GetIndexResponse getIndexResponse = openSearchSearchIndexClient.getClient()
        .indices()
        .get(getIndexRequest);

    Map<String, IndexState> result = getIndexResponse.result();
    assertThat(result).hasSize(1);
    Entry<String, IndexState> entry = result.entrySet().iterator().next();
    assertThat(entry.getKey()).startsWith(indexName);
    String expectedMappingsJson = JsonUtils.writeUnformatted(indexConfig.getIndexMapping().getMappings());
    String actualMappingsJson = JsonUtils.writeUnformatted(entry.getValue().mappings().properties());
    assertThatJson(actualMappingsJson).isEqualTo(expectedMappingsJson);
  }

  @Test
  @ManualIqServerInit
  public void testPopulateIndex_IndexAlreadyExists() throws IOException {
    // Tenant is already provisioned by the AbstractMultiTenantBaseIntegrationTest, including the index creation
    OpenSearchClient openSearchClient = spy(openSearchSearchIndexClient.getClient());
    OpenSearchIndicesClient openSearchIndicesClient = spy(openSearchClient.indices());

    openSearchSearchIndexClient.populateIndex();

    verify(openSearchIndicesClient, never()).create(any(CreateIndexRequest.class));
  }
}
