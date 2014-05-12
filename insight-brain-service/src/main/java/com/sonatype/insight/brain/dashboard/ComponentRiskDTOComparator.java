/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

/**
 * Sorts the component risk DTOs by descending score, using the score breakdown and component hash as tie breaker where
 * needed to provide a stable ordering for later capping results from the top.
 */
class ComponentRiskDTOComparator
    implements Comparator<ComponentRiskDTO>
{
  public static final ComponentRiskDTOComparator INSTANCE = new ComponentRiskDTOComparator();

  @Override
  public int compare(ComponentRiskDTO o1, ComponentRiskDTO o2) {
    int rel = o2.score - o1.score;
    if (rel != 0) {
      return rel;
    }

    rel = o2.scoreCritical - o1.scoreCritical;
    if (rel != 0) {
      return rel;
    }

    rel = o2.scoreSevere - o1.scoreSevere;
    if (rel != 0) {
      return rel;
    }

    rel = o2.scoreModerate - o1.scoreModerate;
    if (rel != 0) {
      return rel;
    }

    rel = o2.scoreLow - o1.scoreLow;
    if (rel != 0) {
      return rel;
    }

    if (o1.hash == null) {
      return (o2.hash == null) ? 0 : 1;
    }
    else if (o2.hash == null) {
      return -1;
    }
    return o1.hash.compareTo(o2.hash);
  }
}
