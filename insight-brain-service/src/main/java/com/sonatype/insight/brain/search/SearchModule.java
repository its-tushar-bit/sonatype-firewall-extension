/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import javax.inject.Singleton;

import com.google.inject.multibindings.OptionalBinder;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.AwsSdkHttpClientProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchTransportProvider;
import com.sonatype.insight.brain.service.InsightConfig;

import org.opensearch.client.transport.OpenSearchTransport;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Guice module to bind the appropriate search classes based on the search configuration.
 */
public class SearchModule
    extends DropwizardAwareModule<InsightConfig>
{
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
      bind(SearchIndexClient.class).to(LuceneSearchIndexClient.class);
      bind(LuceneSearchIndexClient.class);
    }
    else {
      // Validate configuration early to fail fast on startup
      searchConfig.validate();

      bind(SearchIndexClient.class).to(OpenSearchSearchIndexClient.class);
      bind(OpenSearchSearchIndexClient.class);
      bind(SearchConfig.class).toInstance(searchConfig);

      // Use OptionalBinder for SdkHttpClient to allow optional injection in OpenSearchTransportProvider
      // Only bind for AWS OpenSearch configurations to avoid unnecessary resource allocation
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
}
