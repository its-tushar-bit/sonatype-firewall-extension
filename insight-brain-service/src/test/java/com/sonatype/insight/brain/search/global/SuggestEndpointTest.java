/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchCatalogHdsClient;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchSuggestCatalogClient;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchSuggestCatalogClientImpl;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.common.collect.Multimap;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * End-to-end wiring test for {@code /rest/search/suggest}: real {@link GlobalSearchResource} + real
 * {@link SuggestServiceImpl} + real {@link BestMatchResolver} + real catalog client
 * ({@link GlobalSearchSuggestCatalogClientImpl}). The HDS transport and read-context gate are mocked;
 * calls go straight to the resource method rather than through a running container.
 *
 * <p>
 * The IQ-local leg is a hand-rolled fake so the test exercises the full orchestration end-to-end. It
 * covers the source states: flag off, local rows, catalog hits, catalog empty, catalog 5xx, BEST MATCH
 * dedup, and the fixed-order group envelope.
 */
@RunWith(MockitoJUnitRunner.class)
public class SuggestEndpointTest
{
  @Mock
  private GlobalSearchCatalogHdsClient hdsClient;

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private UserPrincipal principal;

  private FakeIqLocal iqLocal;

  private GlobalSearchResource resource;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    // CATALOG_FEDERATION defaults off; the resource rejects ?source=catalog while it is off, so the
    // catalog-source cases below must switch it on.
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(true);

    iqLocal = new FakeIqLocal();
    GlobalSearchSuggestCatalogClient catalog =
        new GlobalSearchSuggestCatalogClientImpl(hdsClient);

    when(currentUser.getUsernameOrSystem()).thenReturn("tester");
    when(currentUser.getUserPrincipal()).thenReturn(principal);

    SuggestService service = new SuggestServiceImpl(iqLocal, catalog, new BestMatchResolver(), currentUser,
        new PerUserRateLimiter(Integer.MAX_VALUE));

    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
    resource = new GlobalSearchResource(
        mock(ResultsService.class), service, searchIndexClient);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  @Test
  public void globalSearchFlagOff_endpointReturns404() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    assertThatThrownBy(() -> resource.suggest("alpha", null)).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void userWithNoReadGrants_endpointReturns403() {
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    assertThatThrownBy(() -> resource.suggest("alpha", null)).isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void catalogFederationOff_catalogSourceRejected_withoutReachingHds() {
    // The frontend hides the catalog toggle when CATALOG_FEDERATION is off; the backend must enforce the
    // same flag rather than trust that clamp, or a hand-crafted ?source=catalog reaches HDS anyway.
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);

    assertThatThrownBy(() -> resource.suggest("alpha", "catalog"))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(hdsClient);
  }

  @Test
  public void catalogFederationOff_localSourceStillServed() {
    // The flag gates only the catalog source; the default local source is unaffected.
    SystemConfigurationPropertyFeature.CATALOG_FEDERATION.setEnabled(false);
    iqLocal.add(row(SuggestItemType.APPLICATION, "app-1", "App One", SearchSource.LOCAL));

    SuggestResponse response = resource.suggest("alpha", "local");

    assertThat(response.catalogAvailable()).isNull();
    verifyNoInteractions(hdsClient);
  }

  @Test
  public void unknownSource_endpointReturns400() {
    assertThatThrownBy(() -> resource.suggest("alpha", "bogus")).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void catalogSource_lopsidedHdsMix_bothGroupsFillToCap() {
    // M1: a lopsided HDS response (8 components + 3 vulns) under one mixed limit must still fill BOTH
    // group caps (5 components, 3 vulns) rather than starving the smaller type.
    List<SearchResult> hits = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      hits.add(component("maven", "org.example", "lib" + i, "1.0." + i));
    }
    for (int i = 0; i < 3; i++) {
      hits.add(vuln("CVE-2024-100" + i, "summary " + i));
    }
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(hits, hits.size(), 0, 6, null));

    SuggestResponse response = resource.suggest("alpha", "catalog");

    assertThat(response.catalogAvailable()).isTrue();
    assertThat(groupOf(response, SuggestItemType.COMPONENT).results())
        .hasSize(SuggestServiceImpl.DEFAULT_PER_TYPE_LIMIT);
    assertThat(groupOf(response, SuggestItemType.VULNERABILITY).results()).hasSize(3);
  }

  @Test
  public void scopedUserWithReadGrant_endpointReturnsResponse() {
    iqLocal.add(row(SuggestItemType.APPLICATION, "app-1", "App One", SearchSource.LOCAL));

    SuggestResponse response = resource.suggest("alpha", null);

    assertThat(response).isNotNull();
    assertThat(groupOf(response, SuggestItemType.APPLICATION).results()).hasSize(1);
  }

  @Test
  public void localSource_returnsIqRows_catalogAvailableNull_noHdsCall() {
    iqLocal.add(row(SuggestItemType.COMPONENT, "pkg:maven/o/lib@1.0.0", "lib", SearchSource.LOCAL));
    iqLocal.add(row(SuggestItemType.VULNERABILITY, "CVE-2024-1", "CVE-2024-1", SearchSource.LOCAL));
    iqLocal.add(row(SuggestItemType.APPLICATION, "my-app", "My App", SearchSource.LOCAL));

    SuggestResponse response = resource.suggest("alpha", null);

    // Catalog not consulted on the local path: catalogAvailable is null (omitted from JSON).
    assertThat(response.catalogAvailable()).isNull();
    assertThat(groupOf(response, SuggestItemType.COMPONENT).source()).isEqualTo(SearchSource.LOCAL);
    assertThat(groupOf(response, SuggestItemType.VULNERABILITY).source()).isEqualTo(SearchSource.LOCAL);
    verifyNoInteractions(hdsClient);
  }

  @Test
  public void catalogSource_http200WithHits_returnsCatalogRows_catalogAvailableTrue() {
    GuideComponentDocument component = component("maven", "org.example", "lib", "1.0.0");
    GuideVulnerabilityDocument vuln = vuln("CVE-2024-12345", "Remote code execution");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of((SearchResult) component, (SearchResult) vuln), 2, 0, 6,
            null));

    SuggestResponse response = resource.suggest("alpha", "catalog");

    assertThat(response.catalogAvailable()).isTrue();
    SuggestGroup components = groupOf(response, SuggestItemType.COMPONENT);
    SuggestGroup vulns = groupOf(response, SuggestItemType.VULNERABILITY);
    assertThat(components.source()).isEqualTo(SearchSource.CATALOG);
    assertThat(vulns.source()).isEqualTo(SearchSource.CATALOG);
    assertThat(components.results()).hasSize(1);
    assertThat(vulns.results()).hasSize(1);
    // No catalog-outbound href on any row.
    assertThat(components.results()).allMatch(r -> r.href() == null);
  }

  @Test
  public void catalogSource_http200Empty_returnsEmpty_catalogAvailableTrue_noFallThrough() {
    // ?source=catalog with an empty catalog response returns empty catalog groups. IQ-local seed rows
    // are NOT surfaced (no cross-source fall-through).
    iqLocal.add(row(SuggestItemType.COMPONENT, "pkg:maven/o/lib@1.0.0", "lib", SearchSource.LOCAL));
    iqLocal.add(row(SuggestItemType.VULNERABILITY, "CVE-2024-1", "CVE-2024-1", SearchSource.LOCAL));
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of(), 0, 0, 6, null));

    SuggestResponse response = resource.suggest("alpha", "catalog");

    assertThat(response.catalogAvailable()).isTrue();
    assertThat(groupOf(response, SuggestItemType.COMPONENT).results()).isEmpty();
    assertThat(groupOf(response, SuggestItemType.VULNERABILITY).results()).isEmpty();
  }

  @Test
  public void catalogSource_http5xx_returnsEmpty_catalogAvailableFalse_noFallThrough() {
    iqLocal.add(row(SuggestItemType.COMPONENT, "pkg:maven/o/lib@1.0.0", "lib", SearchSource.LOCAL));
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenThrow(new BadGatewayException("bad gateway"));

    SuggestResponse response = resource.suggest("alpha", "catalog");

    assertThat(response.catalogAvailable()).isFalse();
    assertThat(groupOf(response, SuggestItemType.COMPONENT).results()).isEmpty();
  }

  @Test
  public void allFiveGroupsPresentInFixedOrder_underLocalSource_evenWhenEmpty() {
    SuggestResponse response = resource.suggest("nothing-matches", null);

    assertThat(response.groups()).extracting(SuggestGroup::type)
        .containsExactly(
            SuggestItemType.VULNERABILITY,
            SuggestItemType.COMPONENT,
            SuggestItemType.APPLICATION,
            SuggestItemType.VIOLATION,
            SuggestItemType.WAIVER);
  }

  @Test
  public void bestMatch_isRemovedFromItsNativeGroup() {
    GuideVulnerabilityDocument vuln = vuln("CVE-2024-12345", "RCE");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of((SearchResult) vuln), 1, 0, 6, null));

    SuggestResponse response = resource.suggest("CVE-2024-12345", "catalog");

    assertThat(response.bestMatch()).isNotNull();
    assertThat(response.bestMatch().id()).isEqualTo("CVE-2024-12345");
    SuggestGroup vulns = groupOf(response, SuggestItemType.VULNERABILITY);
    assertThat(vulns.results()).extracting(SuggestRow::id).doesNotContain("CVE-2024-12345");
  }

  @Test
  public void bestMatch_promotesExactCveMatchFromCatalogHits() {
    GuideVulnerabilityDocument vuln = vuln("CVE-2024-12345", "Remote code execution");
    when(hdsClient.getWithMultimap(eq(GuideGlobalSearchResponse.class), eq("rest/search/global"), any(Multimap.class)))
        .thenReturn(new GuideGlobalSearchResponse(List.of((SearchResult) vuln), 1, 0, 6, null));

    SuggestResponse response = resource.suggest("CVE-2024-12345", "catalog");

    assertThat(response.bestMatch()).isNotNull();
    assertThat(response.bestMatch().type()).isEqualTo(SuggestItemType.VULNERABILITY);
    assertThat(response.bestMatch().id()).isEqualTo("CVE-2024-12345");
    assertThat(response.bestMatch().source()).isEqualTo(SearchSource.CATALOG);
  }

  @Test
  public void suggest_capsPerTypeAndTotal_underLocalSource() {
    for (int i = 0; i < 10; i++) {
      iqLocal.add(row(SuggestItemType.APPLICATION, "app-" + i, "App " + i, SearchSource.LOCAL));
    }

    SuggestResponse response = resource.suggest("alpha", null);

    assertThat(groupOf(response, SuggestItemType.APPLICATION).results())
        .hasSizeLessThanOrEqualTo(SuggestServiceImpl.DEFAULT_PER_TYPE_LIMIT);

    int totalRows = response.groups().stream().mapToInt(g -> g.results().size()).sum();
    if (response.bestMatch() != null) {
      totalRows += 1;
    }
    assertThat(totalRows).isLessThanOrEqualTo(SuggestServiceImpl.TOTAL_ROW_CAP);
  }

  private static SuggestGroup groupOf(final SuggestResponse response, final SuggestItemType type) {
    return response.groups()
        .stream()
        .filter(g -> g.type() == type)
        .findFirst()
        .orElseThrow(() -> new AssertionError("group missing: " + type));
  }

  private static SuggestRow row(
      final SuggestItemType type,
      final String id,
      final String title,
      final SearchSource source)
  {
    return new SuggestRow(id, type, source, title, "", null);
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

  /**
   * In-memory {@link GlobalSearchSuggestIqLocalClient} returning registered rows filtered by requested
   * types and capped per-type. Stands in for the Lucene-backed adapter so orchestration tests do not
   * depend on indexed data.
   */
  private static final class FakeIqLocal
      implements GlobalSearchSuggestIqLocalClient
  {
    private final List<SuggestRow> all = new ArrayList<>();

    void add(final SuggestRow row) {
      all.add(row);
    }

    @Override
    public List<SuggestRow> suggest(
        final String query,
        final List<SuggestItemType> types,
        final int perTypeLimit,
        final UserPrincipal principal)
    {
      if (principal == null) {
        return List.of();
      }
      List<SuggestRow> out = new ArrayList<>();
      for (SuggestItemType type : types) {
        int taken = 0;
        for (SuggestRow row : all) {
          if (row.type() == type && taken < perTypeLimit) {
            out.add(row);
            taken++;
          }
        }
      }
      return out;
    }
  }
}
