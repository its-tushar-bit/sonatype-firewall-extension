/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenSearchIndexingContextTest
{
  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private IndexConfigProvider indexConfigProvider;

  @Mock
  private OpenSearchClient openSearchClient;

  @Mock
  private IndexConfig indexConfig;

  @BeforeEach
  public void setUp() {
    lenient().when(indexConfigProvider.getIndexConfig()).thenReturn(indexConfig);
    lenient().when(indexConfig.getIndexName()).thenReturn("test-index");
    // Return a mutable map so the code can add the timestamp
    lenient().when(conversionHelper.documentToMap(any())).thenAnswer(invocation -> new java.util.HashMap<>());
  }

  @Test
  public void testAddDocuments_EmptyList_DoesNothing() throws Exception {
    // Given
    OpenSearchIndexingContext context = createContext(1000, 0, 1, 1000, 10000);

    // When
    context.addDocuments(Collections.emptyList());

    // Then - No calls to OpenSearch
    verify(openSearchClient, times(0)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_SingleBatch_Success() throws Exception {
    // Given
    OpenSearchIndexingContext context = createContext(1000, 0, 1, 1000, 10000);
    List<Document> documents = createDocuments(10);
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(successResponse);

    // When
    context.addDocuments(documents);

    // Then - Single bulk request
    verify(openSearchClient, times(1)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_MultipleBatches_Success() throws Exception {
    // Given - Batch size of 5, with 12 documents = 3 batches
    OpenSearchIndexingContext context = createContext(5, 0, 1, 1000, 10000);
    List<Document> documents = createDocuments(12);
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(successResponse);

    // When
    context.addDocuments(documents);

    // Then - Three bulk requests (5 + 5 + 2)
    verify(openSearchClient, times(3)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_WithBatchDelay_AppliesDelay() throws Exception {
    // Given - Batch size of 5, delay of 100ms, with 10 documents = 2 batches
    OpenSearchIndexingContext context = createContext(5, 100, 1, 1000, 10000);
    List<Document> documents = createDocuments(10);
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(successResponse);

    // When
    long startTime = System.currentTimeMillis();
    context.addDocuments(documents);
    long duration = System.currentTimeMillis() - startTime;

    // Then - Should have at least one delay (100ms between first and second batch)
    assertThat(duration).isGreaterThanOrEqualTo(100);
    verify(openSearchClient, times(2)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_RateLimitError_RetriesAndSucceeds() throws Exception {
    // Given - Max 3 retries, will fail twice (attempt 1 & 2) then succeed on 3rd attempt
    OpenSearchIndexingContext context = createContext(100, 0, 3, 100, 10000);
    List<Document> documents = createDocuments(5);

    RuntimeException rateLimitException = createRateLimitException();
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class)))
        .thenThrow(rateLimitException)
        .thenThrow(rateLimitException)
        .thenReturn(successResponse);

    // When
    context.addDocuments(documents);

    // Then - Should succeed after 2 retries (3 attempts total: 1 initial + 2 retries)
    verify(openSearchClient, times(3)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_RateLimitError_ExhaustsRetriesAndFails() throws Exception {
    // Given - Max 2 retries (3 total attempts: 1 initial + 2 retries), will fail all attempts
    OpenSearchIndexingContext context = createContext(100, 0, 2, 100, 10000);
    List<Document> documents = createDocuments(5);

    RuntimeException rateLimitException = createRateLimitException();

    when(openSearchClient.bulk(any(BulkRequest.class)))
        .thenThrow(rateLimitException);

    // When/Then - Should fail after exhausting retries
    assertThatThrownBy(() -> context.addDocuments(documents))
        .isInstanceOf(OpenSearchException.class)
        .hasMessageContaining("Request failed: [error] server returned 429");

    // Should attempt 3 times (1 initial + 2 retries)
    verify(openSearchClient, times(3)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_NonRateLimitError_FailsImmediately() throws Exception {
    // Given
    OpenSearchIndexingContext context = createContext(100, 0, 3, 100, 10000);
    List<Document> documents = createDocuments(5);

    IOException otherException = new IOException("Connection timeout");

    when(openSearchClient.bulk(any(BulkRequest.class)))
        .thenThrow(otherException);

    // When/Then - Should fail immediately without retries
    assertThatThrownBy(() -> context.addDocuments(documents))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Connection timeout");

    // Should only attempt once (no retries for non-rate-limit errors)
    verify(openSearchClient, times(1)).bulk(any(BulkRequest.class));
  }

  @Test
  public void testAddDocuments_BulkResponseWithErrors_ThrowsException() throws Exception {
    // Given
    OpenSearchIndexingContext context = createContext(100, 0, 1, 1000, 10000);
    List<Document> documents = createDocuments(5);

    BulkResponse errorResponse = mock(BulkResponse.class);
    when(errorResponse.errors()).thenReturn(true);
    when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(errorResponse);

    // When/Then
    assertThatThrownBy(() -> context.addDocuments(documents))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to add batch of 5 documents to index 'test-index'");
  }

  @Test
  public void testIsRateLimitError_DirectOpenSearchException() {
    // Given
    Exception ex = createOpenSearchException("server returned 429", 429);

    // When/Then
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(ex)).isTrue();
  }

  @Test
  public void testIsRateLimitError_NestedOpenSearchException() {
    // Given
    Exception cause = createOpenSearchException("server returned 429", 429);
    CompletionException wrapper = new CompletionException("Async operation failed", cause);

    // When/Then
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(wrapper)).isTrue();
  }

  @Test
  public void testIsRateLimitError_TooManyRequestsMessage() {
    // Given
    Exception ex = createOpenSearchException("Too Many Requests", 429);

    // When/Then
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(ex)).isTrue();
  }

  @Test
  public void testIsRateLimitError_NonRateLimitError() {
    // Given
    IOException ex = new IOException("Connection timeout");

    // When/Then
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(ex)).isFalse();
  }

  @Test
  public void testIsRateLimitError_OpenSearchExceptionWithoutRateLimit() {
    // Given
    Exception ex = createOpenSearchException("server returned 500", 500);

    // When/Then
    assertThat(OpenSearchSearchIndexClient.isRateLimitError(ex)).isFalse();
  }

  @Test
  public void testExponentialBackoff_FirstAttempt() throws Exception {
    // Given - Backoff base of 1000ms (1 second)
    OpenSearchIndexingContext context = createContext(100, 0, 3, 1000, 10000);
    List<Document> documents = createDocuments(5);

    RuntimeException rateLimitException = createRateLimitException();
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class)))
        .thenThrow(rateLimitException)
        .thenReturn(successResponse);

    // When
    long startTime = System.currentTimeMillis();
    context.addDocuments(documents);
    long duration = System.currentTimeMillis() - startTime;

    // Then - Should wait ~1 second for first retry (1000ms * 2^0)
    assertThat(duration).isGreaterThanOrEqualTo(1000);
    assertThat(duration).isLessThan(1500); // Allow some margin
  }

  @Test
  public void testExponentialBackoff_MultipleAttempts() throws Exception {
    // Given - Backoff base of 500ms, will fail twice
    OpenSearchIndexingContext context = createContext(100, 0, 3, 500, 10000);
    List<Document> documents = createDocuments(5);

    RuntimeException rateLimitException = createRateLimitException();
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class)))
        .thenThrow(rateLimitException)
        .thenThrow(rateLimitException)
        .thenReturn(successResponse);

    // When
    long startTime = System.currentTimeMillis();
    context.addDocuments(documents);
    long duration = System.currentTimeMillis() - startTime;

    // Then - Should wait: 500ms (first retry) + 1000ms (second retry) = 1500ms
    assertThat(duration).isGreaterThanOrEqualTo(1500);
    assertThat(duration).isLessThan(2000); // Allow some margin
  }

  @Test
  public void testExponentialBackoff_MaxBackoffLimit() throws Exception {
    // Given - Backoff base of 100ms, max of 150ms, will fail twice then succeed
    OpenSearchIndexingContext context = createContext(100, 0, 3, 100, 150);
    List<Document> documents = createDocuments(5);

    RuntimeException rateLimitException = createRateLimitException();
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class)))
        .thenThrow(rateLimitException)
        .thenThrow(rateLimitException)
        .thenReturn(successResponse);

    // When
    long startTime = System.currentTimeMillis();
    context.addDocuments(documents);
    long duration = System.currentTimeMillis() - startTime;

    // Then - Should apply max cap
    // Attempt 1: 100ms (100ms * 2^0)
    // Attempt 2: 200ms capped to 150ms
    // Total: ~250ms
    assertThat(duration).isGreaterThanOrEqualTo(250);
    assertThat(duration).isLessThan(400); // Allow margin but keep test fast
  }

  @Test
  public void testAddDocuments_InterruptedDuringDelay_ThrowsIOException() throws Exception {
    // Given
    OpenSearchIndexingContext context = createContext(5, 10000, 1, 1000, 10000);
    List<Document> documents = createDocuments(10);
    BulkResponse successResponse = mockSuccessfulBulkResponse();

    when(openSearchClient.bulk(any(BulkRequest.class))).thenReturn(successResponse);

    // When - Interrupt the thread during execution
    Thread testThread = new Thread(() -> {
      try {
        Thread.currentThread().interrupt(); // Interrupt before adding documents
        context.addDocuments(documents);
      }
      catch (IOException e) {
        // Expected
        assertThat(e).hasMessageContaining("Interrupted while waiting between batches");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
      }
    });

    testThread.start();
    testThread.join(2000);

    // Then - Should throw IOException on interruption
    assertThat(testThread.isAlive()).isFalse();
  }

  private OpenSearchIndexingContext createContext(
      int batchSize,
      int batchDelayMs,
      int maxRetries,
      int retryBackoffMs,
      int maxRetryBackoffMs)
  {
    return new OpenSearchIndexingContext(
        ownerDAO,
        conversionHelper,
        indexConfigProvider,
        openSearchClient,
        batchSize,
        batchDelayMs,
        maxRetries,
        retryBackoffMs,
        maxRetryBackoffMs);
  }

  private List<Document> createDocuments(int count) {
    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Document doc = new Document();
      doc.add(new StringField("id", "doc-" + i, Field.Store.YES));
      documents.add(doc);
    }
    return documents;
  }

  private BulkResponse mockSuccessfulBulkResponse() {
    BulkResponse response = mock(BulkResponse.class);
    when(response.errors()).thenReturn(false);
    return response;
  }

  /**
   * Creates a RuntimeException that wraps an OpenSearchException with a rate limit message.
   */
  private RuntimeException createRateLimitException() {
    return createOpenSearchException("server returned 429", 429);
  }

  /**
   * Creates an OpenSearchException with the given message and status code.
   */
  private OpenSearchException createOpenSearchException(String message, int status) {
    ErrorCause errorCause = ErrorCause.of(builder -> builder
        .type("error")
        .reason(message));

    ErrorResponse errorResponse = ErrorResponse.of(builder -> builder
        .error(errorCause)
        .status(status));

    return new OpenSearchException(errorResponse);
  }
}
