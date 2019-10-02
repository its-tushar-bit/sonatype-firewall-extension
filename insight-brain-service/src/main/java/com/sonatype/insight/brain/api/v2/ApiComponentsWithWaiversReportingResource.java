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
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsWithWaiversReportingService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.75
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsWithWaiversReportingResource.PATH)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiComponentsWithWaiversReportingResource
{
  public static final String PATH = "/componentsWithWaivers";

  private final ApiComponentsWithWaiversReportingService componentsWithWaiversReportingService;

  @Inject
  public ApiComponentsWithWaiversReportingResource(
      ApiComponentsWithWaiversReportingService componentsWithWaiversReportingService)
  {
    this.componentsWithWaiversReportingService = componentsWithWaiversReportingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiComponentWaiversDTO getComponentsWithWaivers() {
    return componentsWithWaiversReportingService.getComponentsWithWaivers();
  }
}
