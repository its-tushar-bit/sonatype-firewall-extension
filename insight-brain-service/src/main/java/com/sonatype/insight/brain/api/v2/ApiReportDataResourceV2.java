/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.13.0
 */
@Named
@Path(PublicApiPaths.REPORT_DATA_SERVICE_PATH_V2)
public class ApiReportDataResourceV2
{
  private final ApiReportDataServiceV2 reportDataService;

  @Inject
  public ApiReportDataResourceV2(ApiReportDataServiceV2 reportDataService) {
    this.reportDataService = reportDataService;
  }

  /**
   * Gets the JSON data for the report of the given application and scan.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiReportDataDTOV2 getData(@PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId) throws Exception
  {
    return reportDataService.getData(applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to this REST resource for the given application and scan.
   */
  public static String getDataUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(PublicApiPaths.REPORT_DATA_SERVICE_PATH_V2).build(applicationPublicId, scanId).toString();
  }
}
