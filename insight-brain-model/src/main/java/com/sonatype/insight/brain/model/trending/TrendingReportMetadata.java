/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class TrendingReportMetadata
{
  private String generatedBy;
  private String generatedFor;
  private long generatedOn;
  private long periodStart;
  private long periodEnd;

  public TrendingReportMetadata() {
  }

  public TrendingReportMetadata(String generatedBy, String generatedFor, long generatedOn, long periodStart, long periodEnd) {
    this.generatedBy = generatedBy;
    this.generatedFor = generatedFor;
    this.generatedOn = generatedOn;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }

  public String getGeneratedBy() {
    return generatedBy;
  }

  public void setGeneratedBy(String generatedBy) {
    this.generatedBy = generatedBy;
  }

  public String getGeneratedFor() {
    return generatedFor;
  }

  public void setGeneratedFor(String generatedFor) {
    this.generatedFor = generatedFor;
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