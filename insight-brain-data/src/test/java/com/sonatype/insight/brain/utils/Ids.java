/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.Collection;
import java.util.Set;

import com.sonatype.insight.model.HasStringId;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

public final class Ids
{
  private Ids() {
  }

  public static <T extends HasStringId> Set<String> extractIds(final Collection<T> entities) {
    return entities.stream()
        .map(HasStringId::getId)
        .collect(toImmutableSet());
  }
}
