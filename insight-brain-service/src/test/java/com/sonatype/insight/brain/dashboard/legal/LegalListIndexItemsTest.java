/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegalListIndexItemsTest
{
  @Test
  public void compositeId_includesLicenseThreatGroupSoSiblingLtgsDoNotCollapse() {
    SearchResultItemDTO copyleft = legalItem("GPL-3.0", "Copyleft");
    SearchResultItemDTO weak = legalItem("GPL-3.0", "Weak Copyleft");

    SearchResultDTO result = searchResult(copyleft, weak);
    LinkedHashMap<String, SearchResultItemDTO> items = LegalListIndexItems.extractLegalItems(result);

    assertThat(items).hasSize(2);
    assertThat(items.keySet()).containsExactly(
        "app-1|hash-1|GPL-3.0|Copyleft|build",
        "app-1|hash-1|GPL-3.0|Weak Copyleft|build");
  }

  @Test
  public void compositeId_rejectsIncompleteIdentity() {
    SearchResultItemDTO incomplete = legalItem("Apache-2.0", "Permissive");
    incomplete.componentHash = null;

    assertThat(LegalListIndexItems.compositeLegalFindingId(incomplete)).isNull();
    assertThat(LegalListIndexItems.extractLegalItems(searchResult(incomplete))).isEmpty();
  }

  private static SearchResultItemDTO legalItem(final String licenseId, final String ltgName) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.LEGAL_VIOLATION.name();
    item.applicationId = "app-1";
    item.componentHash = "hash-1";
    item.componentEffectiveLicenseId = licenseId;
    item.componentLicenseThreatGroupName = ltgName;
    item.policyEvaluationStage = "build";
    return item;
  }

  private static SearchResultDTO searchResult(final SearchResultItemDTO... items) {
    SearchResultDTO result = new SearchResultDTO();
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = new ArrayList<>(List.of(items));
    result.groupingByDTOS = List.of(group);
    return result;
  }
}
