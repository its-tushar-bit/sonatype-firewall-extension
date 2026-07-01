/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dashboard metrics response (CLM-40927). Immutable once built.
 */
public class DashboardMetricsDTO
{
  public final MetricValueDTO applications;

  public final MetricValueDTO violations;

  public final Long lastUpdatedAt;

  @JsonCreator
  public DashboardMetricsDTO(
      @JsonProperty("applications") MetricValueDTO applications,
      @JsonProperty("violations") MetricValueDTO violations,
      @JsonProperty("lastUpdatedAt") Long lastUpdatedAt)
  {
    this.applications = applications;
    this.violations = violations;
    this.lastUpdatedAt = lastUpdatedAt;
  }
}
