/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyEvaluationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.service.InsightConfig;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.75
 */
@Named
@Timed
@Path(PublicApiPaths.THIRD_PARTY_SCAN_PATH)
public class ApiThirdPartyResource
{
  public static final String SCAN_COMPONENTS = "{applicationId}/sources/{source}";

  public static final String SCAN_STATUS = "{applicationId}/status/{scanRequestId}";

  private final ApiThirdPartyEvaluationService thirdPartyEvaluationService;

  private final InsightConfig config;

  @Inject
  public ApiThirdPartyResource(
      final ApiThirdPartyEvaluationService thirdPartyEvaluationService,
      final InsightConfig config)
  {
    this.thirdPartyEvaluationService = thirdPartyEvaluationService;
    this.config = config;
  }

  @Path(SCAN_COMPONENTS)
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_THIRD_PARTY)
  public Response scanComponents(
      @PathParam("applicationId") final String applicationId,
      @PathParam("source") final String source,
      @DefaultValue("build") @QueryParam("stageId") final String stageId,
      String sbom)
  {
    if (config.isThirdPartyEvaluationApiEnabled()) {
      ApiThirdPartyScanTicketDTO ticket =
          thirdPartyEvaluationService.scanComponents(applicationId, source, stageId, sbom);
      return Response.status(Response.Status.ACCEPTED).entity(ticket).build();
    }
    else {
      return Response.status(Response.Status.NOT_IMPLEMENTED).entity("").build();
    }
  }

  @GET
  @Path(SCAN_STATUS)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiThirdPartyScanResultDTO getScanStatus(
      @PathParam("applicationId") String applicationId,
      @PathParam("scanRequestId") String scanRequestId)
  {
    return thirdPartyEvaluationService.getScanStatus(applicationId, scanRequestId);
  }
}
