/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeStatusResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiFirewallCascadeService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST resource for managing cascade re-evaluation operations across repository hierarchies.
 *
 * @since 1.196
 */
@Named
@Singleton
@Timed
@Path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallCascadeResource
{
  private final ApiFirewallCascadeService apiFirewallCascadeService;

  @Inject
  public ApiFirewallCascadeResource(final ApiFirewallCascadeService apiFirewallCascadeService) {
    this.apiFirewallCascadeService = apiFirewallCascadeService;
  }

  @POST
  @Path("/componentHash/{componentHash}")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.INITIATE_CASCADE_REEVALUATION)
  @Operation(
      description = "Initiate cascade re-evaluation for a component across repository hierarchies." +
          "<p>" +
          "This operation asynchronously re-evaluates the specified component across all repositories where the " +
          "component exists."
          + "<p>" +
          "The system will automatically discover all eligible repositories based on component presence."
          + "<p>" +
          "Permissions Required: Evaluate Components at Repository Managers level",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Cascade re-evaluation initiated successfully. The response contains statusUrl with a " +
                "requestId, which can be used to check the cascade re-evaluation status using the GET method." +
                "A requestId for a cascade re-evaluation only lasts 24 hours before being deleted.",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"),
      })
  public CascadeReevaluateTicketDTO initiateCascadeReevaluation(
      @Parameter(description = "The component hash to re-evaluate across all repositories",
          required = true) @PathParam("componentHash") final String componentHash)
  {
    // Initiate cascade re-evaluation
    return apiFirewallCascadeService.initiateCascadeReevaluation(
        componentHash);

  }

  @GET
  @Path("/status/{requestId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Get the status of a cascade re-evaluation request." +
          "<p>" +
          "Returns the current progress of a cascade re-evaluation operation including " +
          "the list of components that have been evaluated and those still pending. " +
          "The overall status will be 'pending' if any components are still being processed, " +
          "or 'completed' if all components have been evaluated." +
          "<p>" +
          "The response includes:" +
          "<ul>" +
          "<li><b>status:</b> Overall status (PENDING, IN_PROGRESS, COMPLETED, NO_COMPONENTS_FOUND, FAILED)</li>" +
          "<li><b>referenceComponentHash:</b> The component hash that was re-evaluated</li>" +
          "<li><b>pending:</b> Components still being processed (PENDING status)</li>" +
          "<li><b>evaluated:</b> Components successfully re-evaluated (COMPLETED or NO_COMPONENTS_FOUND status)</li>" +
          "<li><b>failed:</b> Components that could not be re-evaluated (FAILED status)</li>" +
          "</ul>"
          + "<p>" +
          "Permissions Required: Evaluate Components at Repository Managers level",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Cascade status retrieved successfully",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "404",
            description = "Cascade request not found. It could have been deleted after 24 hours."),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions")
      })
  public CascadeStatusResponseDTO getCascadeStatus(
      @Parameter(description = "The cascade request ID to check status for",
          required = true) @PathParam("requestId") final String requestId)
  {
    return apiFirewallCascadeService.getCascadeStatus(requestId);
  }
}
