/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * @since 1.7
 */
public class TrendingReportMetadata
{
  private long generatedOn;
  private long periodStart;
  private long periodEnd;
  private boolean canRegenerate;
  private boolean regenerating;

  public TrendingReportMetadata() {
  }

  public TrendingReportMetadata(long generatedOn, long periodStart, long periodEnd) {
    this.generatedOn = generatedOn;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }

  /**
   * Returns report generation time, as number of milliseconds since epoch.
   * 
   * @since 1.7
   */
  public long getGeneratedOn() {
    return generatedOn;
  }

  /**
   * Returns beginning of reporting period, as number of milliseconds since epoch.
   * 
   * @since 1.7
   */
  public long getPeriodStart() {
    return periodStart;
  }

  /**
   * Returns end of reporting period, as number of milliseconds since epoch.
   * 
   * @since 1.7
   */
  public long getPeriodEnd() {
    return periodEnd;
  }

  public void setCanRegenerate(boolean value) {
    this.canRegenerate = value;
  }

  /**
   * Returns {@code true} if this report can be regenerated.
   */
  public boolean getCanRegenerate() {
    return canRegenerate;
  }

  public void setRegenerating(boolean value) {
    this.regenerating = value;
  }

  public boolean getRegenerating() {
    return regenerating;
  }
}