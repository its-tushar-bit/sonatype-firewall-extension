/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

/**
 * Provider for a singleton SdkHttpClient used exclusively by AWS OpenSearch transport.
 * <p>
 * <strong>IMPORTANT: Exclusive Ownership</strong><br>
 * This SdkHttpClient is intended for use <strong>exclusively by OpenSearch components</strong>. It should NOT be
 * injected or used by any other components in the system. The lifecycle of this client is tightly coupled to the
 * {@link OpenSearchTransportProvider}, which owns and manages its shutdown.
 * <p>
 * The SdkHttpClient is created once and reused for the lifetime of the application. This prevents resource leaks as the
 * AwsSdk2Transport does not close the httpClient when it is closed.
 * <p>
 * The HTTP client lifecycle is managed by {@link OpenSearchTransportProvider}, which closes both the transport
 * and the HTTP client during application shutdown. If other components in the future need an SdkHttpClient for
 * different purposes (e.g., other AWS service integrations), they should create their own separate instance with
 * independent lifecycle management rather than reusing this OpenSearch-specific client.
 * <p>
 * The HTTP client is configured with connection pool settings from the {@link AwsHttpOpenSearchConfig}:
 * <ul>
 *   <li>maxConcurrency - Maximum concurrent requests (default: 50)</li>
 *   <li>connectionTimeout - Connection timeout duration (default: 30 seconds)</li>
 *   <li>connectionAcquisitionTimeout - Timeout for acquiring connection from pool (default: 10 seconds)</li>
 * </ul>
 * <p>
 * Note: This provider is explicitly bound by SearchModule and should not be auto-discovered by Sisu.
 */
@Singleton
public class AwsSdkHttpClientProvider
    implements Provider<SdkHttpClient>
{
  private static final Logger log = LoggerFactory.getLogger(AwsSdkHttpClientProvider.class);

  private final AwsHttpOpenSearchConfig config;

  private volatile SdkHttpClient httpClient;

  @Inject
  public AwsSdkHttpClientProvider(final SearchConfig searchConfig) {
    if (!(searchConfig instanceof AwsHttpOpenSearchConfig)) {
      throw new OpenSearchConfigurationException(
          "AwsSdkHttpClientProvider requires AwsHttpOpenSearchConfig, but got: " + searchConfig.getClass());
    }
    this.config = (AwsHttpOpenSearchConfig) searchConfig;
  }

  @Override
  public SdkHttpClient get() {
    if (httpClient == null) {
      synchronized (this) {
        if (httpClient == null) {
          log.debug(
              "Initializing AWS SDK HTTP client for OpenSearch with " +
                  "maxConcurrency={}, connectionTimeout={}, connectionAcquisitionTimeout={}",
              config.getMaxConcurrency(), config.getConnectionTimeout(), config.getConnectionAcquisitionTimeout());

          httpClient = AwsCrtHttpClient.builder()
              .maxConcurrency(config.getMaxConcurrency())
              .connectionTimeout(config.getConnectionTimeout())
              .connectionAcquisitionTimeout(config.getConnectionAcquisitionTimeout())
              .build();
        }
      }
    }
    return httpClient;
  }
}
