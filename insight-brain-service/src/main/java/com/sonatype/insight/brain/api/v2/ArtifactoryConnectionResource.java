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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.137
 */
@Named
@Timed
@Path(value = PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2)
@Tag(name = "Configure Artifactory Connection",
    description = "Use this REST API to manage the configuration of Firewall for JFrog Artifactory.")
public class ArtifactoryConnectionResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  private static final String ARTIFACTORY_CONNECTION_ID = "{artifactoryConnectionId}";

  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  static final String BY_ARTIFACTORY = BY_OWNER + "/" + ARTIFACTORY_CONNECTION_ID;

  static final String BY_OWNER_TEST_PATH = BY_OWNER + "/test";

  static final String BY_ARTIFACTORY_TEST_PATH = BY_ARTIFACTORY + "/test";

  private final ApiArtifactoryConnectionService artifactoryConnectionService;

  @Inject
  public ArtifactoryConnectionResource(final ApiArtifactoryConnectionService artifactoryConnectionService) {
    this.artifactoryConnectionService = artifactoryConnectionService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  @Operation(description = "Use this method to add a new Artifactory connection." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of the added Artifactory connection.",
              useReturnTypeSchema = true)
      }
  )
  @Path(BY_OWNER)
  public ApiArtifactoryConnectionDTO addArtifactoryConnection(
      @Parameter(description = "Select the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @RequestBody(description = "Enter values for the new Artifactory connection." +
          "<ul>" +
          "<li>`isAnonymous` indicates if the connection is anonymous.</li>" +
          "<li>`baseUrl` is the baseURL of the Artifactory instance.</li>" +
          "<li>`username` and `password` to authenticate the Artifactory connection.</li>" +
          "</ul>", required = true, useParameterTypeSchema = true
      )
      ApiArtifactoryConnectionDTO artifactoryConnection)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.addArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnection);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  @Operation(description = "Use this method to update an existing Artifactory connection." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the updated Artifactory connection details.",
              useReturnTypeSchema = true)
      }
  )
  @Path(BY_ARTIFACTORY)
  public ApiArtifactoryConnectionDTO updateArtifactoryConnection(
      @Parameter(description = "Specify the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory connection ID.")
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId,
      @RequestBody(description = "Enter values for the new Artifactory connection." +
          "<ul>" +
          "<li>`isAnonymous` indicates if the connection is anonymous.</li>" +
          "<li>`baseUrl` is the baseURL of the Artifactory instance.</li>" +
          "<li>`username` and `password` to authenticate the Artifactory connection.</li>" +
          "</ul>", required = true, useParameterTypeSchema = true)
      ApiArtifactoryConnectionDTO artifactoryConnection)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.updateArtifactoryConnection(ownerType,
        internalOwnerId,
        artifactoryConnectionId,
        artifactoryConnection);
  }

  @DELETE
  @Path(BY_ARTIFACTORY)
  @Audited(AuditEvent.DELETE_ARTIFACTORY_CONNECTION)
  @Operation(description = "Use this method to delete an existing Artifactory connection." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Artifactory connection deleted successfully.")
      })
  public void deleteArtifactoryConnection(
      @Parameter(description = "Select the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory connection ID.")
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    artifactoryConnectionService.deleteArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_ARTIFACTORY)
  @Operation(description = "Use this method to retrieve details for an Artifactory connection." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of the requested Artifactory connection.",
              useReturnTypeSchema = true)
      })
  public ApiArtifactoryConnectionDTO getArtifactoryConnection(
      @Parameter(description = "Select the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory connection ID.")
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.getArtifactoryConnection(ownerType,
        internalOwnerId,
        artifactoryConnectionId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @Operation(description = "Use this method to retrieve Artifactory connection details by " +
      "specifying the owner Id." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of the Artifactory connection.",
              useReturnTypeSchema = true)
      })
  public ApiOwnerArtifactoryConnectionDTO getOwnerArtifactoryConnection(
      @Parameter(description = "Select the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Specify whether to include details from an inherited Artifactory connection.")
      @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.getOwnerArtifactoryConnection(ownerType, internalOwnerId, inherit);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER_TEST_PATH)
  @Operation(description = "Use this method to test an Artifactory connection for the specified owner." +
      "\n" +
      "\n" +
      "Permissons required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "The response contains the `code` and `message` indicating the status of the connection.",
              useReturnTypeSchema = true)
      })
  public ApiStatusDTO testArtifactoryConnection(
      @Parameter(description = "Select the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @RequestBody(description = "Enter values for the Artifactory connection." +
          "<ul>" +
          "<li>`baseUrl` is the baseURL of the Artifactory instance.</li>" +
          "<li>`username` and `password` to authenticate the Artifactory connection.</li>" +
          "</ul>", required = true, useParameterTypeSchema = true
      )
      ApiArtifactoryConnectionDTO artifactoryConnectionDTO)
  {
    checkArtifactoryIntegrationEnabled();
    StatusType status =
        artifactoryConnectionService.testArtifactoryConnection(ownerType,
            internalOwnerId,
            artifactoryConnectionDTO);
    return ApiStatusDTO.fromStatusType(status);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_ARTIFACTORY_TEST_PATH)
  @Operation(description = "Use this method to test an existing Artifactory connection using the connection ID.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the `code` and `message` indicating the status of the connection.",
              useReturnTypeSchema = true)
      })
  public ApiStatusDTO testArtifactoryConnection(
      @Parameter(description = "Enter the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory connection ID.")
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    StatusType status =
        artifactoryConnectionService.testArtifactoryConnection(ownerType,
            internalOwnerId,
            artifactoryConnectionId);
    return ApiStatusDTO.fromStatusType(status);
  }

  private void checkArtifactoryIntegrationEnabled() {
    if (!SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId()
          + " feature is disabled");
    }
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  @Operation(description = "Use this method to enable/disable an existing Artifactory connection for " +
      "the specified owner." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Artifactory connection status successfully updated.")
      })
  public void updateOwnerArtifactoryConnectionStatus(
      @Parameter(description = "Select the owner type.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @RequestBody(description = "Set values for the connection properties `enabled` and `allowOverride`.",
          required = true, useParameterTypeSchema = true)
      ApiArtifactoryConnectionStatusRequestDTO artifactoryConnectionStatusDTO)
  {
    checkArtifactoryIntegrationEnabled();
    artifactoryConnectionService
        .updateOwnerArtifactoryConnectionStatus(ownerType, internalOwnerId, artifactoryConnectionStatusDTO);
  }
}
