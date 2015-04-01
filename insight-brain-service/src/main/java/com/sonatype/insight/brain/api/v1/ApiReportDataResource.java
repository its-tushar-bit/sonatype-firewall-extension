/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.dto.ApiReportDataDTO;
import com.sonatype.insight.brain.api.v1.service.ApiReportDataService;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 *
 * @deprecated since 1.13.0, use {@link ApiReportDataResourceV2}
 *
 * @since 1.9.1
 */
@Deprecated
@Named
@Path(PublicApiPaths.REPORT_DATA_SERVICE_PATH)
public class ApiReportDataResource
{
  private final ApiReportDataService reportDataService;

  @Inject
  public ApiReportDataResource(ApiReportDataService reportDataService) {
    this.reportDataService = reportDataService;
  }

  /**
   * Gets the JSON data for the report of the given application and scan.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiReportDataDTO getData(@PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId) throws Exception
  {
    return reportDataService.getData(applicationPublicId, scanId);
  }
}
