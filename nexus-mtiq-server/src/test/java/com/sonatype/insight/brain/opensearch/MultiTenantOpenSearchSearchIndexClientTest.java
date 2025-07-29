/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.opensearch;

import java.io.IOException;

import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.IndexConfig;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@OpenSearchHttpTest
public class MultiTenantOpenSearchSearchIndexClientTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private OpenSearchSearchIndexClient openSearchSearchIndexClient; //under test

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
    openSearchSearchIndexClient = (OpenSearchSearchIndexClient) getCLMServer().getInstance(SearchIndexClient.class);
    indexConfigProvider = getCLMServer().getInstance(MultiTenantIndexConfigProvider.class);
    indexConfig = indexConfigProvider.getIndexConfig();
  }

  @Test
  @ManualIqServerInit
  public void testCreateIndex_onTenantProvisioning() throws Exception {
    //Tenant is already provisioned by the AbstractMultiTenantBaseIntegrationTest, including the index creation
    GetIndexRequest getIndexRequest = new GetIndexRequest.Builder()
        .index(indexConfig.getIndexName())
        .build();
    GetIndexResponse getIndexResponse = openSearchSearchIndexClient.getClient()
        .indices()
        .get(getIndexRequest);

    assertThat(getIndexResponse.result()).containsKey(indexConfig.getIndexName());
    String expectedMappingsJson = JsonUtils.writeUnformatted(indexConfig.getIndexMapping().getMappings());
    String actualMappingsJson = JsonUtils.writeUnformatted(
        getIndexResponse.result().get(indexConfig.getIndexName()).mappings().properties());
    assertThatJson(actualMappingsJson).isEqualTo(expectedMappingsJson);
  }

  @Test
  @ManualIqServerInit
  public void testCreateIndex_onSecondTenantProvisioning() throws Exception {
    provisionTenant("second-tenant");

    GetIndexRequest getIndexRequest = new GetIndexRequest.Builder()
        .index("second-tenant-mtiq-index")
        .build();
    GetIndexResponse getIndexResponse = openSearchSearchIndexClient.getClient()
        .indices()
        .get(getIndexRequest);

    assertThat(getIndexResponse.result()).containsKey("second-tenant-mtiq-index");
    String expectedMappingsJson = JsonUtils.writeUnformatted(indexConfig.getIndexMapping().getMappings());
    String actualMappingsJson = JsonUtils.writeUnformatted(
        getIndexResponse.result().get("second-tenant-mtiq-index").mappings().properties());
    assertThatJson(actualMappingsJson).isEqualTo(expectedMappingsJson);
  }

  @Test
  @ManualIqServerInit
  public void testCreateIndex_IndexAlreadyExists() throws IOException {
    //Tenant is already provisioned by the AbstractMultiTenantBaseIntegrationTest, including the index creation
    OpenSearchClient openSearchClient = spy(openSearchSearchIndexClient.getClient());
    OpenSearchIndicesClient openSearchIndicesClient = spy(openSearchClient.indices());

    openSearchSearchIndexClient.createIndex();

    verify(openSearchIndicesClient, never()).create(any(CreateIndexRequest.class));
  }
}
