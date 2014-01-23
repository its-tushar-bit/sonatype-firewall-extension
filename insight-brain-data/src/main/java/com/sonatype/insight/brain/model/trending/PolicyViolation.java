/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * Policy violation information
 * 
 * @since 1.7
 */
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

  /**
   * Returns policy name
   * 
   * @since
   */
  public String getName() {
    return name;
  }

  /**
   * Returns policy violation category
   * 
   * @see com.sonatype.insight.brain.trending.TrendingReportProcessor#CATEGORIES
   * @since 1.7
   */
  public String getCategory() {
    return category;
  }

  /**
   * Returns policy violation threat level
   * 
   * @since 1.7
   */
  public int getThreat() {
    return threat;
  }

  /**
   * Returns number of policy violations at the end of each report sub-periods
   * 
   * @since 1.7
   */
  public int[] getViolations() {
    return violations;
  }
}