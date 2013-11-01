/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * @since 1.7
 */
public class DiffData
{
  private String threat;
  private int violations;
  private int previousViolations;

  public DiffData() {
  }

  public DiffData(String threat, int violations, int previousViolations) {
    this.threat = threat;
    this.violations = violations;
    this.previousViolations = previousViolations;
  }

  /**
   * Returns threat severity
   * 
   * @see com.sonatype.insight.brain.trending.TrendingReportProcessor#THREAT_LEVELS
   * @since 1.7
   */
  public String getThreat() {
    return threat;
  }

  /**
   * Returns number of policy violations with this severity at the end of reporting period.
   * 
   * @since 1.7
   */
  public int getViolations() {
    return violations;
  }

  /**
   * Returns number of policy violations with this severity at the beginning of reporting period.
   * 
   * @since 1.7
   */
  public int getPreviousViolations() {
    return previousViolations;
  }
}
