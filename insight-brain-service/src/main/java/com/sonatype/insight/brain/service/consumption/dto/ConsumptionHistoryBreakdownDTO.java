/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import java.util.Map;

/**
 * DTO for monthly consumption history with per-activity-type breakdown.
 *
 * @since 1.204
 */
public class ConsumptionHistoryBreakdownDTO
{
  private String month;

  private long consumed;

  private Map<String, Long> breakdown;

  public ConsumptionHistoryBreakdownDTO() {
  }

  public ConsumptionHistoryBreakdownDTO(String month, long consumed, Map<String, Long> breakdown) {
    this.month = month;
    this.consumed = consumed;
    this.breakdown = breakdown;
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public long getConsumed() {
    return consumed;
  }

  public void setConsumed(long consumed) {
    this.consumed = consumed;
  }

  public Map<String, Long> getBreakdown() {
    return breakdown;
  }

  public void setBreakdown(Map<String, Long> breakdown) {
    this.breakdown = breakdown;
  }
}
