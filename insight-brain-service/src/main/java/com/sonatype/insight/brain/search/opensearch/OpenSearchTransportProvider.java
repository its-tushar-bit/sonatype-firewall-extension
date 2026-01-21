/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.opensearch.client.transport.OpenSearchTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Provider to create OpenSearchTransport instances based on the search configuration.
 * <p>
 * This provider uses the injected AwsCredentialsProvider and SdkHttpClient to create
 * the appropriate OpenSearchTransport for either HTTP or AWS-based OpenSearch configurations.
 * <p>
 * <strong>Exclusive SdkHttpClient Ownership:</strong><br>
 * This provider has exclusive ownership of the SdkHttpClient instance used for AWS OpenSearch connections.
 * The SdkHttpClient is provided by {@link AwsSdkHttpClientProvider} and is intended solely for OpenSearch use.
 * No other components should inject or use this SdkHttpClient instance, as its lifecycle is managed here.
 * <p>
 * Lifecycle Management: This provider registers a shutdown handler to properly close the OpenSearchTransport
 * and underlying SdkHttpClient (for AWS configurations) during application shutdown. The AwsSdk2Transport
 * does not close the httpClient when it is closed, so we must manage this explicitly.
 * <p>
 * Note: This provider is explicitly bound by SearchModule and should not be auto-discovered by Sisu.
 */
@Singleton
public class OpenSearchTransportProvider
    implements Provider<OpenSearchTransport>
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchTransportProvider.class);

  private final SearchConfig searchConfig;

  private final AwsCredentialsProvider awsCredentialsProvider;

  private final Optional<Provider<SdkHttpClient>> sdkHttpClientProvider;

  private volatile OpenSearchTransport transport;

  private volatile SdkHttpClient httpClient;

  /**
   * Constructor for OpenSearchTransportProvider.
   *
   * @param searchConfig the search configuration
   * @param awsCredentialsProvider the AWS credentials provider
   * @param sdkHttpClientProvider optional SDK HTTP client provider (only present for AWS configs)
   * @param shutdownHandler the shutdown handler to register cleanup tasks
   */
  @Inject
  public OpenSearchTransportProvider(
      final SearchConfig searchConfig,
      final AwsCredentialsProvider awsCredentialsProvider,
      final Optional<Provider<SdkHttpClient>> sdkHttpClientProvider,
      final ShutdownHandler shutdownHandler)
  {
    this.searchConfig = searchConfig;
    this.awsCredentialsProvider = awsCredentialsProvider;
    this.sdkHttpClientProvider = sdkHttpClientProvider;

    // Register shutdown handler to close transport and HTTP client
    shutdownHandler.add(() -> {
      // Close the OpenSearchTransport
      if (transport != null) {
        try {
          log.debug("Closing OpenSearch transport");
          transport.close();
        }
        catch (Exception e) {
          log.warn("Error closing OpenSearch transport", e);
        }
      }

      // Close the SdkHttpClient (for AWS configurations)
      // AwsSdk2Transport does not close the httpClient when it is closed, so we must do it here
      if (httpClient != null) {
        try {
          log.debug("Closing AWS SDK HTTP client for OpenSearch");
          httpClient.close();
        }
        catch (Exception e) {
          log.warn("Error closing AWS SDK HTTP client", e);
        }
      }

      // Stop polling, see BooleanSupplierShutdownRequest.execute
      return false;
    });
  }

  @Override
  public OpenSearchTransport get() {
    if (transport == null) {
      synchronized (this) {
        if (transport == null) {
          // Initialize httpClient first if needed, then transport
          // Both assignments are protected by the same synchronized block
          if (searchConfig instanceof HttpOpenSearchConfig) {
            transport = OpenSearchTransportFactory.create((HttpOpenSearchConfig) searchConfig);
          }
          else if (searchConfig instanceof AwsHttpOpenSearchConfig) {
            if (sdkHttpClientProvider.isEmpty()) {
              throw new IllegalStateException(
                  "SdkHttpClient provider is required for AWS OpenSearch configuration but was not bound");
            }
            // Store the HTTP client reference so we can close it during shutdown
            // This assignment is protected by the same synchronized block as transport
            SdkHttpClient client = sdkHttpClientProvider.get().get();
            OpenSearchTransport awsTransport = OpenSearchTransportFactory.create(
                (AwsHttpOpenSearchConfig) searchConfig,
                awsCredentialsProvider,
                client);

            // Assign both fields atomically within the synchronized block
            httpClient = client;
            transport = awsTransport;
          }
          else {
            throw new IllegalStateException("Unknown search config type: " + searchConfig.getClass());
          }
        }
      }
    }
    return transport;
  }
}
