/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class TrendingReportMetadata
{
  private long generatedOn;
  private long periodStart;
  private long periodEnd;

  public TrendingReportMetadata() {
  }

  public TrendingReportMetadata(long generatedOn, long periodStart, long periodEnd) {
    this.generatedOn = generatedOn;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }

  public long getGeneratedOn() {
    return generatedOn;
  }

  public void setGeneratedOn(long generatedOn) {
    this.generatedOn = generatedOn;
  }

  public long getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(long periodStart) {
    this.periodStart = periodStart;
  }

  public long getPeriodEnd() {
    return periodEnd;
  }

  public void setPeriodEnd(long periodEnd) {
    this.periodEnd = periodEnd;
  }
}