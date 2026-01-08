/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchHttpSearchIndexFixture;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.TestOpenSearchTransportFactory;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.test.ContainerRule;

import com.google.inject.Binder;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.testcontainers.OpensearchContainer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class OpenSearchApiAdvancedSearchResourceV2Test
    extends AbstractApiAdvancedSearchResourceV2Test
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
    super.configure(binder);
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
  public void testExportIndex_SearchAfter() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    // Export results for any app but boost app1 so we know it comes first
    HttpResponse response = restRequest().path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:APPLICATION AND (applicationId:* OR applicationId: " + app1.getId() + "^2)")
        .query("page", 0)
        .query("pageSize", 1)
        .get();
    assertResponseStatus(200, response);
    List<String> export = Arrays.stream(response.getBodyText().split("\n")).toList();
    assertThat(export).hasSize(2);
    assertThat(export.get(1)).contains(app1.getPublicId());
    String searchAfter = response.getHeader(SearchService.SEARCH_AFTER_HEADER);

    // Export results again using searchAfter
    // Note that the page is ignored if searchAfter is specified - we effectively set it to the first page
    response = restRequest().path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:APPLICATION AND (applicationId:* OR applicationId: " + app1.getId() + "^2)")
        .query("page", 0)
        .query("pageSize", 1)
        .query("searchAfter", searchAfter)
        .get();
    assertResponseStatus(200, response);
    export = Arrays.stream(response.getBodyText().split("\n")).toList();
    assertThat(export).hasSize(2);
    assertThat(export.get(1)).contains(app2.getPublicId());
  }
}
