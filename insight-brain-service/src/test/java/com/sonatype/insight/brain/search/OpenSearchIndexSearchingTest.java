/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchHttpSearchIndexFixture;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchTransportFactory;
import com.sonatype.insight.brain.search.opensearch.SingleTenantIndexConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.testcontainers.OpensearchContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;

@Category(SlowTest.class)
public class OpenSearchIndexSearchingTest
    extends AbstractIndexSearchingTest
{
  @ClassRule
  public static OpensearchContainer opensearchContainer =
      new OpensearchContainer<>(OpenSearchHttpSearchIndexFixture.OPENSEARCH_IMAGE);

  @Override
  public void configure(final Binder binder) {
    binder.bind(SearchIndexClient.class).to(OpenSearchSearchIndexClient.class);
    binder.bind(OpenSearchTransport.class).toInstance(OpenSearchTransportFactory.create(getHttpOpenSearchConfig()));
    binder.bind(IndexConfigProvider.class).to(SingleTenantIndexConfigProvider.class);
    super.configure(binder);
  }

  @Inject
  private OpenSearchSearchIndexClient openSearchSearchIndexClient;

  @Before
  public void deleteIndex() {
    openSearchSearchIndexClient.deleteIndex();
    openSearchSearchIndexClient.createIndexIfNotExists();
  }

  @Override
  protected void customizeConfig(final InsightConfig config) {
    config.setSearchConfig(getHttpOpenSearchConfig());
  }

  private static HttpOpenSearchConfig getHttpOpenSearchConfig() {
    HttpOpenSearchConfig httpOpenSearchConfig = new HttpOpenSearchConfig();
    String httpAddress = opensearchContainer.getHttpHostAddress();
    httpOpenSearchConfig.setUri(URI.create(httpAddress));
    httpOpenSearchConfig.setUsername(opensearchContainer.getUsername());
    httpOpenSearchConfig.setPassword(opensearchContainer.getPassword());
    return httpOpenSearchConfig;
  }

  @Test
  public void testShouldThrow_ExceptionThatShouldAlwaysThrow() {
    assertThat(openSearchSearchIndexClient.shouldThrow(
        new NullPointerException(),
        new AtomicLong(System.currentTimeMillis()), // It just threw already
        new AtomicReference<>(Duration.ofSeconds(30)),
        Duration.ofMinutes(10))
    ).isTrue();
  }

  @Test
  public void testShouldThrow_ExceptionWithCooldown_WithinCooldown() {
    assertThat(openSearchSearchIndexClient.shouldThrow(
        new ConnectException(),
        new AtomicLong(System.currentTimeMillis()), // It just threw already
        new AtomicReference<>(Duration.ofSeconds(30)),
        Duration.ofMinutes(10))
    ).isFalse();
  }

  @Test
  public void testShouldThrow_ExceptionWithCooldown_OutsideCooldown() {
    AtomicReference<Duration> cooldown = new AtomicReference<>(Duration.ofSeconds(30));
    // Cooldown has expired
    AtomicLong lastRecordedExceptionEpochMs = new AtomicLong(System.currentTimeMillis() - cooldown.get().toMillis());
    assertThat(openSearchSearchIndexClient.shouldThrow(
        new ConnectException(),
        lastRecordedExceptionEpochMs,
        cooldown,
        Duration.ofMinutes(10))
    ).isTrue();
    // Cooldown should be increased by x2
    assertThat(cooldown.get()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  public void testShouldThrow_ExceptionWithCooldown_CantIncreaseCooldownBeyondMax() {
    AtomicReference<Duration> cooldown = new AtomicReference<>(Duration.ofMinutes(9));
    // Cooldown has expired
    AtomicLong lastRecordedExceptionEpochMs = new AtomicLong(System.currentTimeMillis() - cooldown.get().toMillis());
    assertThat(openSearchSearchIndexClient.shouldThrow(
        new ConnectException(),
        lastRecordedExceptionEpochMs,
        cooldown,
        Duration.ofMinutes(10))
    ).isTrue();
    // Cooldown should be increased to max
    assertThat(cooldown.get()).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  public void testUpdateIndex_DoesNotFloodTheLogs() {
    try {
      opensearchContainer.stop();
      await().atMost(Duration.ofSeconds(10)).until(() -> {
        try {
          openSearchSearchIndexClient.getClient().indices().getAlias();
        }
        catch (Exception e) {
          if (e instanceof ConnectException) {
            return true;
          }
        }
        return false;
      });
      tempEntity.newOrganization();

      // No exceptions logged yet, so the first exception should be thrown
      assertThatExceptionOfType(SearchIndexException.class).isThrownBy(openSearchSearchIndexClient::updateIndex);

      // Second exception should be within the cooldown and not thrown
      assertThatNoException().isThrownBy(openSearchSearchIndexClient::updateIndex);
    }
    finally {
      opensearchContainer.start();
    }
  }
}
