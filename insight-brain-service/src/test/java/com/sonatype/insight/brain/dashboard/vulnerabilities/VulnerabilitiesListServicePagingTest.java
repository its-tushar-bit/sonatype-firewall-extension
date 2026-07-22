/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VulnerabilitiesListServicePagingTest
{
  @Test
  public void hasNextPage_falseWhenConsumedReachesMaterializedCapEvenIfEstateTotalLarger() {
    // Estate total 8000, only 5000 materialized; last full page at page 49 (pageSize 100).
    assertThat(VulnerabilitiesListService.hasNextPage(49, 100, 100, 8000, 5000)).isFalse();
    // Empty page past the materialization window must not keep paging.
    assertThat(VulnerabilitiesListService.hasNextPage(50, 100, 0, 8000, 5000)).isFalse();
  }

  @Test
  public void hasNextPage_trueWhileMoreMaterializedRowsRemain() {
    assertThat(VulnerabilitiesListService.hasNextPage(0, 100, 100, 250, 250)).isTrue();
    assertThat(VulnerabilitiesListService.hasNextPage(1, 100, 100, 250, 250)).isTrue();
    assertThat(VulnerabilitiesListService.hasNextPage(2, 100, 50, 250, 250)).isFalse();
  }

  @Test
  public void hasNextPage_usesLongArithmeticForLargePage() {
    assertThat(VulnerabilitiesListService.hasNextPage(21_474_837L, 100, 0, 8000, 5000)).isFalse();
  }

  @Test
  public void bucketSeverityFacets_countsDistinctRowsPerCvssBandWithoutIndexFanOut() {
    Map<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    distinct.put("CVE-1", itemWithScore(9.8f));
    distinct.put("CVE-2", itemWithScore(9.1f));
    distinct.put("CVE-3", itemWithScore(5.0f));
    distinct.put("CVE-4", itemWithScore(null));

    Map<String, Long> severities = VulnerabilitiesListService.bucketSeverityFacets(distinct);

    assertThat(severities.get("critical")).isEqualTo(2L);
    assertThat(severities.get("medium")).isEqualTo(1L);
    assertThat(severities.get("high")).isZero();
    assertThat(severities.get("low")).isZero();
    assertThat(severities.get("none")).isEqualTo(1L);
  }

  @Test
  public void bucketSeverityFacets_mapsOutOfBandScoresToNoneNotUnknown() {
    // 0.05 sits in the CVSS v3 gap between NONE (0.0) and LOW (0.1).
    Map<String, SearchResultItemDTO> distinct = Map.of("CVE-gap", itemWithScore(0.05f));

    Map<String, Long> severities = VulnerabilitiesListService.bucketSeverityFacets(distinct);

    assertThat(severities.get("none")).isEqualTo(1L);
    assertThat(severities).doesNotContainKey("unknown");
  }

  @Test
  public void bucketEcosystemFacets_countsDistinctFormats() {
    Map<String, SearchResultItemDTO> distinct = new LinkedHashMap<>();
    distinct.put("CVE-1", itemWithFormat("maven"));
    distinct.put("CVE-2", itemWithFormat("Maven"));
    distinct.put("CVE-3", itemWithFormat("npm"));
    distinct.put("CVE-4", itemWithFormat(null));

    Map<String, Long> ecosystems = VulnerabilitiesListService.bucketEcosystemFacets(distinct);

    assertThat(ecosystems).containsEntry("maven", 2L).containsEntry("npm", 1L);
    assertThat(ecosystems).doesNotContainKey(null);
  }

  private static SearchResultItemDTO itemWithScore(final Float score) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.vulnerabilitySeverity = score;
    return item;
  }

  private static SearchResultItemDTO itemWithFormat(final String format) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    if (format != null) {
      ApiComponentIdentifierDTOV2 identifier = new ApiComponentIdentifierDTOV2();
      identifier.setFormat(format);
      item.componentIdentifier = identifier;
    }
    return item;
  }
}
