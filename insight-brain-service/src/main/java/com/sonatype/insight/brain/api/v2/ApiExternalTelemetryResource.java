/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.ExternalTelemetryService;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Resource to capture data from external services that interact with the IQ server.
 *
 * @since 1.133
 */
@Named
@Path(PublicApiPaths.EXTERNAL_TELEMETRY_PATH)
@Hidden
public class ApiExternalTelemetryResource
{
  private final ExternalTelemetryService externalTelemetryService;

  @Inject
  public ApiExternalTelemetryResource(ExternalTelemetryService externalTelemetryService) {
    this.externalTelemetryService = externalTelemetryService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public void postExternalTelemetry(
      Map<String, Object> telemetryValues,
      @Context HttpServletRequest req)
  {
    externalTelemetryService.sendTelemetry(telemetryValues, req);
  }
}
