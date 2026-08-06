/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.List;

import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.telemetry.GuideUsageEvent;
import com.sonatype.insight.brain.guide.telemetry.GuideUsageIdentifiers;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.SuggestItemType;
import com.sonatype.insight.brain.search.global.SuggestRow;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;

import com.google.common.collect.Multimap;
import jakarta.ws.rs.InternalServerErrorException;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlobalSearchSuggestCatalogClientImplTest
{
  @Mock
  private GlobalSearchCatalogHdsClient hdsClient;

  private GlobalSearchSuggestCatalogClientImpl underTest;

  @Before
  public void setUp() {
    underTest = new GlobalSearchSuggestCatalogClientImpl(hdsClient);
  }

  @Test
  public void isEnabled_true_baseFunctionality() {
    // Catalog federation is base Nexus One functionality: enabled with any valid IQ license,
    // on both single-tenant and MTIQ, regardless of the GUIDE_SEARCH feature.
    assertThat(underTest.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_true_onMultiTenantDeployment() {
    // Pins the base-functionality contract on MTIQ. The client holds no TenantUtil, so tenancy is not
    // observable from here and cannot be stubbed: unconditional enablement IS the MTIQ guarantee --
    // there is no code path by which a multi-tenant deployment can see isEnabled() == false.
    assertThat(underTest.isEnabled()).isTrue();
  }

  @Test
  public void suggest_onMultiTenantDeployment_reachesHdsAndReturnsCatalogRows() {
    // The MTIQ counterpart of the removed isEnabled_multiTenant_false: a multi-tenant deployment now
    // reaches HDS and receives catalog suggestions. Tenancy is deliberately not arranged -- the client
    // takes no tenancy dependency, so any deployment (single-tenant or MTIQ) exercises exactly this path.
    GuideComponentDocument component = component("maven", "org.example", "lib", "1.0.0");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of((SearchResult) component), 1, 0, 6, null));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isTrue();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows()).allMatch(r -> r.source() == SearchSource.CATALOG);
    verify(hdsClient).getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"),
        any(Multimap.class));
  }

  @Test
  public void suggest_buildsMultimapQueryParams_neverStringConcat() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of(), 0, 0, 0, null));

    underTest.suggest(new CatalogSuggestRequest("hello world", 6));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Multimap<String, String>> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(hdsClient).getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), captor.capture());
    Multimap<String, String> params = captor.getValue();
    assertThat(params.get("query")).containsExactly("hello world");
    assertThat(params.get("limit")).containsExactly("6");
  }

  @Test
  public void suggest_http200WithHits_returnsAvailableTaggedRows_noHref() {
    GuideComponentDocument component = component("maven", "org.example", "lib", "1.0.0");
    GuideVulnerabilityDocument vuln = vuln("CVE-2024-12345", "Remote code execution");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of((SearchResult) component, (SearchResult) vuln), 2, 0, 6,
            null));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isTrue();
    assertThat(result.rows()).hasSize(2);
    assertThat(result.rows()).allMatch(r -> r.source() == SearchSource.CATALOG);
    assertThat(result.rows()).allMatch(r -> r.href() == null, "no catalog-outbound href");
    assertThat(result.rows().stream().map(SuggestRow::type))
        .containsExactly(SuggestItemType.COMPONENT, SuggestItemType.VULNERABILITY);
    assertThat(result.rows().get(0).id()).isEqualTo("pkg:maven/org.example/lib@1.0.0");
  }

  @Test
  public void suggest_malformedComponent_isDropped() {
    GuideComponentDocument missingFormat = component(null, "org.example", "lib", "1.0.0");
    GuideComponentDocument missingName = component("maven", "org.example", null, "1.0.0");
    GuideComponentDocument valid = component("npm", null, "thing", "2.0.0");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(
            List.of((SearchResult) missingFormat, (SearchResult) missingName, (SearchResult) valid), 3, 0, 6, null));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().get(0).id()).isEqualTo("pkg:npm/thing@2.0.0");
  }

  @Test
  public void suggest_vulnWithBlankRefid_isDropped() {
    GuideVulnerabilityDocument blank = vuln("  ", "summary");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of((SearchResult) blank), 1, 0, 6, null));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_http200Empty_returnsAvailableEmpty() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of(), 0, 0, 6, null));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isTrue();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_http404_returnsAvailableEmpty() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new NotFoundException("not found"));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isTrue();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_http500_returnsUnavailable() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new InternalServerErrorException("oops"));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isFalse();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_http5xxBadGateway_returnsUnavailable() {
    // HdsClient maps 502 / 503 / 504 / socket-read timeout / 429 onto BadGatewayException.
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new BadGatewayException("bad gateway"));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isFalse();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_gatewayTimeout_returnsUnavailable() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new GatewayTimeoutException("timeout"));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isFalse();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_paymentRequired_returnsUnavailable() {
    // HDS answers 402 "Feature not enabled" for a licence without GUIDE_SEARCH. IQ no longer pre-judges
    // entitlement, so this is the live path for an unlicensed caller: report unavailable, never a 500.
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new PaymentRequiredException("payment required"));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isFalse();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_unexpectedRuntimeException_returnsUnavailable() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new IllegalStateException("boom"));

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isFalse();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void suggest_nullResponse_returnsUnavailable() {
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(null);

    CatalogSuggestResult result = underTest.suggest(new CatalogSuggestRequest("alpha", 6));

    assertThat(result.available()).isFalse();
    assertThat(result.rows()).isEmpty();
  }

  @Test
  public void telemetryAnnotation_isOnInnerHelper_notPublicSuggest() throws NoSuchMethodException {
    Method publicSuggest = GlobalSearchSuggestCatalogClientImpl.class.getMethod("suggest", CatalogSuggestRequest.class);
    assertThat(publicSuggest.getAnnotation(GuideUsageEvent.class)).isNull();

    Method inner = GlobalSearchSuggestCatalogClientImpl.class.getDeclaredMethod(
        "callCatalogGlobalSearch", CatalogSuggestRequest.class);
    assertThat(inner.getAnnotation(GuideUsageEvent.class)).isNotNull();
  }

  @Test
  public void catalogSuggestRequest_doesNotLeakQueryToUsageExtractor() {
    // GuideUsageIdentifiers.extract returns the first non-blank String arg OR a value from
    // .purl()/.id() accessors. CatalogSuggestRequest exposes only .query() and .limit(), so when
    // handed as a single Object[] arg it must yield null — no telemetry leakage.
    CatalogSuggestRequest request = new CatalogSuggestRequest("hello world", 6);
    String extracted = GuideUsageIdentifiers.extract(new Object[]{request});
    assertThat(extracted).isNull();
  }

  private static GuideComponentDocument component(
      final String format,
      final String namespace,
      final String name,
      final String version)
  {
    return new GuideComponentDocument(
        format, null, namespace, name, version, null, null, null, null, null, null, null, null, null, null);
  }

  private static GuideVulnerabilityDocument vuln(final String refid, final String summary) {
    return new GuideVulnerabilityDocument(
        refid, null, summary, 9.8, null, null, null, null, null, null, null, null, null, null);
  }
}
