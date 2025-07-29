/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

import com.google.common.annotations.VisibleForTesting;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenSearch support for {@link SearchIndexClient}
 * <p>
 * Note: See {@link com.sonatype.insight.brain.search.SearchModule} for Guice bindings
 */
@Singleton
public class OpenSearchSearchIndexClient
    implements SearchIndexClient
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchSearchIndexClient.class);

  private final OpenSearchTransport openSearchTransport;

  private final IndexConfigProvider indexConfigProvider;

  private OpenSearchClient openSearchClient;

  @Inject
  public OpenSearchSearchIndexClient(
      final OpenSearchTransport openSearchTransport,
      final IndexConfigProvider indexConfigProvider)
  {
    this.openSearchTransport = openSearchTransport;
    this.indexConfigProvider = indexConfigProvider;
  }

  @VisibleForTesting
  public OpenSearchClient getClient() {
    if (openSearchClient == null) {
      openSearchClient = new OpenSearchClient(openSearchTransport);
      createIndexIfNotExists();
    }
    return openSearchClient;
  }

  @Override
  public void createIndex() {
    //TODO Add the corresponding index documents when calling this method
    createIndexIfNotExists();
  }

  @Override
  public void updateIndex() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public Long getLastIndexTime() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public long getIndexSize() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public SearchResultDTO searchIndex(
      final String searchQuery,
      final int pageSize,
      final int page,
      final boolean allComponents,
      final boolean isSbomManagerMode)
  {
    throw new UnsupportedOperationException("not yet implemented");
  }

  private void createIndexIfNotExists() {
    IndexConfig indexConfig = indexConfigProvider.getIndexConfig();

    try {
      ExistsRequest existsRequest = new ExistsRequest.Builder()
          .index(indexConfig.getIndexName())
          .build();
      BooleanResponse existResponse = getClient().indices().exists(existsRequest);

      if (existResponse.value()) {
        log.debug("OpenSearch index '{}' already exists", indexConfig.getIndexName());
        return;
      }

      log.info("Creating OpenSearch index: {}", indexConfig.getIndexName());
      TypeMapping typeMapping = new TypeMapping.Builder()
          .properties(indexConfig.getIndexMapping().getMappings())
          .build();
      CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
          .index(indexConfig.getIndexName())
          .mappings(typeMapping)
          .build();
      CreateIndexResponse createIndexResponse = getClient().indices().create(createIndexRequest);

      if (Boolean.TRUE.equals(createIndexResponse.acknowledged())) {
        log.info("OpenSearch index '{}' created successfully", createIndexResponse.index());
      }
    }
    catch (Exception e) {
      throw new RuntimeException(String.format("Error creating OpenSearch index: '%s'", indexConfig.getIndexName()), e);
    }
  }
}
