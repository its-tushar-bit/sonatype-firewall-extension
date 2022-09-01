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
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryConnectionService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.127
 */
@Named
@Timed
@Path(value = PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2)
@Tag(name = "Config Repository Connection")
public class DefaultRepositoryConnectionResource
    implements ApiRepositoryConnectionResourceV2
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
  public DefaultRepositoryConnectionResource(final ApiRepositoryConnectionService repositoryConnectionService) {
    this.repositoryConnectionService = repositoryConnectionService;
  }

  @POST
  @Override
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

  @Override
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

  @Override
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

  @Override
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

  @Override
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

  @Override
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

  @Override
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

  @Override
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
