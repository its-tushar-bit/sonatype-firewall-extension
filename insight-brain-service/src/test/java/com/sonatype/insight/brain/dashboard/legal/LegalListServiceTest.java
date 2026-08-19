/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LegalListServiceTest
{
  private static final String QUERY = "itemType:LEGAL_VIOLATION";

  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private LegalListIndexQueryBuilder indexQueryBuilder;

  @Mock
  private LegalListRequestValidator requestValidator;

  @Mock
  private LegalListFacetsBuilder facetsBuilder;

  private LegalListService service() {
    return new LegalListService(searchIndexClient, indexQueryBuilder, requestValidator, facetsBuilder);
  }

  @Test
  public void listLegalFindings_mapsLegalViolationHitToRowWithCompositeId() {
    when(indexQueryBuilder.buildLegalQuery(any())).thenReturn(QUERY);
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(),
        anyList()))
            .thenReturn(searchResult(legalItem()));

    LegalListRequestDTO request = new LegalListRequestDTO();
    request.includeFacets = false;

    LegalListResponseDTO response = service().listLegalFindings(request);

    assertThat(response.source).isEqualTo(LegalListResponseDTO.SOURCE_INDEX);
    assertThat(response.total).isEqualTo(1);
    assertThat(response.findings).hasSize(1);
    LegalRowDTO row = response.findings.get(0);
    assertThat(row.legalFindingId).isEqualTo("app-1|hash-1|Apache-2.0|Permissive|build");
    assertThat(row.threatLevel).isEqualTo(8);
    assertThat(row.severity).isEqualTo("critical");
    assertThat(row.licenseId).isEqualTo("Apache-2.0");
    assertThat(row.licenseName).isEqualTo("Apache License 2.0");
    assertThat(row.licenseThreatGroupName).isEqualTo("Permissive");
    assertThat(row.applicationId).isEqualTo("app-1");
    assertThat(row.componentHash).isEqualTo("hash-1");
    assertThat(row.stage).isEqualTo("build");
    assertThat(row.reportId).isEqualTo("report-1");
  }

  @Test
  public void descendingSort_ordersHighestThreatFirst_withNullsLast() {
    List<LegalRowDTO> rows = rowsWithThreat(3, null, 10, 8);

    rows.sort(LegalListService.comparator("-licenseThreatLevel"));

    assertThat(rows).extracting(row -> row.threatLevel).containsExactly(10, 8, 3, null);
  }

  @Test
  public void ascendingSort_ordersLowestThreatFirst_withNullsLast() {
    List<LegalRowDTO> rows = rowsWithThreat(3, null, 10, 8);

    rows.sort(LegalListService.comparator("licenseThreatLevel"));

    assertThat(rows).extracting(row -> row.threatLevel).containsExactly(3, 8, 10, null);
  }

  @Test
  public void listLegalFindings_rejectsPageAboveWalkableCeiling() {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.page = LegalListService.MAX_WALKABLE_PAGE + 1;
    request.includeFacets = false;

    assertThatThrownBy(() -> service().listLegalFindings(request))
        .isInstanceOf(com.sonatype.insight.error.exception.BadRequestException.class)
        .hasMessageContaining("Page must be <=");
  }

  @Test
  public void toSearchIndexPage_doesNotOverflowForWalkableCeiling() {
    assertThat(LegalListService.toSearchIndexPage(0)).isEqualTo(0);
    assertThat(LegalListService.toSearchIndexPage(LegalListService.MAX_WALKABLE_PAGE))
        .isEqualTo(LegalListService.MAX_WALKABLE_PAGE + 1);
  }

  private static SearchResultItemDTO legalItem() {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.LEGAL_VIOLATION.name();
    item.applicationId = "app-1";
    item.applicationPublicId = "app-public-1";
    item.applicationName = "App One";
    item.organizationId = "org-1";
    item.organizationName = "Org One";
    item.componentHash = "hash-1";
    item.componentEffectiveLicenseId = "Apache-2.0";
    item.componentEffectiveLicenseName = "Apache License 2.0";
    item.componentLicenseThreatGroupName = "Permissive";
    item.componentLicenseThreatLevel = 8;
    item.componentName = "log4j : 2.17.0";
    item.policyEvaluationStage = "build";
    item.reportId = "report-1";
    return item;
  }

  private static SearchResultDTO searchResult(final SearchResultItemDTO item) {
    SearchResultDTO result = new SearchResultDTO();
    result.totalNumberOfHits = 1;
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = new ArrayList<>(List.of(item));
    result.groupingByDTOS = List.of(group);
    return result;
  }

  private static List<LegalRowDTO> rowsWithThreat(final Integer... threatLevels) {
    List<LegalRowDTO> rows = new ArrayList<>();
    Arrays.stream(threatLevels).forEach(threat -> {
      LegalRowDTO row = new LegalRowDTO();
      row.threatLevel = threat;
      rows.add(row);
    });
    return rows;
  }
}
