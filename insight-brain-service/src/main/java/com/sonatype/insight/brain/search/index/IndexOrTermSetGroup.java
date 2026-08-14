/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.List;

/**
 * One FILTER clause that matches if any alternative {@link IndexTermSetRestriction} matches
 * (Classic dashboard org∪app union). CLM-44783.
 */
public record IndexOrTermSetGroup(List<IndexTermSetRestriction> alternatives)
    implements IndexFilterRestriction
{
  public IndexOrTermSetGroup {
    if (alternatives == null || alternatives.isEmpty()) {
      throw new IllegalArgumentException("alternatives must be non-empty");
    }
    alternatives = List.copyOf(alternatives);
  }

  public static IndexOrTermSetGroup of(final IndexTermSetRestriction... alternatives) {
    return new IndexOrTermSetGroup(List.of(alternatives));
  }

  public static List<IndexFilterRestriction> singleton(final IndexTermSetRestriction... alternatives) {
    return List.of(of(alternatives));
  }
}
