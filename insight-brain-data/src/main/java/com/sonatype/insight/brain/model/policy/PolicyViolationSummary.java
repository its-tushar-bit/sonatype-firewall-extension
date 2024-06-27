/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public class PolicyViolationSummary
{
  private int criticalCount;

  private int severeCount;

  private int moderateCount;

  public PolicyViolationSummary(final Long criticalCount, final Long severeCount, final Long moderateCount) {
    if (criticalCount != null) {
      this.criticalCount = criticalCount.intValue();
    }
    if (severeCount != null) {
      this.severeCount = severeCount.intValue();
    }
    if (moderateCount != null) {
      this.moderateCount = moderateCount.intValue();
    }
  }

  public PolicyViolationSummary() {
  }

  public int getCriticalCount() {
    return criticalCount;
  }

  public void setCriticalCount(final int criticalCount) {
    this.criticalCount = criticalCount;
  }

  public int getSevereCount() {
    return severeCount;
  }

  public void setSevereCount(final int severeCount) {
    this.severeCount = severeCount;
  }

  public int getModerateCount() {
    return moderateCount;
  }

  public void setModerateCount(final int moderateCount) {
    this.moderateCount = moderateCount;
  }

  public int getAffectedComponentCount() {
    return criticalCount + severeCount + moderateCount;
  }
}
