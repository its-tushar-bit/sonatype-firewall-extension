/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.UUID;

import com.sonatype.insight.brain.git.render.model.SeverityInfo;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityIssueComparatorTest
{
  @Test
  public void testCompare_threatLevelGT() {
    runCompareTest(false, 10, 1.3f, 9, 6.7f, 1);
    runCompareTest(true, 10, 1.3f, 9, 6.7f, -1);
  }

  @Test
  public void testCompare_threatLevelLT() {
    runCompareTest(true, 9, 6.7f, 10, 1.4f, 1);
    runCompareTest(false, 9, 6.7f, 10, 1.4f, -1);
  }

  @Test
  public void testCompare_threatLevelEQ_cvssGT() {
    runCompareTest(true, 9, 6.7f, 9, 1.4f, -1);
    runCompareTest(false, 9, 6.7f, 9, 1.4f, 1);
  }

  @Test
  public void testCompare_threatLevelEQ_cvssLT() {
    runCompareTest(true, 9, 6.7f, 9, 7.4f, 1);
    runCompareTest(false, 9, 6.7f, 9, 7.4f, -1);
  }

  @Test
  public void testCompare_threatLevelEQ_cvssEQ() {
    runCompareTest(true, 9, 6.7f, 9, 6.7f, 0);
    runCompareTest(false, 9, 6.7f, 9, 6.7f, 0);
  }

  private void runCompareTest(
      final boolean isAscending,
      final int threatLevel1,
      final float cvssScore1,
      final int threatLevel2,
      final float cvssScore2,
      final int expected)
  {
    final SecurityIssueComparator underTest = new SecurityIssueComparator(isAscending);
    final SecurityIssue securityIssue1 = buildSecurityIssue(threatLevel1, cvssScore1);
    final SecurityIssue securityIssue2 = buildSecurityIssue(threatLevel2, cvssScore2);
    final int actual = underTest.compare(securityIssue1, securityIssue2);
    assertThat(actual).isEqualTo(expected);
  }

  private static SecurityIssue buildSecurityIssue(final int threatLevel, final float cvssScore) {
    final SeverityInfo severityInfo = new SeverityInfo("CVE-123-" + UUID.randomUUID(), cvssScore, null);
    return new SecurityIssue(threatLevel, severityInfo, "some description", "https://example.com/polvol/1");
  }
}
