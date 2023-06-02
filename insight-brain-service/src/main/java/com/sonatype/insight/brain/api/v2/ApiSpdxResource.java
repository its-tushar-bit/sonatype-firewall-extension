/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiSpdxService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.164.0
 */
@Named
@Timed
@Singleton
@Path(PublicApiPaths.SPDX_RESOURCE_PATH)
public class ApiSpdxResource
{
  static final String GET_BY_STAGE_PATH = "{applicationId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "{applicationId}/reports/{scanId}";

  static final String DEFAULT_SPDX_FORMAT = "json";

  static final String DEFAULT_SPDX_VERSION = "2.3";

  private final ApiSpdxService apiSpdxService;

  @Inject
  public ApiSpdxResource(ApiSpdxService apiSpdxService) {
    this.apiSpdxService = apiSpdxService;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatestForStage(
      @PathParam("applicationId") String applicationId,
      @PathParam("stageId") String stageId,
      @DefaultValue(DEFAULT_SPDX_FORMAT) @QueryParam("format") String format,
      @DefaultValue("false") @QueryParam("generateCycloneDx") boolean generateCycloneDx,
      @DefaultValue(DEFAULT_SPDX_VERSION) @QueryParam("spdxVersion") String spdxVersion)
  {
    return apiSpdxService.getLatestForStage(applicationId, stageId, format, generateCycloneDx, spdxVersion);
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByScanId(
      @PathParam("applicationId") String applicationId,
      @PathParam("scanId") String scanId,
      @DefaultValue(DEFAULT_SPDX_FORMAT) @QueryParam("format") String format,
      @DefaultValue("false") @QueryParam("generateCycloneDx") boolean generateCycloneDx,
      @DefaultValue(DEFAULT_SPDX_VERSION) @QueryParam("spdxVersion") String spdxVersion)
  {
    return apiSpdxService.getByScanId(applicationId, scanId, format, generateCycloneDx, spdxVersion);
  }
}
