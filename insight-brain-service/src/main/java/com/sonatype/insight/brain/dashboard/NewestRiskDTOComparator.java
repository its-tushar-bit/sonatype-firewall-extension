/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

/**
 * Sorts the NewestRiskDTOs by threat level, time, policy, application, using the component hash as tie breaker where
 * needed to provide a stable ordering for later capping results from the top.
 */
class NewestRiskDTOComparator
    implements Comparator<NewestRiskDTO>
{
  public static final NewestRiskDTOComparator INSTANCE = new NewestRiskDTOComparator();

  @Override
  public int compare(NewestRiskDTO o1, NewestRiskDTO o2) {
    // Descending by threat level
    int rel = Integer.compare(o2.threatLevel, o1.threatLevel);
    if (rel != 0) {
      return rel;
    }

    // Descending by time
    rel = Long.compare(o2.time, o1.time);
    if (rel != 0) {
      return rel;
    }

    // Ascending by policy name
    rel = String.CASE_INSENSITIVE_ORDER.compare(o1.policyName, o2.policyName);
    if (rel != 0) {
      return rel;
    }

    // Ascending by app name
    rel = String.CASE_INSENSITIVE_ORDER.compare(o1.applicationName, o2.applicationName);
    if (rel != 0) {
      return rel;
    }

    // Ascending by hash, null is greater than any string
    if (o1.hash == null) {
      return (o2.hash == null) ? 0 : 1;
    }
    else if (o2.hash == null) {
      return -1;
    }
    return o1.hash.compareTo(o2.hash);
  }
}
