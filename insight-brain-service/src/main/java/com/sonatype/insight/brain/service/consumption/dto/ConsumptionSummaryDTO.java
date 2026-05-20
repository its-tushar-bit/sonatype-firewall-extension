/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for consumption summary data returned by the consumption API.
 * Represents current billing window consumption with activity breakdown.
 *
 * @since 1.204
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumptionSummaryDTO
{
  private long consumed;

  private Long limit;

  private Integer warningThresholdPct;

  private Double percentUsed;

  private Long remaining;

  private String resetDate;

  private String billingWindowStart;

  private String tier;

  private Map<String, Long> activityBreakdown;

  public ConsumptionSummaryDTO() {
    // Default constructor for serialization/deserialization
  }

  public long getConsumed() {
    return consumed;
  }

  public void setConsumed(long consumed) {
    this.consumed = consumed;
  }

  public Long getLimit() {
    return limit;
  }

  public void setLimit(Long limit) {
    this.limit = limit;
  }

  public Integer getWarningThresholdPct() {
    return warningThresholdPct;
  }

  public void setWarningThresholdPct(Integer warningThresholdPct) {
    this.warningThresholdPct = warningThresholdPct;
  }

  public Double getPercentUsed() {
    return percentUsed;
  }

  public void setPercentUsed(Double percentUsed) {
    this.percentUsed = percentUsed;
  }

  public Long getRemaining() {
    return remaining;
  }

  public void setRemaining(Long remaining) {
    this.remaining = remaining;
  }

  public String getResetDate() {
    return resetDate;
  }

  public void setResetDate(String resetDate) {
    this.resetDate = resetDate;
  }

  public String getBillingWindowStart() {
    return billingWindowStart;
  }

  public void setBillingWindowStart(String billingWindowStart) {
    this.billingWindowStart = billingWindowStart;
  }

  public String getTier() {
    return tier;
  }

  public void setTier(String tier) {
    this.tier = tier;
  }

  public Map<String, Long> getActivityBreakdown() {
    return activityBreakdown;
  }

  public void setActivityBreakdown(Map<String, Long> activityBreakdown) {
    this.activityBreakdown = activityBreakdown;
  }
}
