/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class DiffData
{
  private String threat;
  private int violations;
  private int previousViolations;

  public DiffData(String threat, int violations, int previousViolations) {
    this.threat = threat;
    this.violations = violations;
    this.previousViolations = previousViolations;
  }

  public String getThreat() {
    return threat;
  }

  public int getViolations() {
    return violations;
  }

  public int getPreviousViolations() {
    return previousViolations;
  }
}
