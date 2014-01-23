/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

import java.util.List;

/**
 * @since 1.7
 */
public class Applications
{
  private int total;

  private List<ApplicationRiskSummary> risks;

  public Applications() {
  }

  public Applications(int total, List<ApplicationRiskSummary> risks) {
    this.total = total;
    this.risks = risks;
  }

  /**
   * Returns total number of applications.
   * 
   * @since 1.7
   */
  public int getTotal() {
    return total;
  }

  /**
   * Returns application summary for the applications with highest relative risk, application with highest risk first,
   * application with lowest risk last.
   * 
   * @since 1.7
   */
  public List<ApplicationRiskSummary> getRisks() {
    return risks;
  }
}
