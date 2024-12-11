/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.StatusType;

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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.137
 */
@Named
@Timed
@Path(value = PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2)
@Tag(name = "Configure Artifactory Connection")
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
  @Operation(description = "Use this method to add an Artifactory connection.",
             responses = {
                 @ApiResponse(responseCode = "201",
                              description = "Response contains the details of the added Artifactory connection.",
                              useReturnTypeSchema = true)
             }
  )
  @Path(BY_OWNER)
  public ApiArtifactoryConnectionDTO addArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Provide the details of the Artifactory connection you wish to add." +
          "`isAnonymous` (Boolean) indicates if the connection is anonymous, `baseUrl` (String) is the " +
          "base URL of the Artifactory instance, `username` (String) and `password` (String) are the " +
          "username and password for the Artifactory connection.")
      ApiArtifactoryConnectionDTO artifactoryConnection)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.addArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnection);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  @Operation(description = "Use this method to update an existing Artifactory connection.",
             responses = {
                 @ApiResponse(responseCode = "200",
                              description = "The response contains the updated Artifactory connection details.",
                              useReturnTypeSchema = true)
             }
  )
  @Path(BY_ARTIFACTORY)
  public ApiArtifactoryConnectionDTO updateArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory Connection ID.")
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId,
      @Parameter(description = "Provide the details of the Artifactory connection you wish to update." +
          "`isAnonymous` (Boolean) indicates if the connection is anonymous, `baseUrl` (String) is the " +
          "base URL of the Artifactory instance, `username` (String) and `password` (String) are the " +
          "username and password for the Artifactory connection.")
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
  @Operation(description = "Use this method to delete an existing Artifactory connection.",
             responses = {
                 @ApiResponse(responseCode = "204",
                              description = "Artifactory connection deleted successfully.")
             })
  public void deleteArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory Connection ID.")
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    artifactoryConnectionService.deleteArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_ARTIFACTORY)
  @Operation(description = "Use this method to retrieve Artifactory connection information using its unique ID.",
             responses = {
                 @ApiResponse(responseCode = "200",
                              description = "The response contains the details of the Artifactory connection.",
                              useReturnTypeSchema = true)
             })
  public ApiArtifactoryConnectionDTO getArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory Connection ID.")
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
  @Operation(description = "Use this method to retrieve Artifactory connection information using its owner's ID.",
             responses = {
                 @ApiResponse(responseCode = "200",
                              description = "The response contains the details of the Artifactory connection.",
                              useReturnTypeSchema = true)
             })
  public ApiOwnerArtifactoryConnectionDTO getOwnerArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Specify whether to inherit the Artifactory connection from the parent " +
          "organization.")
      @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.getOwnerArtifactoryConnection(ownerType, internalOwnerId, inherit);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER_TEST_PATH)
  @Operation(description = "Use this method to test an Artifactory connection for a specified owner.",
             responses = {
                 @ApiResponse(responseCode = "200",
                              description = "Artifactory connection is valid.",
                              useReturnTypeSchema = true)
             })
  public ApiStatusDTO testArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Provide the details of the Artifactory connection you wish to test." +
          "`isAnonymous` (Boolean) indicates if the connection is anonymous, `baseUrl` (String) is the " +
          "base URL of the Artifactory instance, `username` (String) and `password` (String) are the " +
          "username and password for the Artifactory connection.")
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
  @Operation(description = "Use this method to test an existing Artifactory connection using its unique ID.",
             responses = {
                 @ApiResponse(responseCode = "200",
                              description = "Artifactory connection is valid.",
                              useReturnTypeSchema = true)
             })
  public ApiStatusDTO testArtifactoryConnection(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the Artifactory Connection ID.")
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
  @Operation(description = "Use this method to update the status of the effective Artifactory connection for " +
      "the specified owner.",
             responses = {
                 @ApiResponse(responseCode = "204",
                              description = "Artifactory connection status successfully updated.")
             })
  public void updateOwnerArtifactoryConnectionStatus(
      @Parameter(description = "Specify the type of owner: `application` or `organization`.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ID of the owner.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Provide the details of the Artifactory connection you wish to update." +
          "`isAnonymous` (Boolean) indicates if the connection is anonymous, `baseUrl` (String) is the " +
          "base URL of the Artifactory instance, `username` (String) and `password` (String) are the " +
          "username and password for the Artifactory connection.")
      ApiArtifactoryConnectionStatusRequestDTO artifactoryConnectionStatusDTO)
  {
    checkArtifactoryIntegrationEnabled();
    artifactoryConnectionService
        .updateOwnerArtifactoryConnectionStatus(ownerType, internalOwnerId, artifactoryConnectionStatusDTO);
  }
}
