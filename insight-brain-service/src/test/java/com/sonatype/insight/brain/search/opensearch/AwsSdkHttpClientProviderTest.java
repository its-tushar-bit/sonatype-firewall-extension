/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AwsSdkHttpClientProviderTest
{
  private AwsHttpOpenSearchConfig config;

  private AwsSdkHttpClientProvider provider;

  @Before
  public void setUp() throws Exception {
    config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    provider = new AwsSdkHttpClientProvider(config);
  }

  @Test
  public void testGet_ReturnsSdkHttpClient() {
    // When
    SdkHttpClient client = provider.get();

    // Then
    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(SdkHttpClient.class);

    // Cleanup
    try {
      client.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testGet_ReturnsSameInstanceOnMultipleCalls() {
    // When
    SdkHttpClient client1 = provider.get();
    SdkHttpClient client2 = provider.get();
    SdkHttpClient client3 = provider.get();

    // Then
    assertThat(client1).isSameAs(client2);
    assertThat(client2).isSameAs(client3);

    // Cleanup
    try {
      client1.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testGet_ThreadSafe_ReturnsSingletonAcrossThreads() throws Exception {
    // Given
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    Set<SdkHttpClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // When - Multiple threads call get() concurrently
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          startLatch.await(); // Wait for all threads to be ready
          SdkHttpClient client = provider.get();
          clients.add(client);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown(); // Start all threads
    boolean completed = doneLatch.await(5, TimeUnit.SECONDS);

    // Then
    assertThat(completed).isTrue();
    assertThat(clients).hasSize(1); // All threads got the same instance
    executorService.shutdown();

    // Cleanup
    try {
      clients.iterator().next().close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testGet_ThreadSafe_NoRaceCondition() throws Exception {
    // Given - Test that even with high concurrency, only one instance is created
    int threadCount = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    List<SdkHttpClient> clientsList = Collections.synchronizedList(new ArrayList<>());

    // When - Many threads try to get the client at exactly the same time
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          startLatch.await();
          SdkHttpClient client = provider.get();
          clientsList.add(client);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown(); // Start all threads simultaneously
    boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

    // Then
    assertThat(completed).isTrue();
    assertThat(clientsList).hasSize(threadCount);

    // Verify all instances are the same object
    SdkHttpClient firstClient = clientsList.get(0);
    for (SdkHttpClient client : clientsList) {
      assertThat(client).isSameAs(firstClient);
    }

    executorService.shutdown();

    // Cleanup
    try {
      firstClient.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testConstructor_WithNonAwsConfig_ThrowsException() {
    // Given
    SearchConfig.HttpOpenSearchConfig httpConfig = new SearchConfig.HttpOpenSearchConfig();

    // When/Then
    assertThatThrownBy(() -> new AwsSdkHttpClientProvider(httpConfig))
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("AwsSdkHttpClientProvider requires AwsHttpOpenSearchConfig");
  }

  @Test
  public void testGet_UsesConfiguredMaxConcurrency() throws Exception {
    // Given - Custom configuration
    AwsHttpOpenSearchConfig customConfig = new AwsHttpOpenSearchConfig();
    customConfig.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    customConfig.setRegion("us-east-1");
    customConfig.setMaxConcurrency(100);
    AwsSdkHttpClientProvider customProvider = new AwsSdkHttpClientProvider(customConfig);

    // When
    SdkHttpClient client = customProvider.get();

    // Then - Client should be created successfully with custom settings
    assertThat(client).isNotNull();
    assertThat(customConfig.getMaxConcurrency()).isEqualTo(100);

    // Cleanup
    try {
      client.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testGet_UsesConfiguredTimeouts() throws Exception {
    // Given - Custom timeout configuration
    AwsHttpOpenSearchConfig customConfig = new AwsHttpOpenSearchConfig();
    customConfig.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    customConfig.setRegion("us-east-1");
    customConfig.setConnectionTimeout(Duration.ofSeconds(60));
    customConfig.setConnectionAcquisitionTimeout(Duration.ofSeconds(20));
    AwsSdkHttpClientProvider customProvider = new AwsSdkHttpClientProvider(customConfig);

    // When
    SdkHttpClient client = customProvider.get();

    // Then - Client should be created successfully with custom timeouts
    assertThat(client).isNotNull();
    assertThat(customConfig.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(60));
    assertThat(customConfig.getConnectionAcquisitionTimeout()).isEqualTo(Duration.ofSeconds(20));

    // Cleanup
    try {
      client.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }

  @Test
  public void testGet_UsesDefaultValues_WhenNotConfigured() {
    // When - Config with no custom settings
    SdkHttpClient client = provider.get();

    // Then - Should use defaults
    assertThat(client).isNotNull();
    assertThat(config.getMaxConcurrency()).isEqualTo(50); // Default value
    assertThat(config.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(30)); // Default value
    assertThat(config.getConnectionAcquisitionTimeout()).isEqualTo(Duration.ofSeconds(10)); // Default value

    // Cleanup
    try {
      client.close();
    }
    catch (Exception e) {
      // Ignore cleanup errors
    }
  }
}
