/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

/**
 * @since 1.52
 */
public class ApiMetricsReportingDTOV2
{
  public final String applicationId;

  public final String applicationPublicId;

  public final String applicationName;

  public final String organizationId;

  public final String organizationName;

  public final List<ApiMetricsReportingAggregationDTOV2> aggregations;

  public ApiMetricsReportingDTOV2(
      String applicationId,
      String applicationPublicId,
      String applicationName,
      String organizationId,
      String organizationName,
      List<ApiMetricsReportingAggregationDTOV2> aggregations)
  {
    this.applicationId = applicationId;
    this.applicationPublicId = applicationPublicId;
    this.applicationName = applicationName;
    this.organizationId = organizationId;
    this.organizationName = organizationName;
    this.aggregations = aggregations;
  }
}
