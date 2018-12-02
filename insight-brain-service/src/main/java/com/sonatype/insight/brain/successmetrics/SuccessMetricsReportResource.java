/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.37
 */
@Named
@Timed
@Path(SuccessMetricsReportResource.RESOURCE_PATH)
public class SuccessMetricsReportResource
{
  public static final String RESOURCE_PATH = "rest/successMetrics/report";

  private final SuccessMetricsReportService successMetricsReportService;

  @Inject
  public SuccessMetricsReportResource(SuccessMetricsReportService successMetricsReportService) {
    this.successMetricsReportService = successMetricsReportService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public SuccessMetricsReportDTO createSuccessMetricsReportForCurrentUser(SuccessMetricsReportDTO successMetricsDTO) {
    return successMetricsReportService.createSuccessMetricsReportForCurrentUser(successMetricsDTO);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<SuccessMetricsReportDTO> getSuccessMetricsReportForCurrentUser() throws IOException {
    return successMetricsReportService.getSuccessMetricsReportsForCurrentUser();
  }

  @DELETE
  @Path("{successMetricsReportId}")
  public void deleteSuccessMetricsReportForCurrentUser(@PathParam("successMetricsReportId") String successMetricsReportId) {
    successMetricsReportService.deleteSuccessMetricsReportForCurrentUser(successMetricsReportId);
  }
}
