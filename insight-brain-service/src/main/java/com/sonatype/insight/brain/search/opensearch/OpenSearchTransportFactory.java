/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;

/**
 * Factory to create instances of `OpenSearchTransport`. Supports both standalone HTTP OpenSearch and AWS OpenSearch
 * Service.
 */
public class OpenSearchTransportFactory
{
  public static OpenSearchTransport create(final HttpOpenSearchConfig httpOpenSearchConfig) {
    final HttpHost host = HttpHost.create(httpOpenSearchConfig.getUri());

    final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(host),
        new UsernamePasswordCredentials(httpOpenSearchConfig.getUsername(),
            httpOpenSearchConfig.getPassword().toCharArray()));

    // Create a new connection manager for each transport to avoid shared state issues
    // when transports are closed (which also closes the connection manager)
    final PoolingAsyncClientConnectionManager connectionManager =
        PoolingAsyncClientConnectionManagerBuilder.create().build();

    return ApacheHttpClient5TransportBuilder.builder(host)
        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
            .setDefaultCredentialsProvider(credentialsProvider)
            .setConnectionManager(connectionManager))
        .build();
  }

  /**
   * Creates an OpenSearchTransport for AWS OpenSearch Service.
   * <p>
   * Note: The SdkHttpClient must be managed by the caller. AwsSdk2Transport does not close the httpClient when it is
   * closed, so the caller is responsible for lifecycle management. Use a singleton provider to avoid creating multiple
   * instances.
   *
   * @param searchConfig the AWS OpenSearch configuration
   * @param credentialsProvider the AWS credentials provider
   * @param httpClient the SdkHttpClient to use (should be reused across calls to avoid resource leaks)
   * @return an AwsSdk2Transport instance
   */
  public static OpenSearchTransport create(
      final AwsHttpOpenSearchConfig searchConfig,
      final AwsCredentialsProvider credentialsProvider,
      final SdkHttpClient httpClient)
  {
    return new AwsSdk2Transport(
        httpClient,
        searchConfig.getDomain().getHost(),
        Region.of(searchConfig.getRegion()),
        AwsSdk2TransportOptions.builder()
            .setCredentials(credentialsProvider)
            .build());
  }
}
