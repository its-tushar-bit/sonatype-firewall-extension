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

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
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
public class VulnerabilitiesListServiceAffectedApplicationsTest
{
  /** Matches {@code VulnerabilitiesListService.INDEX_FETCH_PAGE_SIZE}; a short page ends the scan. */
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
  private ConversionHelper conversionHelper;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private VulnerabilitiesListIndexQueryBuilder indexQueryBuilder() {
    return new VulnerabilitiesListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(configuration));
  }

  private VulnerabilitiesListService service() {
    return new VulnerabilitiesListService(
        searchIndexClient,
        indexQueryBuilder(),
        requestValidator,
        catalogListService,
        scopeFacetsBuilder,
        conversionHelper,
        configuration);
  }

  @Test
  public void mergeAffectedApplications_dedupesByPublicIdAndSkipsBlank() {
    SearchResultItemDTO first = item("app-a", "Alpha", "Org A");
    SearchResultItemDTO duplicate = item("app-a", "Alpha Dup", "Org A");
    SearchResultItemDTO second = item("app-b", "Beta", null);
    SearchResultItemDTO noPublicId = item(null, "Ghost", null);

    LinkedHashMap<String, VulnerabilityAffectedApplicationDTO> byPublicId = new LinkedHashMap<>();
    VulnerabilitiesListService.mergeAffectedApplications(resultOf(first, duplicate, second, noPublicId), byPublicId);

    assertThat(byPublicId).hasSize(2);
    assertThat(byPublicId.get("app-a").applicationName).isEqualTo("Alpha");
    assertThat(byPublicId.get("app-a").organizationName).isEqualTo("Org A");
    assertThat(byPublicId.get("app-b").applicationName).isEqualTo("Beta");
  }

  @Test
  public void buildAffectedApplicationsQuery_escapesVulnerabilityId() {
    String query = indexQueryBuilder().buildAffectedApplicationsQuery("CVE-2021-44228");
    assertThat(query).isEqualTo("itemType:SECURITY_VULNERABILITY AND vulnerabilityId:CVE\\-2021\\-44228");
  }

  @Test
  public void listAffectedApplications_pageBudgetExhausted_reportsTruncatedRatherThanComplete() {
    // Every page is full but re-reports the same applications, so neither the distinct-app cap nor
    // a short page ever stops the scan — only the page budget does.
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> fullPageOf(0));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", null, null);

    assertThat(response.total).isEqualTo(FULL_PAGE);
    assertThat(response.truncated)
        .as("scan stopped on the page budget with matches unread, so the list is not complete")
        .isTrue();
  }

  @Test
  public void listAffectedApplications_indexExhaustedBeforeCaps_reportsComplete() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(item("app-a", "Alpha", null), item("app-b", "Beta", null)));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", null, null);

    assertThat(response.total).isEqualTo(2);
    assertThat(response.truncated).isFalse();
    assertThat(response.page).isZero();
    assertThat(response.pageSize).isEqualTo(2);
    assertThat(response.hasNextPage).isFalse();
  }

  @Test
  public void listAffectedApplications_omittedPaging_returnsFullCollectedList() {
    List<SearchResultItemDTO> items = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      items.add(item("app-" + i, "App " + i, null));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(items.toArray(new SearchResultItemDTO[0])));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", null, null);

    assertThat(response.total).isEqualTo(40);
    assertThat(response.applications).hasSize(40);
    assertThat(response.pageSize).isEqualTo(40);
    assertThat(response.hasNextPage).isFalse();
  }

  @Test
  public void listAffectedApplications_explicitPaging_returnsFirstPageOnly() {
    List<SearchResultItemDTO> items = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      items.add(item("app-" + i, "App " + i, null));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(items.toArray(new SearchResultItemDTO[0])));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", 0, VulnerabilitiesListService.DEFAULT_PAGE_SIZE);

    assertThat(response.total).isEqualTo(40);
    assertThat(response.applications).hasSize(VulnerabilitiesListService.DEFAULT_PAGE_SIZE);
    assertThat(response.hasNextPage).isTrue();
  }

  @Test
  public void listAffectedApplications_pageSlice_returnsRequestedPage() {
    List<SearchResultItemDTO> items = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      items.add(item("app-" + String.format("%02d", i), "App " + String.format("%02d", i), null));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(items.toArray(new SearchResultItemDTO[0])));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", 1, 10);

    assertThat(response.page).isEqualTo(1);
    assertThat(response.pageSize).isEqualTo(10);
    assertThat(response.applications).hasSize(10);
    assertThat(response.applications.get(0).applicationPublicId).isEqualTo("app-10");
    assertThat(response.hasNextPage).isTrue();
  }

  @Test
  public void listAffectedApplications_organizationName_fromIndexHit() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(item("app-a", "Alpha", "Finance")));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", null, null);

    assertThat(response.applications).hasSize(1);
    assertThat(response.applications.get(0).organizationName).isEqualTo("Finance");
  }

  @Test
  public void listAffectedApplications_blankVulnerabilityId_returns400() {
    assertThatThrownBy(() -> service().listAffectedApplications("  ", null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("vulnerabilityId is required");
  }

  @Test
  public void listAffectedApplications_indexLookupFails_reportsTruncatedRatherThanAffectsNothing() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(null);

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", null, null);

    assertThat(response.total).isZero();
    assertThat(response.truncated)
        .as("a failed index lookup must not render as a complete, empty result")
        .isTrue();
  }

  @Test
  public void listAffectedApplications_distinctAppCapReached_reportsTruncatedAtCap() {
    AtomicInteger page = new AtomicInteger();
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenAnswer(invocation -> fullPageOf(page.getAndIncrement()));

    VulnerabilityAffectedApplicationsResponseDTO response =
        service().listAffectedApplications("CVE-2021-44228", 0, 100);

    assertThat(response.total).isEqualTo(VulnerabilitiesListService.MAX_AFFECTED_APPLICATIONS);
    assertThat(response.truncated).isTrue();
  }

  /** A full page of distinct applications, unique per {@code page} so pages accumulate. */
  private static SearchResultDTO fullPageOf(final int page) {
    List<SearchResultItemDTO> items = new ArrayList<>(FULL_PAGE);
    for (int i = 0; i < FULL_PAGE; i++) {
      String publicId = "app-" + page + "-" + i;
      items.add(item(publicId, "App " + publicId, null));
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
      final String publicId,
      final String name,
      final String organizationName)
  {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.applicationPublicId = publicId;
    item.applicationName = name;
    item.organizationName = organizationName;
    return item;
  }
}
