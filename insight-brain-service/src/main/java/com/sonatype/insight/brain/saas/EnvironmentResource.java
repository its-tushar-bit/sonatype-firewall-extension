/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Named
@Path(EnvironmentResource.RESOURCE_PATH)
public class EnvironmentResource
{
  public static final String RESOURCE_PATH = "rest/session/environment";

  private final SaasClient client;

  @Inject
  public EnvironmentResource(SaasClient client) {
    this.client = client;
  }

  @GET
  public Response submitClientEnvironment(@Context HttpServletRequest request) throws Exception {
    return client.doProxy(request, "session/environment");
  }

}
