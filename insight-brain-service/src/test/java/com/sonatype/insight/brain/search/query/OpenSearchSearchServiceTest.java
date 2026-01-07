/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.net.URI;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchHttpSearchIndexFixture;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.SingleTenantIndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.TestOpenSearchTransportFactory;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.testcontainers.OpensearchContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class OpenSearchSearchServiceTest
    extends AbstractSearchServiceTest
{
  @ClassRule
  public static OpensearchContainer opensearchContainer =
      new OpensearchContainer<>(OpenSearchHttpSearchIndexFixture.OPENSEARCH_IMAGE);

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
    String httpAddress = opensearchContainer.getHttpHostAddress();
    httpOpenSearchConfig.setUri(URI.create(httpAddress));
    httpOpenSearchConfig.setUsername(opensearchContainer.getUsername());
    httpOpenSearchConfig.setPassword(opensearchContainer.getPassword());
    return httpOpenSearchConfig;
  }

  @Test
  public void testSearchIndex_SearchAfter() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("uSeRnAmEiıIİ", "Test User", InternalRealm.ID));
    Role role = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), "uSeRnAmEiıIİ");
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    indexService.createSearchIndex();

    // Search for any app but boost app1 so we know it comes first
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("itemType:APPLICATION AND (applicationId:* OR applicationId: " + app1.getId() + "^2)",
            1, 0, false, null, null);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).applicationId).isEqualTo(app1.getId());
    assertThat(searchResultDTO.searchAfter).isNotNull();

    // Search again using searchAfter
    // Note that the page is ignored if searchAfter is specified - we effectively set it to the first page
    searchResultDTO =
        searchService.searchIndex("itemType:APPLICATION AND (applicationId:* OR applicationId: " + app1.getId() + "^2)",
            1, 0, false, null, String.join(",", searchResultDTO.searchAfter));

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).applicationId).isEqualTo(app2.getId());
    assertThat(searchResultDTO.searchAfter).isNotNull();
  }

  @Override
  @Test
  public void testSearchIndex_TooManyBooleanClauses() {
    // no-op, this error is not thrown with OpenSearchSearchService
  }
}
