/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.telemetry.model.TelemetryData;

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
