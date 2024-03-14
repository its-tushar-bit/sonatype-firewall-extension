/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportServiceV2;

import com.codahale.metrics.annotation.Timed;

@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiReportResourceV2.PATH)
@Named
@Timed
public class ApiReportResourceV2
{
  public static final String PATH = "/applications";

  private final ApiReportServiceV2 reportService;

  @Inject
  public ApiReportResourceV2(final ApiReportServiceV2 searchService) {
    this.reportService = searchService;
  }

  @GET
  @Path("{applicationId}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiApplicationReportDTOV2> getByApplicationId(@PathParam("applicationId") String applicationId) {
    return reportService.getByApplicationId(applicationId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiApplicationReportDTOV2> getAll() {
    return reportService.getAll();
  }

  @Path("{applicationId}/history")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiReportHistoryDTO getReportHistoryForApplication(
      @PathParam("applicationId") final String applicationId,
      @QueryParam("stage") String stage,
      @QueryParam("limit") Integer limit)
  {
    return reportService.getReportHistoryForApplication(applicationId, stage, limit);
  }
}
