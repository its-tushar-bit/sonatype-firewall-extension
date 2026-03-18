/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.IndexingContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.document.Document;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.BulkIndexByScrollFailure;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteByQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchIndexingContext
    extends IndexingContext
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchIndexingContext.class);

  private final IndexConfigProvider indexConfigProvider;

  private final OpenSearchClient openSearchClient;

  private final int batchSize;

  private final int batchDelayMs;

  private final int maxRetries;

  private final int retryBackoffMs;

  private final int maxRetryBackoffMs;

  public OpenSearchIndexingContext(
      final OwnerDAO ownerDAO,
      final ConversionHelper conversionHelper,
      final IndexConfigProvider indexConfigProvider,
      final OpenSearchClient openSearchClient,
      final int batchSize,
      final int batchDelayMs,
      final int maxRetries,
      final int retryBackoffMs,
      final int maxRetryBackoffMs)
  {
    super(ownerDAO, conversionHelper);
    this.indexConfigProvider = indexConfigProvider;
    this.openSearchClient = openSearchClient;
    this.batchSize = batchSize;
    this.batchDelayMs = batchDelayMs;
    this.maxRetries = maxRetries;
    this.retryBackoffMs = retryBackoffMs;
    this.maxRetryBackoffMs = maxRetryBackoffMs;
  }

  @Override
  public void deleteDocuments(final String query) throws IOException {
    DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
        .index(indexConfigProvider.getIndexConfig().getIndexName())
        .q(query)
        .refresh(true));
    DeleteByQueryResponse deleteByQueryResponse = openSearchClient.deleteByQuery(request);
    List<String> errors = getErrors(deleteByQueryResponse.failures(), deleteByQueryResponse.timedOut(),
        deleteByQueryResponse.versionConflicts());
    if (!errors.isEmpty()) {
      throw new RuntimeException("Failed to delete documents: " + errors + ".");
    }
  }

  private List<String> getErrors(
      final List<BulkIndexByScrollFailure> bulkIndexByScrollFailures,
      final Boolean timedOut,
      final Long versionConflicts)
  {
    List<String> errors = new ArrayList<>();
    if (Boolean.TRUE.equals(timedOut)) {
      errors.add("timed out");
    }
    if (versionConflicts != null && versionConflicts > 0) {
      errors.add(versionConflicts + " version conflicts");
    }
    if (CollectionUtils.isNotEmpty(bulkIndexByScrollFailures)) {
      bulkIndexByScrollFailures.forEach(
          bulkIndexByScrollFailure -> errors.add(bulkIndexByScrollFailure.cause().type()));
    }
    return errors;
  }

  @Override
  public void addDocuments(final List<Document> documents) throws IOException {
    long now = System.currentTimeMillis();
    int totalDocuments = documents.size();
    int batchCount = (int) Math.ceil((double) totalDocuments / batchSize);

    log.debug("Indexing {} documents in {} batches of up to {} documents each", totalDocuments, batchCount, batchSize);

    for (int i = 0; i < totalDocuments; i += batchSize) {
      int end = Math.min(i + batchSize, totalDocuments);
      List<Document> batch = documents.subList(i, end);
      int batchNumber = (i / batchSize) + 1;

      log.debug("Processing batch {}/{} ({} documents)", batchNumber, batchCount, batch.size());

      // Add batch with retry logic
      addDocumentBatchWithRetry(batch, now);

      // Add delay between batches (except after the last batch)
      if (end < totalDocuments && batchDelayMs > 0) {
        try {
          Thread.sleep(batchDelayMs);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting between batches", e);
        }
      }
    }

    log.debug("Successfully completed indexing {} documents in {} batches", totalDocuments, batchCount);
  }

  private void addDocumentBatchWithRetry(final List<Document> batch, final long createdAtEpochMs) throws IOException {
    int attempt = 0;
    int maxAttempts = maxRetries + 1; // 1 initial attempt + maxRetries retry attempts

    while (attempt < maxAttempts) {
      try {
        addDocumentBatch(batch, createdAtEpochMs);
        if (attempt > 0) {
          log.info("Successfully indexed batch after {} retries", attempt);
        }
        return;
      }
      catch (IOException | RuntimeException e) {
        attempt++;

        if (OpenSearchSearchIndexClient.isRateLimitError(e)) {
          if (attempt < maxAttempts) {
            int backoffMs = calculateBackoff(attempt);
            log.warn("Rate limit error (429) on attempt {}/{}. Retrying after {}s backoff. Error: {}",
                attempt, maxAttempts, backoffMs / 1000, e.getMessage());

            try {
              Thread.sleep(backoffMs);
            }
            catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              throw new IOException("Interrupted during retry backoff", ie);
            }
          }
          else {
            log.error("Rate limit error (429) persisted after {} retries. Giving up.", maxRetries, e);
            throw e;
          }
        }
        else {
          // Non-rate-limit error, don't retry
          throw e;
        }
      }
    }
  }

  private void addDocumentBatch(final List<Document> batch, final long createdAtEpochMs) throws IOException {
    BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
    String indexName = indexConfigProvider.getIndexConfig().getIndexName();

    for (Document document : batch) {
      Map<String, Object> documentMap = getConversionHelper().documentToMap(document);
      documentMap.put(IndexMapping.CREATED_AT_EPOCH_MS, createdAtEpochMs);
      bulkBuilder.operations(op -> op.index(idx -> idx
          .index(indexName)
          .document(documentMap))).refresh(Refresh.WaitFor);
    }

    BulkResponse bulkResponse = openSearchClient.bulk(bulkBuilder.build());
    if (bulkResponse.errors()) {
      throw new IOException(String.format("Failed to add batch of %d documents to index '%s'",
          batch.size(), indexName));
    }
  }

  private int calculateBackoff(int attempt) {
    // Exponential backoff: retryBackoffMs * 2^(attempt-1)
    // Protect against overflow by capping the shift and using long arithmetic
    if (attempt <= 1) {
      return Math.min(retryBackoffMs, maxRetryBackoffMs);
    }
    int shift = Math.min(attempt - 1, 30); // 1 << 30 fits in positive int
    long backoff = (long) retryBackoffMs * (1L << shift);
    long cappedBackoff = Math.min(backoff, (long) maxRetryBackoffMs);
    return (int) cappedBackoff;
  }
}
