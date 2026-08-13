/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
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
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.stream.JsonGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Buckets;
import org.opensearch.client.opensearch._types.aggregations.CardinalityAggregate;
import org.opensearch.client.opensearch._types.aggregations.FilterAggregate;
import org.opensearch.client.opensearch._types.aggregations.MaxAggregate;
import org.opensearch.client.opensearch._types.aggregations.RangeAggregate;
import org.opensearch.client.opensearch._types.aggregations.RangeBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;

/**
 * No-cluster unit tests for the security-critical metric query / RBAC construction in
 * {@link OpenSearchSearchIndexClient#count(String)} and
 * {@link OpenSearchSearchIndexClient#aggregateCountByField(String, String, Map)} (CLM-40927).
 * <p>
 * These tests do not require a running OpenSearch cluster (the integration fixture was removed in CLM-39882). They
 * mock the {@link OpenSearchClient} returned by {@code getClient()}, capture the issued {@link SearchRequest}, and
 * assert on the serialized request body — proving the RBAC filter is built correctly without any cluster, DB, or
 * {@code populateIndex()}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenSearchSearchIndexClientMetricQueryTest
{
  private static final String INDEX_NAME = "test-index";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private OwnerDAO ownerDAO;

  private PermissionService permissionService;

  private CurrentUser currentUser;

  private IndexConfigProvider indexConfigProvider;

  private OpenSearchClient openSearchClient;

  private OpenSearchSearchIndexClient client;

  @Before
  public void setUp() {
    ownerDAO = mock(OwnerDAO.class);
    permissionService = mock(PermissionService.class);
    currentUser = mock(CurrentUser.class);
    indexConfigProvider = mock(IndexConfigProvider.class);
    openSearchClient = mock(OpenSearchClient.class);

    IndexConfig indexConfig = mock(IndexConfig.class);
    lenient().when(indexConfig.getIndexName()).thenReturn(INDEX_NAME);
    when(indexConfigProvider.getIndexConfig()).thenReturn(indexConfig);

    // Real ConversionHelper so the query parsing / field validation path runs exactly like production.
    ConversionHelper conversionHelper = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));

    OpenSearchSearchIndexClient realClient = new OpenSearchSearchIndexClient(
        mock(ApplicationDAO.class),
        mock(LabelDAO.class),
        mock(OrganizationDAO.class),
        ownerDAO,
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
        permissionService,
        mock(com.sonatype.insight.brain.security.AuthorizationChecker.class),
        currentUser,
        conversionHelper,
        mock(org.opensearch.client.transport.OpenSearchTransport.class),
        indexConfigProvider,
        mock(ClusterLockManager.class),
        mock(SearchConfig.class),
        mock(ShutdownHandler.class),
        null);

    // Seam: avoid real client/transport wiring by stubbing getClient() on a spy. Production behavior is unchanged.
    client = spy(realClient);
    doReturn(openSearchClient).when(client).getClient();
  }

  @SuppressWarnings("unchecked")
  private SearchRequest captureCountRequest() throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(null);

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.count("itemType:" + ItemType.APPLICATION.name());

    verify(openSearchClient).search(any(SearchRequest.class), eq(Map.class));
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private SearchRequest captureCountDistinctRequest() throws Exception {
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.countDistinct(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        List.of(FieldIdentifier.APPLICATION_ID.label, FieldIdentifier.COMPONENT_HASH.label));

    verify(openSearchClient).search(any(SearchRequest.class), eq(Map.class));
    return captor.getValue();
  }

  private static JsonNode toJsonTree(SearchRequest request) throws Exception {
    JsonpMapper mapper = new JacksonJsonpMapper();
    StringWriter writer = new StringWriter();
    try (JsonGenerator generator = mapper.jsonProvider().createGenerator(writer)) {
      request.serialize(generator, mapper);
    }
    return OBJECT_MAPPER.readTree(writer.toString());
  }

  private void grantGlobalAccess() {
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Set.of(MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testCount_GlobalUser_HasNoRbacFilter() throws Exception {
    grantGlobalAccess();

    JsonNode root = toJsonTree(captureCountRequest());

    assertThat(root.path("size").asInt()).isZero();
    assertThat(root.path("track_total_hits").asBoolean()).isTrue();

    JsonNode query = root.path("query");
    // Global access => the metric query is issued verbatim with no RBAC wrapping.
    assertThat(query.has("query_string")).isTrue();
    assertThat(query.has("bool")).isFalse();
    assertThat(root.toString()).doesNotContain("match_none");
  }

  @Test
  public void testCount_FailClosed_UsesMatchNone() throws Exception {
    // Restricted user with NO readable contexts: the RBAC filter MUST be match_none, never unscoped.
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Collections.emptySet());

    JsonNode root = toJsonTree(captureCountRequest());

    JsonNode bool = root.path("query").path("bool");
    assertThat(bool.path("must").get(0).has("query_string")).isTrue();

    JsonNode filter = bool.path("filter");
    assertThat(filter.isArray()).isTrue();
    assertThat(filter).hasSize(1);
    assertThat(filter.get(0).has("match_none")).isTrue();
  }

  @Test
  public void testCount_RestrictedUser_UsesLowercasedTermsFilter() throws Exception {
    String mixedCaseAppId = "App-MixedCase";
    String mixedCaseOrgId = "Org-MixedCase";

    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Set.of(mixedCaseAppId, mixedCaseOrgId));

    when(ownerDAO.expandReadableContexts(Set.of(mixedCaseAppId, mixedCaseOrgId)))
        .thenReturn(Map.of(
            mixedCaseAppId, OwnerType.APPLICATION,
            mixedCaseOrgId, OwnerType.ORGANIZATION));

    JsonNode root = toJsonTree(captureCountRequest());

    JsonNode bool = root.path("query").path("bool");
    assertThat(bool.path("must").get(0).has("query_string")).isTrue();

    JsonNode rbac = bool.path("filter").get(0).path("bool");
    assertThat(rbac.path("minimum_should_match").asText()).isEqualTo("1");

    Map<String, List<String>> termsByField = collectTerms(rbac.path("should"));
    assertThat(termsByField)
        .containsEntry(FieldIdentifier.APPLICATION_ID.label, List.of("app-mixedcase"))
        .containsEntry(FieldIdentifier.ORGANIZATION_ID.label, List.of("org-mixedcase"));

    // Mixed-case ids must not leak into the terms values (keyword fields are lowercased on index).
    assertThat(root.toString()).doesNotContain(mixedCaseAppId);
    assertThat(root.toString()).doesNotContain(mixedCaseOrgId);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByField_BuildsRangeAggregationWithExclusiveUpperBound() throws Exception {
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(null);
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    MetricAggregationResult result = client.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.name(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        Map.of("critical", new int[]{8, 10}));

    // No aggregation buckets in the mocked response => total 0 and the requested band defaults to 0.
    assertThat(result.total).isZero();
    assertThat(result.buckets).containsEntry("critical", 0L);

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode range = aggs.path("metricBuckets").path("range");
    assertThat(range.path("field").asText()).isEqualTo(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);

    JsonNode band = null;
    for (JsonNode candidate : range.path("ranges")) {
      if ("critical".equals(candidate.path("key").asText())) {
        band = candidate;
        break;
      }
    }
    assertThat(band).isNotNull();
    assertThat(band.path("from").asText()).isEqualTo("8");
    // Upper bound is exclusive in OpenSearch range aggs, so 10 (inclusive) => to = 11.
    assertThat(band.path("to").asText()).isEqualTo("11");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByField_UpperBoundMaxValueDoesNotOverflow() throws Exception {
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(null);
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.name(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        Map.of("unbounded", new int[]{0, Integer.MAX_VALUE}));

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode range = aggs.path("metricBuckets").path("range");
    JsonNode band = null;
    for (JsonNode candidate : range.path("ranges")) {
      if ("unbounded".equals(candidate.path("key").asText())) {
        band = candidate;
        break;
      }
    }
    assertThat(band).isNotNull();
    // (long) MAX_VALUE + 1 must not overflow to a negative value.
    assertThat(band.path("to").asText()).isEqualTo("2147483648");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByField_AllThreatLevelBandsFromResponse() throws Exception {
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(TotalHits.of(t -> t.value(5).relation(TotalHitsRelation.Eq)));

    RangeBucket criticalBucket = mock(RangeBucket.class);
    when(criticalBucket.key()).thenReturn("critical");
    when(criticalBucket.docCount()).thenReturn(2L);
    RangeBucket severeBucket = mock(RangeBucket.class);
    when(severeBucket.key()).thenReturn("severe");
    when(severeBucket.docCount()).thenReturn(1L);
    RangeBucket moderateBucket = mock(RangeBucket.class);
    when(moderateBucket.key()).thenReturn("moderate");
    when(moderateBucket.docCount()).thenReturn(1L);
    RangeBucket lowBucket = mock(RangeBucket.class);
    when(lowBucket.key()).thenReturn("low");
    when(lowBucket.docCount()).thenReturn(1L);

    Buckets<RangeBucket> rangeBuckets = mock(Buckets.class);
    when(rangeBuckets.array()).thenReturn(List.of(criticalBucket, severeBucket, moderateBucket, lowBucket));

    RangeAggregate rangeAggregate = mock(RangeAggregate.class);
    when(rangeAggregate.buckets()).thenReturn(rangeBuckets);

    Aggregate aggregate = mock(Aggregate.class);
    when(aggregate.isRange()).thenReturn(true);
    when(aggregate.range()).thenReturn(rangeAggregate);

    when(response.aggregations()).thenReturn(Map.of("metricBuckets", aggregate));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    MetricAggregationResult result = client.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.name(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    assertThat(result.total).isEqualTo(5);
    assertThat(result.buckets.keySet()).containsExactlyElementsOf(ThreatLevel.searchAggregationBands().keySet());
    assertThat(result.buckets).containsEntry("critical", 2L);
    assertThat(result.buckets).containsEntry("severe", 1L);
    assertThat(result.buckets).containsEntry("moderate", 1L);
    assertThat(result.buckets).containsEntry("low", 1L);
    assertThat(result.buckets.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(result.total);

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode ranges = aggs.path("metricBuckets").path("range").path("ranges");
    assertThat(ranges).hasSize(4);
  }

  @Test
  public void testCountDistinct_FailClosed_UsesMatchNone() throws Exception {
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Collections.emptySet());

    JsonNode root = toJsonTree(captureCountDistinctRequest());

    JsonNode bool = root.path("query").path("bool");
    assertThat(bool.path("must").get(0).has("query_string")).isTrue();

    JsonNode filter = bool.path("filter");
    assertThat(filter.isArray()).isTrue();
    assertThat(filter).hasSize(1);
    assertThat(filter.get(0).has("match_none")).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountDistinct_BuildsCardinalityAggregationWithCompositeKeyScriptAndRbacFilter() throws Exception {
    // Restricted user with one application context: the distinct count must still apply the RBAC terms filter and
    // count distinct (applicationId, componentHash) via a cardinality aggregation over a composite-key script.
    String mixedCaseAppId = "App-MixedCase";
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Set.of(mixedCaseAppId));

    when(ownerDAO.expandReadableContexts(Set.of(mixedCaseAppId)))
        .thenReturn(Map.of(mixedCaseAppId, OwnerType.APPLICATION));

    SearchResponse<Map> response = mock(SearchResponse.class);
    // countDistinct reads only the cardinality aggregate; an empty aggregations map => 0.
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    long result = client.countDistinct(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        List.of(FieldIdentifier.APPLICATION_ID.label, FieldIdentifier.COMPONENT_HASH.label));

    // No cardinality aggregate in the mocked response => 0.
    assertThat(result).isZero();

    JsonNode root = toJsonTree(captor.getValue());
    assertThat(root.path("size").asInt()).isZero();
    assertThat(root.has("track_total_hits")).isFalse();

    JsonNode rbac = root.path("query").path("bool").path("filter").get(0).path("bool");
    assertThat(rbac.path("minimum_should_match").asText()).isEqualTo("1");
    Map<String, List<String>> termsByField = collectTerms(rbac.path("should"));
    assertThat(termsByField).containsEntry(FieldIdentifier.APPLICATION_ID.label, List.of("app-mixedcase"));

    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode cardinality = aggs.path("distinctCompositeKeys").path("cardinality");
    assertThat(cardinality.isMissingNode()).isFalse();
    String scriptSource = cardinality.path("script").path("source").asText();
    assertThat(scriptSource)
        .contains("doc['" + FieldIdentifier.APPLICATION_ID.label + "'].size() > 0")
        .contains("doc['" + FieldIdentifier.COMPONENT_HASH.label + "'].size() > 0");
    assertThat(cardinality.path("precision_threshold").asInt()).isEqualTo(40_000);

    // Composite-key cardinality must not be a plain single-field aggregation (would under/over-count pairs).
    assertThat(cardinality.path("field").isMissingNode()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByFloatField_BuildsRangeAggregationWithHalfOpenBounds() throws Exception {
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(null);
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    MetricAggregationResult result = client.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    // No aggregation buckets in the mocked response => total 0 and every band defaults to 0.
    assertThat(result.total).isZero();
    assertThat(result.buckets.keySet())
        .containsExactlyElementsOf(CvssV3Severity.halfOpenScoreBands().keySet());
    assertThat(result.buckets.values()).containsOnly(0L);

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode range = aggs.path("metricBuckets").path("range");
    assertThat(range.path("field").asText()).isEqualTo(FieldIdentifier.VULNERABILITY_SEVERITY.label);

    // High band is [7.0, 9.0): from is the inclusive lower, to is the EXCLUSIVE upper passed verbatim
    // (no +1 unlike the int overload) — OpenSearch range-agg `to` is already exclusive, so a 7.0 lands
    // in High and a 9.0 does not (it starts Critical). This is the boundary parity check vs Lucene.
    JsonNode high = null;
    JsonNode medium = null;
    for (JsonNode candidate : range.path("ranges")) {
      if ("high".equals(candidate.path("key").asText())) {
        high = candidate;
      }
      if ("medium".equals(candidate.path("key").asText())) {
        medium = candidate;
      }
    }
    assertThat(high).isNotNull();
    assertThat((float) high.path("from").asDouble()).isEqualTo(7.0f);
    assertThat((float) high.path("to").asDouble()).isEqualTo(9.0f);
    assertThat(medium).isNotNull();
    assertThat((float) medium.path("from").asDouble()).isEqualTo(4.0f);
    // Medium's exclusive upper equals High's inclusive lower: a 7.0 cannot fall in both.
    assertThat((float) medium.path("to").asDouble()).isEqualTo(7.0f);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByFloatField_AllBandsFromResponse() throws Exception {
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(TotalHits.of(t -> t.value(9).relation(TotalHitsRelation.Eq)));

    RangeBucket noneBucket = mock(RangeBucket.class);
    when(noneBucket.key()).thenReturn("none");
    when(noneBucket.docCount()).thenReturn(1L);
    RangeBucket lowBucket = mock(RangeBucket.class);
    when(lowBucket.key()).thenReturn("low");
    when(lowBucket.docCount()).thenReturn(2L);
    RangeBucket mediumBucket = mock(RangeBucket.class);
    when(mediumBucket.key()).thenReturn("medium");
    when(mediumBucket.docCount()).thenReturn(2L);
    RangeBucket highBucket = mock(RangeBucket.class);
    when(highBucket.key()).thenReturn("high");
    when(highBucket.docCount()).thenReturn(2L);
    RangeBucket criticalBucket = mock(RangeBucket.class);
    when(criticalBucket.key()).thenReturn("critical");
    when(criticalBucket.docCount()).thenReturn(2L);

    Buckets<RangeBucket> rangeBuckets = mock(Buckets.class);
    when(rangeBuckets.array())
        .thenReturn(List.of(noneBucket, lowBucket, mediumBucket, highBucket, criticalBucket));

    RangeAggregate rangeAggregate = mock(RangeAggregate.class);
    when(rangeAggregate.buckets()).thenReturn(rangeBuckets);

    Aggregate aggregate = mock(Aggregate.class);
    when(aggregate.isRange()).thenReturn(true);
    when(aggregate.range()).thenReturn(rangeAggregate);

    when(response.aggregations()).thenReturn(Map.of("metricBuckets", aggregate));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    MetricAggregationResult result = client.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.total).isEqualTo(9);
    assertThat(result.buckets.keySet())
        .containsExactlyElementsOf(CvssV3Severity.halfOpenScoreBands().keySet());
    assertThat(result.buckets).containsEntry("none", 1L);
    assertThat(result.buckets).containsEntry("low", 2L);
    assertThat(result.buckets).containsEntry("medium", 2L);
    assertThat(result.buckets).containsEntry("high", 2L);
    assertThat(result.buckets).containsEntry("critical", 2L);
    assertThat(result.buckets.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(result.total);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByFloatField_DistinctField_AddsCardinalitySubAggPerBand() throws Exception {
    // With distinctField set, each float range bucket must host a `cardinality` sub-agg on the distinct
    // field (the range-agg analogue of countDistinctGroupedBy's terms+cardinality), so a band counts
    // distinct CVEs, not raw docs. Assert the request shape (the live re-test asserts the counts).
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(null);
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands(),
        FieldIdentifier.VULNERABILITY_ID.label);

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode metricBuckets = aggs.path("metricBuckets");
    assertThat(metricBuckets.path("range").path("field").asText())
        .isEqualTo(FieldIdentifier.VULNERABILITY_SEVERITY.label);
    JsonNode subAggs = metricBuckets.has("aggregations")
        ? metricBuckets.path("aggregations")
        : metricBuckets.path("aggs");
    JsonNode cardinality = subAggs.path("distinct").path("cardinality");
    assertThat(cardinality.isMissingNode()).isFalse();
    assertThat(cardinality.path("precision_threshold").asInt()).isEqualTo(40_000);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAggregateCountByFloatField_DistinctField_ReadsCardinalityPerBand() throws Exception {
    // The per-band value is read from each range bucket's `distinct` cardinality sub-agg, not docCount.
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(TotalHits.of(t -> t.value(6).relation(TotalHitsRelation.Eq)));

    RangeBucket highBucket = mock(RangeBucket.class);
    when(highBucket.key()).thenReturn("high");
    when(highBucket.docCount()).thenReturn(4L); // raw docs; must be ignored in favor of distinct
    CardinalityAggregate highCard = mock(CardinalityAggregate.class);
    when(highCard.value()).thenReturn(2L);
    Aggregate highDistinct = mock(Aggregate.class);
    when(highDistinct.isCardinality()).thenReturn(true);
    when(highDistinct.cardinality()).thenReturn(highCard);
    when(highBucket.aggregations()).thenReturn(Map.of("distinct", highDistinct));

    RangeBucket criticalBucket = mock(RangeBucket.class);
    when(criticalBucket.key()).thenReturn("critical");
    when(criticalBucket.docCount()).thenReturn(2L);
    CardinalityAggregate critCard = mock(CardinalityAggregate.class);
    when(critCard.value()).thenReturn(1L);
    Aggregate critDistinct = mock(Aggregate.class);
    when(critDistinct.isCardinality()).thenReturn(true);
    when(critDistinct.cardinality()).thenReturn(critCard);
    when(criticalBucket.aggregations()).thenReturn(Map.of("distinct", critDistinct));

    Buckets<RangeBucket> rangeBuckets = mock(Buckets.class);
    when(rangeBuckets.array()).thenReturn(List.of(highBucket, criticalBucket));
    RangeAggregate rangeAggregate = mock(RangeAggregate.class);
    when(rangeAggregate.buckets()).thenReturn(rangeBuckets);
    Aggregate aggregate = mock(Aggregate.class);
    when(aggregate.isRange()).thenReturn(true);
    when(aggregate.range()).thenReturn(rangeAggregate);
    when(response.aggregations()).thenReturn(Map.of("metricBuckets", aggregate));

    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);

    MetricAggregationResult result = client.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands(),
        FieldIdentifier.VULNERABILITY_ID.label);

    assertThat(result.total).isEqualTo(6); // raw doc total, unaffected by distinctField
    assertThat(result.buckets).containsEntry("high", 2L); // distinct CVEs, not the 4 raw docs
    assertThat(result.buckets).containsEntry("critical", 1L);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountDistinctGroupedByBands_NestsRangeThenTermsThenCardinality() throws Exception {
    // C1 per-severity component counts: one request with a range agg over the threat-level bands, each
    // band hosting a terms agg over the requested component hashes, each group hosting a cardinality
    // sub-agg on policyViolationId (distinct violations). Assert the nested request shape (the live
    // re-test asserts the counts vs a raw OpenSearch aggregation — the C1 landmine cross-check).
    grantGlobalAccess();

    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Collections.emptyMap());
    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.countDistinctGroupedByBands(
        "itemType:" + ItemType.POLICY_VIOLATION.name(),
        FieldIdentifier.COMPONENT_HASH.label,
        FieldIdentifier.POLICY_VIOLATION_ID.label,
        Set.of("hashA"),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode aggs = root.has("aggregations") ? root.path("aggregations") : root.path("aggs");
    JsonNode bands = aggs.path("bands");
    assertThat(bands.path("range").path("field").asText())
        .isEqualTo(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
    JsonNode bandSub = bands.has("aggregations") ? bands.path("aggregations") : bands.path("aggs");
    JsonNode groups = bandSub.path("groups");
    assertThat(groups.path("terms").path("field").asText()).isEqualTo(FieldIdentifier.COMPONENT_HASH.label);
    JsonNode groupSub = groups.has("aggregations") ? groups.path("aggregations") : groups.path("aggs");
    JsonNode cardinality = groupSub.path("distinct").path("cardinality");
    assertThat(cardinality.isMissingNode()).isFalse();
    assertThat(cardinality.path("field").asText()).isEqualTo(FieldIdentifier.POLICY_VIOLATION_ID.label);
    assertThat(cardinality.path("precision_threshold").asInt()).isEqualTo(40_000);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountDistinctGroupedByBands_FailClosed_UsesMatchNone() throws Exception {
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Collections.emptySet());

    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Collections.emptyMap());
    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.countDistinctGroupedByBands(
        "itemType:" + ItemType.POLICY_VIOLATION.name(),
        FieldIdentifier.COMPONENT_HASH.label,
        FieldIdentifier.POLICY_VIOLATION_ID.label,
        Set.of("hashA"),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode filter = root.path("query").path("bool").path("filter");
    assertThat(filter.isArray()).isTrue();
    assertThat(filter.get(0).has("match_none")).isTrue();
  }

  @Test
  public void testAggregateCountByFloatField_FailClosed_UsesMatchNone() throws Exception {
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Collections.emptySet());

    SearchResponse<Map> response = mock(SearchResponse.class);
    @SuppressWarnings("unchecked")
    HitsMetadata<Map> hits = mock(HitsMetadata.class);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(null);
    when(response.aggregations()).thenReturn(Collections.emptyMap());

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(openSearchClient.search(captor.capture(), eq(Map.class))).thenReturn(response);

    client.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    JsonNode root = toJsonTree(captor.getValue());
    JsonNode filter = root.path("query").path("bool").path("filter");
    assertThat(filter.isArray()).isTrue();
    assertThat(filter.get(0).has("match_none")).isTrue();
  }

  @Test
  public void testRankGroupsByMaxMetric_ReadsGroupsMetricsAndBandsFromPopulatedResponse() throws Exception {
    // Every prior rankGroupsByMaxMetric test stubs an empty aggregation map, so the response-parsing
    // half of the method — bucket order, max sub-agg extraction, per-band cardinality, and the
    // distinct/banded subtraction that yields unbandedGroupCount — was never exercised.
    grantGlobalAccess();

    stubRankedResponse(
        List.of(termBucket("cve-2021-44228", 10.0d), termBucket("cve-2022-22965", 9.8d)),
        7L,
        Map.of("critical", 4L, "high", 2L));

    RankedGroupsResult result = client.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("cve-2021-44228", 10.0f),
        new RankedGroup("cve-2022-22965", 9.8f));
    assertThat(result.distinctGroupCount()).isEqualTo(7L);
    assertThat(result.bandCounts()).containsEntry("critical", 4L).containsEntry("high", 2L);
    // Bands absent from the response stay zero-filled rather than dropping out of the map.
    assertThat(result.bandCounts()).containsEntry("none", 0L).containsEntry("low", 0L);
    assertThat(result.bandCounts().keySet())
        .containsExactlyElementsOf(CvssV3Severity.halfOpenScoreBands().keySet());
    assertThat(result.unbandedGroupCount()).isEqualTo(1L); // 7 distinct - (4 + 2) banded
  }

  @Test
  public void testRankGroupsByMaxMetric_NanMaxValue_YieldsNullMetric() throws Exception {
    // OpenSearch reports an empty max sub-aggregation as NaN. That must surface as a null metric —
    // the same "no metric" signal the Lucene collector encodes with its Float.NaN sentinel — so an
    // unscored CVE is never mistaken for one scoring 0.0.
    grantGlobalAccess();

    stubRankedResponse(List.of(termBucket("cve-2024-00001", Double.NaN)), 1L, Map.of());

    RankedGroupsResult result = client.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.groups()).containsExactly(new RankedGroup("cve-2024-00001", null));
    assertThat(result.unbandedGroupCount()).isEqualTo(1L);
  }

  @Test
  public void testRankGroupsByMaxMetric_UnscoredOnlyGroup_PadsAfterScored() throws Exception {
    // Unscored-only groups come from the unscored exists-filter pad, not a missing sentinel on max.
    grantGlobalAccess();

    stubRankedResponse(
        List.of(termBucket("cve-scored", 4.0d)),
        List.of(unscoredTermBucket("cve-unscored")),
        2L,
        Map.of());

    RankedGroupsResult result = client.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        true,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.groups()).containsExactly(
        new RankedGroup("cve-scored", 4.0f),
        new RankedGroup("cve-unscored", null));
    assertThat(result.unbandedGroupCount()).isEqualTo(2L);
  }

  @Test
  public void testRankGroupsByMaxMetric_ZeroScore_SurvivesTheSentinelCheck() throws Exception {
    // 0.0 is a real CVSS score and must reach the row as a score, not as an unscored null metric.
    grantGlobalAccess();

    stubRankedResponse(List.of(termBucket("cve-2024-00002", 0.0d)), 1L, Map.of());

    RankedGroupsResult result = client.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.groups()).containsExactly(new RankedGroup("cve-2024-00002", 0.0f));
  }

  @Test
  public void testRankGroupsByMaxMetric_BandsExceedingDistinctTotal_ClampsUnbandedAtZero() throws Exception {
    // Bands come from raw document metrics while the distinct total is a HyperLogLog++ estimate, so a
    // group spanning two bands (or plain estimation noise) can push the banded sum above the total.
    // The difference must clamp at zero rather than surfacing a negative count.
    grantGlobalAccess();

    stubRankedResponse(List.of(termBucket("cve-2021-44228", 10.0d)), 3L, Map.of("critical", 3L, "medium", 2L));

    RankedGroupsResult result = client.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.unbandedGroupCount()).isZero();
  }

  private static StringTermsBucket termBucket(final String key, final double maxValue) {
    MaxAggregate max = mock(MaxAggregate.class);
    when(max.value()).thenReturn(maxValue);
    Aggregate metric = mock(Aggregate.class);
    when(metric.isMax()).thenReturn(true);
    when(metric.max()).thenReturn(max);
    Map<String, Aggregate> subAggregations = Map.of("groupMetric", metric);

    StringTermsBucket bucket = mock(StringTermsBucket.class);
    when(bucket.key()).thenReturn(key);
    when(bucket.aggregations()).thenReturn(subAggregations);
    return bucket;
  }

  private static Aggregate cardinalityAggregate(final long value) {
    CardinalityAggregate cardinality = mock(CardinalityAggregate.class);
    when(cardinality.value()).thenReturn(value);
    Aggregate aggregate = mock(Aggregate.class);
    when(aggregate.isCardinality()).thenReturn(true);
    when(aggregate.cardinality()).thenReturn(cardinality);
    return aggregate;
  }

  private static StringTermsBucket unscoredTermBucket(final String key) {
    StringTermsBucket bucket = mock(StringTermsBucket.class);
    when(bucket.key()).thenReturn(key);
    return bucket;
  }

  @SuppressWarnings("unchecked")
  private void stubRankedResponse(
      final List<StringTermsBucket> termBuckets,
      final long distinctGroups,
      final Map<String, Long> bandDistincts) throws Exception
  {
    stubRankedResponse(termBuckets, List.of(), distinctGroups, bandDistincts);
  }

  @SuppressWarnings("unchecked")
  private void stubRankedResponse(
      final List<StringTermsBucket> scoredBuckets,
      final List<StringTermsBucket> unscoredBuckets,
      final long distinctGroups,
      final Map<String, Long> bandDistincts) throws Exception
  {
    Aggregate scored = filterTermsAggregate(scoredBuckets);
    Aggregate unscored = filterTermsAggregate(unscoredBuckets);

    List<RangeBucket> rangeBuckets = new ArrayList<>();
    bandDistincts.forEach((label, value) -> {
      // Build the sub-aggregation before opening the outer when(...): Mockito rejects stubbing a
      // second mock while an enclosing stub is still awaiting its thenReturn.
      Map<String, Aggregate> subAggregations = Map.of("distinct", cardinalityAggregate(value));
      RangeBucket bucket = mock(RangeBucket.class);
      when(bucket.key()).thenReturn(label);
      when(bucket.aggregations()).thenReturn(subAggregations);
      rangeBuckets.add(bucket);
    });
    Buckets<RangeBucket> bandBuckets = mock(Buckets.class);
    when(bandBuckets.array()).thenReturn(rangeBuckets);
    RangeAggregate range = mock(RangeAggregate.class);
    when(range.buckets()).thenReturn(bandBuckets);
    Aggregate bands = mock(Aggregate.class);
    when(bands.isRange()).thenReturn(true);
    when(bands.range()).thenReturn(range);

    Map<String, Aggregate> aggregations = Map.of(
        "scoredGroups", scored,
        "unscoredGroups", unscored,
        "distinctGroups", cardinalityAggregate(distinctGroups),
        "metricBands", bands);
    SearchResponse<Map> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(aggregations);
    when(openSearchClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(response);
  }

  @SuppressWarnings("unchecked")
  private static Aggregate filterTermsAggregate(final List<StringTermsBucket> termBuckets) {
    Buckets<StringTermsBucket> buckets = mock(Buckets.class);
    when(buckets.array()).thenReturn(termBuckets);
    StringTermsAggregate sterms = mock(StringTermsAggregate.class);
    when(sterms.buckets()).thenReturn(buckets);
    Aggregate terms = mock(Aggregate.class);
    when(terms.isSterms()).thenReturn(true);
    when(terms.sterms()).thenReturn(sterms);
    FilterAggregate filterAgg = mock(FilterAggregate.class);
    when(filterAgg.aggregations()).thenReturn(Map.of("groups", terms));
    Aggregate filter = mock(Aggregate.class);
    when(filter.isFilter()).thenReturn(true);
    when(filter.filter()).thenReturn(filterAgg);
    return filter;
  }

  private static Map<String, List<String>> collectTerms(JsonNode shouldArray) {
    Map<String, List<String>> termsByField = new HashMap<>();
    for (JsonNode should : shouldArray) {
      JsonNode terms = should.path("terms");
      terms.fieldNames().forEachRemaining(field -> {
        JsonNode values = terms.path(field);
        if (values.isArray()) {
          List<String> stringValues = new ArrayList<>();
          values.forEach(value -> stringValues.add(value.asText()));
          termsByField.put(field, stringValues);
        }
      });
    }
    return termsByField;
  }
}
