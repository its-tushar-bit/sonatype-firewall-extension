/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class PolicyViolation
{
  private String name;
  private String category;
  private int threat;
  private int[] violations;

  public PolicyViolation() {
  }

  public PolicyViolation(String name, String category, int threat, int[] violations) {
    this.name = name;
    this.category = category;
    this.threat = threat;
    this.violations = violations; // beware. encapsulation is broken for this
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public int getThreat() {
    return threat;
  }

  public int[] getViolations() {
    return violations;
  }
}