/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.70
 */
@Named
@Timed
@Singleton
@Path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH)
public class ApiCycloneDxResourceV2
{
  static final String GET_BY_STAGE_PATH = "{applicationId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "{applicationId}/reports/{reportId}";

  private final ApiCycloneDxServiceV2 apiCycloneDxService;

  @Inject
  public ApiCycloneDxResourceV2(ApiCycloneDxServiceV2 apiCycloneDxService) {
    this.apiCycloneDxService = apiCycloneDxService;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatest(@PathParam("applicationId") String applicationId, @PathParam("stageId") String stageId) {
    return apiCycloneDxService.getLatest(applicationId, stageId);
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByReportId(
      @PathParam("applicationId") String applicationId,
      @PathParam("reportId") String reportId)
  {
    return apiCycloneDxService.getByScanId(applicationId, reportId);
  }
}
