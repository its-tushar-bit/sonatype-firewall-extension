/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

public class SbomVulnerabilitiesSummaryDTO
{
  private long vulnerabilityNone;

  private long vulnerabilityLow;

  private long vulnerabilityMedium;

  private long vulnerabilityHigh;

  private long vulnerabilityCritical;

  public long getVulnerabilityNone() {
    return vulnerabilityNone;
  }

  public long getVulnerabilityLow() {
    return vulnerabilityLow;
  }

  public long getVulnerabilityMedium() {
    return vulnerabilityMedium;
  }

  public long getVulnerabilityHigh() {
    return vulnerabilityHigh;
  }

  public long getVulnerabilityCritical() {
    return vulnerabilityCritical;
  }

  public void setVulnerabilityNone(final long vulnerabilityNone) {
    this.vulnerabilityNone = vulnerabilityNone;
  }

  public void setVulnerabilityLow(final long vulnerabilityLow) {
    this.vulnerabilityLow = vulnerabilityLow;
  }

  public void setVulnerabilityMedium(final long vulnerabilityMedium) {
    this.vulnerabilityMedium = vulnerabilityMedium;
  }

  public void setVulnerabilityHigh(final long vulnerabilityHigh) {
    this.vulnerabilityHigh = vulnerabilityHigh;
  }

  public void setVulnerabilityCritical(final long vulnerabilityCritical) {
    this.vulnerabilityCritical = vulnerabilityCritical;
  }
}
