/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.search.index.ItemType;

public enum CatalogEntityType
{
  COMPONENT(Tab.COMPONENT, Set.of(ItemType.NON_VULNERABLE_COMPONENT)),
  VULNERABILITY(Tab.VULNERABILITY, Set.of(ItemType.SECURITY_VULNERABILITY));

  private final Tab tab;

  private final Set<ItemType> localItemTypes;

  CatalogEntityType(final Tab tab, final Set<ItemType> localItemTypes) {
    this.tab = tab;
    this.localItemTypes = localItemTypes;
  }

  public Tab tab() {
    return tab;
  }

  public Set<ItemType> localItemTypes() {
    return localItemTypes;
  }

  public static CatalogEntityType fromWireValue(final String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("entityType must not be blank");
    }
    try {
      return valueOf(raw.strip().toUpperCase(Locale.ROOT));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("unknown entityType");
    }
  }
}
