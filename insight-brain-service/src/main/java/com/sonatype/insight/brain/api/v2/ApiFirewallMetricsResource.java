/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.roi.dto.RoiFirewallMetricsDTO;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.170.0
 */
@Named
@Timed
@Path(ApiFirewallMetricsResource.RESOURCE_PATH)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallMetricsResource
{
  public static final String RESOURCE_PATH = "/api/v2/firewall/metrics/embedded";

  public static final String ROI_FIREWALL_METRICS_PATH = "/roi-firewall-metrics/{currencyType}";

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  @Inject
  public ApiFirewallMetricsResource(
      ApiFirewallMetricsService apiFirewallMetricsService)
  {
    this.apiFirewallMetricsService = apiFirewallMetricsService;
  }

  @GET
  @Operation(description = "Use this method to retrieve firewall dashboard metrics." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains a map of firewall metric name to value including the last " +
                "updated time.",
            useReturnTypeSchema = true)
      })
  @Produces(MediaType.APPLICATION_JSON)
  public Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getFirewallMetrics() {
    return apiFirewallMetricsService.getFirewallMetrics();
  }

  @GET
  @Path(ROI_FIREWALL_METRICS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Hidden
  @Operation(description = "Use this method to retrieve ROI firewall metrics for the specified currency type." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved ROI firewall metrics.",
            useReturnTypeSchema = true)
      })
  public RoiFirewallMetricsDTO getRoiFirewallMetrics(
      @Parameter(description = "The currency to use for the ROI firewall metrics.",
          required = true) @PathParam("currencyType") String currencyType)
  {
    return apiFirewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.fromString(currencyType));
  }
}
