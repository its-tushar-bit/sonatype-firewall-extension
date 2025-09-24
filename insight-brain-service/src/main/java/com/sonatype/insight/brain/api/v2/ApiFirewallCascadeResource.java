/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeStatusResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiFirewallCascadeService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
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
@Path(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
@Hidden
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
          "This operation asynchronously re-evaluates the specified component across all repositories " +
          "where the user has EVALUATE_COMPONENT permission at the Repository Managers level and the component exists."
          + "<p>" +
          "The system will automatically discover all eligible repositories based on user permissions " +
          "and component presence.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Cascade re-evaluation initiated successfully. Returns the task ID for tracking progress.",
              useReturnTypeSchema = true
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden - insufficient permissions or no accessible repositories found"
          ),
      }
  )
  public CascadeReevaluateTicketDTO initiateCascadeReevaluation(
      @Parameter(description = "The component hash to re-evaluate across all accessible repositories", required = true)
      @PathParam("componentHash") final String componentHash)
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
          "<li><b>status:</b> Overall status ('pending' or 'completed')</li>" +
          "<li><b>referenceComponentHash:</b> The component hash that was re-evaluated</li>" +
          "<li><b>evaluated:</b> List of components that have been processed (COMPLETED or FAILED status)</li>" +
          "<li><b>pending:</b> List of components still being processed (PENDING status)</li>" +
          "</ul>",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Cascade status retrieved successfully",
              useReturnTypeSchema = true
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Cascade request not found"
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden - insufficient permissions"
          )
      }
  )
  public CascadeStatusResponseDTO getCascadeStatus(
      @Parameter(description = "The cascade request ID to check status for", required = true)
      @PathParam("requestId") final String requestId)
  {
    return apiFirewallCascadeService.getCascadeStatus(requestId);
  }
}
