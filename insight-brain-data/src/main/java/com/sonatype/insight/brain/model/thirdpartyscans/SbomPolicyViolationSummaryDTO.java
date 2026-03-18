/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

public class SbomPolicyViolationSummaryDTO
{
  private int low;

  private int moderate;

  private int severe;

  private int critical;

  public SbomPolicyViolationSummaryDTO() {
    // for Jackson
  }

  public SbomPolicyViolationSummaryDTO(Object[] resultantObject) {
    critical = ((Long) resultantObject[1]).intValue();
    severe = ((Long) (resultantObject[2])).intValue();
    moderate = ((Long) (resultantObject[3])).intValue();
    low = ((Long) (resultantObject[4])).intValue();
  }

  public int getLow() {
    return low;
  }

  public int getModerate() {
    return moderate;
  }

  public int getSevere() {
    return severe;
  }

  public int getCritical() {
    return critical;
  }

  public void setLow(final int low) {
    this.low = low;
  }

  public void setModerate(final int moderate) {
    this.moderate = moderate;
  }

  public void setSevere(final int severe) {
    this.severe = severe;
  }

  public void setCritical(final int critical) {
    this.critical = critical;
  }
}
