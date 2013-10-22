/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

import java.util.List;

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

  public int getTotal() {
    return total;
  }

  public List<ApplicationRiskSummary> getRisks() {
    return risks;
  }
}
