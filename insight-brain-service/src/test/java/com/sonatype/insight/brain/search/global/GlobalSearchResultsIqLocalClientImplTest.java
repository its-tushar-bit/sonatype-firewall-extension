/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalRow;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Item-type mapping for the local (My Scan Data) leg. The local source serves every tab from the IQ
 * index, including COMPONENT and VULNERABILITY, which map to the {@code NON_VULNERABLE_COMPONENT} and
 * {@code SECURITY_VULNERABILITY} item types respectively.
 */
public class GlobalSearchResultsIqLocalClientImplTest
{
  private IqLocalSearchService iqService;

  private GlobalSearchResultsIqLocalClientImpl client;

  @BeforeEach
  public void setUp() {
    iqService = mock(IqLocalSearchService.class);
    client = new GlobalSearchResultsIqLocalClientImpl(iqService);
    when(iqService.mintNextCursor(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any(), any()))
        .thenReturn(null);
  }

  private static SearchResultItemDTO componentDoc() {
    SearchResultItemDTO dto = new SearchResultItemDTO();
    dto.componentHash = "hash-1";
    dto.componentName = "org.example:widget:1.0.0";
    return dto;
  }

  private static SearchResultItemDTO vulnerabilityDoc() {
    SearchResultItemDTO dto = new SearchResultItemDTO();
    dto.vulnerabilityId = "CVE-2024-0001";
    dto.vulnerabilityDescription = "example";
    dto.vulnerabilityStatus = "OPEN";
    return dto;
  }

  private void stubSearch(SearchResultItemDTO doc) {
    when(iqService.search(any())).thenReturn(new IqLocalSearchResponse(
        List.of(new IqLocalRow(SearchSource.LOCAL.value(), doc)),
        1L, true, List.of(), GlobalSearchSortAllowlist.RELEVANCE, List.of(), "lucene"));
  }

  @Test
  public void componentTab_local_reachesSearchNative_mapsLocalComponentItemType() {
    stubSearch(componentDoc());
    ResultsRequest req =
        new ResultsRequest("widget", Tab.COMPONENT, 1, 25, null, null, SearchSource.LOCAL);

    Optional<SectionResult> result = client.searchNative(req);

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.rows()).hasSize(1);
    ResultRow row = section.rows().get(0);
    assertThat(row.getType()).isEqualTo(Tab.COMPONENT.name());
    assertThat(row.getSource()).isEqualTo(SearchSource.LOCAL.value());
    assertThat(row.getHref()).isNull();

    ArgumentCaptor<SearchInputs> captor = ArgumentCaptor.forClass(SearchInputs.class);
    org.mockito.Mockito.verify(iqService).search(captor.capture());
    assertThat(captor.getValue().itemTypes()).containsExactly(ItemType.NON_VULNERABLE_COMPONENT);
  }

  @Test
  public void vulnerabilityTab_local_reachesSearchNative_mapsLocalVulnerabilityItemType() {
    stubSearch(vulnerabilityDoc());
    ResultsRequest req =
        new ResultsRequest("cve", Tab.VULNERABILITY, 1, 25, null, null, SearchSource.LOCAL);

    Optional<SectionResult> result = client.searchNative(req);

    assertThat(result).isPresent();
    SectionResult section = result.get();
    assertThat(section.rows()).hasSize(1);
    ResultRow row = section.rows().get(0);
    assertThat(row.getType()).isEqualTo(Tab.VULNERABILITY.name());
    assertThat(row.getSource()).isEqualTo(SearchSource.LOCAL.value());
    assertThat(row.getHref()).isNull();

    ArgumentCaptor<SearchInputs> captor = ArgumentCaptor.forClass(SearchInputs.class);
    org.mockito.Mockito.verify(iqService).search(captor.capture());
    assertThat(captor.getValue().itemTypes()).containsExactly(ItemType.SECURITY_VULNERABILITY);
  }

  @Test
  public void allTab_returnsEmptyOptional_packerComposesSections() {
    ResultsRequest req = new ResultsRequest("q", Tab.ALL, 1, 25, null, null, SearchSource.LOCAL);
    assertThat(client.searchNative(req)).isEmpty();
  }

  @Test
  public void violationTab_mapsComponentAsTitleAndPolicyAsSubtitle() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.policyViolationId = "violation-1";
    doc.policyViolationPolicyName = "Architecture-Quality";
    doc.componentName = "log4j : log4j : 1.2.17";
    doc.applicationPublicId = "mock-app";
    doc.applicationName = "Mock App";
    doc.policyViolationThreatLevel = 1;
    stubSearch(doc);

    Optional<SectionResult> result = client.searchNative(
        new ResultsRequest("log4j", Tab.VIOLATION, 1, 25, null, null, SearchSource.LOCAL));

    assertThat(result).isPresent();
    assertThat(result.get().rows()).hasSize(1);
    ResultRow row = result.get().rows().get(0);
    assertThat(row.getId()).isEqualTo("violation-1");
    assertThat(row.getType()).isEqualTo(Tab.VIOLATION.name());
    assertThat(row.getTitle()).isEqualTo("log4j : log4j : 1.2.17");
    assertThat(row.getSubtitle()).isEqualTo("Architecture-Quality");
    assertThat(row.getFields()).containsEntry("componentName", "log4j : log4j : 1.2.17");
    assertThat(row.getFields()).containsEntry("policyName", "Architecture-Quality");
    assertThat(row.getFields()).containsEntry("applicationName", "Mock App");
    assertThat(row.getFields()).containsEntry("threatLevel", 1);
  }

  @Test
  public void violationTab_dropsRowWhenComponentNameBlank() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.policyViolationId = "violation-1";
    doc.policyViolationPolicyName = "Architecture-Quality";
    doc.componentName = "";
    stubSearch(doc);

    Optional<SectionResult> result = client.searchNative(
        new ResultsRequest("log4j", Tab.VIOLATION, 1, 25, null, null, SearchSource.LOCAL));

    assertThat(result).isPresent();
    assertThat(result.get().rows()).isEmpty();
  }

  @Test
  public void violationTab_dropsRowWhenPolicyNameBlank() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.policyViolationId = "violation-1";
    doc.policyViolationPolicyName = "  ";
    doc.componentName = "log4j : log4j : 1.2.17";
    stubSearch(doc);

    Optional<SectionResult> result = client.searchNative(
        new ResultsRequest("log4j", Tab.VIOLATION, 1, 25, null, null, SearchSource.LOCAL));

    assertThat(result).isPresent();
    assertThat(result.get().rows()).isEmpty();
  }
}
