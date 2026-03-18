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
 * Test utility for creating OpenSearch transports with isolated connection managers.
 * <p>
 * This utility provides test-specific transport creation that allows tests to safely
 * close transports without affecting other tests. Each transport created by this
 * utility gets its own isolated connection manager.
 * <p>
 * <b>When to use this utility:</b>
 * <ul>
 * <li>Tests that use OpenSearch testcontainers (@ClassRule with OpensearchContainer)</li>
 * <li>Tests that need to close transports for cleanup</li>
 * <li>Tests that should not share connection state with other tests</li>
 * </ul>
 * <p>
 * <b>Note:</b> This is a test-only utility and should not be used in production code.
 * Production code should use {@link OpenSearchTransportFactory} which uses a shared
 * connection manager for efficiency.
 */
public class TestOpenSearchTransportFactory
{
  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private TestOpenSearchTransportFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates an OpenSearchTransport with an isolated connection manager for testing.
   * <p>
   * This method creates a new connection manager for each transport instance, ensuring
   * that tests can safely close transports without affecting other tests. The isolated
   * connection manager prevents "Connection pool shut down" errors that occur when tests
   * share a static connection manager.
   *
   * @param httpOpenSearchConfig the HTTP OpenSearch configuration
   * @return an OpenSearchTransport instance with its own isolated connection manager
   */
  public static OpenSearchTransport createIsolatedForTest(final HttpOpenSearchConfig httpOpenSearchConfig) {
    final HttpHost host = HttpHost.create(httpOpenSearchConfig.getUri());

    final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(host),
        new UsernamePasswordCredentials(httpOpenSearchConfig.getUsername(),
            httpOpenSearchConfig.getPassword().toCharArray()));

    // Create a new isolated connection manager for this transport
    // This ensures test isolation and allows safe cleanup via transport.close()
    final PoolingAsyncClientConnectionManager isolatedConnectionManager =
        PoolingAsyncClientConnectionManagerBuilder
            .create()
            .build();

    return ApacheHttpClient5TransportBuilder.builder(host)
        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
            .setDefaultCredentialsProvider(credentialsProvider)
            .setConnectionManager(isolatedConnectionManager))
        .build();
  }
}
