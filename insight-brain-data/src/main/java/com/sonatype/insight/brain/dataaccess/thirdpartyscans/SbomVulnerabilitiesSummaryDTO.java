/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

public class SbomVulnerabilitiesSummaryDTO
{
  private long none;

  private long low;

  private long medium;

  private long high;

  private long critical;

  public long getNone() {
    return none;
  }

  public long getLow() {
    return low;
  }

  public long getMedium() {
    return medium;
  }

  public long getHigh() {
    return high;
  }

  public long getCritical() {
    return critical;
  }

  public void setNone(final long none) {
    this.none = none;
  }

  public void setLow(final long low) {
    this.low = low;
  }

  public void setMedium(final long medium) {
    this.medium = medium;
  }

  public void setHigh(final long high) {
    this.high = high;
  }

  public void setCritical(final long critical) {
    this.critical = critical;
  }
}
