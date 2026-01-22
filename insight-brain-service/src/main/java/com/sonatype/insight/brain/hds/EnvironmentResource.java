/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(EnvironmentResource.RESOURCE_PATH)
public class EnvironmentResource
{
  public static final String RESOURCE_PATH = "rest/session/environment";

  private final HdsClient client;

  @Inject
  public EnvironmentResource(HdsClient client) {
    this.client = client;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String submitClientEnvironment(@Context HttpServletRequest request) throws Exception {
    return client.relay(request, String.class, "session/environment").content;
  }
}
