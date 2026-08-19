/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Comparator;

import com.sonatype.insight.brain.git.render.model.SeverityInfo;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.comparators.NullComparator;

public class SecurityIssueComparator
    implements Comparator<SecurityIssue>
{
  public static final SecurityIssueComparator ASC = new SecurityIssueComparator(true);

  private static final Comparator<Float> FLOAT_NULL_COMPARATOR = new NullComparator<>(Float::compareTo, false);

  @VisibleForTesting
  static final Comparator<SeverityInfo> CVSS_SCORE_COMPARATOR =
      new NullComparator<>((a, b) -> FLOAT_NULL_COMPARATOR.compare(a.getCvssScore(), b.getCvssScore()), false);

  private final boolean isAscending;

  public SecurityIssueComparator(final boolean isAscending) {
    this.isAscending = isAscending;
  }

  @Override
  public int compare(final SecurityIssue o1, final SecurityIssue o2) {
    if (isAscending) {
      return descendingCompare(o2, o1);
    }
    return descendingCompare(o1, o2);
  }

  private int descendingCompare(final SecurityIssue o1, final SecurityIssue o2) {
    final int first = Integer.compare(o1.getThreatLevel(), o2.getThreatLevel());
    if (first == 0) {
      return CVSS_SCORE_COMPARATOR.compare(o1.getSeverityInfo(), o2.getSeverityInfo());
    }
    return first;
  }
}
