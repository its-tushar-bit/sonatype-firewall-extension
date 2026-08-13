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
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
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
import com.sonatype.insight.brain.utils.CvssV3Severity;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

@ExtendWith(MockitoExtension.class)
public class OpenSearchSearchIndexClientTest
{
  private static final String INDEX_NAME = "test-index";

  private OpenSearchClient openSearchClient;

  private OpenSearchSearchIndexClient client;

  @BeforeEach
  public void setUp() {
    IndexConfigProvider indexConfigProvider = mock(IndexConfigProvider.class);
    openSearchClient = mock(OpenSearchClient.class);

    IndexConfig indexConfig = mock(IndexConfig.class);
    lenient().when(indexConfig.getIndexName()).thenReturn(INDEX_NAME);
    lenient().when(indexConfigProvider.getIndexConfig()).thenReturn(indexConfig);

    ConversionHelper conversionHelper = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));

    OpenSearchSearchIndexClient realClient = new OpenSearchSearchIndexClient(
        mock(ApplicationDAO.class),
        mock(LabelDAO.class),
        mock(OrganizationDAO.class),
        mock(OwnerDAO.class),
        mock(PolicyDAO.class),
        mock(PolicyWaiverDAO.class),
        mock(AutoPolicyWaiverDAO.class),
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
        mock(com.sonatype.insight.brain.security.AuthorizationChecker.class),
        mock(CurrentUser.class),
        conversionHelper,
        mock(org.opensearch.client.transport.OpenSearchTransport.class),
        indexConfigProvider,
        mock(ClusterLockManager.class),
        mock(SearchConfig.class),
        mock(ShutdownHandler.class),
        null);

    client = spy(realClient);
    lenient().doReturn(openSearchClient).when(client).getClient();
    lenient().doReturn(true).when(client).isSearchPreviewEnabled();
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
  public void searchGlobal_sourceWithNullListElement_convertsWithoutNpe() throws Exception {
    // Waiver docs carry open-ended multi-value fields whose _source arrays contain a trailing null
    // (e.g. policyWaiverExpiresAt = [<date>, null]). The null element must be skipped during
    // document conversion rather than NPE on value.getClass().
    Map<String, Object> source = new java.util.HashMap<>();
    source.put("itemType", "POLICY_WAIVER");
    source.put("policyWaiverExpiresAt", java.util.Arrays.asList("2023-11-16T16:21:57.929Z", null));

    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(source);
    stubSearchResponse(List.of(hit), 1L);

    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), null, 10, List.of());

    var result = client.searchGlobal(request);
    assertThat(result.rows()).hasSize(1);
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
  public void searchGlobal_numericFieldSort_emitsNumericFieldSortDesc_beforeDocumentKeyTieBreak() throws Exception {
    // Both backends receive the SAME (field, direction) from IqLocalSearchService.buildSortField. On
    // OpenSearch the epoch-millis field is mapped as a long, so the FieldSort sorts numerically (not
    // lexicographically); the DOCUMENT_KEY tie-break is always appended last for a stable cursor.
    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(Map.of("itemType", "APPLICATION"));
    stubSearchResponse(List.of(hit), 1L);

    // Mirrors IqLocalSearchService.buildSortField for a numeric field: a descending SortedNumericSortField.
    org.apache.lucene.search.Sort numericSort = new org.apache.lucene.search.Sort(
        new org.apache.lucene.search.SortedNumericSortField(
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            org.apache.lucene.search.SortField.Type.LONG, true));
    GlobalSearchRequest request =
        new GlobalSearchRequest(new MatchAllDocsQuery(), numericSort, 10, List.of());
    client.searchGlobal(request);

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    List<org.opensearch.client.opensearch._types.SortOptions> fieldSorts = captor.getValue()
        .sort()
        .stream()
        .filter(org.opensearch.client.opensearch._types.SortOptions::isField)
        .toList();
    // First field sort: the epoch-millis twin, descending (newest first).
    assertThat(fieldSorts.get(0).field().field())
        .isEqualTo(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label);
    assertThat(fieldSorts.get(0).field().order())
        .isEqualTo(org.opensearch.client.opensearch._types.SortOrder.Desc);
    // Last field sort: the DOCUMENT_KEY tie-break, ascending.
    var last = fieldSorts.get(fieldSorts.size() - 1).field();
    assertThat(last.field()).isEqualTo(FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(last.order()).isEqualTo(org.opensearch.client.opensearch._types.SortOrder.Asc);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchGlobal_intTypedNumericSort_emitsSameFieldSortAsLongTyped() throws Exception {
    // The Lucene threat sorts use SortField.Type.INT so the comparator width matches the 4-byte
    // IntPoint the field is indexed with. OpenSearch takes only (field, direction) from the SortField
    // and orders from its own `integer` mapping, so the emitted FieldSort is identical whether the
    // Lucene-side type is INT or LONG — the width fix is Lucene-only and cannot skew OpenSearch order.
    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(Map.of("itemType", "POLICY_VIOLATION"));
    stubSearchResponse(List.of(hit), 1L);

    org.apache.lucene.search.Sort intSort = new org.apache.lucene.search.Sort(
        new org.apache.lucene.search.SortedNumericSortField(
            FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
            org.apache.lucene.search.SortField.Type.INT, true));
    client.searchGlobal(new GlobalSearchRequest(new MatchAllDocsQuery(), intSort, 10, List.of()));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    List<org.opensearch.client.opensearch._types.SortOptions> fieldSorts = captor.getValue()
        .sort()
        .stream()
        .filter(org.opensearch.client.opensearch._types.SortOptions::isField)
        .toList();
    assertThat(fieldSorts.get(0).field().field())
        .isEqualTo(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
    assertThat(fieldSorts.get(0).field().order())
        .as("threat sorts highest-first on OpenSearch too")
        .isEqualTo(org.opensearch.client.opensearch._types.SortOrder.Desc);
    var tieBreak = fieldSorts.get(fieldSorts.size() - 1).field();
    assertThat(tieBreak.field()).isEqualTo(FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(tieBreak.order()).isEqualTo(org.opensearch.client.opensearch._types.SortOrder.Asc);
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

  @Test
  @SuppressWarnings("unchecked")
  public void countDistinctGroupedBy_restrictsTermsAggToRequestedValues() throws Exception {
    // Regression guard: the terms aggregation must carry an include filter scoped to the requested
    // (lowercased) group values. Without it a plain terms agg returns only the global top-`size`
    // buckets by doc count, so any requested value outside that window is silently reported as zero
    // once the corpus holds more distinct group values than a page (affectedApps read 0 for most
    // components). The count itself is asserted end-to-end by the live catalog re-test.
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    client.countDistinctGroupedBy(
        "itemType:security_vulnerability",
        "vulnerabilityId",
        "applicationId",
        List.of("CVE-2021-44228", "CVE-2021-33813"));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    var terms = captor.getValue().aggregations().get("groups").terms();
    assertThat(terms.include()).isNotNull();
    assertThat(terms.include().isTerms()).isTrue();
    assertThat(terms.include().terms())
        .containsExactlyInAnyOrder("cve-2021-44228", "cve-2021-33813");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchIndex_emptySearchAfter_doesNotSetSearchAfterOnRequest() throws Exception {
    // The default violations/vulnerabilities/applications read path passes List.of() (empty, not
    // null). OpenSearch rejects search_after:[] with illegal_argument_exception, 500ing every
    // first-page request; the empty cursor must be treated as "no cursor".
    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(Map.of("itemType", "POLICY_VIOLATION"));
    stubSearchResponse(List.of(hit), 1L);

    stubMaxResultWindow();
    client.searchIndex("itemType:policy_violation", 10, 0, false, false, List.of());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    assertThat(captor.getValue().searchAfter()).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchIndex_populatedSearchAfter_setsSearchAfterOnRequest() throws Exception {
    Hit<Map> hit = mock(Hit.class);
    when(hit.source()).thenReturn(Map.of("itemType", "POLICY_VIOLATION"));
    stubSearchResponse(List.of(hit), 1L);

    stubMaxResultWindow();
    client.searchIndex("itemType:policy_violation", 10, 0, false, false, List.of("1.5", "doc-1"));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    assertThat(captor.getValue().searchAfter()).containsExactly("1.5", "doc-1");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchIndex_emptySearchAfter_pageTwo_offsetsToSecondWindow() throws Exception {
    // An empty cursor on page >= 2 must be treated as "no cursor" so the offset falls back to
    // (page-1)*pageSize. Before normalize-at-entry the empty list was treated as a cursor, forcing
    // desiredStartIndex=0 on every page, so page 2 re-requested the first window (page-1 rows) and
    // the vulnerabilities list truncated on first render.
    // A page-2 request offsets past the single stub hit, so its source is never read; only the
    // built SearchRequest matters here.
    Hit<Map> hit = mock(Hit.class);
    stubSearchResponse(List.of(hit), 1L);

    stubMaxResultWindow();
    client.searchIndex("itemType:policy_violation", 10, 2, false, false, List.of());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    // desiredStartIndex=(2-1)*10=10, so size=desiredStartIndex+pageSize=20 (the SECOND window),
    // not 10 (a repeat of the first page).
    assertThat(captor.getValue().size()).isEqualTo(20);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rankGroupsByMaxMetric_ordersTermsAggregationByMaxSubAggregation() throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    client.rankGroupsByMaxMetric(
        "itemType:security_vulnerability",
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    SearchRequest request = captor.getValue();

    var scored = request.aggregations().get("scoredGroups");
    assertThat(scored.filter()).isNotNull();
    var terms = scored.aggregations().get("groups").terms();
    assertThat(terms.size()).isEqualTo(25);
    assertThat(terms.shardSize()).isEqualTo(1000);
    assertThat(scored.aggregations().get("groups").aggregations()).containsKey("groupMetric");
    assertThat(request.aggregations()).containsKeys("scoredGroups", "unscoredGroups", "distinctGroups", "metricBands");
    assertThat(request.aggregations().get("distinctGroups").cardinality().precisionThreshold())
        .isEqualTo(40_000);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rankGroupsByMaxMetric_ordersTermsAggregationByMetricThenKeyAscendingTieBreak() throws Exception {
    // Many CVEs share a CVSS score (7.5 and 9.8 are extremely common), so a terms order on the metric
    // sub-aggregation alone leaves ties unspecified. The list service pages this with offset
    // arithmetic, so an unstable tie order lets a row appear on two pages or be skipped entirely.
    // A second _key:asc criterion pins the tie-break to the lowercased term's byte order, matching
    // the ascending-groupValue tie-break of the Lucene global-ordinal comparator.
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    client.rankGroupsByMaxMetric(
        "itemType:security_vulnerability",
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    var order = captor.getValue().aggregations().get("scoredGroups").aggregations().get("groups").terms().order();

    assertThat(order).hasSize(2);
    assertThat(order.get(0)).containsEntry("groupMetric", org.opensearch.client.opensearch._types.SortOrder.Desc);
    assertThat(order.get(1)).containsEntry("_key", org.opensearch.client.opensearch._types.SortOrder.Asc);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rankGroupsByMaxMetric_ascendingRequest_keyTieBreakStaysAscending() throws Exception {
    // The contract ties-break ascending regardless of the metric direction, the same way the Lucene
    // comparator does: only the primary "groupMetric" criterion should flip with `ascending`.
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    client.rankGroupsByMaxMetric(
        "itemType:security_vulnerability",
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        true,
        CvssV3Severity.halfOpenScoreBands());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    var order = captor.getValue().aggregations().get("scoredGroups").aggregations().get("groups").terms().order();

    assertThat(order).hasSize(2);
    assertThat(order.get(0)).containsEntry("groupMetric", org.opensearch.client.opensearch._types.SortOrder.Asc);
    assertThat(order.get(1)).containsEntry("_key", org.opensearch.client.opensearch._types.SortOrder.Asc);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rankGroupsByMaxMetric_reportsDistinctCountAsInexactAndZeroFillsBands() throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    RankedGroupsResult result = client.rankGroupsByMaxMetric(
        "itemType:security_vulnerability",
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.distinctGroupCountExact()).isFalse();
    assertThat(result.groups()).isEmpty();
    assertThat(result.bandCounts().keySet())
        .containsExactlyElementsOf(CvssV3Severity.halfOpenScoreBands().keySet());
    assertThat(result.bandCounts().values()).containsOnly(0L);
    assertThat(result.unbandedGroupCount()).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rankGroupsByMaxMetric_hugeLimit_shardSizeStaysPositive() throws Exception {
    // shardSize over-fetches 5x the limit. Computed in int arithmetic that product overflows to a
    // negative number for limits above ~429M, and OpenSearch rejects a negative shard_size outright.
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    client.rankGroupsByMaxMetric(
        "itemType:security_vulnerability",
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        Integer.MAX_VALUE,
        false,
        CvssV3Severity.halfOpenScoreBands());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    assertThat(captor.getValue().aggregations().get("scoredGroups").aggregations().get("groups").terms().shardSize())
        .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rankGroupsByMaxMetric_splitsScoredAndUnscoredViaExistsFilters() throws Exception {
    // A missing sentinel on max cannot put unscored groups last for ascending without corrupting
    // mixed groups (a high sentinel wins max). Split scored vs unscored-only with exists filters in
    // one round-trip instead — scored terms carry a plain max, unscored pad afterward.
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(null);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    client.rankGroupsByMaxMetric(
        "itemType:security_vulnerability",
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        true,
        CvssV3Severity.halfOpenScoreBands());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    org.mockito.Mockito.verify(openSearchClient).search(captor.capture(), eq(Map.class));
    var scoredMax = captor.getValue()
        .aggregations()
        .get("scoredGroups")
        .aggregations()
        .get("groups")
        .aggregations()
        .get("groupMetric")
        .max();
    assertThat(scoredMax.missing()).isNull();
    assertThat(captor.getValue().aggregations().get("scoredGroups").filter().exists().field())
        .isEqualTo(FieldIdentifier.VULNERABILITY_SEVERITY.label);
    assertThat(captor.getValue().aggregations().get("unscoredGroups").filter().bool().mustNot()).isNotEmpty();
  }

  private void stubMaxResultWindow() throws Exception {
    OpenSearchIndicesClient indicesClient = mock(OpenSearchIndicesClient.class);
    GetIndicesSettingsResponse settingsResponse = mock(GetIndicesSettingsResponse.class);
    when(settingsResponse.result()).thenReturn(Map.of());
    when(indicesClient.getSettings(any(GetIndicesSettingsRequest.class))).thenReturn(settingsResponse);
    when(openSearchClient.indices()).thenReturn(indicesClient);
    doReturn(INDEX_NAME).when(client).getRealIndexName();
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
