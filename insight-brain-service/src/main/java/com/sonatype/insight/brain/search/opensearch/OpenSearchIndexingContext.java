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

public class OpenSearchIndexingContext
    extends IndexingContext
{
  private final IndexConfigProvider indexConfigProvider;

  private final OpenSearchClient openSearchClient;

  public OpenSearchIndexingContext(
      final OwnerDAO ownerDAO,
      final ConversionHelper conversionHelper,
      final IndexConfigProvider indexConfigProvider,
      final OpenSearchClient openSearchClient)
  {
    super(ownerDAO, conversionHelper);
    this.indexConfigProvider = indexConfigProvider;
    this.openSearchClient = openSearchClient;
  }

  @Override
  public void deleteDocuments(final String query) throws IOException {
    DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
        .index(indexConfigProvider.getIndexConfig().getIndexName())
        .q(query)
        .refresh(true)
    );
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
    BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
    long now = System.currentTimeMillis();
    for (Document document : documents) {
      Map<String, Object> documentMap = getConversionHelper().documentToMap(document);
      documentMap.put(IndexMapping.CREATED_AT_EPOCH_MS, now);
      bulkBuilder.operations(op -> op.index(idx -> idx
              .index(indexConfigProvider.getIndexConfig().getIndexName())
              .document(documentMap)
          )
      ).refresh(Refresh.WaitFor);
    }
    BulkResponse bulkResponse = openSearchClient.bulk(bulkBuilder.build());
    if (bulkResponse.errors()) {
      throw new RuntimeException("Failed to add documents.");
    }
  }
}
