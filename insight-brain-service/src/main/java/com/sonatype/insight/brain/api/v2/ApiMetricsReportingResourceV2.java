/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingFlattenedDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiMetricsReportingServiceV2;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.52
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiMetricsReportingResourceV2.PATH)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiMetricsReportingResourceV2
{
  public static final String PATH = "/metrics";

  private final ApiMetricsReportingServiceV2 metricsReportingService;

  @Inject
  public ApiMetricsReportingResourceV2(final ApiMetricsReportingServiceV2 metricsReportingService) {
    this.metricsReportingService = metricsReportingService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiMetricsReportingDTOV2> getMetrics(ApiMetricsReportingQueryDTOV2 queryDTO) {
    return metricsReportingService.getMetrics(queryDTO);
  }

  @POST
  @Produces("text/csv")
  public List<ApiMetricsReportingFlattenedDTOV2> getFlattenedMetrics(ApiMetricsReportingQueryDTOV2 queryDTO) {
    return metricsReportingService.getFlattenedMetrics(queryDTO);
  }
}
