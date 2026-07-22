/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.LinkedHashMap;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Extracts estate-distinct SECURITY_VULNERABILITY hits keyed by {@code vulnerabilityId}.
 */
final class VulnerabilitiesListIndexItems
{
  private VulnerabilitiesListIndexItems() {
  }

  static void mergeDistinctVulnerabilityItems(
      final SearchResultDTO searchResult,
      final LinkedHashMap<String, SearchResultItemDTO> distinctByVulnerabilityId)
  {
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (!ItemType.SECURITY_VULNERABILITY.name().equals(item.itemType)
            || StringUtils.isBlank(item.vulnerabilityId))
        {
          continue;
        }
        distinctByVulnerabilityId.putIfAbsent(item.vulnerabilityId, item);
      }
    }
  }
}
