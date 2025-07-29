/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * Factory to create instances of `OpenSearchTransport`. Currently the only type support is regular HTTP
 * but in the future we may use the specific classes in the SDK for AWS OpenSearch Service (better support
 * for Amazon specifics)
 */
public class OpenSearchTransportFactory
{
  private static final PoolingAsyncClientConnectionManager connectionManager =
      PoolingAsyncClientConnectionManagerBuilder
          .create()
          .build();

  public static OpenSearchTransport create(final HttpOpenSearchConfig httpOpenSearchConfig) {
    final HttpHost host = HttpHost.create(httpOpenSearchConfig.getUri());

    final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(host),
        new UsernamePasswordCredentials(httpOpenSearchConfig.getUsername(),
            httpOpenSearchConfig.getPassword().toCharArray()));

    return ApacheHttpClient5TransportBuilder.builder(host)
        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
            .setDefaultCredentialsProvider(credentialsProvider)
            .setConnectionManager(connectionManager))
        .build();
  }
}
