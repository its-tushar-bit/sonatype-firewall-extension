/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.IOException;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.LuceneTest;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Category(SlowTest.class)
public class SearchIndexRuleAndFixturesTest
{
  @Rule
  public SearchIndexRule searchIndexRule = SearchIndexRule.getInstance(SearchIndexRuleAndFixturesTest.class);

  @Test
  @LuceneTest
  public void testSearch_DefaultIsLucene() {
    SearchConfig searchConfig = searchIndexRule.getSearchConfig();
    assertThat(searchConfig).isNull();
    // nothing else to assert here as the main class does all the work and there technically is no real fixture
  }

  @Test
  @OpenSearchHttpTest
  public void testOpenSearch_HTTP() throws IOException {
    HttpOpenSearchConfig searchConfig = (HttpOpenSearchConfig) searchIndexRule.getSearchConfig();
    assertThat(searchConfig).isInstanceOf(HttpOpenSearchConfig.class);

    // Note that admin:admin comes from Test Containers `OpensearchContainer`
    assertThat(searchConfig.getUri().toString()).containsPattern("http://localhost:\\d\\d\\d\\d\\d");
    assertThat(searchConfig.getUsername()).isEqualTo("admin");
    assertThat(searchConfig.getPassword()).isEqualTo("admin");

    assertHttpOpenSearchConnection();
  }

  private void assertHttpOpenSearchConnection() throws IOException {
    HttpOpenSearchConfig httpOpenSearchConfig = (HttpOpenSearchConfig) searchIndexRule.getSearchConfig();

    final HttpHost[] hosts = new HttpHost[]{
      HttpHost.create(httpOpenSearchConfig.getUri())
    };

    final ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder.builder(hosts)
        .setMapper(new JacksonJsonpMapper())
        .setHttpClientConfigCallback(httpClientBuilder -> {
          final var credentialsProvider = new BasicCredentialsProvider();
          for (final var host : hosts) {
            credentialsProvider.setCredentials(new AuthScope(host),
                new UsernamePasswordCredentials(httpOpenSearchConfig.getUsername(),
                    httpOpenSearchConfig.getPassword().toCharArray()));
          }

          return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        })
        .build();

    OpenSearchClient client = new OpenSearchClient(transport);

    BooleanResponse booleanResponse = client.ping();
    assertThat(booleanResponse.value()).isTrue();

    InfoResponse info = client.info();
    assertThat(info.version().distribution()).isEqualTo("opensearch");
  }
}
