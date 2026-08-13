/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VulnerabilitiesListServiceImpactedComponentsTest
{
  private static final int FULL_PAGE = 100;

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private VulnerabilitiesListRequestValidator requestValidator;

  @Mock
  private VulnerabilitiesCatalogListService catalogListService;

  @Mock
  private VulnerabilitiesListScopeFacetsBuilder scopeFacetsBuilder;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private VulnerabilitiesListService service() {
    return new VulnerabilitiesListService(
        searchIndexClient,
        new VulnerabilitiesListIndexQueryBuilder(
            new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration)),
        requestValidator,
        catalogListService,
        scopeFacetsBuilder,
        configuration);
  }

  @Test
  public void mergeImpactedComponents_dedupesByHashAndSkipsBlank() {
    SearchResultItemDTO first = item("hash-a", "Alpha", "maven");
    SearchResultItemDTO duplicate = item("hash-a", "Alpha Dup", "maven");
    SearchResultItemDTO second = item("hash-b", "Beta", "npm");
    SearchResultItemDTO noHash = item(null, "Ghost", null);

    LinkedHashMap<String, VulnerabilityImpactedComponentDTO> byHash = new LinkedHashMap<>();
    VulnerabilitiesListService.mergeImpactedComponents(resultOf(first, duplicate, second, noHash), byHash);

    assertThat(byHash).hasSize(2);
    assertThat(byHash.get("hash-a").componentName).isEqualTo("Alpha");
    assertThat(byHash.get("hash-a").ecosystem).isEqualTo("maven");
    assertThat(byHash.get("hash-b").ecosystem).isEqualTo("npm");
  }

  @Test
  public void listImpactedComponents_indexExhaustedBeforeCaps_reportsComplete() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(item("hash-a", "Alpha", "maven"), item("hash-b", "Beta", "npm")));

    VulnerabilityImpactedComponentsResponseDTO response =
        service().listImpactedComponents("CVE-2021-44228", null, null);

    assertThat(response.total).isEqualTo(2);
    assertThat(response.truncated).isFalse();
    assertThat(response.page).isZero();
    assertThat(response.pageSize).isEqualTo(2);
    assertThat(response.hasNextPage).isFalse();
    assertThat(response.components).hasSize(2);
  }

  @Test
  public void listImpactedComponents_pageSlice_returnsRequestedPage() {
    List<SearchResultItemDTO> items = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      items.add(item("hash-" + String.format("%02d", i), "Comp " + String.format("%02d", i), "maven"));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(items.toArray(new SearchResultItemDTO[0])));

    VulnerabilityImpactedComponentsResponseDTO response =
        service().listImpactedComponents("CVE-2021-44228", 1, 10);

    assertThat(response.page).isEqualTo(1);
    assertThat(response.pageSize).isEqualTo(10);
    assertThat(response.components).hasSize(10);
    assertThat(response.components.get(0).componentHash).isEqualTo("hash-10");
    assertThat(response.hasNextPage).isTrue();
  }

  @Test
  public void listImpactedComponents_pageBudgetExhausted_reportsTruncatedRatherThanComplete() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> fullPageOf(0));

    VulnerabilityImpactedComponentsResponseDTO response =
        service().listImpactedComponents("CVE-2021-44228", 0, 100);

    assertThat(response.total).isEqualTo(FULL_PAGE);
    assertThat(response.truncated).isTrue();
  }

  @Test
  public void listImpactedComponents_distinctComponentCapReached_reportsTruncatedAtCap() {
    AtomicInteger page = new AtomicInteger();
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> fullPageOf(page.getAndIncrement()));

    VulnerabilityImpactedComponentsResponseDTO response =
        service().listImpactedComponents("CVE-2021-44228", 0, 100);

    assertThat(response.total).isEqualTo(VulnerabilitiesListService.MAX_IMPACTED_COMPONENTS);
    assertThat(response.truncated).isTrue();
  }

  @Test
  public void listImpactedComponents_indexLookupFails_reportsTruncatedRatherThanEmptyComplete() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(null);

    VulnerabilityImpactedComponentsResponseDTO response =
        service().listImpactedComponents("CVE-2021-44228", null, null);

    assertThat(response.total).isZero();
    assertThat(response.truncated).isTrue();
  }

  @Test
  public void listImpactedComponents_blankVulnerabilityId_returns400() {
    assertThatThrownBy(() -> service().listImpactedComponents("", null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("vulnerabilityId is required");
  }

  private static SearchResultDTO fullPageOf(final int page) {
    List<SearchResultItemDTO> items = new ArrayList<>(FULL_PAGE);
    for (int i = 0; i < FULL_PAGE; i++) {
      String hash = "hash-" + page + "-" + i;
      items.add(item(hash, "Comp " + hash, "maven"));
    }
    return resultOf(items.toArray(new SearchResultItemDTO[0]));
  }

  private static SearchResultDTO resultOf(final SearchResultItemDTO... items) {
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = List.of(items);
    SearchResultDTO result = new SearchResultDTO();
    result.groupingByDTOS = List.of(group);
    return result;
  }

  private static SearchResultItemDTO item(
      final String hash,
      final String name,
      final String ecosystem)
  {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.componentHash = hash;
    item.componentName = name;
    if (ecosystem != null) {
      ApiComponentIdentifierDTOV2 identifier = new ApiComponentIdentifierDTOV2();
      identifier.setFormat(ecosystem);
      item.componentIdentifier = identifier;
    }
    return item;
  }
}
