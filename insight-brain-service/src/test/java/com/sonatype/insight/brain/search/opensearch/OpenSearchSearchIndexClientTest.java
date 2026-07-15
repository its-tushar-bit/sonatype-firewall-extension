/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

@RunWith(MockitoJUnitRunner.class)
public class OpenSearchSearchIndexClientTest
{
  private static final String INDEX_NAME = "test-index";

  private OpenSearchClient openSearchClient;

  private OpenSearchSearchIndexClient client;

  @Before
  public void setUp() {
    IndexConfigProvider indexConfigProvider = mock(IndexConfigProvider.class);
    openSearchClient = mock(OpenSearchClient.class);

    IndexConfig indexConfig = mock(IndexConfig.class);
    lenient().when(indexConfig.getIndexName()).thenReturn(INDEX_NAME);
    when(indexConfigProvider.getIndexConfig()).thenReturn(indexConfig);

    ConversionHelper conversionHelper = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));

    OpenSearchSearchIndexClient realClient = new OpenSearchSearchIndexClient(
        mock(ApplicationDAO.class),
        mock(LabelDAO.class),
        mock(OrganizationDAO.class),
        mock(OwnerDAO.class),
        mock(PolicyDAO.class),
        mock(SearchIndexChangeDAO.class),
        mock(TagDAO.class),
        mock(ThirdPartySbomMetadataDAO.class),
        mock(DocumentBuilderHelper.class),
        mock(ProductLicense.class),
        mock(TelemetrySender.class),
        mock(LuceneComponents.class),
        mock(AdvancedSearchTelemetryMetrics.class),
        mock(Configuration.class),
        mock(PermissionService.class),
        mock(CurrentUser.class),
        conversionHelper,
        mock(org.opensearch.client.transport.OpenSearchTransport.class),
        indexConfigProvider,
        mock(ClusterLockManager.class),
        mock(SearchConfig.class),
        mock(ShutdownHandler.class));

    client = spy(realClient);
    doReturn(openSearchClient).when(client).getClient();
    doReturn(true).when(client).isGlobalSearchEnabled();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchGlobal_hasMoreWithMissingSortTuple_throwsSearchIndexException() throws Exception {
    Hit<Map> boundary = mock(Hit.class);
    when(boundary.source()).thenReturn(Map.of("itemType", "APPLICATION"));
    when(boundary.sort()).thenReturn(null);
    Hit<Map> extra = mock(Hit.class);

    stubSearchResponse(List.of(boundary, extra), 5L);

    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), null, 1, List.of());

    assertThatExceptionOfType(SearchIndexException.class)
        .isThrownBy(() -> client.searchGlobal(request))
        .withMessageContaining("no sort tuple");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchGlobal_nullSourceHit_throwsSearchIndexException() throws Exception {
    Hit<Map> nullSource = mock(Hit.class);
    when(nullSource.source()).thenReturn(null);
    when(nullSource.id()).thenReturn("doc-1");

    stubSearchResponse(List.of(nullSource), 1L);

    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), null, 10, List.of());

    assertThatExceptionOfType(SearchIndexException.class)
        .isThrownBy(() -> client.searchGlobal(request))
        .withMessageContaining("null _source");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchGlobal_tieBreakerSortsOnDocumentKey_notId() throws Exception {
    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(Map.of("itemType", "APPLICATION"));
    stubSearchResponse(List.of(hit), 1L);

    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), null, 10, List.of());
    client.searchGlobal(request);

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    List<String> sortFields = captor.getValue()
        .sort()
        .stream()
        .filter(org.opensearch.client.opensearch._types.SortOptions::isField)
        .map(o -> o.field().field())
        .toList();
    assertThat(sortFields).contains(FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(sortFields).doesNotContain("_id");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchGlobal_hasMoreWithSortTuple_emitsCursor() throws Exception {
    Hit<Map> boundary = mock(Hit.class);
    when(boundary.source()).thenReturn(Map.of("itemType", "APPLICATION"));
    when(boundary.sort()).thenReturn(List.of("alpha", "42"));
    Hit<Map> extra = mock(Hit.class);

    stubSearchResponse(List.of(boundary, extra), 5L);

    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), null, 1, List.of());

    var result = client.searchGlobal(request);
    assertThat(result.nextSearchAfter()).containsExactly("alpha", "42");
    assertThat(result.rows()).hasSize(1);
  }

  @Test
  public void searchGlobal_searchAfterTupleLengthMismatch_throwsBadRequest() {
    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), null, 10, List.of("only-one"));

    assertThatExceptionOfType(com.sonatype.insight.error.exception.BadRequestException.class)
        .isThrownBy(() -> client.searchGlobal(request))
        .withMessageContaining("Invalid searchAfter tuple");
  }

  @SuppressWarnings("unchecked")
  private void stubSearchResponse(final List<Hit<Map>> hits, final long total) throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.total()).thenReturn(TotalHits.of(t -> t.value(total).relation(TotalHitsRelation.Eq)));
    when(hitsMetadata.hits()).thenReturn(hits);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }
}
