/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.37
 */
@Named
@Timed
@Path(SuccessMetricsReportResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SUCCESS_METRICS)
public class SuccessMetricsReportResource
{
  public static final String RESOURCE_PATH = "rest/successMetrics/report";

  static final String CHART_DATA_PATH = "{successMetricsReportId}/chartData";

  static final String COMPONENT_COUNTS_PATH = "{successMetricsReportId}/componentCounts";

  private final SuccessMetricsReportService successMetricsReportService;

  private final SuccessMetricsReportDataService successMetricsReportDataService;

  @Inject
  public SuccessMetricsReportResource(
      SuccessMetricsReportService successMetricsReportService,
      SuccessMetricsReportDataService successMetricsReportDataService)
  {
    this.successMetricsReportService = successMetricsReportService;
    this.successMetricsReportDataService = successMetricsReportDataService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_SUCCESS_METRICS_REPORT)
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
  @Audited(AuditEvent.DELETE_SUCCESS_METRICS_REPORT)
  public void deleteSuccessMetricsReportForCurrentUser(
      @PathParam("successMetricsReportId") String successMetricsReportId)
  {
    successMetricsReportService.deleteSuccessMetricsReportForCurrentUser(successMetricsReportId);
  }

  /**
   * @since 1.56
   */
  @GET
  @Path(CHART_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getSuccessMetricsChartDataExceptionMeter")
  @Audited(AuditEvent.VIEW_SUCCESS_METRICS_REPORT)
  public SuccessMetricsChartDataDTO getChartData(@PathParam("successMetricsReportId") String successMetricsReportId) {
    return successMetricsReportDataService.getChartData(successMetricsReportId);
  }

  /**
   * @since 1.56
   */
  @GET
  @Path(COMPONENT_COUNTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getComponentCountsExceptionMeter")
  @Audited(AuditEvent.VIEW_SUCCESS_METRICS_REPORT)
  public ComponentCountsDTO getComponentCounts(@PathParam("successMetricsReportId") String successMetricsReportId) {
    return successMetricsReportDataService.getComponentCounts(successMetricsReportId);
  }
}
