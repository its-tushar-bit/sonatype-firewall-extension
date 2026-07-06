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

  public final MetricValueDTO components;

  public final MetricValueDTO organizations;

  public final MetricValueDTO policies;

  public final MetricValueDTO vulnerabilities;

  public final MetricValueDTO legal;

  public final MetricValueDTO waivers;

  public final Long lastUpdatedAt;

  @JsonCreator
  public DashboardMetricsDTO(
      @JsonProperty("applications") MetricValueDTO applications,
      @JsonProperty("violations") MetricValueDTO violations,
      @JsonProperty("components") MetricValueDTO components,
      @JsonProperty("organizations") MetricValueDTO organizations,
      @JsonProperty("policies") MetricValueDTO policies,
      @JsonProperty("vulnerabilities") MetricValueDTO vulnerabilities,
      @JsonProperty("legal") MetricValueDTO legal,
      @JsonProperty("waivers") MetricValueDTO waivers,
      @JsonProperty("lastUpdatedAt") Long lastUpdatedAt)
  {
    this.applications = applications;
    this.violations = violations;
    this.components = components;
    this.organizations = organizations;
    this.policies = policies;
    this.vulnerabilities = vulnerabilities;
    this.legal = legal;
    this.waivers = waivers;
    this.lastUpdatedAt = lastUpdatedAt;
  }
}
