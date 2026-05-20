/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import java.time.LocalDate;

public class ConsumptionMonthlyBreakdown
{
  private final LocalDate billingMonth;

  private final String groupKey;

  private final long componentCount;

  public ConsumptionMonthlyBreakdown(LocalDate billingMonth, String groupKey, long componentCount) {
    this.billingMonth = billingMonth;
    this.groupKey = groupKey;
    this.componentCount = componentCount;
  }

  public LocalDate getBillingMonth() {
    return billingMonth;
  }

  public String getGroupKey() {
    return groupKey;
  }

  public long getComponentCount() {
    return componentCount;
  }
}
