/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @since 1.117
 */
public class DefaultBranchMonitoringConfig
{
  @NotNull
  @Pattern(regexp = "\\d{1,2}:\\d{2}")
  private String startTime = "00:00";

  @NotNull
  @Min(1)
  private Integer intervalInHours = 24;

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(final String startTime) {
    this.startTime = startTime;
  }

  public Integer getIntervalInHours() {
    return intervalInHours;
  }

  public void setIntervalInHours(final Integer intervalInHours) {
    this.intervalInHours = intervalInHours;
  }
}
