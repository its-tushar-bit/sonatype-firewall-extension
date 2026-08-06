/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-level wiring test for {@code /rest/search/results}: real {@link ResultsService} + real
 * {@link GlobalSearchResource} + registered exception mappers, invoked by calling the resource methods
 * directly (no servlet container). Verifies flag gating, stale-cursor mapping, and filter validation.
 * The framework-level path (Shiro filter, JAX-RS binding, @Provider mapper dispatch) is covered by
 * {@link ResultsResourceAuthzTest}.
 */
public class ResultsEndpointTest
{
  private FakeGlobalSearchResultsIqLocalClient iq;

  private FakeCatalogClient catalog;

  private GlobalSearchResource resource;

  private ResultsService service;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    iq = new FakeGlobalSearchResultsIqLocalClient();
    catalog = new FakeCatalogClient();
    service = new ResultsService(iq, catalog, UnusedIndexQueryServices.throwOnUse());
    // Default: authenticated caller with a scoped READ grant so the resource's runtime
    // verifyReadOnAnyContext() gate passes and the test focuses on flag / cursor / filter behaviour.
    SearchIndexClient searchIndexClient = org.mockito.Mockito.mock(SearchIndexClient.class);
    org.mockito.Mockito.when(searchIndexClient.getCurrentUserContextIdsWithReadPermission())
        .thenReturn(Set.of("org-1"));
    resource = new GlobalSearchResource(
        service, org.mockito.Mockito.mock(SuggestService.class), searchIndexClient);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  @Test
  public void flagOff_returns404() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    assertThatThrownBy(() -> resource.getResults("q", "APPLICATION", 1, 25, null, null, null, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void staleCursor_isMappedTo410WithRetryHeader() {
    // Build an outer ALL-tab cursor and bump the generation so decode fails.
    Map<Tab, AllTabCursor.SectionCursor> sections = new EnumMap<>(Tab.class);
    sections.put(Tab.COMPONENT, AllTabCursor.SectionCursor.nonExhausted(null, 1));
    String encoded = new AllTabCursor(null, 25, sections).encode();
    String saved = GlobalSearchCursor.currentGenerationToken();
    try {
      GlobalSearchCursor.bumpGenerationToken("g-rotated-after-reindex");

      StaleCursorException thrown = null;
      try {
        resource.getResults("q", "ALL", 1, 25, null, encoded, null, null, null);
      }
      catch (StaleCursorException e) {
        thrown = e;
      }
      assertThat(thrown).isNotNull();

      // Verify the mapper produces the 410 + retry header shape.
      Response response = new StaleCursorExceptionMapper().toResponse(thrown);
      assertThat(response.getStatus()).isEqualTo(410);
      assertThat(response.getHeaderString(StaleCursorExceptionMapper.RETRY_HINT_HEADER))
          .isEqualTo(StaleCursorExceptionMapper.RETRY_HINT_VALUE);
    }
    finally {
      GlobalSearchCursor.bumpGenerationToken(saved);
    }
  }

  @Test
  public void unknownSort_isMappedTo400WithGenericBody() {
    FilterValidationException thrown = null;
    try {
      resource.getResults("q", "COMPONENT", 1, 25, "noSuchSort", null, null, null, null);
    }
    catch (FilterValidationException e) {
      thrown = e;
    }
    assertThat(thrown).isNotNull();

    Response response = new FilterValidationExceptionMapper().toResponse(thrown);
    assertThat(response.getStatus()).isEqualTo(400);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getEntity();
    assertThat(body).containsEntry("code", "SORT_NOT_ALLOWED");
    assertThat(body).containsEntry("message", FilterValidationException.Code.SORT_NOT_ALLOWED.clientMessage());
    // The response body must not echo the caller-supplied sort key or the allowlist.
    assertThat(body.get("message").toString()).doesNotContain("noSuchSort");
    assertThat(body.get("message").toString()).doesNotContain("Allowed");
  }

  @Test
  public void validRequest_delegatesToService_iqLocalTab() {
    iq.registerRow(Tab.APPLICATION,
        ResultRow.builder().type("APPLICATION").source(SearchSource.LOCAL.value()).id("a1").title("App 1").build());
    ResultsResponse response =
        (ResultsResponse) resource.getResults("q", "APPLICATION", 1, 25, null, null, null, null, null).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);
  }

  @Test
  public void tabCounts_serializeAsUppercaseEnumKeys_inTabDeclarationOrder() throws Exception {
    iq.registerRow(Tab.APPLICATION,
        ResultRow.builder().type("APPLICATION").source(SearchSource.LOCAL.value()).id("a1").title("App 1").build());
    // includeTabCounts=true: the sibling probe that populates every badge is opt-in.
    ResultsResponse response =
        (ResultsResponse) resource.getResults("q", "APPLICATION", 1, 25, null, null, null, null, true).getEntity();

    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    String json = mapper.writeValueAsString(response);

    // Keys are the uppercase enum names, matching the `tab` field, so a client can index tabCounts by
    // the same token it sends.
    assertThat(json).contains("\"APPLICATION\":1");

    // EnumMap ordering survives construction, so the badge keys serialize in Tab declaration order
    // rather than the unspecified order Map.copyOf would give. Read the key order off the parsed node so
    // no substring can be mismatched, and compare against Tab.values() so the assertion tracks the enum.
    java.util.List<String> keyOrder = new java.util.ArrayList<>();
    mapper.readTree(json).get("tabCounts").fieldNames().forEachRemaining(keyOrder::add);

    assertThat(keyOrder)
        .containsExactlyElementsOf(java.util.Arrays.stream(Tab.values()).map(Tab::name).toList());
  }

  @Test
  public void facets_serializeInInsertionOrder_andAreAbsentWhenNull() throws Exception {
    // The response stores facets in a LinkedHashMap so the filter rail renders in FACET_FIELDS declaration
    // order. Assert the order survives serialization rather than only the in-memory map, since that is what
    // a client actually reads.
    java.util.Map<String, java.util.List<IndexQueryFacetBucket>> facets = new java.util.LinkedHashMap<>();
    facets.put("status", java.util.List.of(new IndexQueryFacetBucket("OPEN", 3L)));
    facets.put("auto", java.util.List.of());
    facets.put("threatLevel", java.util.List.of());
    facets.put("scope", java.util.List.of());
    ResultsResponse withFacets = new ResultsResponse(
        Tab.WAIVER, 1, 25, 3L, null, List.of(), null, List.of(), true, facets);

    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    java.util.List<String> keyOrder = new java.util.ArrayList<>();
    mapper.readTree(mapper.writeValueAsString(withFacets))
        .get("facets")
        .fieldNames()
        .forEachRemaining(keyOrder::add);

    assertThat(keyOrder).containsExactly("status", "auto", "threatLevel", "scope");

    // A null facets map is omitted entirely (@JsonInclude NON_NULL), so an existing client sees no new field.
    ResultsResponse withoutFacets = new ResultsResponse(
        Tab.WAIVER, 1, 25, 3L, null, List.of(), null, List.of(), true, null);
    assertThat(mapper.readTree(mapper.writeValueAsString(withoutFacets)).has("facets")).isFalse();
  }

  @Test
  public void catalogFederationOff_catalogSourceRejected_withoutReachingTheCatalogLeg() {
    // The frontend hides the catalog data source when CATALOG_FEDERATION is off. The backend must enforce
    // the same flag rather than trust that clamp: a hand-crafted ?source=catalog must be rejected at the
    // boundary instead of dispatching to the catalog leg (and from there to HDS).
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);

    assertThatThrownBy(() -> resource.getResults("q", "COMPONENT", 1, 25, null, null, "catalog", null, null))
        .isInstanceOf(jakarta.ws.rs.BadRequestException.class);
    assertThat(catalog.searchCalls).isZero();
  }

  @Test
  public void catalogFederationOn_catalogSourceReachesTheCatalogLeg() {
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(true);
    catalog.enabled = true;

    ResultsResponse response = (ResultsResponse) resource
        .getResults("q", "COMPONENT", 1, 25, null, null, "catalog", null, null)
        .getEntity();

    assertThat(response.getTab()).isEqualTo(Tab.COMPONENT);
    // The flag is on and the fake reports the catalog reachable, so the dispatcher consults the leg.
    assertThat(catalog.searchCalls).isPositive();
  }

  @Test
  public void catalogFederationOff_localSourceStillServed() {
    // The flag gates only the catalog source; the default local source is unaffected.
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);
    iq.registerRow(Tab.APPLICATION,
        ResultRow.builder().type("APPLICATION").source(SearchSource.LOCAL.value()).id("a1").title("App 1").build());

    ResultsResponse response = (ResultsResponse) resource
        .getResults("q", "APPLICATION", 1, 25, null, null, "local", null, null)
        .getEntity();

    assertThat(response.getResults()).hasSize(1);
    assertThat(catalog.searchCalls).isZero();
  }

  private static final class FakeGlobalSearchResultsIqLocalClient
      implements GlobalSearchResultsIqLocalClient
  {
    private final Map<Tab, List<ResultRow>> byTab = new EnumMap<>(Tab.class);

    void registerRow(Tab tab, ResultRow row) {
      byTab.computeIfAbsent(tab, t -> new java.util.ArrayList<>()).add(row);
    }

    @Override
    public java.util.Optional<SectionResult> searchNative(ResultsRequest request) {
      List<ResultRow> rows = byTab.getOrDefault(request.getTab(), List.of());
      return java.util.Optional.of(new SectionResult(request.getTab(), rows, rows.size(), null, true));
    }
  }

  private static final class FakeCatalogClient
      implements GlobalSearchResultsCatalogClient
  {
    /** Counts dispatches into the catalog leg, so a test can assert the leg was never reached. */
    private int searchCalls;

    /** Off by default, matching a deployment with no reachable catalog. */
    private boolean enabled;

    @Override
    public java.util.Optional<SectionResult> searchResults(ResultsRequest request) {
      searchCalls++;
      return java.util.Optional.of(SectionResult.empty(request.getTab(), true));
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }
  }
}
