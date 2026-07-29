/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Distinct collapse + application accumulation for Wave B applicationCount (CLM-43210).
 */
public class VulnerabilitiesListIndexItemsTest
{
  @Test
  public void mergeDistinct_keepsFirstHitAndCountsDistinctPublicIds() {
    LinkedHashMap<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    Map<String, Set<String>> applicationIds = new HashMap<>();

    VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(
        result(
            vuln("CVE-1", "app-a", "public-a"),
            vuln("CVE-1", "app-b", "public-b"),
            vuln("CVE-1", "app-a-dup", "public-a"),
            vuln("CVE-2", "app-c", "public-c")),
        distinct,
        applicationIds);

    assertThat(distinct).containsOnlyKeys("CVE-1", "CVE-2");
    assertThat(distinct.get("CVE-1").applicationId).isEqualTo("app-a");
    assertThat(applicationIds.get("CVE-1")).containsExactlyInAnyOrder("public-a", "public-b");
    assertThat(applicationIds.get("CVE-2")).containsExactly("public-c");
  }

  @Test
  public void mergeDistinct_skipsHitsWithoutPublicId_matchingImpactTab() {
    LinkedHashMap<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    Map<String, Set<String>> applicationIds = new HashMap<>();

    VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(
        result(vuln("CVE-1", "app-internal", null), vuln("CVE-1", "app-internal", "  ")),
        distinct,
        applicationIds);

    assertThat(distinct).containsOnlyKeys("CVE-1");
    // Same as Impact merge: blank public id does not inflate applicationCount.
    assertThat(applicationIds).doesNotContainKey("CVE-1");
  }

  @Test
  public void mergeDistinct_ignoresBlankApplicationKeys() {
    LinkedHashMap<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    Map<String, Set<String>> applicationIds = new HashMap<>();

    VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(
        result(vuln("CVE-1", null, null), vuln("CVE-1", "  ", null)),
        distinct,
        applicationIds);

    assertThat(distinct).containsOnlyKeys("CVE-1");
    assertThat(applicationIds).doesNotContainKey("CVE-1");
  }

  @Test
  public void mergeDistinct_skipsNullGroupAndNullItem() {
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = null;
    SearchResultDTO dto = new SearchResultDTO();
    dto.groupingByDTOS = java.util.Arrays.asList(null, group);

    LinkedHashMap<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    Map<String, Set<String>> applicationIds = new HashMap<>();
    VulnerabilitiesListIndexItems.mergeDistinctVulnerabilityItems(dto, distinct, applicationIds);

    assertThat(distinct).isEmpty();
    assertThat(applicationIds).isEmpty();
  }

  private static SearchResultDTO result(final SearchResultItemDTO... items) {
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = List.of(items);
    SearchResultDTO dto = new SearchResultDTO();
    dto.groupingByDTOS = List.of(group);
    return dto;
  }

  private static SearchResultItemDTO vuln(
      final String vulnerabilityId,
      final String applicationId,
      final String applicationPublicId)
  {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.SECURITY_VULNERABILITY.name();
    item.vulnerabilityId = vulnerabilityId;
    item.applicationId = applicationId;
    item.applicationPublicId = applicationPublicId;
    return item;
  }
}
