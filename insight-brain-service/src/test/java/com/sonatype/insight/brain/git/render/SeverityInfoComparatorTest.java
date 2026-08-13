/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Comparator;

import com.sonatype.insight.brain.git.render.model.SeverityInfo;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.git.render.SecurityIssueComparator.CVSS_SCORE_COMPARATOR;
import static com.sonatype.insight.brain.git.render.model.MDImages.DIRECT_DEP_LOGO;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SeverityInfoComparatorTest
{
  private static final Comparator<SeverityInfo> UNDER_TEST = CVSS_SCORE_COMPARATOR;

  @Test
  public void testCompare_eq() {
    runCompareTest(1.1f, 1.1f, 0);
  }

  @Test
  public void testCompare_lt() {
    runCompareTest(1.1f, 3.1f, -1);
  }

  @Test
  public void testCompare_gt() {
    runCompareTest(3.1f, 1.1f, 1);
  }

  @Test
  public void testCompare_nullLeft() {
    runCompareTest(null, 1.1f, -1);
  }

  @Test
  public void testCompare_nullRight() {
    runCompareTest(1.1f, null, 1);
  }

  @Test
  public void testCompare_bothNull() {
    runCompareTest(null, null, 0);
  }

  private static void runCompareTest(final Float left, final Float right, final int expected) {
    final int actual = UNDER_TEST.compare(generateSeverityInfo(left), generateSeverityInfo(right));
    assertThat(actual).isEqualTo(expected);
  }

  private static SeverityInfo generateSeverityInfo(final Float score) {
    return new SeverityInfo("", score, DIRECT_DEP_LOGO);
  }
}
