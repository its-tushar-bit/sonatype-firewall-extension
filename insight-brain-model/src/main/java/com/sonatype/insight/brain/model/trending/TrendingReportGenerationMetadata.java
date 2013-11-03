/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class TrendingReportGenerationMetadata
{
  private boolean enabled;
  private long runningTime;
  private int applicationsTotal;
  private int applicationsCurrent;

  public TrendingReportGenerationMetadata() {
  }

  public TrendingReportGenerationMetadata(boolean enabled, long runningTime, int applicationsTotal,
      int applicationsCurrent)
  {
    this.enabled = enabled;
    this.runningTime = runningTime;
    this.applicationsTotal = applicationsTotal;
    this.applicationsCurrent = applicationsCurrent;
  }

  /**
   * Returns {@code true} if trending report can be regenerated.
   */
  public boolean isEnabled() {
    return enabled;
  }

  public boolean isRunning() {
    return runningTime >= 0;
  }

  /**
   * Returns trending report generation running time in milliseconds. Returns {@code -1} if trending report
   * generation is not running.
   */
  public long getRunningTime() {
    return runningTime;
  }

  /**
   * Returns total number of applications to be processed during trending report generation.
   */
  public int getApplicationsTotal() {
    return applicationsTotal;
  }

  /**
   * Returns current number of applications processed during trending report generation.
   */
  public int getApplicationsCurrent() {
    return applicationsCurrent;
  }
}
