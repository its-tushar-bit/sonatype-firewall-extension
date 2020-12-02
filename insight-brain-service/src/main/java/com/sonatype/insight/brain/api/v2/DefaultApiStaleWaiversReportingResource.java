/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiversResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiStaleWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.81
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + DefaultApiStaleWaiversReportingResource.PATH)
@Consumes(MediaType.APPLICATION_JSON)
public class DefaultApiStaleWaiversReportingResource implements ApiStaleWaiversReportingResource
{
  public static final String PATH = "/waivers/stale";

  private final ApiStaleWaiverService staleWaiverService;

  @Inject
  public DefaultApiStaleWaiversReportingResource(final ApiStaleWaiverService staleWaiverService) {
    this.staleWaiverService = staleWaiverService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_STALE_WAIVERS)
  public ApiStaleWaiversResponseDTO getStaleWaivers() {
    ApiStaleWaiversResponseDTO staleWaiversResponseDTO = new ApiStaleWaiversResponseDTO();
    staleWaiversResponseDTO.staleWaivers = staleWaiverService.getStaleWaivers();

    return staleWaiversResponseDTO;
  }
}
