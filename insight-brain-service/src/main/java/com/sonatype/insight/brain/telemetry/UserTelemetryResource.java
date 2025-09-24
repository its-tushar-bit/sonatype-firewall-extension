/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.PendoService.PendoConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
@Named
@Singleton
@Path(UserTelemetryResource.RESOURCE_PATH)
public class UserTelemetryResource
{
  private static final Logger log = LoggerFactory.getLogger(UserTelemetryResource.class);

  public static final String CONFIG_PATH = "config";

  public static final String EVENTS = "events";

  public static final String EVENTS_PATH = EVENTS + "/{path:.*}";

  public static final String JAVASCRIPT_PATH = "javascript";

  public static final String CSS_EVENT_PATH = "style.css";

  public static final String CONFIGURATION_EVENT_PATH = "rte/v1/configuration/";

  public static final String RESOURCE_SUBPATH = "user-telemetry";

  public static final String RESOURCE_PATH = "rest/" + RESOURCE_SUBPATH;

  /*
   * This set contains the list of paths for the event endpoint that returns no content(empty response) and therefore
   *  are safe to fire-and-forget.
   * */
  public static final Set<String> FIRE_AND_FORGET_SAFE_PATHS = Set.of("rte/v2/kc", "rte/v1/inapp");

  private final PendoService pendoService;

  private final UserTelemetryThreadPoolExecutor executor;

  @Inject
  public UserTelemetryResource(
      PendoService pendoService,
      final Configuration configuration,
      final ShutdownHandler shutdownHandler)
  {
    this.pendoService = pendoService;
    this.executor = new UserTelemetryThreadPoolExecutor(configuration.getUserTelemetryPoolSize());
    shutdownHandler.add(executor);
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
    if (isFireAndForgetEvent(pendoPath)) {
      return proxyRequestAsync(request, pendoPath);
    }
    return proxyRequestSync(request, pendoPath);
  }

  @GET
  @Path(EVENTS_PATH)
  public Response proxyGet(@Context HttpServletRequest request, @PathParam("path") String pendoPath) {
    if (isFireAndForgetEvent(pendoPath)) {
      return proxyRequestAsync(request, pendoPath);
    }

    //CLM-34770:
    // Boostrap process for Gainsight sdk to load properly in the browser. The responses from these methods are required
    //    * 1. Javascript
    //    * 2. Event rte/v1/configuration/<Product Key>
    //    * 3. Event style.css

    if (pendoPath != null && pendoPath.contains(CSS_EVENT_PATH)) {
      // If CSS content cannot be retrieved, return empty css empty file to avoid breaking the
      // Gainsight js sdk bootstrapping/initialization in the frontend.
      return proxyCssRequestAndCache();
    }
    else {
      return proxyRequestSync(request, pendoPath);
    }
  }

  /**
   * CLM-34770: Fire-and-forget telemetry to prevent UI blocking during Gainsight outages. Returns immediately while
   * telemetry processes asynchronously in background.
   */
  private Response proxyRequestAsync(HttpServletRequest request, String path) {
    // Submit telemetry async - don't wait for completion because we are not expecting anything in the response
    CompletableFuture.runAsync(() -> {
      try {
        pendoService.proxyWithoutRetry(request, path);
        log.debug("Telemetry submitted successfully for path: {}", path);
      }
      catch (Exception e) {
        log.debug("Telemetry submission failed for path: {} - this is expected during Gainsight outages", path, e);
      }
    }, executor);

    // Return immediately to prevent browser connection exhaustion
    return Response.ok().build();
  }

  private Response proxyRequestSync(HttpServletRequest request, String path) {
    RelayResponse<?> response = pendoService.proxyWithoutRetry(request, path);
    if (request.getMethod().equals("GET") && path != null && path.contains(CONFIGURATION_EVENT_PATH)) {
      // If configuration content cannot be retrieved, return empty json object to avoid breaking the
      // Gainsight js sdk bootstrapping/initialization in the frontend.
      return Response.ok("{}", MediaType.APPLICATION_JSON).build();
    }

    return response == null ? Response.ok().build() : Response.ok(response.content, response.contentType).build();
  }

  private Response proxyCssRequestAndCache() {
    byte[] cssCachedContent = pendoService.getCss();
    String responseBody = "";
    if (cssCachedContent != null) {
      responseBody = new String(cssCachedContent);
    }

    return Response.ok(responseBody, "text/css").build();
  }

  private boolean isFireAndForgetEvent(String path) {
    return FIRE_AND_FORGET_SAFE_PATHS.stream().anyMatch(path::contains);
  }
}
