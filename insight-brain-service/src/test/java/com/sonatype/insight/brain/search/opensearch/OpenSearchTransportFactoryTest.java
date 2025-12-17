/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.net.URI;

import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;

import org.junit.Test;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenSearchTransportFactoryTest
{
  @Test
  public void testCreate_HttpOpenSearchConfig_ReturnsApacheHttpClient5Transport() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(URI.create("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("admin123");

    // When
    OpenSearchTransport transport = OpenSearchTransportFactory.create(config);

    // Then
    assertThat(transport).isNotNull();
    assertThat(transport).isInstanceOf(ApacheHttpClient5Transport.class);

    // Cleanup
    try {
      transport.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testCreate_HttpOpenSearchConfigWithHttp() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(URI.create("http://localhost:9200"));
    config.setUsername("user");
    config.setPassword("pass");

    // When
    OpenSearchTransport transport = OpenSearchTransportFactory.create(config);

    // Then
    assertThat(transport).isNotNull();
    assertThat(transport).isInstanceOf(ApacheHttpClient5Transport.class);

    // Cleanup
    try {
      transport.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testCreate_AwsHttpOpenSearchConfig_ReturnsAwsSdk2Transport() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(URI.create("https://search-test-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");

    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("accessKey", "secretKey"));
    SdkHttpClient httpClient = AwsCrtHttpClient.builder().build();

    // When
    OpenSearchTransport transport = OpenSearchTransportFactory.create(config, credentialsProvider, httpClient);

    // Then
    assertThat(transport).isNotNull();
    assertThat(transport).isInstanceOf(AwsSdk2Transport.class);

    // Cleanup
    try {
      transport.close();
      httpClient.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testCreate_AwsHttpOpenSearchConfigWithDifferentRegion() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(URI.create("https://search-test-domain.eu-west-1.es.amazonaws.com"));
    config.setRegion("eu-west-1");

    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("accessKey", "secretKey"));
    SdkHttpClient httpClient = AwsCrtHttpClient.builder().build();

    // When
    OpenSearchTransport transport = OpenSearchTransportFactory.create(config, credentialsProvider, httpClient);

    // Then
    assertThat(transport).isNotNull();
    assertThat(transport).isInstanceOf(AwsSdk2Transport.class);

    // Cleanup
    try {
      transport.close();
      httpClient.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testCreate_HttpOpenSearchConfigWithCustomPort() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(URI.create("https://opensearch.example.com:9443"));
    config.setUsername("admin");
    config.setPassword("password");

    // When
    OpenSearchTransport transport = OpenSearchTransportFactory.create(config);

    // Then
    assertThat(transport).isNotNull();
    assertThat(transport).isInstanceOf(ApacheHttpClient5Transport.class);

    // Cleanup
    try {
      transport.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }
}
