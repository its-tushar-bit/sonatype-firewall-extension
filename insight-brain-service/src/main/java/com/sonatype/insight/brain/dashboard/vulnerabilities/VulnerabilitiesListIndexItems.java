/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;

/**
 * Extracts estate-distinct SECURITY_VULNERABILITY hits keyed by {@code vulnerabilityId}, and
 * accumulates the distinct applications seen for each vulnerability as a walk pages through hits.
 * List hydration takes only the distinct hits; the Impact tabs also take the accumulated applications.
 * <p>
 * Application identity uses {@code applicationPublicId} only — same key as the Impact affected-apps
 * endpoint — so card counts do not include hits that Impact would skip.
 */
final class VulnerabilitiesListIndexItems
{
  private VulnerabilitiesListIndexItems() {
  }

  /**
   * @return the vulnerability ids this result carried, in their document casing, so a caller paging
   *         towards a known set of ids can tell what the page covered without re-reading everything
   *         merged so far
   */
  static Set<String> mergeDistinctVulnerabilityItems(
      final SearchResultDTO searchResult,
      final LinkedHashMap<String, SearchResultItemDTO> distinctByVulnerabilityId,
      final Map<String, Set<String>> applicationIdsByVulnerabilityId)
  {
    Set<String> vulnerabilityIds = new HashSet<>();
    if (searchResult == null || searchResult.groupingByDTOS == null) {
      return vulnerabilityIds;
    }
    for (var group : searchResult.groupingByDTOS) {
      if (group == null || group.searchResultItemDTOS == null) {
        continue;
      }
      for (SearchResultItemDTO item : group.searchResultItemDTOS) {
        if (item == null
            || !ItemType.SECURITY_VULNERABILITY.name().equals(item.itemType)
            || StringUtils.isBlank(item.vulnerabilityId))
        {
          continue;
        }
        distinctByVulnerabilityId.putIfAbsent(item.vulnerabilityId, item);
        vulnerabilityIds.add(item.vulnerabilityId);
        String applicationKey = applicationKey(item);
        if (applicationKey != null && applicationIdsByVulnerabilityId != null) {
          applicationIdsByVulnerabilityId
              .computeIfAbsent(item.vulnerabilityId, ignored -> new HashSet<>())
              .add(applicationKey);
        }
      }
    }
    return vulnerabilityIds;
  }

  /** Impact/deep-link key only — blank public id is skipped (matches affected-apps merge). */
  static String applicationKey(final SearchResultItemDTO item) {
    if (item == null || StringUtils.isBlank(item.applicationPublicId)) {
      return null;
    }
    return item.applicationPublicId;
  }
}
