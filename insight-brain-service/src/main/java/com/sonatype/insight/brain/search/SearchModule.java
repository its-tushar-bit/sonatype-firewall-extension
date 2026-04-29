/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import jakarta.inject.Singleton;

import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.AwsSdkHttpClientProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchTransportProvider;
import com.sonatype.insight.brain.service.InsightConfig;

import org.opensearch.client.transport.OpenSearchTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Guice module to bind the appropriate search classes based on the search configuration.
 * <p>
 * Supports three search modes:
 * <ul>
 * <li>{@link SearchMode#LUCENE}: built-in Lucene only (also the default when no search config is present)</li>
 * <li>{@link SearchMode#OPENSEARCH}: OpenSearch only, no Lucene fallback</li>
 * <li>{@link SearchMode#HYBRID}: OpenSearch primary with automatic Lucene fallback</li>
 * </ul>
 */
public class SearchModule
    extends DropwizardAwareModule<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(SearchModule.class);

  private final SearchConfigSupplier searchConfigSupplier;

  public SearchModule() {
    this.searchConfigSupplier = () -> configuration().getSearchConfig();
  }

  public SearchModule(final SearchConfigSupplier searchConfigSupplier) {
    this.searchConfigSupplier = searchConfigSupplier;
  }

  @Override
  public void configure() {
    SearchConfig searchConfig = searchConfigSupplier.getSearchConfig();
    if (searchConfig == null) {
      log.debug("Using Lucene search");
      configureLuceneOnly();
    }
    else {
      searchConfig.validate();

      switch (searchConfig.getMode()) {
        case LUCENE -> {
          log.info("Using Lucene search");
          configureLuceneOnly();
        }
        case OPENSEARCH -> {
          log.debug("Using OpenSearch search");
          bind(SearchConfig.class).toInstance(searchConfig);
          configureOpenSearchTransport(searchConfig);
          configureOpenSearchOnly();
        }
        case HYBRID -> {
          log.debug("Using hybrid search (OpenSearch with Lucene fallback)");
          bind(SearchConfig.class).toInstance(searchConfig);
          configureOpenSearchTransport(searchConfig);
          configureHybridMode();
        }
        default -> throw new IllegalStateException("Unknown search mode: " + searchConfig.getMode());
      }
    }
  }

  private void configureLuceneOnly() {
    bind(SearchIndexClient.class).to(LuceneSearchIndexClient.class);
    bind(LuceneSearchIndexClient.class);
  }

  private void configureOpenSearchOnly() {
    bind(SearchIndexClient.class).to(OpenSearchSearchIndexClient.class);
    bind(OpenSearchSearchIndexClient.class);
  }

  private void configureHybridMode() {
    bind(SearchIndexClient.class).annotatedWith(Names.named("primary")).to(OpenSearchSearchIndexClient.class);
    bind(OpenSearchSearchIndexClient.class);

    bind(SearchIndexClient.class).annotatedWith(Names.named("secondary")).to(LuceneSearchIndexClient.class);
    bind(LuceneSearchIndexClient.class);

    bind(SearchIndexClient.class).to(HybridSearchIndexClient.class);
    bind(HybridSearchIndexClient.class);
  }

  /**
   * Configures OpenSearch transport and AWS SDK client if using AWS OpenSearch.
   * <p>
   * <strong>IMPORTANT:</strong> The SdkHttpClient bound here is exclusively for OpenSearch use.
   * It should not be injected by other components. Its lifecycle is managed by OpenSearchTransportProvider.
   * If other components need an SdkHttpClient in the future, they should create their own separate instance
   * with independent lifecycle management.
   */
  private void configureOpenSearchTransport(SearchConfig searchConfig) {
    // Use OptionalBinder for SdkHttpClient to allow optional injection in OpenSearchTransportProvider
    // Only bind for AWS OpenSearch configurations to avoid unnecessary resource allocation
    // This client is exclusively owned by OpenSearch components and should not be used elsewhere
    OptionalBinder<SdkHttpClient> optionalSdkHttpClient =
        OptionalBinder.newOptionalBinder(binder(), SdkHttpClient.class);
    if (searchConfig instanceof SearchConfig.AwsHttpOpenSearchConfig) {
      optionalSdkHttpClient.setBinding().toProvider(AwsSdkHttpClientProvider.class);
    }

    // Bind OpenSearchTransport as singleton to ensure single instance is reused
    // This is critical for AWS OpenSearch to maintain consistent credential signing
    bind(OpenSearchTransport.class).toProvider(OpenSearchTransportProvider.class).in(Singleton.class);
  }
}
