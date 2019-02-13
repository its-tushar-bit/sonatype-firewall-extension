/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

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
  public String submitClientEnvironment(@Context HttpServletRequest request) throws Exception {
    return client.relay(request, String.class, "session/environment");
  }
}
