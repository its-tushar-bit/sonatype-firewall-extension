/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Named
@Timed
@Path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
public class ApiSastScanResource
{
  private final ApiSastScanService sastScanService;

  @Inject
  public ApiSastScanResource(final ApiSastScanService sastScanService) {
    this.sastScanService = sastScanService;
  }

  @GET
  @Path("{sastScanId}")
  @Produces(APPLICATION_JSON)
  public SastScanResponseDTO getSastScan(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("sastScanId") final String sastScanId)
  {
    return sastScanService.getSastScan(applicationPublicId, sastScanId);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_SAST_SCAN)
  public SastScanResponseDTO createSastScan(
      @PathParam("applicationPublicId") final String applicationPublicId,
      final SastScanRequestDTO createSastScanRequest)
  {
    return sastScanService.createSastScan(applicationPublicId, createSastScanRequest);
  }
}
