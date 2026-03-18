/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifiersDTO;
import com.sonatype.insight.brain.api.v2.service.ApiHashComponentIdentifierService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Claim Components", description = "Use this REST API to manage components that are developed in-house " +
    "and are not open-source." +
    "\n" +
    "\n" +
    "Claiming the component stores the identity information for the component hash and " +
    "avoids triggering the Component-Unknown policy." +
    "\n" +
    "\n" +
    "Components will have a match state as `Exact` and Identification Source as `Manual`," +
    "\n" +
    "for subsequent scans or evaluations.")
/**
 * @since 1.85
 */
@Named
@Timed
@Path(value = PublicApiPaths.CLAIM_PATH_V2)
public class ApiHashComponentIdentifierResource
{
  private final ApiHashComponentIdentifierService apiHashComponentIdentifierService;

  @Inject
  public ApiHashComponentIdentifierResource(
      ApiHashComponentIdentifierService apiHashComponentIdentifierService)
  {
    this.apiHashComponentIdentifierService = apiHashComponentIdentifierService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{hash}")
  @Operation(description = "Use this method to retrieve details of a claimed component by specifying its hash." +
      "\n" +
      "\n" +
      "Permissions required: Claim components",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains the truncated SHA1 hash of the component, the datetime when " +
                "the component was published (not the time it was claimed), the format and coordinates of " +
                "the claimed component (componentIdentifier) and the package URL of the claimed component.",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "404",
            description = "Component Claim for this hash does not exist."

        )
      })
  public ApiHashComponentIdentifierDTO get(
      @Parameter(description = "The hash of the claimed component.", required = true) @PathParam("hash") String hash)
  {
    return apiHashComponentIdentifierService.get(hash);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve details of all claimed components." +
      "\n" +
      "\n" +
      "Permissions required: Claim components",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains the truncated SHA1 hash of each component, the datetime when " +
                "the component was published (not the time it was claimed), the format and coordinates of " +
                "the claimed component (componentIdentifier) and the package URL of the claimed component.",
            useReturnTypeSchema = true)
      })
  public ApiHashComponentIdentifiersDTO getAll() {
    return apiHashComponentIdentifierService.getAll();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to claim a component, or update the component details for a previously " +
      "claimed component." +
      "\n" +
      "\n" +
      "Permissions required: Claim components",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response shows the new/updated details for the claimed component.",
            useReturnTypeSchema = true)
      })
  @Audited(AuditEvent.SET_COMPONENT_IDENTITY)
  public ApiHashComponentIdentifierDTO set(
      @RequestBody(description = "Specify the hash (required), comment (optional), createTime (optional), and the" +
          " component identifier/package URL (required) with non-null/non-empty format and coordinates, " +
          " for the component to be claimed.",
          required = true) ApiHashComponentIdentifierDTO hashComponentIdentifier)
  {
    return apiHashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @DELETE
  @Path("{hash}")
  @Operation(description = "Use this method to delete a claim on a previously claimed component by providing its " +
      "hash." +
      "\n" +
      "\n" +
      "Permissions required: Claim components",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Component Claim for this hash was deleted."),
        @ApiResponse(
            responseCode = "404",
            description = "Component Claim for this hash does not exist.")
      })
  @Audited(AuditEvent.UNSET_COMPONENT_IDENTITY)
  public void delete(
      @Parameter(description = "Enter the SHA1 hash for the component.",
          required = true) @PathParam("hash") String hash)
  {
    apiHashComponentIdentifierService.delete(hash);
  }
}
