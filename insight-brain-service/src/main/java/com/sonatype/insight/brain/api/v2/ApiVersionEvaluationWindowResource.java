/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowsDTO;
import com.sonatype.insight.brain.api.v2.service.ApiVersionEvaluationWindowService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Named
@Singleton
@Timed
@Path(PublicApiPaths.VERSION_EVALUATION_WINDOW_RESOURCE_PATH)
@Hidden // Temporarily hidden until CLM-38616 is ready
@Tag(name = "Version Evaluation Window",
    description = "Manage version evaluation window configurations. " +
        "A version evaluation window determines which application versions are monitored.")
public class ApiVersionEvaluationWindowResource
{
  static final String OWNER_PATH = "{ownerType: organization|application}/{ownerId}";

  private final ApiVersionEvaluationWindowService service;

  private final IdUtils idUtils;

  @Inject
  public ApiVersionEvaluationWindowResource(
      final ApiVersionEvaluationWindowService service,
      final IdUtils idUtils)
  {
    this.service = service;
    this.idUtils = idUtils;
  }

  @GET
  @Path(OWNER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Retrieve the version evaluation window configurations for an organization or application." +
          "<p>" +
          "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The version evaluation window configurations.",
            useReturnTypeSchema = true)
      })
  public ApiVersionEvaluationWindowsDTO getVersionEvaluationWindows(
      @Parameter(description = "The owner type.", required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal or public owner id.",
          required = true) @PathParam("ownerId") final String ownerId)
  {
    return service.getVersionEvaluationWindows(idUtils.getOwnerNotNull(ownerType, ownerId));
  }

  @PUT
  @Path(OWNER_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_VERSION_EVALUATION_WINDOW)
  @Operation(
      description = "Set or update a version evaluation window configuration for an organization or application." +
          "<p>" +
          "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The version evaluation window configuration has been set successfully.")
      })
  public void setVersionEvaluationWindow(
      @Parameter(description = "The owner type.", required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal or public owner id.",
          required = true) @PathParam("ownerId") final String ownerId,
      @RequestBody(description = "The version evaluation window configuration.",
          required = true) ApiVersionEvaluationWindowDTO dto)
  {
    service.setVersionEvaluationWindow(idUtils.getOwnerNotNull(ownerType, ownerId), dto);
  }

  @DELETE
  @Path(OWNER_PATH)
  @Audited(AuditEvent.DELETE_VERSION_EVALUATION_WINDOW)
  @Operation(
      description = "Delete one or all version evaluation window configurations for an organization or application." +
          "<p>" +
          "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The version evaluation window configuration has been deleted successfully.")
      })
  public void deleteVersionEvaluationWindows(
      @Parameter(description = "The owner type.", required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal or public owner id.",
          required = true) @PathParam("ownerId") final String ownerId,
      @Parameter(description = "The context id for which to delete the version evaluation window. " +
          "If omitted, all version evaluation windows will be deleted for the given owner.") @QueryParam("contextId") final String contextId)
  {
    service.deleteVersionEvaluationWindows(idUtils.getOwnerNotNull(ownerType, ownerId), contextId);
  }
}
