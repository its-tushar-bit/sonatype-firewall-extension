/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import java.time.LocalDate;
import java.util.Objects;

public final class ConsumptionDateRange
{
  private final LocalDate startDate;

  private final LocalDate endDate;

  public ConsumptionDateRange(LocalDate startDate, LocalDate endDate) {
    this.startDate = Objects.requireNonNull(startDate);
    this.endDate = Objects.requireNonNull(endDate);
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }
}
