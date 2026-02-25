/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchHttpSearchIndexFixture;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.SingleTenantIndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.TestOpenSearchTransportFactory;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.ContainerRule;

import com.google.inject.Binder;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.testcontainers.OpensearchContainer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;

@Category(SlowTest.class)
public class OpenSearchIndexSearchingTest
    extends AbstractIndexSearchingTest
{
  @ClassRule
  public static ContainerRule<OpensearchContainer<?>> opensearchContainer =
      new ContainerRule<>(new OpensearchContainer<>(OpenSearchHttpSearchIndexFixture.OPENSEARCH_IMAGE));

  @Override
  public void configure(final Binder binder) {
    HttpOpenSearchConfig searchConfig = getHttpOpenSearchConfig();

    // Bind SearchConfig for OpenSearchSearchIndexClient constructor
    binder.bind(SearchConfig.class).toInstance(searchConfig);

    binder.bind(SearchIndexClient.class).to(OpenSearchSearchIndexClient.class);
    // Use test utility to create isolated transport - prevents "Connection pool shut down" errors
    binder.bind(OpenSearchTransport.class).toInstance(
        TestOpenSearchTransportFactory.createIsolatedForTest(searchConfig));
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
    String httpAddress = opensearchContainer.getContainer().getHttpHostAddress();
    httpOpenSearchConfig.setUri(URI.create(httpAddress));
    httpOpenSearchConfig.setUsername(opensearchContainer.getContainer().getUsername());
    httpOpenSearchConfig.setPassword(opensearchContainer.getContainer().getPassword());
    return httpOpenSearchConfig;
  }

  @Test
  public void testUpdateIndex_DoesNotFloodTheLogs() {
    try {
      opensearchContainer.getContainer().stop();
      await().atMost(Duration.ofSeconds(20)).until(() -> {
        try {
          openSearchSearchIndexClient.getClient().indices().getAlias();
        }
        catch (Exception e) {
          if (e instanceof ConnectTimeoutException || e instanceof ConnectException) {
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
      opensearchContainer.getContainer().start();
    }
  }
}
