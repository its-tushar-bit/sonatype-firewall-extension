/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.net.URI;
import java.util.Optional;
import javax.inject.Provider;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenSearchTransportProviderTest
{
  @Mock
  private AwsCredentialsProvider awsCredentialsProvider;

  @Mock
  private Provider<SdkHttpClient> sdkHttpClientProvider;

  @Mock
  private ShutdownHandler shutdownHandler;

  private SdkHttpClient sdkHttpClient;

  private OpenSearchTransportProvider provider;

  @Before
  public void setUp() {
    sdkHttpClient = AwsCrtHttpClient.builder().build();
    when(sdkHttpClientProvider.get()).thenReturn(sdkHttpClient);
  }

  @Test
  public void testGet_HttpOpenSearchConfig_ReturnsApacheHttpClient5Transport() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(URI.create("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("admin");
    provider = new OpenSearchTransportProvider(config, awsCredentialsProvider, Optional.empty(), shutdownHandler);

    // When
    OpenSearchTransport transport = provider.get();

    try {
      // Then
      assertThat(transport).isNotNull();
      assertThat(transport).isInstanceOf(ApacheHttpClient5Transport.class);
    }
    finally {
      // Cleanup
      try {
        transport.close();
      }
      catch (Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testGet_AwsHttpOpenSearchConfig_ReturnsAwsSdk2Transport() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(URI.create("https://search-test-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    provider = new OpenSearchTransportProvider(config, awsCredentialsProvider, Optional.of(sdkHttpClientProvider),
        shutdownHandler);

    // When
    OpenSearchTransport transport = provider.get();

    try {
      // Then
      assertThat(transport).isNotNull();
      assertThat(transport).isInstanceOf(AwsSdk2Transport.class);
    }
    finally {
      // Cleanup
      try {
        transport.close();
      }
      catch (Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testGet_UnknownSearchConfig_ThrowsIllegalStateException() {
    // Given
    SearchConfig unknownConfig = new SearchConfig()
    {
      @Override
      public void validate() {
        // No-op for test
      }
    };
    provider =
        new OpenSearchTransportProvider(unknownConfig, awsCredentialsProvider, Optional.empty(), shutdownHandler);

    // When/Then
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> provider.get())
        .withMessageContaining("Unknown search config type");
  }

  @Test
  public void testGet_HttpOpenSearchConfigWithHttpUri() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(URI.create("http://localhost:9200"));
    config.setUsername("user");
    config.setPassword("pass");
    provider = new OpenSearchTransportProvider(config, awsCredentialsProvider, Optional.empty(), shutdownHandler);

    // When
    OpenSearchTransport transport = provider.get();

    try {
      // Then
      assertThat(transport).isNotNull();
      assertThat(transport).isInstanceOf(ApacheHttpClient5Transport.class);
    }
    finally {
      // Cleanup
      try {
        transport.close();
      }
      catch (Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testGet_AwsHttpOpenSearchConfigWithDifferentRegion() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(URI.create("https://search-test-domain.eu-west-1.es.amazonaws.com"));
    config.setRegion("eu-west-1");
    provider = new OpenSearchTransportProvider(config, awsCredentialsProvider, Optional.of(sdkHttpClientProvider),
        shutdownHandler);

    // When
    OpenSearchTransport transport = provider.get();

    try {
      // Then
      assertThat(transport).isNotNull();
      assertThat(transport).isInstanceOf(AwsSdk2Transport.class);
    }
    finally {
      // Cleanup
      try {
        transport.close();
      }
      catch (Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testGet_AwsHttpOpenSearchConfigWithoutSdkHttpClient_ThrowsException() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(URI.create("https://search-test-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    provider = new OpenSearchTransportProvider(config, awsCredentialsProvider, Optional.empty(), shutdownHandler);

    // When/Then
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> provider.get())
        .withMessageContaining("SdkHttpClient provider is required for AWS OpenSearch configuration");
  }
}
