/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;
import com.sonatype.insight.brain.search.indexquery.IndexQueryService;

import org.junit.Before;
import org.junit.Test;

/**
 * Wiring tests for the {@code includeFacets} path on {@link ResultsService}: facets are computed only
 * when requested, only for a single IQ-local entity tab, and never for ALL or the catalog source.
 */
public class ResultsServiceFacetsTest
{
  private GlobalSearchResultsIqLocalClient iq;

  private GlobalSearchResultsCatalogClient catalog;

  private IndexQueryService indexQueryService;

  private ResultsService service;

  @Before
  public void setUp() {
    iq = mock(GlobalSearchResultsIqLocalClient.class);
    catalog = mock(GlobalSearchResultsCatalogClient.class);
    indexQueryService = mock(IndexQueryService.class);
    service = new ResultsService(iq, catalog, indexQueryService);
  }

  private static ResultsRequest request(Tab tab, boolean includeFacets) {
    return new ResultsRequest("log4j", tab, 1, 25, null, null, SearchSource.DEFAULT, includeFacets);
  }

  private static ResultsRequest pageRequest(Tab tab, boolean includeFacets, int page, String cursor) {
    return new ResultsRequest("log4j", tab, page, 25, null, cursor, SearchSource.DEFAULT, includeFacets);
  }

  private static ResultsRequest catalogRequest(Tab tab, boolean includeFacets) {
    return new ResultsRequest("log4j", tab, 1, 25, null, null, SearchSource.CATALOG, includeFacets);
  }

  private static SectionResult section(Tab tab) {
    return new SectionResult(tab,
        List.of(ResultRow.builder().type(tab.name()).source(SearchSource.LOCAL.value()).id("x").title("x").build()),
        1L, null, true);
  }

  private static Map<String, List<IndexQueryFacetBucket>> sampleFacets() {
    return Map.of("policyTypes", List.of(new IndexQueryFacetBucket("SECURITY", 3L)));
  }

  @Test
  public void includeFacetsTrue_singleEntityTab_populatesFacets() {
    ResultsRequest req = request(Tab.VIOLATION, true);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.VIOLATION)));
    when(indexQueryService.facetsForResults(eq(Tab.VIOLATION), eq("log4j"), any())).thenReturn(sampleFacets());

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNotNull();
    assertThat(response.getFacets()).containsKey("policyTypes");
    verify(indexQueryService).facetsForResults(eq(Tab.VIOLATION), eq("log4j"), any());
  }

  @Test
  public void facetKeyOrder_isPreservedInTheResponse() {
    // computeFacets builds a LinkedHashMap in FACET_FIELDS declaration order (fixed facets first), so the
    // response must keep that order -- a rail rendered in response order would otherwise reshuffle between
    // requests. Map.copyOf would drop it, same trap tabCounts avoids with an EnumMap.
    java.util.Map<String, List<IndexQueryFacetBucket>> ordered = new java.util.LinkedHashMap<>();
    ordered.put("status", List.of(new IndexQueryFacetBucket("OPEN", 1L)));
    ordered.put("auto", List.of());
    ordered.put("threatLevel", List.of());
    ordered.put("scope", List.of());
    ResultsRequest req = request(Tab.WAIVER, true);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.WAIVER)));
    when(indexQueryService.facetsForResults(eq(Tab.WAIVER), any(), any())).thenReturn(ordered);

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets().keySet())
        .containsExactly("status", "auto", "threatLevel", "scope");
  }

  @Test
  public void facetTruncationWarning_reachesResponseWarnings() {
    // computeFacets appends FACET_COUNTS_TRUNCATED to the warnings list it is handed when the count
    // budget is exhausted. That list must reach the response, or the frontend renders an incomplete
    // filter rail with no signal.
    ResultsRequest req = request(Tab.VIOLATION, true);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.VIOLATION)));
    when(indexQueryService.facetsForResults(eq(Tab.VIOLATION), eq("log4j"), any())).thenAnswer(inv -> {
      List<String> sink = inv.getArgument(2);
      sink.add("facet counts truncated");
      return sampleFacets();
    });

    ResultsResponse response = service.search(req);

    assertThat(response.getWarnings()).contains("facet counts truncated");
    assertThat(response.getFacets()).containsKey("policyTypes");
  }

  @Test
  public void includeFacetsFalse_neverComputesFacets() {
    ResultsRequest req = request(Tab.VIOLATION, false);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.VIOLATION)));

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNull();
    verify(indexQueryService, never()).facetsForResults(any(), any(), any());
  }

  @Test
  public void allTab_neverComputesFacets_evenWhenRequested() {
    ResultsRequest req = request(Tab.ALL, true);
    // ALL is packed from per-section suppliers; each entity section returns empty here.
    when(iq.searchNative(any())).thenReturn(Optional.of(SectionResult.empty(Tab.APPLICATION)));

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNull();
    verify(indexQueryService, never()).facetsForResults(any(), any(), any());
  }

  @Test
  public void catalogSource_neverComputesFacets_evenWhenRequested() {
    ResultsRequest req = catalogRequest(Tab.COMPONENT, true);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.searchResults(any())).thenReturn(Optional.of(section(Tab.COMPONENT)));

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNull();
    verify(indexQueryService, never()).facetsForResults(any(), any(), any());
  }

  @Test
  public void componentTab_localSource_nullFacetSet_leavesFacetsNull() {
    // facetsForResults returns null for tabs with no facet set (COMPONENT today); the response must
    // carry a null facets map, not an empty one.
    ResultsRequest req = request(Tab.COMPONENT, true);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.COMPONENT)));
    when(indexQueryService.facetsForResults(eq(Tab.COMPONENT), any(), any())).thenReturn(null);

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNull();
  }

  @Test
  public void includeFacetsTrue_firstPage_computesFacets() {
    ResultsRequest req = pageRequest(Tab.VIOLATION, true, 1, null);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.VIOLATION)));
    when(indexQueryService.facetsForResults(eq(Tab.VIOLATION), any(), any())).thenReturn(sampleFacets());

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).containsOnlyKeys("policyTypes");
    verify(indexQueryService).facetsForResults(eq(Tab.VIOLATION), any(), any());
  }

  @Test
  public void includeFacetsTrue_secondPage_leavesFacetsNullAndSkipsTheFacetSearch() {
    // The facet map is identical on every page of a query, so pages after the first omit it instead of
    // paying another full index search plus a count query per bucket to rebuild it.
    ResultsRequest req = pageRequest(Tab.VIOLATION, true, 2, null);
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.VIOLATION)));

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNull();
    verify(indexQueryService, never()).facetsForResults(any(), any(), any());
  }

  @Test
  public void includeFacetsTrue_withCursor_skipsTheFacetSearchEvenWhenPageReportsOne() {
    // A cursor means the caller is resuming a previous page regardless of the page number it reports,
    // matching the sibling count probe's first-page test.
    ResultsRequest req = pageRequest(Tab.VIOLATION, true, 1, "opaque-cursor");
    when(iq.searchNative(req)).thenReturn(Optional.of(section(Tab.VIOLATION)));

    ResultsResponse response = service.search(req);

    assertThat(response.getFacets()).isNull();
    verify(indexQueryService, never()).facetsForResults(any(), any(), any());
  }
}
