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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@OpenSearchHttpTest
public class OpenSearchSearchIndexClientTest
    extends AbstractBrainServiceIntegrationTest
{
  private OpenSearchSearchIndexClient openSearchSearchIndexClient; //under test

  private IndexConfigProvider indexConfigProvider;

  private IndexConfig indexConfig;

  private CurrentUser currentUser;

  @Override
  public void configure(Binder binder) {
    currentUser = mock(CurrentUser.class);
    binder.bind(CurrentUser.class).toInstance(currentUser);
    super.configure(binder);
  }

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

  @Test
  @ManualIqServerInit
  public void testSearchIndex_HandlesVeryLongQuery() {
    openSearchSearchIndexClient.populateIndex();

    UserPrincipal userPrincipal = new UserPrincipal("username", "displayName", InternalRealm.ID);
    when(currentUser.getUserPrincipal()).thenReturn(userPrincipal);

    StringBuilder longQueryBuilder = new StringBuilder();
    longQueryBuilder.append("applicationName:(");
    // Create a very long application name that would exceed 4096 bytes when URL encoded
    for (int i = 0; i < 1000; i++) {
      longQueryBuilder.append("VeryLongApplicationNameThatWouldExceedHttpLineLimitsWhenPutInTheUrlAsAQueryParameter");
      if (i < 999) {
        longQueryBuilder.append(" OR ");
      }
    }
    longQueryBuilder.append(")");
    String longQuery = longQueryBuilder.toString();

    assertThat(longQuery.length()).isGreaterThan(4096);

    SearchResultDTO result = openSearchSearchIndexClient.searchIndex(longQuery, 10, 1, false, false, null);

    assertThat(result).isNotNull();
    assertThat(result.searchQuery).isEqualTo(longQuery);
  }

  @Test
  public void testSearchIndex_ReturnsOldIndexResultsDuringFullReindex() throws Exception {
    UserPrincipal userPrincipal = new UserPrincipal("username", "displayName", InternalRealm.ID);
    when(currentUser.getUserPrincipal()).thenReturn(userPrincipal);
    Role role = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), "username");

    // Create the first org and check search works
    Organization org1 = tempEntity.newOrganization();
    openSearchSearchIndexClient.populateIndex();
    SearchResultDTO result = openSearchSearchIndexClient.searchIndex(
        "itemType:ORGANIZATION AND organizationId:" + org1.getId(), 10, 1, false, false, null);
    assertThat(result.totalNumberOfHits).isEqualTo(1);

    // Trigger a full re-index in a new thread
    OpenSearchSearchIndexClient spy = spy(openSearchSearchIndexClient);
    CountDownLatch preFullReindexBlock = new CountDownLatch(1);
    CountDownLatch fullReindexBlock = new CountDownLatch(1);
    AtomicReference<String> oldRealIndexName = new AtomicReference<>();
    doAnswer(invocation -> {
      oldRealIndexName.set(openSearchSearchIndexClient.getRealIndexName());
      invocation.callRealMethod();
      preFullReindexBlock.countDown();
      assertThat(fullReindexBlock.await(5, TimeUnit.SECONDS)).isTrue();
      return null;
    }).when(spy).createIndexAndOverwriteIfNeeded();
    Thread thread = new Thread(spy::populateIndex);
    thread.start();
    assertThat(preFullReindexBlock.await(5, TimeUnit.SECONDS)).isTrue();

    // Whilst it's waiting to trigger an update
    Organization org2 = tempEntity.newOrganization();
    // Documents before the full re-index should be available
    result = openSearchSearchIndexClient.searchIndex(
        "itemType:ORGANIZATION AND organizationId:" + org1.getId(), 10, 1, false, false, null);
    assertThat(result.totalNumberOfHits).isEqualTo(1);
    // New org should not be available yet
    result = openSearchSearchIndexClient.searchIndex(
        "itemType:ORGANIZATION AND organizationId:" + org2.getId(), 10, 1, false, false, null);
    assertThat(result.totalNumberOfHits).isZero();
    // Let the full re-index finish
    fullReindexBlock.countDown();
    // Force index updates to be processed
    openSearchSearchIndexClient.updateIndex();
    thread.join(5000);
    assertThat(thread.isAlive()).isFalse();
    // Force index updates to be processed
    openSearchSearchIndexClient.updateIndex();
    // Both orgs should be available just once
    result = openSearchSearchIndexClient.searchIndex(
        "itemType:ORGANIZATION AND organizationId:" + org1.getId(), 10, 1, false, false, null);
    assertThat(result.totalNumberOfHits).isEqualTo(1);
    result = openSearchSearchIndexClient.searchIndex(
        "itemType:ORGANIZATION AND organizationId:" + org2.getId(), 10, 1, false, false, null);
    assertThat(result.totalNumberOfHits).isEqualTo(1);
    // Check that the old search index is deleted
    IndicesResponse indicesResponse = openSearchSearchIndexClient.getClient().cat().indices();
    assertThat(indicesResponse.valueBody()).extracting(IndicesRecord::index).doesNotContain(oldRealIndexName.get());
  }
}
