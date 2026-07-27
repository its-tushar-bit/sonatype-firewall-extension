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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.guide.api.dto.ApiSearchResponse;
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
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.stream.Collectors;

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
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(true);

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isGlobalSearchEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));

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
    d.componentIdentifier = new com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2();
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
      d.componentIdentifier = new com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2();
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
    d.componentIdentifier = new com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2();
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
    d.componentIdentifier = new com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2();
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
    d.componentIdentifier = new com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2();
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

  private static List<String> manyValues(final int n) {
    return java.util.stream.IntStream.range(0, n).mapToObj(i -> "v" + i).collect(Collectors.toList());
  }

  private static CatalogRequest request(final Map<String, Object> filters) {
    return new CatalogRequest("COMPONENT", "catalog", filters, 1, 25, null, null, false);
  }
}
