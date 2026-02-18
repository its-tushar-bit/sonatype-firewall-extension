/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiOrganizationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.organization.MoveOrganizationService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.11.0
 */
@Named
@Timed
@Path(PublicApiPaths.ORG_RESOURCE_PATH)
@Tag(name = "Organizations",
    description = "Use this REST API to create new organizations, retrieve, edit " +
        "or delete existing organizations.")
public class ApiOrganizationResourceV2
{
  public static final String ORGANIZATION_ID = "{organizationId}";

  public static final String MOVE_ORGANIZATION_PATH = ORGANIZATION_ID + "/move/destination/{destinationId}";

  private final ApiOrganizationService apiOrganizationService;

  private final MoveOrganizationService moveOrganizationService;

  @Inject
  public ApiOrganizationResourceV2(
      final ApiOrganizationService apiOrganizationService,
      final MoveOrganizationService moveOrganizationService)
  {
    this.apiOrganizationService = apiOrganizationService;
    this.moveOrganizationService = moveOrganizationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve organizations with names matching those specified or " +
      "all if not specified." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains a list of organizations. For each " +
                  "organization the response contains organization id, organization name, " +
                  "parent organization id, and its associated tags with additional details.",
              useReturnTypeSchema = true
          )
      })
  public ApiOrganizationListDTO getOrganizations(
      @Parameter(description = "Enter the organization names.")
      @QueryParam("organizationName") Set<String> organizationNames)
  {
    return apiOrganizationService.getOrganizations(organizationNames);
  }

  /**
   * @since 1.201
   */
  @GET
  @Path("/byid")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = """
      Use this method to retrieve organizations by their internal IDs.

      Permissions required: View IQ Elements""",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains a list of organizations. For each " +
                  "organization the response contains organization id, organization name, " +
                  "and parent organization id.",
              useReturnTypeSchema = true
          )
      })
    public ApiOrganizationListDTO getOrganizationsByIds(
      @Parameter(description = "Enter the internal organization IDs.")
      @QueryParam("id") Set<String> ids)
  {
    return apiOrganizationService.getOrganizationsByIds(ids);
  }

  /**
   * @since 1.81
   */
  @GET
  @Path(ORGANIZATION_ID)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the details of an organization by providing the organization " +
      "id." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains the details for the specified  " +
                  "organization including organization id, organization name, " +
                  "parent organization id, and its associated tags with additional details.",
              useReturnTypeSchema = true
          )
      })
  public ApiOrganizationDTO getOrganization(
      @Parameter(description = "Enter the organization id.", required = true)
      @PathParam("organizationId") String organizationId)
  {
    return apiOrganizationService.getOrganizationById(organizationId);
  }

  /**
   * @since 1.42
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ORGANIZATION)
  @Operation(description = "Use this method to add a new organization." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains the assigned organization id and all other organization " +
                  "details specified.",
              useReturnTypeSchema = true
          )
      })
  public ApiOrganizationDTO addOrganization(
      @RequestBody(
          description = "The request JSON should include the name of the organization (should be unique), " +
              "name of the parent organization and tags containing additional organization details. " +
              "If the parent organization is not specified, this organization will be created under the root " +
              "organization. " +
              "Tags represent identifying characteristics of an application. They are created at the organization " +
              "level and then applied to applications under the organization. The tags can be used to decide which " +
              "applications will be evaluated against a selected policy."

      ) final ApiOrganizationDTO organizationDTO)
  {
    return apiOrganizationService.addOrganization(organizationDTO);
  }

  /**
   * @since 1.161
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Path(MOVE_ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_ORGANIZATION)
  @Operation(description = "Use this method to change the parent organization." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The organization has been successfully moved under the parent organization id provided.",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = MoveOrganizationResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "409",
              description = "Encountered conflicts while inheriting policy elements of the new parent organization. " +
                  "The organization could not be moved under the new parent organization id provided.",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = MoveOrganizationResponseDTO.class)
              )
          )
      }
  )
  public Response moveOrganization(
      @Parameter(description = "Enter the id for the organization to be moved under the new parent.")
      @PathParam("organizationId") final String orgId,
      @Parameter(description = "Enter the id for the new parent organization.")
      @PathParam("destinationId") final String newParentOrgId,
      @DefaultValue("false") @QueryParam("failEarlyOnError") final boolean failEarlyOnError
  )
  {
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(orgId, newParentOrgId, failEarlyOnError);

    if (!moveOrganizationResponseDTO.errors.isEmpty()) {
      return Response.status(Status.CONFLICT)
          .entity(moveOrganizationResponseDTO)
          .build();
    }
    return Response.status(Status.OK)
        .entity(moveOrganizationResponseDTO)
        .build();
  }

  @DELETE
  @Path(ORGANIZATION_ID)
  @Audited(AuditEvent.DELETE_ORGANIZATION)
  @Operation(description = "Use this method to delete an existing organization, by providing the organization id." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "The specified organization has been deleted."
          )
      })
  public void deleteOrganization(
      @Parameter(description = "Enter the organization id to be deleted.")
      @PathParam("organizationId") final String organizationId) throws IOException
  {
    apiOrganizationService.deleteOrganization(organizationId);
  }
}
