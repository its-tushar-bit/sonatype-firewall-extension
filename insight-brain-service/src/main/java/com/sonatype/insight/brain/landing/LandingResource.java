/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(LandingResource.RESOURCE_PATH)
@UnlicensedPath
public class LandingResource
{
  public static final String RESOURCE_PATH = "";

  private final LandingService landingService;

  @Inject
  public LandingResource(LandingService landingService) {
    this.landingService = landingService;
  }

  @GET
  public Response home() {
    return Response.seeOther(landingService.getDestination()).build();
  }
}
