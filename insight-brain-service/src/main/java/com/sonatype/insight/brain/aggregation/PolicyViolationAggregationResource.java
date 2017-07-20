/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

import java.util.List;

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

  public static final String GET_MTTRS = "mttr";

  public static final String GET_AVERAGES = "averages";

  public static final String GET_APPLICATION_COUNTS = "applicationCounts";

  private final PolicyViolationAggregationService violationAggregationService;

  @Inject
  public PolicyViolationAggregationResource(PolicyViolationAggregationService violationAggregationService) {
    this.violationAggregationService = violationAggregationService;
  }

  @POST
  @Path(GET_MTTRS)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getMttrsExceptionMeter")
  public List<MttrDTO> getMttrs(OwnerFilterDTO ownerFilterDTO) {
    return violationAggregationService.getMttrs(ownerFilterDTO.organizationIds, ownerFilterDTO.applicationIds);
  }

  @POST
  @Path(GET_AVERAGES)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getAveragesExceptionMeter")
  public SuccessMetricsAveragesDTO getAverages(OwnerFilterDTO ownerFilterDTO) {
    return violationAggregationService.getAverages(ownerFilterDTO.organizationIds, ownerFilterDTO.applicationIds);
  }

  @POST
  @Path(GET_APPLICATION_COUNTS)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getApplicationCountsExceptionMeter")
  public ApplicationCountsDTO getApplicationCounts(OwnerFilterDTO ownerFilterDTO) {
    return violationAggregationService.getApplicationCounts(ownerFilterDTO.organizationIds,
        ownerFilterDTO.applicationIds);
  }
}
