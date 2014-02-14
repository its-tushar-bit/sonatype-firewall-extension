/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

@Named
@Path(LandingResource.SERVICE_PATH)
@UnlicensedPath
public class LandingResource
{
  public static final String SERVICE_PATH = "";

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