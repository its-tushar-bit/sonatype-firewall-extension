/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.LinkedHashMap;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Extracts APPLICATION hits from index search results in stable encounter order.
 */
final class ApplicationsListIndexItems
{
  private ApplicationsListIndexItems() {
  }

  static LinkedHashMap<String, SearchResultItemDTO> extractApplicationItems(final SearchResultDTO searchResult) {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return items;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (!ItemType.APPLICATION.name().equals(item.itemType) || StringUtils.isBlank(item.applicationId)) {
          continue;
        }
        items.putIfAbsent(item.applicationId, item);
      }
    }
    return items;
  }
}
