/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

/**
 * Provider for a singleton SdkHttpClient used by AWS OpenSearch transport.
 * <p>
 * The SdkHttpClient is created once and reused for the lifetime of the application. This prevents resource leaks as the
 * AwsSdk2Transport does not close the httpClient when it is closed.
 * <p>
 * The provider registers a shutdown hook to properly close the HTTP client on application shutdown, ensuring all
 * resources are released.
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
  public AwsSdkHttpClientProvider(final SearchConfig searchConfig, final ShutdownHandler shutdownHandler) {
    if (!(searchConfig instanceof AwsHttpOpenSearchConfig)) {
      throw new OpenSearchConfigurationException(
          "AwsSdkHttpClientProvider requires AwsHttpOpenSearchConfig, but got: " + searchConfig.getClass());
    }
    this.config = (AwsHttpOpenSearchConfig) searchConfig;

    shutdownHandler.add(() -> {
      if (httpClient != null) {
        try {
          log.debug("Closing AWS SDK HTTP client for OpenSearch");
          httpClient.close();
          return true;
        }
        catch (Exception e) {
          log.warn("Error closing AWS SDK HTTP client", e);
          return false;
        }
      }
      return true;
    }, ShutdownPriority.DEFAULT);
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
