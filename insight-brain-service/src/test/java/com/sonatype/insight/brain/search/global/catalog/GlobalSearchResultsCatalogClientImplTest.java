/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentLicense;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.global.ResultRow;
import com.sonatype.insight.brain.search.global.ResultsRequest;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.SectionResult;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalSearchResultsCatalogClientImplTest
{
  private GlobalSearchCatalogHdsClient hdsClient;

  private ProductLicense productLicense;

  private TenantUtil tenantUtil;

  private GlobalSearchResultsCatalogClientImpl client;

  @Before
  public void setUp() {
    hdsClient = mock(GlobalSearchCatalogHdsClient.class);
    productLicense = mock(ProductLicense.class);
    tenantUtil = mock(TenantUtil.class);
    client = new GlobalSearchResultsCatalogClientImpl(hdsClient, productLicense, tenantUtil);
  }

  private void entitle() {
    when(tenantUtil.isMultiTenant()).thenReturn(false);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(true);
  }

  private static ResultsRequest request(final Tab tab) {
    return new ResultsRequest("react", tab, 1, 25, null, null, SearchSource.CATALOG);
  }

  private static GuideGlobalSearchResponse responseOf(final SearchResult... hits) {
    return new GuideGlobalSearchResponse(List.of(hits), hits.length, 0, 25, Map.of());
  }

  private static GuideGlobalSearchResponse responseWithTotal(final long total, final SearchResult... hits) {
    return new GuideGlobalSearchResponse(List.of(hits), total, 0, 25, Map.of());
  }

  private static GuideComponentDocument component(final String name) {
    return new GuideComponentDocument(
        "maven", "originId", "org.example", name, "1.0.0", "registry",
        List.of(), List.of(), Boolean.TRUE, 90, 1.0,
        Instant.parse("2024-01-02T03:04:05Z"), Boolean.FALSE, null, null);
  }

  private static String offsetParam(final Multimap<String, String> params) {
    return params.get("offset").iterator().next();
  }

  @SuppressWarnings("unchecked")
  private Multimap<String, String> captureHdsParams() {
    ArgumentCaptor<Multimap<String, String>> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(hdsClient).getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), captor.capture());
    return captor.getValue();
  }

  @Test
  public void searchResults_page1MoreHits_sendsOffsetZero_mintsNonNullCursor() {
    entitle();
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(5L, component("a"), component("b")));
    ResultsRequest page1 = new ResultsRequest("react", Tab.COMPONENT, 1, 2, null, null, SearchSource.CATALOG);

    Optional<SectionResult> result = client.searchResults(page1);

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.rows()).hasSize(2);
    assertThat(section.totalEstimate()).isEqualTo(5L);
    assertThat(section.nextSearchAfter()).isNotNull();
    assertThat(offsetParam(captureHdsParams())).isEqualTo("0");
  }

  @Test
  public void searchResults_page2Cursor_sendsAdvancedOffset_toHds() {
    entitle();
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(5L, component("a"), component("b")));
    ResultsRequest page1 = new ResultsRequest("react", Tab.COMPONENT, 1, 2, null, null, SearchSource.CATALOG);
    String cursor = client.searchResults(page1).get().nextSearchAfter();

    reset(hdsClient);
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(5L, component("c"), component("d")));
    ResultsRequest page2 = new ResultsRequest("react", Tab.COMPONENT, 1, 2, null, cursor, SearchSource.CATALOG);

    Optional<SectionResult> result = client.searchResults(page2);

    assertThat(result).isPresent();
    assertThat(offsetParam(captureHdsParams())).isEqualTo("2");
    assertThat(result.get().rows().get(0).getId()).isEqualTo("pkg:maven/org.example/c@1.0.0");
  }

  @Test
  public void searchResults_lastPage_drained_nullCursor() {
    entitle();
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(4L, component("a"), component("b")));
    ResultsRequest page1 = new ResultsRequest("react", Tab.COMPONENT, 1, 2, null, null, SearchSource.CATALOG);
    String cursor = client.searchResults(page1).get().nextSearchAfter();
    assertThat(cursor).isNotNull();

    reset(hdsClient);
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(4L, component("c"), component("d")));
    ResultsRequest page2 = new ResultsRequest("react", Tab.COMPONENT, 1, 2, null, cursor, SearchSource.CATALOG);

    SectionResult section = client.searchResults(page2).get();

    assertThat(offsetParam(captureHdsParams())).isEqualTo("2");
    assertThat(section.nextSearchAfter()).isNull();
  }

  @Test
  public void searchResults_singlePageAllHits_nullCursor() {
    entitle();
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(2L, component("a"), component("b")));
    ResultsRequest req = new ResultsRequest("react", Tab.COMPONENT, 1, 25, null, null, SearchSource.CATALOG);

    SectionResult section = client.searchResults(req).get();

    assertThat(section.nextSearchAfter()).isNull();
    assertThat(section.totalEstimate()).isEqualTo(2L);
  }

  @Test
  public void searchResults_mixedStream_pageAllOtherType_emptyRowsButAdvancesByHitsConsumed() {
    entitle();
    // tab=VULNERABILITY but the HDS page holds only component hits (the mixed stream's other subtype).
    // The page maps to zero vulnerability rows, yet paging must keep advancing: mint a non-null cursor
    // whose offset advances by HITS CONSUMED (2), not mapped rows (0), while more hits remain (total=5).
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(5L, component("a"), component("b")));
    ResultsRequest page1 = new ResultsRequest("react", Tab.VULNERABILITY, 1, 2, null, null, SearchSource.CATALOG);

    SectionResult section = client.searchResults(page1).get();

    assertThat(section.rows()).isEmpty();
    assertThat(section.totalEstimate()).isEqualTo(5L);
    assertThat(section.nextSearchAfter()).isNotNull();

    reset(hdsClient);
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseWithTotal(5L, component("c"), component("d")));
    ResultsRequest page2 =
        new ResultsRequest("react", Tab.VULNERABILITY, 1, 2, null, section.nextSearchAfter(), SearchSource.CATALOG);

    client.searchResults(page2);

    // The follow-up call advanced the HDS offset by the 2 hits consumed on page 1, not by mapped rows.
    assertThat(offsetParam(captureHdsParams())).isEqualTo("2");
  }

  @Test
  public void searchResults_mixedStream_consumedOffsetReachesTotal_nullCursor() {
    entitle();
    // Consumed offset would reach total (offset 3 + 2 hits = 5 == total): stop paging even though this
    // page mapped zero rows of the requested subtype.
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(
            responseWithTotal(5L, component("a"), component("b"), component("c"), component("d"), component("e")));
    ResultsRequest req = new ResultsRequest("react", Tab.VULNERABILITY, 1, 25, null, null, SearchSource.CATALOG);

    SectionResult section = client.searchResults(req).get();

    assertThat(section.rows()).isEmpty();
    assertThat(section.nextSearchAfter()).isNull();
  }

  @Test
  public void isEnabled_entitledSingleTenant_true() {
    entitle();
    assertThat(client.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_multiTenant_false() {
    when(tenantUtil.isMultiTenant()).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(true);
    assertThat(client.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_missingGuideSearchFeature_false() {
    when(tenantUtil.isMultiTenant()).thenReturn(false);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(false);
    assertThat(client.isEnabled()).isFalse();
  }

  @Test
  public void searchResults_multiTenant_degradesWithoutHds() {
    when(tenantUtil.isMultiTenant()).thenReturn(true);

    Optional<SectionResult> result = client.searchResults(request(Tab.COMPONENT));

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.rows()).isEmpty();
    assertThat(section.catalogAvailable()).isFalse();
    assertThat(section.warnings()).contains("catalog source is unavailable");
    verify(hdsClient, never()).getWithMultimap(any(), any(), any());
  }

  @Test
  public void searchResults_missingFeature_degradesWithoutHds() {
    when(tenantUtil.isMultiTenant()).thenReturn(false);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(false);

    Optional<SectionResult> result = client.searchResults(request(Tab.VULNERABILITY));

    assertThat(result).isPresent();
    assertThat(result.get().catalogAvailable()).isFalse();
    assertThat(result.get().warnings()).contains("catalog source is unavailable");
    verify(hdsClient, never()).getWithMultimap(any(), any(), any());
  }

  @Test
  public void searchResults_componentEntitled_mapsRowWithCatalogFields_noHref() {
    entitle();
    GuideComponentDocument doc = new GuideComponentDocument(
        "maven", "originId", "org.example", "widget", "1.2.3", "registry",
        List.of(new GuideComponentLicense("Apache-2.0", "Copyleft", 1)),
        List.of("java", "ui"),
        Boolean.TRUE, 90, 7.5,
        Instant.parse("2024-01-02T03:04:05Z"),
        Boolean.FALSE, null, null);
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseOf(doc));

    Optional<SectionResult> result = client.searchResults(request(Tab.COMPONENT));

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.catalogAvailable()).isTrue();
    assertThat(section.rows()).hasSize(1);
    ResultRow row = section.rows().get(0);
    assertThat(row.getType()).isEqualTo(Tab.COMPONENT.name());
    assertThat(row.getSource()).isEqualTo(SearchSource.CATALOG.value());
    assertThat(row.getId()).isEqualTo("pkg:maven/org.example/widget@1.2.3");
    assertThat(row.getTitle()).isEqualTo("widget");
    assertThat(row.getSubtitle()).isEqualTo("1.2.3");
    assertThat(row.getHref()).isNull();
    Map<String, Object> fields = row.getFields();
    assertThat(fields).containsEntry("ecosystem", "maven");
    assertThat(fields).containsEntry("name", "widget");
    assertThat(fields).containsEntry("namespace", "org.example");
    assertThat(fields).containsEntry("latest", "1.2.3");
    assertThat(fields).containsEntry("licenses", List.of("Apache-2.0"));
    assertThat(fields).containsEntry("categories", List.of("java", "ui"));
    assertThat(fields).containsEntry("latestStable", Boolean.TRUE);
    assertThat(fields).containsEntry("versionScore", 90);
    assertThat(fields).containsEntry("latestMaxCvss", 7.5);
    assertThat(fields).containsEntry("publishedDate", "2024-01-02T03:04:05Z");
    assertThat(fields).containsEntry("malware", Boolean.FALSE);
  }

  @Test
  public void searchResults_vulnerabilityEntitled_mapsRowWithCatalogFields_noHref() {
    entitle();
    GuideVulnerabilityDocument doc = new GuideVulnerabilityDocument(
        "CVE-2024-1234",
        List.of("GHSA-xxxx"),
        "A nasty bug",
        9.8, 8.1,
        List.of("CWE-79"), List.of("CWE-89"),
        List.of("maven", "npm"),
        Boolean.FALSE, Boolean.TRUE, 0.42, "NVD",
        Instant.parse("2024-05-06T07:08:09Z"),
        "manual");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseOf(doc));

    Optional<SectionResult> result = client.searchResults(request(Tab.VULNERABILITY));

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.catalogAvailable()).isTrue();
    assertThat(section.rows()).hasSize(1);
    ResultRow row = section.rows().get(0);
    assertThat(row.getType()).isEqualTo(Tab.VULNERABILITY.name());
    assertThat(row.getSource()).isEqualTo(SearchSource.CATALOG.value());
    assertThat(row.getId()).isEqualTo("CVE-2024-1234");
    assertThat(row.getTitle()).isEqualTo("CVE-2024-1234");
    assertThat(row.getSubtitle()).isEqualTo("A nasty bug");
    assertThat(row.getHref()).isNull();
    Map<String, Object> fields = row.getFields();
    assertThat(fields).containsEntry("reference", "CVE-2024-1234");
    assertThat(fields).containsEntry("aliases", List.of("GHSA-xxxx"));
    assertThat(fields).containsEntry("vulnerabilitySource", "NVD");
    assertThat(fields).containsEntry("severity", 9.8);
    assertThat(fields).containsEntry("sonatypeSeverity", 8.1);
    assertThat(fields).containsEntry("cwe", List.of("CWE-79"));
    assertThat(fields).containsEntry("affectedEcosystems", List.of("maven", "npm"));
    assertThat(fields).containsEntry("isKev", Boolean.TRUE);
    assertThat(fields).containsEntry("epssScore", 0.42);
    assertThat(fields).containsEntry("isMalware", Boolean.FALSE);
    assertThat(fields).containsEntry("researchType", "manual");
    assertThat(fields).containsEntry("publishedAt", "2024-05-06T07:08:09Z");
  }

  @Test
  public void searchResults_hdsError_degradesSectionOnly() {
    entitle();
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenThrow(new BadGatewayException("upstream down"));

    Optional<SectionResult> result = client.searchResults(request(Tab.COMPONENT));

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.rows()).isEmpty();
    assertThat(section.catalogAvailable()).isFalse();
    assertThat(section.warnings()).contains("catalog source is unavailable");
  }

  @Test
  public void searchResults_nonCatalogTab_emptyAvailableSectionNoHds() {
    entitle();

    Optional<SectionResult> result = client.searchResults(request(Tab.APPLICATION));

    assertThat(result).isPresent();
    assertThat(result.get().rows()).isEmpty();
    verify(hdsClient, never()).getWithMultimap(any(), any(), any());
  }

  @Test
  public void searchResults_emptyHits_availableEmptySection() {
    entitle();
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), any(), any()))
        .thenReturn(responseOf());

    Optional<SectionResult> result = client.searchResults(request(Tab.COMPONENT));

    assertThat(result).isPresent();
    assertThat(result.get().rows()).isEmpty();
    assertThat(result.get().catalogAvailable()).isTrue();
  }
}
