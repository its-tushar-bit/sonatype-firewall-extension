/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.IdeUsersOverviewDTO;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyScanService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.thirdparty.ThirdPartyUtils.SbomFormat;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.75
 */
@Named
@Timed
@Path(PublicApiPaths.THIRD_PARTY_SCAN_PATH)
public class ApiThirdPartyScanResource
{
  public static final String SCAN_COMPONENTS = "{applicationId}/sources/{source}";

  public static final String SCAN_STATUS = "{applicationId}/status/{scanRequestId}";

  public static final String IDE_USER_OVERVIEW = "ideUser/overview";

  public static final String SINCE_UTC_TIMESTAMP = "sinceUtcTimestamp";

  private final ApiThirdPartyScanService thirdPartyScanService;

  @Inject
  public ApiThirdPartyScanResource(final ApiThirdPartyScanService thirdPartyScanService) {
    this.thirdPartyScanService = thirdPartyScanService;
  }

  @Path(SCAN_COMPONENTS)
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_THIRD_PARTY)
  public Response scanComponents(
      @PathParam("applicationId") final String applicationId,
      @PathParam("source") final String source,
      @DefaultValue("build") @QueryParam("stageId") final String stageId,
      @Context final HttpServletRequest request,
      final String sbom)
  {
    SbomFormat format = request.getContentType().equalsIgnoreCase(MediaType.APPLICATION_XML) ? SbomFormat.XML
        : SbomFormat.JSON;
    ApiThirdPartyScanTicketDTO ticket = thirdPartyScanService.scanComponents(applicationId, source, stageId, sbom,
        DefaultHdsClient.getClientUserAgent(request), format);
    return Response.status(Response.Status.ACCEPTED).entity(ticket).build();
  }

  @GET
  @Path(SCAN_STATUS)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiThirdPartyScanResultDTO getScanStatus(
      @PathParam("applicationId") String applicationId,
      @PathParam("scanRequestId") String scanRequestId)
  {
    return thirdPartyScanService.getScanStatus(applicationId, scanRequestId);
  }

  @GET
  @Path(IDE_USER_OVERVIEW)
  @Produces(MediaType.APPLICATION_JSON)
  public IdeUsersOverviewDTO getIdeUsersOverview(
      @QueryParam(SINCE_UTC_TIMESTAMP) final Long sinceUtcTimestamp)
  {
    return thirdPartyScanService.getIdeUsersOverview(sinceUtcTimestamp);
  }
}
