/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.guide.core.GuideLicenseRevocationHandler;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.NotFoundException;

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
public class VulnerabilitiesCatalogListServiceTest
{
  @Mock
  private HdsClient hdsClient;

  @Mock
  private GuideLicenseRevocationHandler revocationHandler;

  private VulnerabilitiesCatalogListService underTest;

  @Before
  public void setUp() {
    underTest = new VulnerabilitiesCatalogListService(hdsClient, revocationHandler);
  }

  @Test
  public void listCatalog_mapsHitsAndQueryParams() {
    GuideVulnerabilityDocument doc = new GuideVulnerabilityDocument(
        "CVE-2024-1234",
        List.of(),
        "Example vuln",
        9.1,
        9.1,
        List.of(),
        List.of(),
        List.of("maven"),
        false,
        true,
        null,
        "NVD",
        Instant.parse("2024-01-15T00:00:00Z"),
        null);
    when(hdsClient.getWithMultimap(eq(GuideVulnerabilitySearchResponse.class),
        eq("rest/search/vulnerabilities"), any()))
            .thenReturn(new GuideVulnerabilitySearchResponse(
                List.of(doc),
                1L,
                0,
                25,
                Map.of("severities", Map.of("critical", 1L))));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.search = "log4j";
    request.severities = Set.of("critical");
    request.minCvssScore = 7.0f;
    request.maxCvssScore = 10.0f;
    request.ecosystems = Set.of("maven");
    request.orderBy = "-cvssScore";
    request.page = 0;
    request.pageSize = 25;

    VulnerabilitiesListResponseDTO body = underTest.listCatalog(request, 0, 25, true);

    assertThat(body.source).isEqualTo(VulnerabilitiesListResponseDTO.SOURCE_CATALOG);
    assertThat(body.total).isEqualTo(1);
    assertThat(body.vulnerabilities).hasSize(1);
    assertThat(body.vulnerabilities.get(0).vulnerabilityId).isEqualTo("CVE-2024-1234");
    assertThat(body.vulnerabilities.get(0).severity).isEqualTo("critical");
    assertThat(body.vulnerabilities.get(0).ecosystem).isEqualTo("maven");
    assertThat(body.facets.severities).containsEntry("critical", 1L);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Multimap<String, String>> paramsCaptor = ArgumentCaptor.forClass(Multimap.class);
    verify(hdsClient).getWithMultimap(eq(GuideVulnerabilitySearchResponse.class),
        eq("rest/search/vulnerabilities"), paramsCaptor.capture());
    Multimap<String, String> params = paramsCaptor.getValue();
    assertThat(params.get("query")).containsExactly("log4j");
    assertThat(params.get("sortField")).containsExactly("cvssSeverity");
    assertThat(params.get("sortOrder")).containsExactly("desc");
    assertThat(params.get("severities")).containsExactly("critical");
    assertThat(params.get("affectedEcosystems")).containsExactly("maven");
    assertThat(params.get("minCvss")).containsExactly("7.0");
    assertThat(params.get("maxCvss")).containsExactly("10.0");
  }

  @Test
  public void listCatalog_sendsCvssBoundsWithoutFloatWidening() {
    when(hdsClient.getWithMultimap(eq(GuideVulnerabilitySearchResponse.class),
        eq("rest/search/vulnerabilities"), any()))
            .thenReturn(new GuideVulnerabilitySearchResponse(List.of(), 0L, 0, 25, null));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.minCvssScore = 7.1f;
    request.maxCvssScore = 7.1f;

    underTest.listCatalog(request, 0, 25, false);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Multimap<String, String>> paramsCaptor = ArgumentCaptor.forClass(Multimap.class);
    verify(hdsClient).getWithMultimap(eq(GuideVulnerabilitySearchResponse.class),
        eq("rest/search/vulnerabilities"), paramsCaptor.capture());
    Multimap<String, String> params = paramsCaptor.getValue();
    assertThat(params.get("minCvss")).containsExactly("7.1");
    assertThat(params.get("maxCvss")).containsExactly("7.1");
  }

  @Test
  public void listCatalog_emptyOnHdsNotFound() {
    when(hdsClient.getWithMultimap(eq(GuideVulnerabilitySearchResponse.class),
        eq("rest/search/vulnerabilities"), any()))
            .thenThrow(new NotFoundException("Not Found"));

    VulnerabilitiesListResponseDTO body = underTest.listCatalog(new VulnerabilitiesListRequestDTO(), 0, 25, true);

    assertThat(body.source).isEqualTo(VulnerabilitiesListResponseDTO.SOURCE_CATALOG);
    assertThat(body.total).isZero();
    assertThat(body.vulnerabilities).isEmpty();
  }

  @Test
  public void pageOffset_usesLongMultiplySoLargePageDoesNotWrap() {
    assertThat(VulnerabilitiesCatalogListService.pageOffset(21_474_837, 100))
        .isEqualTo(2_147_483_700L);
    assertThat(VulnerabilitiesCatalogListService.pageOffset(Integer.MAX_VALUE, 100))
        .isGreaterThan(0L);
  }

  @Test
  public void hasNextPage_widensBeforeIncrementSoMaxIntPageDoesNotWrap() {
    assertThat(VulnerabilitiesCatalogListService.hasNextPage(Integer.MAX_VALUE, 100, Long.MAX_VALUE))
        .isTrue();
    assertThat(VulnerabilitiesCatalogListService.hasNextPage(0, 25, 25L)).isFalse();
    assertThat(VulnerabilitiesCatalogListService.hasNextPage(0, 25, 26L)).isTrue();
  }
}
