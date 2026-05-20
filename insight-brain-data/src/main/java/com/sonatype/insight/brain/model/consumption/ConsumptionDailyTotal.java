/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import java.time.LocalDate;

/**
 * Represents aggregated daily consumption totals. Used by DAO GROUP BY queries on event_timestamp.
 *
 * @since 1.204
 */
public class ConsumptionDailyTotal
{
  private LocalDate day;

  private long componentCount;

  public ConsumptionDailyTotal() {
  }

  public ConsumptionDailyTotal(LocalDate day, long componentCount) {
    this.day = day;
    this.componentCount = componentCount;
  }

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  public long getComponentCount() {
    return componentCount;
  }

  public void setComponentCount(long componentCount) {
    this.componentCount = componentCount;
  }
}
