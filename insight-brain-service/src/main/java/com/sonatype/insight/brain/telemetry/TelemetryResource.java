/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.47
 */
@Named
@Timed
@Path(TelemetryResource.RESOURCE_PATH)
public class TelemetryResource
{
  public static final String RESOURCE_PATH = "rest/environment/stats";

  private final TelemetryService telemetryService;

  @Inject
  public TelemetryResource(TelemetryService telemetryService) {
    this.telemetryService = telemetryService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public void forwardFrontendTelemetryToHds(@Context HttpServletRequest request, TelemetryData data) {
    telemetryService.forwardFrontendTelemetryToHds(data, request.getSession(false).getId());
  }
}
