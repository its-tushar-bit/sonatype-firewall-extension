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

import org.opensearch.client.transport.OpenSearchTransport;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Provider to create OpenSearchTransport instances based on the search configuration.
 * <p>
 * This provider uses the injected AwsCredentialsProvider and SdkHttpClient to create
 * the appropriate OpenSearchTransport for either HTTP or AWS-based OpenSearch configurations.
 * The SdkHttpClient is managed as a singleton to avoid resource leaks, as AwsSdk2Transport
 * does not close the httpClient when it is closed.
 * <p>
 * Note: This provider is explicitly bound by SearchModule and should not be auto-discovered by Sisu.
 */
@Singleton
public class OpenSearchTransportProvider
    implements Provider<OpenSearchTransport>
{
  private final SearchConfig searchConfig;

  private final AwsCredentialsProvider awsCredentialsProvider;

  private final Optional<Provider<SdkHttpClient>> sdkHttpClientProvider;

  private volatile OpenSearchTransport transport;

  /**
   * Constructor for OpenSearchTransportProvider.
   *
   * @param searchConfig the search configuration
   * @param awsCredentialsProvider the AWS credentials provider
   * @param sdkHttpClientProvider optional SDK HTTP client provider (only present for AWS configs)
   */
  @Inject
  public OpenSearchTransportProvider(
      final SearchConfig searchConfig,
      final AwsCredentialsProvider awsCredentialsProvider,
      final Optional<Provider<SdkHttpClient>> sdkHttpClientProvider)
  {
    this.searchConfig = searchConfig;
    this.awsCredentialsProvider = awsCredentialsProvider;
    this.sdkHttpClientProvider = sdkHttpClientProvider;
  }

  @Override
  public OpenSearchTransport get() {
    if (transport == null) {
      synchronized (this) {
        if (transport == null) {
          if (searchConfig instanceof HttpOpenSearchConfig) {
            transport = OpenSearchTransportFactory.create((HttpOpenSearchConfig) searchConfig);
          }
          else if (searchConfig instanceof AwsHttpOpenSearchConfig) {
            if (!sdkHttpClientProvider.isPresent()) {
              throw new IllegalStateException(
                  "SdkHttpClient provider is required for AWS OpenSearch configuration but was not bound");
            }
            transport = OpenSearchTransportFactory.create(
                (AwsHttpOpenSearchConfig) searchConfig,
                awsCredentialsProvider,
                sdkHttpClientProvider.get().get());
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
