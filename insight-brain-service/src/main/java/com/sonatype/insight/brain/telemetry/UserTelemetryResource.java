/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.telemetry.PendoService.PendoConfig;

/**
 * @since 1.50
 */
@Named
@Singleton
@Path(UserTelemetryResource.RESOURCE_PATH)
public class UserTelemetryResource
{
  public static final String CONFIG_PATH = "config";

  public static final String EVENTS = "events";

  public static final String EVENTS_PATH = EVENTS + "/{path:.*}";

  public static final String JAVASCRIPT_PATH = "javascript";

  public static final String RESOURCE_SUBPATH = "user-telemetry";

  public static final String RESOURCE_PATH = "rest/" + RESOURCE_SUBPATH;

  private final PendoService pendoService;

  @Inject
  public UserTelemetryResource(PendoService pendoService) {
    this.pendoService = pendoService;
  }

  @GET
  @Path(JAVASCRIPT_PATH)
  @Produces("application/javascript")
  public Response getJavascript() {
    byte[] js = pendoService.getJavascript();
    if (js != null) {
      return Response.ok(js).build();
    }
    // If telemetry is known to be disabled or if there is an error contacting the HDS we return an empty file. The
    // initialization snippet creates a dummy Pendo object with queues, we make a single call to this object so memory
    // utilization is not an issue.
    return Response.ok().build();
  }

  @GET
  @Path(CONFIG_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public PendoConfig getConfig() {
    return pendoService.getConfig();
  }

  @POST
  @Path(EVENTS_PATH)
  public Response proxyPost(@Context HttpServletRequest request, @PathParam("path") String pendoPath) {
    return proxyRequest(request, pendoPath);
  }

  @GET
  @Path(EVENTS_PATH)
  public Response proxyGet(@Context HttpServletRequest request, @PathParam("path") String pendoPath) {
    return proxyRequest(request, pendoPath);
  }

  private Response proxyRequest(HttpServletRequest request, String path) {
    RelayResponse<?> response = pendoService.proxy(request, path);
    return Response.ok(response.content, response.contentType).build();
  }
}
