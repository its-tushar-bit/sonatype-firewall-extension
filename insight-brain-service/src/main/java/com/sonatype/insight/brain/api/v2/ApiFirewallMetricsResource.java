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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.170.0
 */
@Named
@Timed
@Path(ApiFirewallMetricsResource.RESOURCE_PATH)
@Tag(name = "Malware-Defense",
    description = "Use this REST API to view metrics for malware defense.")
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
  @Operation(description = "Use this method to retrieve malware defense dashboard metrics." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains a map of malware defense metric name to value including the last " +
                  "updated time."
          )
      })
  @Produces(MediaType.APPLICATION_JSON)
  public Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getFirewallMetrics() {
    return apiFirewallMetricsService.getFirewallMetrics();
  }
}
