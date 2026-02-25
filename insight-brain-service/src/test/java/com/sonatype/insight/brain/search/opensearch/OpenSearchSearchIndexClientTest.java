/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.sonatype.insight.brain.common.test.SlowTest;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.apache.lucene.queryparser.classic.ParseException;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@OpenSearchHttpTest
@Category(SlowTest.class)
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
      invocation.callRealMethod();
      preFullReindexBlock.countDown();
      assertThat(fullReindexBlock.await(5, TimeUnit.SECONDS)).isTrue();
      return null;
    }).when(spy).createIndex(anyString());
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

  @Test
  public void testIsChangeSpecificError_ParseException() {
    Exception e = new IOException(new ParseException("Parse error"));
    assertThat(openSearchSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_IllegalArgumentException() {
    Exception e = new IOException(new IllegalArgumentException("Invalid field"));
    assertThat(openSearchSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_NullPointerException() {
    Exception e = new IOException(new NullPointerException("Null field"));
    assertThat(openSearchSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_MapperParsingException() {
    Exception e = new RuntimeException("mapper_parsing_exception: failed to parse field");
    assertThat(openSearchSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_DocumentParsingException() {
    Exception e = new RuntimeException("document_parsing_exception: failed to parse document");
    assertThat(openSearchSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_UnknownHostException() {
    Exception e = new UnknownHostException("opensearch.example.com");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_SocketException() {
    Exception e = new SocketException("Connection reset");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_SocketTimeoutException() {
    Exception e = new SocketTimeoutException("Read timed out");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_TimeoutException() {
    Exception e = new IOException(new TimeoutException("Operation timed out"));
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_OpenSearchException500() {
    OpenSearchException e = createOpenSearchException("Internal server error", 500);
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_OpenSearchException503() {
    OpenSearchException e = createOpenSearchException("Service unavailable", 503);
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_CircuitBreakerException() {
    OpenSearchException e = createOpenSearchExceptionWithType("circuit_breaking_exception");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_ClusterBlockException() {
    OpenSearchException e = createOpenSearchExceptionWithType("cluster_block_exception");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_RateLimitError() {
    OpenSearchException e = createOpenSearchException("Too many requests", 429);
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsRateLimitError_Status429() {
    OpenSearchException e = createOpenSearchException("Too many requests", 429);
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(e)).isTrue();
  }

  @Test
  public void testIsRateLimitError_TooManyRequestsMessage() {
    OpenSearchException e = createOpenSearchExceptionWithReason("too many requests");
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(e)).isTrue();
  }

  @Test
  public void testIsRateLimitError_NotARateLimitError() {
    Exception e = new IOException("Generic error");
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(e)).isFalse();
  }

  @Test
  public void testIsSystemicError_UnavailableShardsException() {
    OpenSearchException e = createOpenSearchExceptionWithType("unavailable_shards_exception");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_NoShardAvailableActionException() {
    OpenSearchException e = createOpenSearchExceptionWithType("no_shard_available_action_exception");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_MasterNotDiscoveredException() {
    OpenSearchException e = createOpenSearchExceptionWithType("master_not_discovered_exception");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_ThrottlingInReason() {
    OpenSearchException e = createOpenSearchExceptionWithReason("Request throttling applied");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_InternalFailureInReason() {
    OpenSearchException e = createOpenSearchExceptionWithReason("Internal failure occurred");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_TimeoutInMessage() {
    Exception e = new RuntimeException("Connection timeout while indexing");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_ServiceUnavailableInMessage() {
    Exception e = new RuntimeException("Service unavailable");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_ConnectionRefusedInMessage() {
    Exception e = new RuntimeException("Connection refused by server");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_UnreachableInMessage() {
    Exception e = new RuntimeException("Host unreachable");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_ThrottlingInMessage() {
    Exception e = new RuntimeException("Throttling limit exceeded");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_InternalFailureInMessage() {
    Exception e = new RuntimeException("Internal failure detected");
    assertThat(openSearchSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_IllegalArgumentExceptionMessage() {
    Exception e = new RuntimeException("illegal_argument_exception: invalid field value");
    assertThat(openSearchSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  private OpenSearchException createOpenSearchException(String reason, int status) {
    ErrorCause errorCause = ErrorCause.of(builder -> builder
        .type("test_error")
        .reason(reason));
    ErrorResponse errorResponse = ErrorResponse.of(builder -> builder
        .error(errorCause)
        .status(status));
    return new OpenSearchException(errorResponse);
  }

  private OpenSearchException createOpenSearchExceptionWithType(String type) {
    ErrorCause errorCause = ErrorCause.of(builder -> builder
        .type(type)
        .reason("Test error"));
    ErrorResponse errorResponse = ErrorResponse.of(builder -> builder
        .error(errorCause)
        .status(400));
    return new OpenSearchException(errorResponse);
  }

  private OpenSearchException createOpenSearchExceptionWithReason(String reason) {
    ErrorCause errorCause = ErrorCause.of(builder -> builder
        .type("test_error")
        .reason(reason));
    ErrorResponse errorResponse = ErrorResponse.of(builder -> builder
        .error(errorCause)
        .status(400));
    return new OpenSearchException(errorResponse);
  }
}
