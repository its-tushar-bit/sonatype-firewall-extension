/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Stream;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Extracts LEGAL_VIOLATION hits from index search results in stable encounter order.
 */
final class LegalListIndexItems
{
  private LegalListIndexItems() {
  }

  /**
   * Streams non-null {@code LEGAL_VIOLATION} items from a search result in encounter order.
   * Shared by list extraction and facet discovery so result-shape walks stay in one place.
   */
  static Stream<SearchResultItemDTO> legalHits(final SearchResultDTO searchResult) {
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return Stream.empty();
    }
    return searchResult.groupingByDTOS.stream()
        .filter(Objects::nonNull)
        .filter(group -> group.searchResultItemDTOS != null)
        .flatMap(group -> group.searchResultItemDTOS.stream())
        .filter(Objects::nonNull)
        .filter(item -> ItemType.LEGAL_VIOLATION.name().equals(item.itemType));
  }

  static LinkedHashMap<String, SearchResultItemDTO> extractLegalItems(final SearchResultDTO searchResult) {
    LinkedHashMap<String, SearchResultItemDTO> items = new LinkedHashMap<>();
    legalHits(searchResult).forEach(item -> putIfLegalFinding(items, item));
    return items;
  }

  private static void putIfLegalFinding(
      final LinkedHashMap<String, SearchResultItemDTO> items,
      final SearchResultItemDTO item)
  {
    String compositeId = compositeLegalFindingId(item);
    if (StringUtils.isBlank(compositeId)) {
      return;
    }
    items.putIfAbsent(compositeId, item);
  }

  /**
   * Stable row id for a LEGAL_VIOLATION doc. Includes license threat group because the index
   * emits one doc per (component, effective license, LTG) — omitting LTG collapsed siblings.
   */
  static String compositeLegalFindingId(final SearchResultItemDTO item) {
    if (item == null) {
      return null;
    }
    // Require the identity parts that define a distinct finding; blank LTG is allowed (unnamed).
    if (StringUtils.isBlank(item.applicationId)
        || StringUtils.isBlank(item.componentHash)
        || StringUtils.isBlank(item.componentEffectiveLicenseId)
        || StringUtils.isBlank(item.policyEvaluationStage))
    {
      return null;
    }
    return String.join(
        "|",
        item.applicationId,
        item.componentHash,
        item.componentEffectiveLicenseId,
        nullToEmpty(item.componentLicenseThreatGroupName),
        item.policyEvaluationStage);
  }

  private static String nullToEmpty(final String value) {
    return value == null ? "" : value;
  }
}
