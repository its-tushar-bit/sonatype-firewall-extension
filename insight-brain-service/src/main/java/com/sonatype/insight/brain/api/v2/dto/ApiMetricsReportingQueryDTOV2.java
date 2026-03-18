/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Set;

import com.sonatype.insight.brain.model.successmetrics.TimePeriod;

/**
 * @since 1.52
 */
public class ApiMetricsReportingQueryDTOV2
{
  public TimePeriod timePeriod;

  /**
   * Expected to be an ISO 8601 year-month or year-week specifier
   */
  public String firstTimePeriod;

  /**
   * Expected to be an ISO 8601 year-month or year-week specifier. Can be null. Must represent a date equal to or
   * greater than firstTimePeriod
   */
  public String lastTimePeriod;

  public Set<String> applicationIds;

  public Set<String> organizationIds;

  public ApiMetricsReportingQueryDTOV2() {
  }

  public ApiMetricsReportingQueryDTOV2(
      TimePeriod timePeriod,
      String firstTimePeriod,
      String lastTimePeriod,
      Set<String> applicationIds,
      Set<String> organizationIds)
  {
    this.timePeriod = timePeriod;
    this.firstTimePeriod = firstTimePeriod;
    this.lastTimePeriod = lastTimePeriod;
    this.applicationIds = applicationIds;
    this.organizationIds = organizationIds;
  }
}
