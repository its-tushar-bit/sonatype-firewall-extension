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
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
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
import org.opensearch.client.opensearch._types.aggregations.RangeAggregate;
import org.opensearch.client.opensearch._types.aggregations.RangeBucket;

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
        currentUser,
        conversionHelper,
        mock(org.opensearch.client.transport.OpenSearchTransport.class),
        indexConfigProvider,
        mock(ClusterLockManager.class),
        mock(SearchConfig.class),
        mock(ShutdownHandler.class));

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

    Owner appOwner = mock(Owner.class);
    when(appOwner.getId()).thenReturn(mixedCaseAppId);
    when(appOwner.getType()).thenReturn(OwnerType.APPLICATION);
    Owner orgOwner = mock(Owner.class);
    when(orgOwner.getId()).thenReturn(mixedCaseOrgId);
    when(orgOwner.getType()).thenReturn(OwnerType.ORGANIZATION);

    when(ownerDAO.getById(mixedCaseAppId)).thenReturn(appOwner);
    when(ownerDAO.getById(mixedCaseOrgId)).thenReturn(orgOwner);
    when(ownerDAO.walkChildren(any(Owner.class))).thenReturn(List.of());

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
