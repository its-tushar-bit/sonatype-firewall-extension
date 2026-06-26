/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

/**
 * Nexus One dashboard metric endpoints. Shares the {@code rest/dashboard} class-level path with
 * {@link com.sonatype.insight.brain.dashboard.DashboardResource} — sub-paths must stay disjoint
 * between the two classes. Keep new {@code metrics}-prefixed sub-paths here.
 */
@Named
@Timed
@Path(DashboardMetricsResource.RESOURCE_PATH)
public class DashboardMetricsResource
{
  public static final String RESOURCE_PATH = "rest/dashboard";

  public static final String METRICS_PATH = "metrics";

  private final DashboardMetricsService service;

  @Inject
  public DashboardMetricsResource(DashboardMetricsService service) {
    this.service = service;
  }

  @POST
  @Path(METRICS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getDashboardMetricsExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_METRICS)
  public DashboardMetricsDTO getMetrics(DashboardMetricsRequestDTO request) {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.verifyEnabled();
    return service.getMetrics(request);
  }
}
