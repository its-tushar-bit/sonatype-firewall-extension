/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;

/**
 * One per-entity-type section of the suggest response. All rows in a group share the same
 * {@code type} and {@code source}. {@code results} is never {@code null} — an empty list is
 * returned when nothing matched.
 */
public record SuggestGroup(
    SuggestItemType type,
    SearchSource source,
    List<SuggestRow> results)
{
  public SuggestGroup {
    results = results == null ? List.of() : List.copyOf(results);
  }
}
