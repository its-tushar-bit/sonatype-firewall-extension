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
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private VulnerabilitiesListIndexQueryBuilder indexQueryBuilder() {
    return new VulnerabilitiesListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration));
  }

  private VulnerabilitiesListService service() {
    return new VulnerabilitiesListService(
        searchIndexClient,
        indexQueryBuilder(),
        requestValidator,
        catalogListService,
        scopeFacetsBuilder);
  }

  @Test
  public void mergeAffectedApplications_dedupesByPublicIdAndSkipsBlank() {
    SearchResultItemDTO first = item("app-a", "Alpha");
    SearchResultItemDTO duplicate = item("app-a", "Alpha Dup");
    SearchResultItemDTO second = item("app-b", "Beta");
    SearchResultItemDTO noPublicId = item(null, "Ghost");

    LinkedHashMap<String, VulnerabilityAffectedApplicationDTO> byPublicId = new LinkedHashMap<>();
    VulnerabilitiesListService.mergeAffectedApplications(resultOf(first, duplicate, second, noPublicId), byPublicId);

    assertThat(byPublicId).hasSize(2);
    assertThat(byPublicId.get("app-a").applicationName).isEqualTo("Alpha");
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

    VulnerabilityAffectedApplicationsResponseDTO response = service().listAffectedApplications("CVE-2021-44228");

    assertThat(response.total).isEqualTo(FULL_PAGE);
    assertThat(response.truncated)
        .as("scan stopped on the page budget with matches unread, so the list is not complete")
        .isTrue();
  }

  @Test
  public void listAffectedApplications_indexExhaustedBeforeCaps_reportsComplete() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(item("app-a", "Alpha"), item("app-b", "Beta")));

    VulnerabilityAffectedApplicationsResponseDTO response = service().listAffectedApplications("CVE-2021-44228");

    assertThat(response.total).isEqualTo(2);
    assertThat(response.truncated).isFalse();
  }

  @Test
  public void listAffectedApplications_indexLookupFails_reportsTruncatedRatherThanAffectsNothing() {
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(null);

    VulnerabilityAffectedApplicationsResponseDTO response = service().listAffectedApplications("CVE-2021-44228");

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

    VulnerabilityAffectedApplicationsResponseDTO response = service().listAffectedApplications("CVE-2021-44228");

    assertThat(response.total).isEqualTo(VulnerabilitiesListService.MAX_AFFECTED_APPLICATIONS);
    assertThat(response.truncated).isTrue();
  }

  /** A full page of distinct applications, unique per {@code page} so pages accumulate. */
  private static SearchResultDTO fullPageOf(final int page) {
    List<SearchResultItemDTO> items = new ArrayList<>(FULL_PAGE);
    for (int i = 0; i < FULL_PAGE; i++) {
      String publicId = "app-" + page + "-" + i;
      items.add(item(publicId, "App " + publicId));
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

  private static SearchResultItemDTO item(final String publicId, final String name) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.applicationPublicId = publicId;
    item.applicationName = name;
    return item;
  }
}
