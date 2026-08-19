/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.ItemType;

public enum IndexQueryType
{
  APPLICATION(Tab.APPLICATION, Set.of(ItemType.APPLICATION)),
  // VIOLATION unions POLICY_VIOLATION + LEGAL_VIOLATION, mirroring the global-search VIOLATION tab.
  VIOLATION(Tab.VIOLATION, Set.of(ItemType.POLICY_VIOLATION, ItemType.LEGAL_VIOLATION)),
  POLICY(null, Set.of(ItemType.POLICY)),
  // WAIVER unions manual/auto waivers (POLICY_WAIVER docs) with waiver requests (POLICY_WAIVER_REQUEST
  // docs). The waiverStates filter selects across both: existing = POLICY_WAIVER, requested/rejected =
  // POLICY_WAIVER_REQUEST by status.
  WAIVER(Tab.WAIVER, Set.of(ItemType.POLICY_WAIVER, ItemType.POLICY_WAIVER_REQUEST));

  private final Tab tab;

  private final Set<ItemType> itemTypes;

  IndexQueryType(final Tab tab, final Set<ItemType> itemTypes) {
    this.tab = tab;
    this.itemTypes = itemTypes;
  }

  /** {@code null} for {@link #POLICY}, which has no global-search tab and sorts by relevance only. */
  public Tab tab() {
    return tab;
  }

  public Set<ItemType> itemTypes() {
    return itemTypes;
  }

  public static IndexQueryType fromWireValue(final String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("entityType must not be blank");
    }
    try {
      return valueOf(raw.strip().toUpperCase(Locale.ROOT));
    }
    catch (IllegalArgumentException e) {
      // Enumerate the valid values, never the caller-supplied raw input, so this reaches the 400 body clean.
      throw new IllegalArgumentException(
          "entityType must be one of: " + Arrays.stream(values()).map(Enum::name).collect(joining(", ")));
    }
  }
}
