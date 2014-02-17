/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.10
 */
@Named
@Path(ReportDataResource.SERVICE_PATH)
public class ReportDataResource
{
  public static final String SERVICE_PATH = "api/v1/application/{applicationPublicId}/report/{scanId}/data";

  private final ReportDataService reportDataService;

  @Inject
  public ReportDataResource(ReportDataService reportDataService) {
    this.reportDataService = reportDataService;
  }

  /**
   * Gets the JSON data for the report of the given application and scan.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ReportData getData(@PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId) throws Exception
  {
    return reportDataService.getData(applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to this REST resource for the given application and scan.
   */
  public static String getDataUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(ReportDataResource.SERVICE_PATH).build(applicationPublicId, scanId).toString();
  }
}
