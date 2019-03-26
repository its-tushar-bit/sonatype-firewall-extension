/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.service.BaseUrl;

import com.codahale.metrics.annotation.Timed;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
public class ApiReportDataResourceV2
{
  public static final String RAW_DATA_PATH = "raw";

  private final ApiReportDataServiceV2 reportDataService;

  private final BaseUrl baseUrl;

  @Inject
  public ApiReportDataResourceV2(ApiReportDataServiceV2 reportDataService, BaseUrl baseUrl) {
    this.reportDataService = reportDataService;
    this.baseUrl = baseUrl;
  }

  /**
   * NOTE: prior to IQ 64, this endpoint was the actual implementation that is now at the RAW_DATA_PATH, rather
   * than a redirect
   */
  @GET
  public Response getData(@PathParam("applicationPublicId") String applicationPublicId,
                          @PathParam("scanId") String scanId) throws Exception
  {
    return Response.temporaryRedirect(new URI(baseUrl.get()).resolve(getDataUrl(applicationPublicId, scanId))).build();
  }

  /**
   * Gets the JSON data for the report of the given application and scan.
   * @since 1.64
   */
  @GET
  @Path(RAW_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public ApiReportDataDTOV2 getRawData(@PathParam("applicationPublicId") String applicationPublicId,
                                       @PathParam("scanId") String scanId) throws Exception
  {
    AuditData.get().setReportId(scanId);
    return reportDataService.getData(applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to this REST resource for the given application and scan.
   */
  public static String getDataUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(ApiReportDataResourceV2.RAW_DATA_PATH)
        .build(applicationPublicId, scanId).toString();
  }
}
