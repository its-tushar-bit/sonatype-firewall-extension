/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.33
 */
@Named
@Timed
@Path(PolicyViolationAggregationResource.RESOURCE_PATH)
public class PolicyViolationAggregationResource
{
  public static final String RESOURCE_PATH = "rest/aggregation/policyViolation";

  private final SuccessMetricsReportDataService successMetricsReportDataService;

  @Inject
  public PolicyViolationAggregationResource(SuccessMetricsReportDataService successMetricsReportDataService) {
    this.successMetricsReportDataService = successMetricsReportDataService;
  }

  @GET
  @Path("{successMetricsReportId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getSuccessMetricsChartDataExceptionMeter")
  /**
   * @since 1.39
   */
  public SuccessMetricsChartDataDTO getChartData(@PathParam("successMetricsReportId") String successMetricsReportId) {
    return successMetricsReportDataService.getChartData(successMetricsReportId);
  }
}
