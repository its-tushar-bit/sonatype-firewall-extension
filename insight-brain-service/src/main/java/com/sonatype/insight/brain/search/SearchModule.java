/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchTransportFactory;
import com.sonatype.insight.brain.service.InsightConfig;

import org.opensearch.client.transport.OpenSearchTransport;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

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
      bind(SearchIndexClient.class).to(OpenSearchSearchIndexClient.class);
      bind(OpenSearchSearchIndexClient.class);
      bind(SearchConfig.class).toInstance(searchConfig);

      if (searchConfig instanceof HttpOpenSearchConfig) {
        bind(OpenSearchTransport.class).toInstance(
            OpenSearchTransportFactory.create((HttpOpenSearchConfig) searchConfig));
      }
    }
  }
}
