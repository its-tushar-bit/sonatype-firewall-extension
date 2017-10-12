/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;

/**
 * @since 1.33
 */
@Named
@Path(PolicyViolationAggregationResource.RESOURCE_PATH)
public class PolicyViolationAggregationResource
{
  public static final String RESOURCE_PATH = "rest/aggregation/policyViolation";

  private final PolicyViolationAggregationService violationAggregationService;

  @Inject
  public PolicyViolationAggregationResource(PolicyViolationAggregationService violationAggregationService) {
    this.violationAggregationService = violationAggregationService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getSuccessMetricsChartDataExceptionMeter")
  /**
   * @since 1.39
   */
  public SuccessMetricsChartDataDTO getChartData(OwnerFilterDTO ownerFilterDTO ) {
    return violationAggregationService
        .getChartData(ownerFilterDTO.organizationIds, ownerFilterDTO.applicationIds, ownerFilterDTO.includeLatestData);
  }
}
