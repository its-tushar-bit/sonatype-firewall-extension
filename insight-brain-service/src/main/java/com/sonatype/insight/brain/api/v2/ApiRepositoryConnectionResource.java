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
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryConnectionService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * @since 1.127
 */
@Named
@Timed
@Path(value = PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2)
@Hidden
@ProductLicenseEnforcementPoint(LicensedFeature.INNER_SOURCE_REPOSITORIES)
public class ApiRepositoryConnectionResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  private static final String REPOSITORY_CONNECTION_ID = "{repositoryConnectionId}";

  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  static final String BY_REPOSITORY = BY_OWNER + "/" + REPOSITORY_CONNECTION_ID;

  static final String BY_OWNER_TEST_PATH = BY_OWNER + "/test";

  static final String BY_REPOSITORY_TEST_PATH = BY_REPOSITORY + "/test";

  private final ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  public ApiRepositoryConnectionResource(final ApiRepositoryConnectionService repositoryConnectionService) {
    this.repositoryConnectionService = repositoryConnectionService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION)
  @Path(BY_OWNER)
  public ApiRepositoryConnectionDTO addRepositoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiRepositoryConnectionDTO repositoryConnection)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    return repositoryConnectionService.addRepositoryConnection(ownerType, internalOwnerId, repositoryConnection);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION)
  @Path(BY_REPOSITORY)
  public ApiRepositoryConnectionDTO updateRepositoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("repositoryConnectionId") String repositoryConnectionId,
      ApiRepositoryConnectionDTO repositoryConnection)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    return repositoryConnectionService.updateRepositoryConnection(ownerType, internalOwnerId, repositoryConnectionId,
        repositoryConnection);
  }

  @DELETE
  @Path(BY_REPOSITORY)
  @Audited(AuditEvent.DELETE_REPOSITORY_CONNECTION)
  public void deleteRepositoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("repositoryConnectionId") String repositoryConnectionId)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    repositoryConnectionService.deleteRepositoryConnection(ownerType, internalOwnerId, repositoryConnectionId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_REPOSITORY)
  public ApiRepositoryConnectionDTO getRepositoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("repositoryConnectionId") String repositoryConnectionId)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    return repositoryConnectionService.getRepositoryConnection(ownerType, internalOwnerId, repositoryConnectionId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  public ApiOwnerRepositoryConnectionsDTO getOwnerRepositoryConnections(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    return repositoryConnectionService.getOwnerRepositoryConnections(ownerType, internalOwnerId, inherit);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER_TEST_PATH)
  public ApiStatusDTO testRepositoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiRepositoryConnectionDTO repositoryConnectionDTO)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    StatusType status =
        repositoryConnectionService.testRepositoryConnection(ownerType, internalOwnerId, repositoryConnectionDTO);
    return ApiStatusDTO.fromStatusType(status);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_REPOSITORY_TEST_PATH)
  public ApiStatusDTO testRepositoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("repositoryConnectionId") String repositoryConnectionId)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    StatusType status =
        repositoryConnectionService.testRepositoryConnection(ownerType, internalOwnerId, repositoryConnectionId);
    return ApiStatusDTO.fromStatusType(status);
  }

  private void checkInnerSourceRepositoryIntegrationEnabled() {
    if (!SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.isEnabled()) {
      throw new NotAuthorizedException(
          SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
    }
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION)
  public void updateOwnerRepositoryConnectionStatus(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiRepositoryConnectionStatusRequestDTO repositoryConnectionStatusDTO)
  {
    checkInnerSourceRepositoryIntegrationEnabled();
    repositoryConnectionService
        .updateOwnerRepositoryConnectionStatus(ownerType, internalOwnerId, repositoryConnectionStatusDTO);
  }
}
