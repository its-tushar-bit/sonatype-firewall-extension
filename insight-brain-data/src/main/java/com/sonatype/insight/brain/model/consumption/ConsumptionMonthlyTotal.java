/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import java.time.LocalDate;

/**
 * Represents aggregated monthly consumption totals. Used by DAO GROUP BY queries.
 *
 * @since 1.204
 */
public class ConsumptionMonthlyTotal
{
  private LocalDate billingMonth;

  private long totalConsumed;

  public ConsumptionMonthlyTotal() {
    // Default constructor for serialization/deserialization
  }

  public ConsumptionMonthlyTotal(LocalDate billingMonth, long totalConsumed) {
    this.billingMonth = billingMonth;
    this.totalConsumed = totalConsumed;
  }

  public LocalDate getBillingMonth() {
    return billingMonth;
  }

  public void setBillingMonth(LocalDate billingMonth) {
    this.billingMonth = billingMonth;
  }

  public long getTotalConsumed() {
    return totalConsumed;
  }

  public void setTotalConsumed(long totalConsumed) {
    this.totalConsumed = totalConsumed;
  }
}
