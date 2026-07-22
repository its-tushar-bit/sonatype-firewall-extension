/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.LinkedHashMap;
import java.util.List;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;

/**
 * Extracts POLICY_VIOLATION hits from index search results in stable encounter order.
 */
final class ViolationsListIndexItems
{
  private ViolationsListIndexItems() {
  }

  static LinkedHashMap<String, SearchResultItemDTO> extractViolationItems(final SearchResultDTO searchResult) {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return items;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        putIfViolation(items, item);
      }
    }
    return items;
  }

  static LinkedHashMap<String, SearchResultItemDTO> extractViolationItems(final List<Document> documents) {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    if (documents == null) {
      return items;
    }
    for (Document document : documents) {
      putIfViolation(items, new SearchResultItemDTO(document));
    }
    return items;
  }

  private static void putIfViolation(
      final LinkedHashMap<String, SearchResultItemDTO> items,
      final SearchResultItemDTO item)
  {
    if (!ItemType.POLICY_VIOLATION.name().equals(item.itemType) || StringUtils.isBlank(item.policyViolationId)) {
      return;
    }
    items.putIfAbsent(item.policyViolationId, item);
  }
}
