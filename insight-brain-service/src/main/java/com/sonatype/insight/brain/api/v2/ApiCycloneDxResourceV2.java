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

@Named
@Timed
@Singleton
@Path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH)
public class ApiCycloneDxResourceV2
{
  private ApiCycloneDxServiceV2 apiCycloneDxService;

  @Inject
  public ApiCycloneDxResourceV2(ApiCycloneDxServiceV2 apiCycloneDxService) {
    this.apiCycloneDxService = apiCycloneDxService;
  }

  @GET
  @Path("{applicationId}/{stageId}")
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatest(@PathParam("applicationId") String applicationId, @PathParam("stageId") String stageId) {
    return apiCycloneDxService.getLatest(applicationId, stageId);
  }

  @GET
  @Path("{applicationId}/scans/{scanId}")
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByScanId(@PathParam("applicationId") String applicationId, @PathParam("scanId") String scanId) {
    return apiCycloneDxService.getByScanId(applicationId, scanId);
  }
}
