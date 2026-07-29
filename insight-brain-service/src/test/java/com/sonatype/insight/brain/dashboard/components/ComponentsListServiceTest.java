/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.ComponentRiskDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentsListServiceTest
{
  @Test
  public void toSearchIndexPage_mapsZeroBasedClientPagesToIndexContract() {
    assertThat(ComponentsListService.toSearchIndexPage(0)).isEqualTo(0);
    assertThat(ComponentsListService.toSearchIndexPage(1)).isEqualTo(2);
    assertThat(ComponentsListService.toSearchIndexPage(2)).isEqualTo(3);
  }

  @Test
  public void clampThreatLevel_mapsUnboundedMaxToTenNotNegativeShort() {
    assertThat(ApplicationComponentDAO.clampThreatLevel(Integer.MAX_VALUE)).isEqualTo((short) 10);
    assertThat(ApplicationComponentDAO.clampThreatLevel(Integer.MIN_VALUE)).isEqualTo((short) 0);
    assertThat(ApplicationComponentDAO.clampThreatLevel(8)).isEqualTo((short) 8);
  }

  @Test
  public void mergeIndexPageWithEnrichment_prefersSqlCardsAndKeepsIndexOrder() {
    LinkedHashMap<String, SearchResultItemDTO> pageItems = new LinkedHashMap<>();
    pageItems.put("hash-a", item("hash-a", "comp-a"));
    pageItems.put("hash-b", item("hash-b", "comp-b"));

    ComponentRiskDTO sqlA = new ComponentRiskDTO();
    sqlA.hash = "hash-a";
    sqlA.score = 42;
    sqlA.affectedApplications = 7;

    List<ComponentRiskDTO> merged = ComponentsListService.mergeIndexPageWithEnrichment(
        pageItems,
        Map.of("hash-a", Set.of("app-1"), "hash-b", Set.of("app-1", "app-2")),
        List.of(sqlA));

    assertThat(merged).hasSize(2);
    assertThat(merged.get(0).hash).isEqualTo("hash-a");
    assertThat(merged.get(0).score).isEqualTo(42);
    assertThat(merged.get(0).affectedApplications).isEqualTo(7);
    assertThat(merged.get(1).hash).isEqualTo("hash-b");
    // Index stubs leave scores null so NOUX cards omit 0/0/0/0 badge chrome.
    assertThat(merged.get(1).score).isNull();
    assertThat(merged.get(1).scoreCritical).isNull();
    assertThat(merged.get(1).scoreSevere).isNull();
    assertThat(merged.get(1).scoreModerate).isNull();
    assertThat(merged.get(1).scoreLow).isNull();
    assertThat(merged.get(1).affectedApplications).isEqualTo(2);
    assertThat(merged.get(1).derivedComponentName).isEqualTo("comp-b");
  }

  private static SearchResultItemDTO item(final String hash, final String name) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.componentHash = hash;
    item.componentName = name;
    item.itemType = "NON_VULNERABLE_COMPONENT";
    return item;
  }
}
