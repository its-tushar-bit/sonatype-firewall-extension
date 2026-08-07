/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.StaleCursorException;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;

import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatalogServiceTest
{
  private SearchIndexClient searchIndexClient;

  private SearchApiClient searchApiClient;

  private ProductLicense productLicense;

  private TenantUtil tenantUtil;

  private CatalogService service;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(true);

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    // Severity-band facet default: empty-band result so vulnerability-facet tests that don't
    // exercise band counts don't NPE on a null MetricAggregationResult. Lenient: not every test
    // reaches the facet path.
    org.mockito.Mockito.lenient()
        .when(searchIndexClient.aggregateCountByFloatField(anyString(), anyString(), anyMap(), any()))
        .thenReturn(new MetricAggregationResult(0L, Map.of()));

    searchApiClient = mock(SearchApiClient.class);
    productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(true);
    tenantUtil = mock(TenantUtil.class);
    when(tenantUtil.isMultiTenant()).thenReturn(false);
    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    service = new CatalogService(iq, searchApiClient, searchIndexClient, productLicense, tenantUtil);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  @Test
  public void catalogSource_component_mapsCatalogDocumentsToRows() {
    GuideComponentDocument doc = new GuideComponentDocument(
        "npm", null, "@scope", "react", "18.0.0", null, List.of(), List.of("ui"), true, 90, 5.5,
        Instant.parse("2024-01-01T00:00:00Z"), false, null, null);
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.<ComponentDocument>of(doc), 1, 0, 25, null));

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));

    assertThat(response.catalogAvailable()).isTrue();
    assertThat(response.source()).isEqualTo(SearchSource.CATALOG);
    assertThat(response.rows()).hasSize(1);
    assertThat(response.rows().get(0).getFields()).containsEntry("ecosystem", "npm");
    assertThat(response.rows().get(0).getFields()).containsEntry("latest", "18.0.0");
  }

  @Test
  public void catalogSource_vulnerability_mapsCatalogDocumentsToRows() {
    GuideVulnerabilityDocument doc = new GuideVulnerabilityDocument(
        "CVE-2021-44228", List.of(), "Log4Shell", 10.0, 10.0, List.of("CWE-502"), List.of(),
        List.of("maven"), false, true, 0.97, "NVD", Instant.parse("2021-12-10T00:00:00Z"), null);
    when(searchApiClient.searchVulnerabilities(any()))
        .thenReturn(new GuideVulnerabilitySearchResponse(List.<VulnerabilityDocument>of(doc), 1, 0, 25, null));

    CatalogResponse response =
        service.search(CatalogEntityType.VULNERABILITY, SearchSource.CATALOG, request(Map.of()));

    assertThat(response.catalogAvailable()).isTrue();
    assertThat(response.rows()).hasSize(1);
    assertThat(response.rows().get(0).getId()).isEqualTo("CVE-2021-44228");
    assertThat(response.rows().get(0).getFields()).containsEntry("isKev", true);
    assertThat(response.rows().get(0).getFields()).containsEntry("vulnerabilitySource", "NVD");
    assertThat(response.rows().get(0).getFields()).doesNotContainKey("source");
  }

  @Test
  public void search_pageBelowOne_throwsBadRequest() {
    CatalogRequest req = new CatalogRequest("COMPONENT", "catalog", Map.of(), 0, 25, null, null, false);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
  }

  @Test
  public void localSource_vulnerability_severitiesFilter_recordedAsWarning() {
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    CatalogRequest req = new CatalogRequest(
        "VULNERABILITY", "local", Map.of("severities", List.of("Critical")), 1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);
    assertThat(response.warnings()).anyMatch(w -> w.contains("severities"));
  }

  @Test
  public void catalogSource_catalogSourceDown_returnsCatalogUnavailable_notLocalRows() {
    when(searchApiClient.searchComponents(any()))
        .thenThrow(new GuideApiException(Response.Status.BAD_GATEWAY, "down"));

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));

    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.rows()).isEmpty();
    assertThat(response.warnings()).contains("catalog source is unavailable");
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void catalogSource_catalogFederationFlagOff_returnsCatalogUnavailable() {
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.rows()).isEmpty();
    verify(searchApiClient, never()).searchComponents(any());
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void localSource_queriesIndex_notCatalog() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "log4j-core";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request(Map.of()));

    assertThat(response.source()).isEqualTo(SearchSource.LOCAL);
    assertThat(response.catalogAvailable()).isTrue();
    assertThat(response.rows()).hasSize(1);
    assertThat(response.rows().get(0).getSource()).isEqualTo("local");
    verify(searchApiClient, never()).searchComponents(any());
  }

  @Test
  public void localSource_catalogOnlyFilter_recordedAsWarning() {
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    CatalogResponse response = service.search(
        CatalogEntityType.COMPONENT, SearchSource.LOCAL, request(Map.of("epss", List.of(0.5, 1.0))));
    assertThat(response.warnings()).anyMatch(w -> w.contains("epss"));
  }

  @Test
  public void catalogSource_facetsFromAggregations() {
    Map<String, Map<String, Long>> aggs = Map.of("ecosystem", Map.of("npm", 3L, "maven", 2L));
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 5, 0, 25, aggs));

    CatalogRequest req = new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req);

    assertThat(response.facets()).isNotNull();
    assertThat(response.facets()).containsKey("ecosystem");
  }

  @Test
  public void localSource_sbomManagerOnlyLicense_mapsTo404_notServerError() {
    // The tenant is SBOM-Manager-only, so the mode check inside IqLocalSearchService.search rejects
    // this Lifecycle-only read with an InvalidLicenseException; the endpoint must surface a clean 404.
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenThrow(new InvalidLicenseException("Only SBOM Manager mode is supported by your license."));
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, false);

    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));

    assertThat(thrown).isInstanceOf(NotFoundException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(404);
  }

  @Test
  public void localSource_pageBeyondFirstWithoutCursor_mapsTo400() {
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 2, 25, null, null, false);

    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));

    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void catalogSource_sortAndSearchAfter_recordedAsWarnings() {
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 0, 0, 25, null));
    CatalogRequest req = new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 25, "name", "cursor", false);

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req);

    assertThat(response.warnings()).anyMatch(w -> w.contains("sort is not applied for the catalog source"));
    assertThat(response.warnings()).anyMatch(w -> w.contains("searchAfter is not applied for the catalog source"));
  }

  @Test
  public void localSource_nonRelevanceSort_isAppliedAsFieldSort_noRelevanceOnlyWarning() {
    // Field sort is enabled: a local COMPONENT name sort runs a real field sort, so no
    // "relevance-only" warning is emitted and the validated key is echoed for cursor minting.
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    when(searchIndexClient.searchGlobal(captor.capture()))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, "name", null, false);

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.warnings()).noneMatch(w -> w.contains("relevance-only"));
    assertThat(captor.getValue().sort()).as("local name sort must be a real field sort").isNotNull();
    assertThat(captor.getValue().sort().getSort()[0].getField())
        .isEqualTo(com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME.label);
  }

  @Test
  public void catalogSource_totalAtCap_reportedInexact() {
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 10_000, 0, 25, null));
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.totalEstimate()).isEqualTo(10_000);
    assertThat(response.exactTotalEstimate()).isFalse();
  }

  @Test
  public void catalogSource_cvssRangeFilter_isAccepted() {
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 0, 0, 25, null));
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("cvss", List.of(7.0, 10.0)), 1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req);
    assertThat(response.catalogAvailable()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void catalogSource_nullHits_doesNotNpe_returnsEmptyRows() {
    // A backend payload whose hits() is null must be treated as empty, not NPE into a 500.
    ApiSearchResponse<ComponentDocument> malformed = mock(ApiSearchResponse.class);
    when(malformed.hits()).thenReturn(null);
    when(malformed.total()).thenReturn(0L);
    when(malformed.aggregations()).thenReturn(null);
    when(searchApiClient.searchComponents(any())).thenReturn(malformed);

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));

    assertThat(response.catalogAvailable()).isTrue();
    assertThat(response.rows()).isEmpty();
  }

  @Test
  public void catalogSource_backendRuntimeException_degradesToCatalogUnavailable_not500() {
    when(searchApiClient.searchComponents(any())).thenThrow(new IllegalStateException("transport failure"));

    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));

    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.rows()).isEmpty();
  }

  @Test
  public void catalogUnavailable_reportsInexactTotal() {
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.totalEstimate()).isZero();
    assertThat(response.exactTotalEstimate()).isFalse();
  }

  @Test
  public void catalogUnavailable_carriesSortAndSearchAfterWarnings() {
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);
    CatalogRequest req = new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 25, "name", "cursor", false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req);
    assertThat(response.warnings()).contains("catalog source is unavailable");
    assertThat(response.warnings()).anyMatch(w -> w.contains("sort is not applied for the catalog source"));
    assertThat(response.warnings()).anyMatch(w -> w.contains("searchAfter is not applied for the catalog source"));
  }

  @Test
  public void invertedRangeFilter_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("cvss", List.of(10.0, 0.0)), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(thrown.getMessage()).doesNotContain("cvss").doesNotContain("10");
  }

  @Test
  public void versionScore_nonIntegralBound_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("versionScore", List.of(7.9, 10.0)), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void catalogSource_pageSize150_isAccepted() {
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 0, 0, 150, null));
    CatalogRequest req = new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 150, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req);
    assertThat(response.catalogAvailable()).isTrue();
  }

  @Test
  public void localSource_pageSize150_mapsTo400() {
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 150, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void pageBeyondMaximum_mapsTo400() {
    CatalogRequest req = new CatalogRequest("COMPONENT", "catalog", Map.of(), 1_000_000, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void catalogSource_component_maxCvssKey_isCamelCase() {
    GuideComponentDocument doc = new GuideComponentDocument(
        "npm", null, null, "react", "18.0.0", null, List.of(), List.of(), true, 90, 5.5,
        null, false, null, null);
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.<ComponentDocument>of(doc), 1, 0, 25, null));
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.rows().get(0).getFields()).containsKey("latestMaxCvss");
    assertThat(response.rows().get(0).getFields()).doesNotContainKey("latest_max_cvss");
  }

  @Test
  public void localSource_component_facetCounts_areWholeCorpus_notPageOnly() {
    // One npm component on this page, but the whole RBAC-scoped corpus has 42 npm components.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("npm");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(contains("componentFormat:\"npm\""))).thenReturn(42L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("ecosystem");
    CatalogResponse.CatalogFacetBucket npm = response.facets()
        .get("ecosystem")
        .stream()
        .filter(b -> b.value().equals("npm"))
        .findFirst()
        .orElseThrow();
    assertThat(npm.count()).isEqualTo(42L);
    // The count must be RBAC-scoped over the item type, not the single page row.
    verify(searchIndexClient).count(contains("itemType:non_vulnerable_component"));
  }

  @Test
  public void localSource_component_organizationsFilter_compilesToOrganizationField_andFacet() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.organizationName = "Acme";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(7L);

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("organizations", List.of("Acme")), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    // The organizations filter is honoured locally (not dropped as a warning) and compiles onto the
    // ancestor-carrying organization field.
    assertThat(response.warnings()).noneMatch(w -> w.contains("organizations"));
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    assertThat(captor.getValue().baseQuery().toString().toLowerCase()).contains("parentorganizationname");

    // An Organizations facet is present with a whole-corpus count.
    assertThat(response.facets()).containsKey("organization");
    assertThat(response.facets().get("organization")).anyMatch(b -> b.value().equals("Acme") && b.count() == 7L);
    verify(searchIndexClient).count(contains("organizationName:\"Acme\""));
  }

  @Test
  public void catalogSource_organizationsFilter_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("organizations", List.of("Acme")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void localSource_component_facetCountQueries_boundedByGlobalBudget() {
    // Build a page with many distinct ecosystem and organization values (the two real component
    // facets) so the total distinct-value count exercises the global count() budget. Verifies fan-out
    // stays bounded by MAX_FACET_COUNT_QUERIES using fields a real NON_VULNERABLE_COMPONENT doc
    // actually carries (componentFormat / organizationName), not a synthetic license-threat-group.
    List<SearchResultItemDTO> page = new ArrayList<>();
    for (int i = 0; i < CatalogService.MAX_FACET_BUCKETS_PER_FIELD; i++) {
      SearchResultItemDTO d = new SearchResultItemDTO();
      d.itemType = "NON_VULNERABLE_COMPONENT";
      d.componentName = "comp-" + i;
      d.componentIdentifier = new ApiComponentIdentifierDTOV2();
      d.componentIdentifier.setFormat("fmt-" + i);
      d.organizationName = "org-" + i;
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.copyOf(page), page.size(), List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(1L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 100, null, null, true);
    service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    // Total count() fan-out never exceeds the per-request budget regardless of distinct-value count.
    verify(searchIndexClient, atMost(CatalogService.MAX_FACET_COUNT_QUERIES)).count(anyString());
  }

  @Test
  public void localSource_component_facetCounts_notTruncated_whenWithinBudget() {
    // A single ecosystem value: one count() call, well within budget, no truncation warning.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("npm");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(1L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.warnings()).doesNotContain(CatalogService.CatalogWarnings.FACET_COUNTS_TRUNCATED);
  }

  @Test
  public void localSource_termsFilterNotArray_mapsTo400() {
    // A known TERMS filter given a bare string (not an array) is a client type error, rejected as a
    // 400 rather than masked as "unavailable locally".
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("ecosystems", "npm"), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    assertThat(thrown.getMessage()).doesNotContain("npm");
  }

  @Test
  public void catalogSource_scalarFilterNotString_mapsTo400() {
    // A SCALAR filter given a non-string value must 400 rather than being coerced and forwarded.
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("latestStable", List.of("recent")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchApiClient, never()).searchComponents(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void catalogSource_wrongTypeDoc_isDropped_notSilentlySkipped_andWarns() {
    // A backend response carrying a ComponentDocument that is NOT the expected GuideComponentDocument
    // subtype (schema drift / wrong response shape) must not be silently skipped: it is dropped (not
    // mapped into a row) and accounted for in collect()'s type-mismatch WARN path.
    ApiSearchResponse<ComponentDocument> response = mock(ApiSearchResponse.class);
    ComponentDocument wrongType = mock(ComponentDocument.class);
    when(response.hits()).thenReturn(List.of(wrongType));
    when(response.total()).thenReturn(1L);
    when(response.aggregations()).thenReturn(null);
    when(searchApiClient.searchComponents(any())).thenReturn(response);

    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CatalogService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    final CatalogResponse result;
    try {
      result = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    }
    finally {
      logger.detachAppender(appender);
    }

    assertThat(result.catalogAvailable()).isTrue();
    assertThat(result.rows()).isEmpty();
    // The type-mismatched doc is reported (count of 1), not silently swallowed.
    ILoggingEvent warn = appender.list.stream()
        .filter(e -> e.getLevel() == Level.WARN)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected a WARN log for the dropped doc"));
    assertThat(warn.getFormattedMessage()).contains("1 of an unexpected type");
  }

  @Test
  public void catalogComponent_row_hasNoHref_andIsNotDropped() {
    // Catalog rows stay within Lifecycle: no Guide href is emitted. A null href must not drop the row.
    GuideComponentDocument doc = new GuideComponentDocument(
        "npm", null, "@scope", "react", "18.0.0", null, List.of(), List.of(), true, 90, 5.5,
        null, false, null, null);

    CatalogRow row = CatalogRowMapper.catalogComponent(doc);

    assertThat(row).isNotNull();
    assertThat(row.getId()).isEqualTo("pkg:npm/@scope/react@18.0.0");
    assertThat(row.getHref()).isNull();
  }

  @Test
  public void catalogVulnerability_row_hasNoHref_andIsNotDropped() {
    GuideVulnerabilityDocument doc = new GuideVulnerabilityDocument(
        "CVE-2021-44228", List.of(), "Log4Shell", 10.0, 10.0, List.of(), List.of(), List.of("maven"),
        false, true, 0.97, "NVD", null, null);

    CatalogRow row = CatalogRowMapper.catalogVulnerability(doc);

    assertThat(row).isNotNull();
    assertThat(row.getId()).isEqualTo("CVE-2021-44228");
    assertThat(row.getHref()).isNull();
  }

  @Test
  public void catalogSource_unlicensedSingleTenant_deniesCatalogLeg_noHdsCall() {
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.rows()).isEmpty();
    verify(searchApiClient, never()).searchComponents(any());
  }

  @Test
  public void catalogSource_multiTenant_deniesCatalogLeg_noHdsCall() {
    when(tenantUtil.isMultiTenant()).thenReturn(true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.rows()).isEmpty();
    verify(searchApiClient, never()).searchComponents(any());
  }

  @Test
  public void catalogSource_licensedSingleTenant_allowsCatalogLeg() {
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 0, 0, 25, null));
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, request(Map.of()));
    assertThat(response.catalogAvailable()).isTrue();
    verify(searchApiClient).searchComponents(any());
  }

  @Test
  public void catalogSource_malformedFilter_mapsTo400_evenWhenFederationOff() {
    // Request validation runs before the availability/entitlement short-circuit, so a malformed
    // filter is a consistent 400 regardless of the CATALOG_FEDERATION flag state.
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("bogusKey", List.of("x")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
  }

  @Test
  public void localSource_page1WithCursor_mapsTo400() {
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, "cursor", false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void localSource_textFilterAsArray_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("query", List.of("react")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void localSource_rangeFilterMalformed_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("cvss", List.of(7.0)), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void localSource_scalarFilterAsObject_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("latestStable", Map.of("k", "v")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void localSource_restrictedReadContext_composesPermissionFilterIntoRowQuery_andScopesFacets() {
    // A caller whose read scope is a single context id. buildAllowedContextIdsFilter yields a real
    // TermInSetQuery over allowedContextIds; buildPermittedQuery (real method, see setUp) ANDs it into
    // the base query. Override wrapWithPermissionFilter to actually compose (setUp's default is
    // identity, which would drop the filter) so we can assert the restriction lands in the row query.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("ctx-allowed"));
    TermInSetQuery permissionFilter =
        new TermInSetQuery(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, List.of(new BytesRef("ctx-allowed")));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(permissionFilter);
    // Compose base AND filter here (the interface default wrap throws): mirrors AbstractSearchIndexClient.
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> {
      org.apache.lucene.search.Query base = inv.getArgument(0);
      org.apache.lucene.search.Query filter = inv.getArgument(1);
      if (filter == null) {
        return base;
      }
      return new org.apache.lucene.search.BooleanQuery.Builder()
          .add(base, org.apache.lucene.search.BooleanClause.Occur.MUST)
          .add(filter, org.apache.lucene.search.BooleanClause.Occur.FILTER)
          .build();
    });

    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("npm");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(3L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    // (a) The row query actually carries the RBAC restriction: the permission TermInSetQuery over
    // allowedContextIds is composed (AND/FILTER) into the base query handed to searchGlobal. A
    // regression that dropped the wrap would leave the restriction absent and fail here.
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    assertThat(captor.getValue().baseQuery().toString()).contains(FieldIdentifier.ALLOWED_CONTEXT_IDS.label);

    // (b) The restricted caller's rows + facet counts come back scoped: rows reflect only the permitted
    // page, and each facet bucket count is the RBAC-scoped count() over the same item type.
    assertThat(response.rows()).hasSize(1);
    assertThat(response.facets()).containsKey("ecosystem");
    assertThat(response.facets().get("ecosystem")).anyMatch(b -> b.value().equals("npm") && b.count() == 3L);
    verify(searchIndexClient).count(contains("itemType:non_vulnerable_component"));
  }

  @Test
  public void localSource_noReadableContexts_facetCountsAreZero_notUnscopedTotal() {
    // Fail-closed: a caller with no permitted contexts must count 0 per bucket, not an unscoped total.
    // The RBAC scoping lives inside the real count() impl; the mock models a scoped store returning 0.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("npm");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(0L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.facets().get("ecosystem")).allMatch(b -> b.count() == 0L);
  }

  @Test
  public void catalogSource_termsFilterExceedingCap_mapsTo400() {
    List<String> tooMany = manyValues(CatalogRequestBuilder.MAX_TERMS_PER_FILTER + 1);
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("ecosystems", tooMany), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchApiClient, never()).searchComponents(any());
  }

  @Test
  public void localSource_termsFilterExceedingCap_mapsTo400() {
    List<String> tooMany = manyValues(CatalogRequestBuilder.MAX_TERMS_PER_FILTER + 1);
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("ecosystems", tooMany), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void catalogSource_termsFilterAtCap_isAccepted() {
    when(searchApiClient.searchComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 0, 0, 25, null));
    List<String> atCap = manyValues(CatalogRequestBuilder.MAX_TERMS_PER_FILTER);
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("ecosystems", atCap), 1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req);
    assertThat(response.catalogAvailable()).isTrue();
  }

  @Test
  public void malwareBooleanScalar_acceptedOnBothSources() {
    // {"malware": true} (JSON boolean) must be accepted on catalog AND local, not a source-dependent
    // type contract. Catalog routes it through yesNoBool; local validateShapes now accepts a boolean.
    when(searchApiClient.searchVulnerabilities(any()))
        .thenReturn(new GuideVulnerabilitySearchResponse(List.of(), 0, 0, 25, null));
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));

    CatalogRequest catalogReq = new CatalogRequest(
        "VULNERABILITY", "catalog", Map.of("malware", Boolean.TRUE), 1, 25, null, null, false);
    assertThat(service.search(CatalogEntityType.VULNERABILITY, SearchSource.CATALOG, catalogReq).catalogAvailable())
        .isTrue();

    CatalogRequest localReq = new CatalogRequest(
        "VULNERABILITY", "local", Map.of("malware", Boolean.TRUE), 1, 25, null, null, false);
    assertThat(service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, localReq).source())
        .isEqualTo(SearchSource.LOCAL);
  }

  @Test
  public void malwareStringScalar_acceptedOnBothSources() {
    when(searchApiClient.searchVulnerabilities(any()))
        .thenReturn(new GuideVulnerabilitySearchResponse(List.of(), 0, 0, 25, null));
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));

    CatalogRequest catalogReq = new CatalogRequest(
        "VULNERABILITY", "catalog", Map.of("malware", "true"), 1, 25, null, null, false);
    assertThat(service.search(CatalogEntityType.VULNERABILITY, SearchSource.CATALOG, catalogReq).catalogAvailable())
        .isTrue();

    CatalogRequest localReq = new CatalogRequest(
        "VULNERABILITY", "local", Map.of("malware", "true"), 1, 25, null, null, false);
    assertThat(service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, localReq).source())
        .isEqualTo(SearchSource.LOCAL);
  }

  @Test
  public void malwareObjectScalar_mapsTo400_onBothSources() {
    CatalogRequest catalogReq = new CatalogRequest(
        "VULNERABILITY", "catalog", Map.of("malware", Map.of("k", "v")), 1, 25, null, null, false);
    Throwable catalogThrown =
        catchThrowable(() -> service.search(CatalogEntityType.VULNERABILITY, SearchSource.CATALOG, catalogReq));
    assertThat(catalogThrown).isInstanceOf(BadRequestException.class);

    CatalogRequest localReq = new CatalogRequest(
        "VULNERABILITY", "local", Map.of("malware", Map.of("k", "v")), 1, 25, null, null, false);
    Throwable localThrown =
        catchThrowable(() -> service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, localReq));
    assertThat(localThrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void latestStableBooleanScalar_mapsTo400_onBothSources() {
    // latestStable/publishedWindow are string-only SCALARs on the catalog path (scalarString), so a
    // JSON boolean must 400 on BOTH sources. Only the yes/no fields (malware/kev/patchAvailable)
    // accept a boolean; the shape contract stays symmetric for the non-yes/no SCALARs.
    CatalogRequest catalogReq = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("latestStable", Boolean.TRUE), 1, 25, null, null, false);
    Throwable catalogThrown =
        catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, catalogReq));
    assertThat(catalogThrown).isInstanceOf(BadRequestException.class);

    CatalogRequest localReq = new CatalogRequest(
        "COMPONENT", "local", Map.of("latestStable", Boolean.TRUE), 1, 25, null, null, false);
    Throwable localThrown =
        catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, localReq));
    assertThat(localThrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void rangeBoundNaN_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("cvss", List.of(Double.NaN, 10.0)), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
  }

  @Test
  public void rangeBoundInfinity_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("epss", List.of(0.0, Double.POSITIVE_INFINITY)), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
  }

  @Test
  public void localSource_malformedSearchAfterCursor_mapsTo400() {
    // A searchAfter cursor that fails to decode is a 400 (BadRequestException from the service),
    // never a 500. page > 1 so the cursor is required and reaches the decode path.
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 2, 25, null, "!!not-base64!!", false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
  }

  @Test
  public void localSource_staleCursor_mapsTo410() {
    // A well-formed cursor minted under an older generation token is stale -> 410 Gone, not a 500.
    String stale = com.sonatype.insight.brain.search.global.GlobalSearchCursor
        .newCursor("stale-generation-token", List.of("x"))
        .encode();
    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 2, 25, null, stale, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(StaleCursorException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(410);
  }

  @Test
  public void localSource_malformedValueForCatalogOnlyKey_mapsTo400_notWarning() {
    // latestStable is a catalog-only SCALAR with no local field mapping. A wrong-shaped value (array)
    // must 400 up front (validateShapes over ALL keys) rather than being swallowed as unavailable-locally.
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("latestStable", List.of("recent")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(new ErrorResponseGenerator().mapExceptionAndLog(thrown).getStatusCode()).isEqualTo(400);
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void localSource_component_idIsComponentHash_notName() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "abc123hash";
    d.componentName = "log4j-core";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    // The stable, component-centric id is the hash; the display name is the title.
    assertThat(response.rows().get(0).getId()).isEqualTo("abc123hash");
    assertThat(response.rows().get(0).getTitle()).isEqualTo("log4j-core");
    assertThat(response.rows().get(0).getFields()).containsEntry("componentHash", "abc123hash");
  }

  @Test
  public void localSource_component_enrichesVersionAndCoordinatePurl() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hash1";
    d.componentName = "react";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("npm");
    d.componentIdentifier.setCoordinates(Map.of("packageId", "react", "version", "18.0.0"));
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    assertThat(response.rows().get(0).getFields()).containsEntry("version", "18.0.0");
    assertThat(response.rows().get(0).getFields()).containsEntry("coordinates", "pkg:npm/react@18.0.0");
    assertThat(response.rows().get(0).getFields()).containsEntry("ecosystem", "npm");
  }

  @Test
  public void localSource_component_malformedCoordinate_dropsPurl_keepsRow() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hash1";
    d.componentName = "no-format";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    // No format => cannot build a purl; the row must still be returned on its hash id.
    d.componentIdentifier.setCoordinates(Map.of("version", "1.0"));
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    assertThat(response.rows()).hasSize(1);
    assertThat(response.rows().get(0).getFields()).doesNotContainKey("coordinates");
  }

  @Test
  public void localSource_component_affectedApps_isDistinctAppCountOverComponentHash() {
    // A single component on the page used by 2 applications: affectedApps must be 2 (distinct app
    // count grouped by the row's componentHash), computed in ONE grouped read scoped to the component
    // item type + RBAC (no caller filter clauses — global reach).
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hashA";
    d.componentName = "commons-io";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    // The client keys its grouped-count map by the lowercased group value (keyword fields carry a
    // lowercase normalizer); enrichLocalCounts looks up with the lowercased row value to match.
    when(searchIndexClient.countDistinctGroupedBy(
        contains("itemType:non_vulnerable_component"), eq("componentHash"), eq("applicationId"), eq(Set.of("hashA"))))
            .thenReturn(Map.of("hasha", 2L));

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 2L);
    // Exactly one grouped read for the whole page (item-type scoped, RBAC applied in the client).
    verify(searchIndexClient)
        .countDistinctGroupedBy(anyString(), eq("componentHash"), eq("applicationId"), anyCollection());
  }

  @Test
  public void localSource_component_affectedApps_globalReach_ignoresActiveFilters() {
    // affectedApps is global reach: an applications:[X] filter must NOT re-scope the count query
    // (it would collapse to 1 and mislead). The grouped read carries item type only, no app clause.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hashA";
    d.componentName = "commons-io";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("componentHash"), eq("applicationId"),
        anyCollection()))
            .thenReturn(Map.of("hasha", 5L));

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("applications", List.of("My App")), 1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 5L);
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient)
        .countDistinctGroupedBy(queryCaptor.capture(), eq("componentHash"), eq("applicationId"), anyCollection());
    assertThat(queryCaptor.getValue().toLowerCase()).doesNotContain("applicationname");
  }

  @Test
  public void localSource_component_applicationsAndStagesFilters_compileToLocalFields() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hashA";
    d.componentName = "commons-io";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local",
        Map.of("applications", List.of("My App"), "stages", List.of("build")),
        1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    // Both filters are honoured locally (not dropped as warnings) and compile onto the index fields.
    assertThat(response.warnings()).noneMatch(w -> w.contains("applications"));
    assertThat(response.warnings()).noneMatch(w -> w.contains("stages"));
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    String q = captor.getValue().baseQuery().toString().toLowerCase();
    assertThat(q).contains("applicationname");
    assertThat(q).contains("policyevaluationstage");
  }

  @Test
  public void catalogSource_applicationsFilter_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("applications", List.of("My App")), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    verify(searchApiClient, never()).searchComponents(any());
  }

  @Test
  public void localSource_vulnerability_enrichesSeverityAndHref() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2021-44228";
    d.vulnerabilitySeverity = 10.0f;
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("maven");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response =
        service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, request("VULNERABILITY", Map.of()));

    assertThat(response.rows().get(0).getFields()).containsEntry("severity", 10.0f);
    assertThat(response.rows().get(0).getFields()).containsEntry("ecosystem", "maven");
    // NOUX-safe relative classic route; the frontend prefixes the context-path.
    assertThat(response.rows().get(0).getHref()).isEqualTo("#/vulnerabilities/CVE-2021-44228");
  }

  @Test
  public void localSource_vulnerability_emitsFirstSeenIso_whenPresent() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2021-44228";
    d.vulnerabilityFirstSeenEpochMs = 1_700_000_000_000L;
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response =
        service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, request("VULNERABILITY", Map.of()));

    // ISO instant matching the All-tab's publishedAt date shape.
    assertThat(response.rows().get(0).getFields())
        .containsEntry("firstSeen", java.time.Instant.ofEpochMilli(1_700_000_000_000L).toString());
  }

  @Test
  public void localSource_vulnerability_firstSeenAbsent_whenNoViolationOpenTime() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2021-44228";
    d.vulnerabilityFirstSeenEpochMs = null;
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogResponse response =
        service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, request("VULNERABILITY", Map.of()));

    // A non-violating vuln carries no first-seen time; the row omits the firstSeen field entirely
    // (CatalogRow.field drops nulls), so the frontend renders a blank first-seen.
    assertThat(response.rows().get(0).getFields()).doesNotContainKey("firstSeen");
  }

  @Test
  public void firstSeenWindow_localOnly_rejectedOnCatalogSource() {
    CatalogRequest catalogReq = new CatalogRequest(
        "VULNERABILITY", "catalog", Map.of("firstSeenWindow", "30d"), 1, 25, null, null, false);
    Throwable thrown =
        catchThrowable(() -> service.search(CatalogEntityType.VULNERABILITY, SearchSource.CATALOG, catalogReq));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void firstSeenWindow_unknownToken_localSource_mapsTo400() {
    CatalogRequest localReq = new CatalogRequest(
        "VULNERABILITY", "local", Map.of("firstSeenWindow", "5w"), 1, 25, null, null, false);
    Throwable thrown =
        catchThrowable(() -> service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, localReq));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void localSource_vulnerability_affectedCounts_areDistinctOverVulnerabilityId() {
    // One CVE on the page affecting 3 apps across 2 distinct components, computed in TWO grouped
    // reads (one per metric) for the whole page, item-type scoped (global reach) + RBAC.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0001";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    // The client keys its grouped-count map by the lowercased group value (keyword fields carry a
    // lowercase normalizer); enrichLocalCounts looks up with the lowercased row value to match.
    when(searchIndexClient.countDistinctGroupedBy(
        contains("itemType:security_vulnerability"), eq("vulnerabilityId"), eq("applicationId"),
        eq(Set.of("CVE-2020-0001")))).thenReturn(Map.of("cve-2020-0001", 3L));
    when(searchIndexClient.countDistinctGroupedBy(
        contains("itemType:security_vulnerability"), eq("vulnerabilityId"), eq("componentHash"),
        eq(Set.of("CVE-2020-0001")))).thenReturn(Map.of("cve-2020-0001", 2L));

    CatalogResponse response =
        service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, request("VULNERABILITY", Map.of()));

    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 3L);
    assertThat(response.rows().get(0).getFields()).containsEntry("affectedComponents", 2L);
    verify(searchIndexClient)
        .countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("applicationId"), anyCollection());
    verify(searchIndexClient)
        .countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("componentHash"), anyCollection());
  }

  @Test
  public void localSource_vulnerability_affectedCounts_uppercaseCveId_matchLowercasedGroupedKey() {
    // Regression: the vulnerabilityId keyword field carries a lowercase normalizer, so the grouped-count
    // client keys its result map by the lowercased CVE ("cve-2021-44228") while the row value from _source
    // is uppercase ("CVE-2021-44228"). enrichLocalCounts must lowercase the lookup so the count resolves;
    // before the fix the case-sensitive lookup missed the bucket and every vuln row read affectedApps 0.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2021-44228";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("applicationId"),
        anyCollection())).thenReturn(Map.of("cve-2021-44228", 3L));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("componentHash"),
        anyCollection())).thenReturn(Map.of("cve-2021-44228", 2L));

    CatalogResponse response =
        service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, request("VULNERABILITY", Map.of()));

    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 3L);
    assertThat(response.rows().get(0).getFields()).containsEntry("affectedComponents", 2L);
  }

  @Test
  public void localSource_component_affectedApps_uppercaseHash_matchLowercasedGroupedKey() {
    // The latent COMPONENT counterpart: componentHash is normally lowercase hex (why the bug hid on
    // components), but the same lowercase normalizer applies. A mixed-case hash row must still resolve
    // its count against the lowercased grouped-map key.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "AbCdEf01";
    d.componentName = "commons-io";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("componentHash"), eq("applicationId"),
        anyCollection())).thenReturn(Map.of("abcdef01", 4L));

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 4L);
  }

  @Test
  public void localSource_vulnerability_affectedCounts_multiRowPage_singlePairOfGroupedReads() {
    // A full multi-row page still issues exactly two grouped reads total (not two per row), and each
    // row picks up its own count from the returned group map. A vuln absent from the map reads 0.
    List<SearchResultItemDTO> docs = new ArrayList<>();
    for (int i = 0; i < 80; i++) {
      SearchResultItemDTO d = new SearchResultItemDTO();
      d.itemType = "SECURITY_VULNERABILITY";
      d.vulnerabilityId = "CVE-" + i;
      docs.add(d);
    }
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(docs, docs.size(), List.of()));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("applicationId"),
        anyCollection()))
            .thenReturn(Map.of("cve-0", 4L));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("componentHash"),
        anyCollection()))
            .thenReturn(Map.of("cve-0", 2L));

    CatalogResponse response = service.search(
        CatalogEntityType.VULNERABILITY, SearchSource.LOCAL,
        new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 100, null, null, false));

    assertThat(response.rows()).hasSize(80);
    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 4L);
    assertThat(response.rows().get(0).getFields()).containsEntry("affectedComponents", 2L);
    // A vuln not present in the grouped result maps reads zero, never truncated/missing.
    assertThat(response.rows().get(1).getFields()).containsEntry("affectedApps", 0L);
    assertThat(response.rows().get(1).getFields()).containsEntry("affectedComponents", 0L);
    // Exactly two grouped reads for the whole page regardless of the 80 rows.
    verify(searchIndexClient, times(1))
        .countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("applicationId"), anyCollection());
    verify(searchIndexClient, times(1))
        .countDistinctGroupedBy(anyString(), eq("vulnerabilityId"), eq("componentHash"), anyCollection());
    verify(searchIndexClient, never()).countDistinct(anyString(), anyList());
  }

  @Test
  public void localSource_vulnerability_orgAppStageFilters_compileToLocalFields() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0002";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "VULNERABILITY", "local",
        Map.of("organizations", List.of("Acme"), "applications", List.of("My App"), "stages", List.of("build")),
        1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    // The filters are honoured locally (not dropped as unavailable-locally warnings) and compile
    // onto the index fields. A stages filter on vulns does add the SBOM-exclusion warning (finding 5),
    // asserted separately below.
    assertThat(response.warnings()).noneMatch(w -> w.contains("not available on the local source"));
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    String q = captor.getValue().baseQuery().toString().toLowerCase();
    assertThat(q).contains("parentorganizationname");
    assertThat(q).contains("applicationname");
    assertThat(q).contains("policyevaluationstage");
  }

  @Test
  public void localSource_vulnerability_stagesFilter_addsSbomExclusionWarning() {
    // SBOM-sourced vulns carry no policyEvaluationStage, so a stages filter silently excludes them;
    // the response must surface that as a warning rather than dropping them without a signal.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0009";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "VULNERABILITY", "local", Map.of("stages", List.of("build")), 1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    assertThat(response.warnings()).contains(CatalogService.CatalogWarnings.STAGES_EXCLUDE_SBOM_VULNS);
  }

  @Test
  public void localSource_component_stagesFilter_doesNotAddSbomExclusionWarning() {
    // The SBOM-exclusion warning is vuln-specific; a component stages filter must not add it.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hashA";
    d.componentName = "commons-io";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("stages", List.of("build")), 1, 25, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.warnings()).doesNotContain(CatalogService.CatalogWarnings.STAGES_EXCLUDE_SBOM_VULNS);
  }

  @Test
  public void localSource_vulnerability_ecosystemsFacet_countsDistinctCves() {
    // Vuln docs are per-app-per-stage, so a facet bucket must count DISTINCT vulnerabilityId (not raw
    // doc occurrences) to avoid inflating a CVE spanning multiple apps/stages (finding 4).
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0003";
    d.componentIdentifier = new ApiComponentIdentifierDTOV2();
    d.componentIdentifier.setFormat("maven");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinct(contains("componentFormat:\"maven\""), eq(List.of("vulnerabilityId"))))
        .thenReturn(9L);

    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("ecosystem");
    assertThat(response.facets().get("ecosystem"))
        .anyMatch(b -> b.value().equals("maven") && b.count() == 9L);
    // Distinct-CVE bucket count, scoped to the vuln item type (whole-corpus, RBAC-scoped, not page).
    // atLeastOnce: the severity-band facet also issues item-type-scoped distinct-CVE counts.
    verify(searchIndexClient, atLeastOnce())
        .countDistinct(contains("itemType:security_vulnerability"), eq(List.of("vulnerabilityId")));
  }

  @Test
  public void localSource_component_appsFacet_countsDistinctComponentsPerApp() {
    // A component recurs once per (app, stage), so the apps facet bucket must count DISTINCT
    // componentHash (distinct components in that app), not raw per-stage docs.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.componentHash = "hash-react";
    d.applicationName = "Acme Prod";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinct(contains("applicationName:\"Acme Prod\""), eq(List.of("componentHash"))))
        .thenReturn(12L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("application");
    assertThat(response.facets().get("application"))
        .anyMatch(b -> b.value().equals("Acme Prod") && b.count() == 12L);
    // Distinct-component bucket count, scoped to the component item type (whole-corpus, RBAC, not page).
    verify(searchIndexClient)
        .countDistinct(contains("itemType:non_vulnerable_component"), eq(List.of("componentHash")));
  }

  @Test
  public void localSource_component_policyTypesFacet_countsDistinctComponents_wholeCorpus() {
    // The denormalized componentViolationPolicyType set seeds the policyTypes facet. A component recurs
    // once per (app, stage), so each bucket counts DISTINCT componentHash, not raw per-stage docs, and
    // the bucket value round-trips through the policyTypes filter (matching componentViolationPolicyType).
    SearchResultItemDTO a = new SearchResultItemDTO();
    a.itemType = "NON_VULNERABLE_COMPONENT";
    a.componentName = "react";
    a.componentHash = "hash-react";
    a.componentViolationPolicyTypes = List.of("security", "license");
    SearchResultItemDTO b = new SearchResultItemDTO();
    b.itemType = "NON_VULNERABLE_COMPONENT";
    b.componentName = "lodash";
    b.componentHash = "hash-lodash";
    b.componentViolationPolicyTypes = List.of("security");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    when(searchIndexClient.countDistinct(
        contains("componentViolationPolicyType:\"security\""), eq(List.of("componentHash")))).thenReturn(13L);
    when(searchIndexClient.countDistinct(
        contains("componentViolationPolicyType:\"license\""), eq(List.of("componentHash")))).thenReturn(4L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("policyTypes");
    Map<String, Long> policyTypes = response.facets()
        .get("policyTypes")
        .stream()
        .collect(Collectors.toMap(
            CatalogResponse.CatalogFacetBucket::value, CatalogResponse.CatalogFacetBucket::count));
    assertThat(policyTypes).containsEntry("security", 13L).containsEntry("license", 4L);
    verify(searchIndexClient, atLeastOnce())
        .countDistinct(contains("itemType:non_vulnerable_component"), eq(List.of("componentHash")));
  }

  @Test
  public void localSource_component_violationStatesFacet_countsDistinctComponents_wholeCorpus() {
    SearchResultItemDTO a = new SearchResultItemDTO();
    a.itemType = "NON_VULNERABLE_COMPONENT";
    a.componentName = "react";
    a.componentHash = "hash-react";
    a.componentViolationStates = List.of("open", "waived");
    SearchResultItemDTO b = new SearchResultItemDTO();
    b.itemType = "NON_VULNERABLE_COMPONENT";
    b.componentName = "lodash";
    b.componentHash = "hash-lodash";
    b.componentViolationStates = List.of("open");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(a, b), 2, List.of()));
    when(searchIndexClient.countDistinct(
        contains("componentViolationState:\"open\""), eq(List.of("componentHash")))).thenReturn(21L);
    when(searchIndexClient.countDistinct(
        contains("componentViolationState:\"waived\""), eq(List.of("componentHash")))).thenReturn(6L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("violationStates");
    Map<String, Long> states = response.facets()
        .get("violationStates")
        .stream()
        .collect(Collectors.toMap(
            CatalogResponse.CatalogFacetBucket::value, CatalogResponse.CatalogFacetBucket::count));
    assertThat(states).containsEntry("open", 21L).containsEntry("waived", 6L);
  }

  @Test
  public void localSource_component_diversePage_keepsBoundedFacets_andOnlyTruncatesNameFacets() {
    // A maximally-diverse COMPONENT page: 20 distinct ecosystems + 20 orgs + 20 app names, which alone
    // want 60 counts against a 40-count budget. The bounded policyTypes/violationStates facets are
    // counted FIRST, so they survive intact; only the trailing high-cardinality name facets are cut, and
    // the truncation warning names them. Ordering these bounded facets last would return them EMPTY,
    // silently removing the policy-type and violation-state sections from the left nav.
    List<SearchResultItemDTO> page = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      SearchResultItemDTO d = new SearchResultItemDTO();
      d.itemType = "NON_VULNERABLE_COMPONENT";
      d.componentName = "comp-" + i;
      d.componentHash = "hash-" + i;
      d.organizationName = "Org " + i;
      d.applicationName = "App " + i;
      d.componentIdentifier = new ApiComponentIdentifierDTOV2();
      d.componentIdentifier.setFormat("fmt-" + i);
      d.componentViolationPolicyTypes = List.of("security", "license");
      d.componentViolationStates = List.of("open", "waived");
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(page, page.size(), List.of()));
    when(searchIndexClient.countDistinct(any(), any())).thenReturn(1L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    // The bounded facets are complete: every distinct value on the page has a bucket.
    assertThat(response.facets().get("policyTypes")).extracting(CatalogResponse.CatalogFacetBucket::value)
        .containsExactlyInAnyOrder("security", "license");
    assertThat(response.facets().get("violationStates")).extracting(CatalogResponse.CatalogFacetBucket::value)
        .containsExactlyInAnyOrder("open", "waived");
    // The budget was exhausted by the name facets, and the warning names them rather than the bounded ones.
    String warning = response.warnings()
        .stream()
        .filter(w -> w.startsWith(CatalogService.CatalogWarnings.FACET_COUNTS_TRUNCATED))
        .findFirst()
        .orElseThrow();
    assertThat(warning).doesNotContain("policyTypes").doesNotContain("violationStates");
  }

  @Test
  public void localSource_component_hasNoSeveritiesFacet() {
    // Component docs carry no threat/severity field (a deferred data gap), so a severities facet is
    // omitted rather than fabricated. Only ecosystem/organization/application facets are present.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentName = "react";
    d.componentHash = "hash-react";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    when(searchIndexClient.countDistinct(anyString(), anyList())).thenReturn(1L);

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.facets())
        .containsOnlyKeys("ecosystem", "organization", "application", "policyTypes", "violationStates");
    assertThat(response.facets()).doesNotContainKeys("severities", "severity");
  }

  @Test
  public void localSource_vulnerability_orgsFacet_countsDistinctCves() {
    // Vuln docs are per-app-per-stage, so the orgs facet bucket must count DISTINCT vulnerabilityId
    // (distinct CVEs in that org), not raw docs. organizationName is hierarchy-rewritten by the metric
    // layer to parentOrganizationName.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0100";
    d.organizationName = "Acme";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinct(contains("organizationName:\"Acme\""), eq(List.of("vulnerabilityId"))))
        .thenReturn(5L);

    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("organization");
    assertThat(response.facets().get("organization"))
        .anyMatch(b -> b.value().equals("Acme") && b.count() == 5L);
    // atLeastOnce: the severity-band facet also issues item-type-scoped distinct-CVE counts.
    verify(searchIndexClient, atLeastOnce())
        .countDistinct(contains("itemType:security_vulnerability"), eq(List.of("vulnerabilityId")));
  }

  @Test
  public void localSource_vulnerability_appsFacet_countsDistinctCves() {
    // apps facet bucket counts DISTINCT vulnerabilityId (distinct CVEs affecting that app), not raw docs.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0101";
    d.applicationName = "Acme Prod";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinct(contains("applicationName:\"Acme Prod\""), eq(List.of("vulnerabilityId"))))
        .thenReturn(8L);

    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("application");
    assertThat(response.facets().get("application"))
        .anyMatch(b -> b.value().equals("Acme Prod") && b.count() == 8L);
  }

  @Test
  public void localSource_vulnerability_hasSeverityBandFacet_withFiveCvssBands() {
    // vulnerabilitySeverity is a FloatPoint CVSS score; the float-range aggregation primitive now bands
    // it, so the local Vulnerabilities leg exposes a "severity" facet with the five fixed CVSS bands
    // alongside the status/ecosystem/organization/application facets.
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0102";
    d.vulnerabilitySeverity = 5.0f;
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    when(searchIndexClient.countDistinct(anyString(), anyList())).thenReturn(1L);
    when(searchIndexClient.aggregateCountByFloatField(anyString(), anyString(), anyMap(), anyString()))
        .thenReturn(new MetricAggregationResult(1L, Map.of()));

    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey("severity");
    assertThat(response.facets().get("severity"))
        .extracting(CatalogResponse.CatalogFacetBucket::value)
        .containsExactly("none", "low", "medium", "high", "critical");
  }

  @Test
  public void localSource_vulnerability_severitySort_isRejected() {
    // severity is held OUT of the allowlist until numeric field-sort machinery lands (finding 3):
    // vulnerabilitySeverity is a numeric FloatPoint with no sorted-numeric twin, so an unknown-sort
    // 400 is the correct, safe behavior rather than a latent lexicographic/failed sort.
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, "severity", null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void localSource_unknownSort_mapsTo400() {
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));
    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, "bogusSort", null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void localSource_component_affectedApps_fullPage_singleGroupedRead() {
    // A full page issues exactly ONE grouped read (not one distinct-count per row): the old per-row
    // fan-out blew the query budget and opened a reader per row (findings 1 + 2).
    List<SearchResultItemDTO> page = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      SearchResultItemDTO d = new SearchResultItemDTO();
      d.itemType = "NON_VULNERABLE_COMPONENT";
      d.componentHash = "hash-" + i;
      d.componentName = "comp-" + i;
      page.add(d);
    }
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.copyOf(page), page.size(), List.of()));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), eq("componentHash"), eq("applicationId"),
        anyCollection()))
            .thenReturn(Map.of("hash-0", 7L));

    CatalogRequest req = new CatalogRequest("COMPONENT", "local", Map.of(), 1, 100, null, null, false);
    CatalogResponse response = service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(response.rows()).hasSize(100);
    assertThat(response.rows().get(0).getFields()).containsEntry("affectedApps", 7L);
    // Every other row reads zero (absent from the grouped map), never omitted/truncated.
    assertThat(response.rows().get(1).getFields()).containsEntry("affectedApps", 0L);
    verify(searchIndexClient, times(1))
        .countDistinctGroupedBy(anyString(), eq("componentHash"), eq("applicationId"), anyCollection());
    verify(searchIndexClient, never()).countDistinct(anyString(), anyList());
  }

  @Test
  public void localSource_vulnerability_severityFacet_hasFiveCvssBands_withDistinctCveCounts() {
    // A multi-app CVE set: the same CVE recurs across per-app-per-stage docs, so the band count must be
    // distinct vulnerabilityId (not raw docs) — mirroring the orgs/apps vuln facets. Two rows on the page
    // seed nothing for severity (bands are fixed), so the buckets come purely from the per-band
    // countDistinct over the half-open CVSS range on vulnerabilitySeverity.
    SearchResultItemDTO high = new SearchResultItemDTO();
    high.itemType = "SECURITY_VULNERABILITY";
    high.vulnerabilityId = "CVE-2021-44228";
    high.vulnerabilitySeverity = 7.5f;
    SearchResultItemDTO crit = new SearchResultItemDTO();
    crit.itemType = "SECURITY_VULNERABILITY";
    crit.vulnerabilityId = "CVE-2022-0001";
    crit.vulnerabilitySeverity = 9.8f;
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(high, crit), 2, List.of()));

    // Distinct-CVE counts per band, computed in a single float-band aggregation pass over
    // vulnerabilitySeverity with distinctField=vulnerabilityId (single source of truth for the bands:
    // CvssV3Severity.halfOpenScoreBands()). These are the exact same per-band numbers the previous
    // per-band countDistinct loop produced — the refactor to one aggregation pass must not change them.
    java.util.Map<String, Long> bandCounts = new java.util.LinkedHashMap<>();
    bandCounts.put("none", 1L);
    bandCounts.put("low", 4L);
    bandCounts.put("medium", 6L);
    bandCounts.put("high", 3L);
    bandCounts.put("critical", 2L);
    when(searchIndexClient.aggregateCountByFloatField(
        contains("itemType:security_vulnerability"),
        eq(FieldIdentifier.VULNERABILITY_SEVERITY.label),
        eq(CvssV3Severity.halfOpenScoreBands()),
        eq("vulnerabilityId")))
            .thenReturn(new MetricAggregationResult(16L, bandCounts));

    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, null, null, true);
    CatalogResponse response = service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    assertThat(response.facets()).containsKey(CatalogService.SEVERITY_FACET_KEY);
    List<CatalogResponse.CatalogFacetBucket> severity =
        response.facets().get(CatalogService.SEVERITY_FACET_KEY);
    assertThat(severity).extracting(CatalogResponse.CatalogFacetBucket::value)
        .containsExactly("none", "low", "medium", "high", "critical");
    assertThat(severity).extracting(CatalogResponse.CatalogFacetBucket::count)
        .containsExactly(1L, 4L, 6L, 3L, 2L);
  }

  @Test
  public void localSource_vulnerability_severityFacet_isRbacScopedItemTypeCount() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2020-0001";
    d.vulnerabilitySeverity = 5.0f;
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    when(searchIndexClient.countDistinct(anyString(), anyList())).thenReturn(1L);
    when(searchIndexClient.aggregateCountByFloatField(anyString(), anyString(), anyMap(), anyString()))
        .thenReturn(new MetricAggregationResult(1L, Map.of()));

    CatalogRequest req = new CatalogRequest("VULNERABILITY", "local", Map.of(), 1, 25, null, null, true);
    service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, req);

    // The severity-band facet is a single item-type-scoped distinct-CVE aggregation pass (RBAC applied
    // inside the client, fail-closed), bucketing vulnerabilitySeverity by the half-open CVSS bands with
    // distinctField=vulnerabilityId — not a raw doc count and not scoped to the page. The half-open bands
    // (single source of truth CvssV3Severity.halfOpenScoreBands()) put a 7.0 in High, never Medium.
    verify(searchIndexClient).aggregateCountByFloatField(
        contains("itemType:security_vulnerability"),
        eq(FieldIdentifier.VULNERABILITY_SEVERITY.label),
        eq(CvssV3Severity.halfOpenScoreBands()),
        eq("vulnerabilityId"));
  }

  // ---- C1: Components leg per-severity active-violation counts (query-time, page-bounded) ----

  @Test
  public void localSource_component_perSeverityCounts_mapBandsToRowFields() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "hashA";
    d.componentName = "commons-io";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    // Result keyed by lowercased group value (keyword lowercase normalizer). severe->high, moderate->medium.
    // Note: int[] band-map values have identity equality, so eq(...searchAggregationBands()) would not
    // match a freshly-built map; match the bands with anyMap() and assert the exact bands via the
    // verify() below (the argument the service actually passes).
    when(searchIndexClient.countDistinctGroupedByBands(
        contains("itemType:policy_violation"), eq("componentHash"), eq("policyViolationId"),
        eq(Set.of("hashA")), eq("policyViolationThreatLevel"), anyMap()))
            .thenReturn(Map.of("hasha",
                Map.of("critical", 4L, "severe", 2L, "moderate", 1L, "low", 3L)));

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    assertThat(response.rows().get(0).getFields())
        .containsEntry("latest_critical_count", 4L)
        .containsEntry("latest_high_count", 2L)
        .containsEntry("latest_medium_count", 1L)
        .containsEntry("latest_low_count", 3L);
    // Active-only: the base query pins waiverStatus=Active so waived violations aren't counted.
    ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, int[]>> bandsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(searchIndexClient).countDistinctGroupedByBands(q.capture(), eq("componentHash"),
        eq("policyViolationId"), anyCollection(), eq("policyViolationThreatLevel"), bandsCaptor.capture());
    assertThat(q.getValue()).contains("policyViolationWaiverStatus:\"Active\"");
    // The four ThreatLevel severity bands drive the counts (single source of truth).
    assertThat(bandsCaptor.getValue().keySet()).containsExactlyInAnyOrderElementsOf(
        com.sonatype.insight.brain.utils.ThreatLevel.searchAggregationBands().keySet());
  }

  @Test
  public void localSource_component_perSeverityCounts_absentComponentReadsZero() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "noViolations";
    d.componentName = "clean-lib";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));
    // A component with no active violation is absent from the grouped result -> all four counts zero.
    when(searchIndexClient.countDistinctGroupedByBands(anyString(), eq("componentHash"), eq("policyViolationId"),
        anyCollection(), eq("policyViolationThreatLevel"), anyMap()))
            .thenReturn(Map.of());

    CatalogResponse response =
        service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    assertThat(response.rows().get(0).getFields())
        .containsEntry("latest_critical_count", 0L)
        .containsEntry("latest_high_count", 0L)
        .containsEntry("latest_medium_count", 0L)
        .containsEntry("latest_low_count", 0L);
  }

  @Test
  public void localSource_component_perSeverityCounts_emptyPage_noAggregation() {
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));

    service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, request("COMPONENT", Map.of()));

    verify(searchIndexClient, never()).countDistinctGroupedByBands(
        anyString(), anyString(), anyString(), anyCollection(), anyString(), anyMap());
  }

  @Test
  public void localSource_vulnerability_noPerSeverityAggregation() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "SECURITY_VULNERABILITY";
    d.vulnerabilityId = "CVE-2021-1";
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    service.search(CatalogEntityType.VULNERABILITY, SearchSource.LOCAL, request("VULNERABILITY", Map.of()));

    verify(searchIndexClient, never()).countDistinctGroupedByBands(
        anyString(), anyString(), anyString(), anyCollection(), anyString(), anyMap());
  }

  // ---- C2/C3/C4: Components leg violation filters (local-only + catalog-source rejection) ----

  @Test
  public void localSource_component_policyTypesFilter_compilesToDenormalizedField() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "h";
    d.componentName = "c";
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    when(searchIndexClient.searchGlobal(captor.capture()))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyTypes", List.of("Security")), 1, 25, null, null, false);
    service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(captor.getValue().baseQuery().toString().toLowerCase()).contains("componentviolationpolicytype");
  }

  @Test
  public void localSource_component_violationStatesFilter_compilesToDenormalizedField() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "h";
    d.componentName = "c";
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    when(searchIndexClient.searchGlobal(captor.capture()))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("violationStates", List.of("Open")), 1, 25, null, null, false);
    service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(captor.getValue().baseQuery().toString().toLowerCase()).contains("componentviolationstate");
  }

  @Test
  public void localSource_component_policyThreatLevelRange_compilesToIntRangeOnDenormalizedField() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "h";
    d.componentName = "c";
    ArgumentCaptor<GlobalSearchRequest> captor = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    when(searchIndexClient.searchGlobal(captor.capture()))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyThreatLevel", List.of(8, 10)), 1, 25, null, null, false);
    service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req);

    assertThat(captor.getValue().baseQuery().toString().toLowerCase()).contains("componentmaxpolicythreatlevel");
  }

  @Test
  public void localSource_component_invertedPolicyThreatLevelRange_mapsTo400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyThreatLevel", List.of(10, 0)), 1, 25, null, null, false);
    Throwable thrown = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, req));
    assertThat(thrown).isInstanceOf(BadRequestException.class);
    assertThat(thrown.getMessage()).doesNotContain("10").doesNotContain("policyThreatLevel");
  }

  @Test
  public void localSource_component_nonFinitePolicyThreatLevelRange_mapsTo400() {
    CatalogRequest nan = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyThreatLevel", List.of(Double.NaN, 10.0)), 1, 25, null, null, false);
    Throwable thrownNan = catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, nan));
    assertThat(thrownNan).isInstanceOf(BadRequestException.class);
    assertThat(thrownNan.getMessage()).doesNotContain("policyThreatLevel");

    CatalogRequest infinity = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyThreatLevel", List.of(0.0, Double.POSITIVE_INFINITY)), 1, 25, null, null,
        false);
    assertThat(catchThrowable(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, infinity)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void localSource_component_openEndedPolicyThreatLevelRange_isAccepted() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "NON_VULNERABLE_COMPONENT";
    d.componentHash = "h";
    d.componentName = "c";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(d), 1, List.of()));

    CatalogRequest lowerOnly = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyThreatLevel", java.util.Arrays.asList(8, null)), 1, 25, null, null, false);
    assertThat(service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, lowerOnly)).isNotNull();

    CatalogRequest upperOnly = new CatalogRequest(
        "COMPONENT", "local", Map.of("policyThreatLevel", java.util.Arrays.asList(null, 10)), 1, 25, null, null, false);
    assertThat(service.search(CatalogEntityType.COMPONENT, SearchSource.LOCAL, upperOnly)).isNotNull();
  }

  @Test
  public void catalogSource_component_policyTypesFilter_rejectedWith400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("policyTypes", List.of("Security")), 1, 25, null, null, false);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
  }

  @Test
  public void catalogSource_component_violationStatesFilter_rejectedWith400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("violationStates", List.of("Open")), 1, 25, null, null, false);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
  }

  @Test
  public void catalogSource_component_policyThreatLevelFilter_rejectedWith400() {
    CatalogRequest req = new CatalogRequest(
        "COMPONENT", "catalog", Map.of("policyThreatLevel", List.of(0, 10)), 1, 25, null, null, false);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.search(CatalogEntityType.COMPONENT, SearchSource.CATALOG, req));
  }

  private static List<String> manyValues(final int n) {
    return IntStream.range(0, n).mapToObj(i -> "v" + i).collect(Collectors.toList());
  }

  private static CatalogRequest request(final Map<String, Object> filters) {
    return new CatalogRequest("COMPONENT", "catalog", filters, 1, 25, null, null, false);
  }

  private static CatalogRequest request(final String entityType, final Map<String, Object> filters) {
    return new CatalogRequest(entityType, "local", filters, 1, 25, null, null, false);
  }
}
