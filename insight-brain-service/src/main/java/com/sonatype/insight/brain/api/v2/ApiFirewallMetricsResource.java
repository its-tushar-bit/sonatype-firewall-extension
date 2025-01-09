/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.170.0
 */
@Named
@Timed
@Path(ApiFirewallMetricsResource.RESOURCE_PATH)
public class ApiFirewallMetricsResource
{
  public static final String RESOURCE_PATH = "/api/v2/malware-defense/metrics/embedded";

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  @Inject
  public ApiFirewallMetricsResource(
      ApiFirewallMetricsService apiFirewallMetricsService)
  {
    this.apiFirewallMetricsService = apiFirewallMetricsService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getFirewallMetrics() {
    return apiFirewallMetricsService.getFirewallMetrics();
  }
}
