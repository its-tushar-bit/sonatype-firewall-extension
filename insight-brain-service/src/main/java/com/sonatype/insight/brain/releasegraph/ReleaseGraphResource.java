/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Named
@Path("rest/report/{applicationId}/{scanId}/releaseGraph")
public class ReleaseGraphResource
{
  private static final long YEAR = 365 * 24 * 60 * 60 * 1000;

  private final ReleaseGraphService releaseGraphService;

  @Inject
  public ReleaseGraphResource(ReleaseGraphService releaseGraphService) {
    this.releaseGraphService = releaseGraphService;
  }

  @GET
  public Response getImage(@PathParam("applicationId") final String applicationPublicId,
      @PathParam("scanId") final String scanId, @QueryParam("groupId") String groupId,
      @QueryParam("artifactId") String artifactId, @QueryParam("version") String version)
  {
    return Response
        .ok(releaseGraphService.getImage(applicationPublicId, scanId, groupId, artifactId, version), ReleaseGraphService.CONTENT_TYPE)
        .expires(new Date(System.currentTimeMillis() + YEAR)).build();
  }
}
