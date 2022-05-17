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
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.137
 */
@Named
@Timed
@Path(value = PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2)
public class DefaultArtifactoryConnectionResource
    implements ApiArtifactoryConnectionResourceV2
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
  public DefaultArtifactoryConnectionResource(final ApiArtifactoryConnectionService artifactoryConnectionService) {
    this.artifactoryConnectionService = artifactoryConnectionService;
  }

  @POST
  @Override
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  @Path(BY_OWNER)
  public ApiArtifactoryConnectionDTO addArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiArtifactoryConnectionDTO artifactoryConnection)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.addArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnection);
  }

  @Override
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  @Path(BY_ARTIFACTORY)
  public ApiArtifactoryConnectionDTO updateArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId,
      ApiArtifactoryConnectionDTO artifactoryConnection)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.updateArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionId,
        artifactoryConnection);
  }

  @Override
  @DELETE
  @Path(BY_ARTIFACTORY)
  @Audited(AuditEvent.DELETE_ARTIFACTORY_CONNECTION)
  public void deleteArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    artifactoryConnectionService.deleteArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionId);
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_ARTIFACTORY)
  public ApiArtifactoryConnectionDTO getArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.getArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionId);
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  public ApiOwnerArtifactoryConnectionDTO getOwnerArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    checkArtifactoryIntegrationEnabled();
    return artifactoryConnectionService.getOwnerArtifactoryConnection(ownerType, internalOwnerId, inherit);
  }

  @Override
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER_TEST_PATH)
  public ApiStatusDTO testArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiArtifactoryConnectionDTO artifactoryConnectionDTO)
  {
    checkArtifactoryIntegrationEnabled();
    Status status =
        artifactoryConnectionService.testArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionDTO);
    return ApiStatusDTO.fromStatus(status);
  }

  @Override
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_ARTIFACTORY_TEST_PATH)
  public ApiStatusDTO testArtifactoryConnection(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("artifactoryConnectionId") String artifactoryConnectionId)
  {
    checkArtifactoryIntegrationEnabled();
    Status status =
        artifactoryConnectionService.testArtifactoryConnection(ownerType, internalOwnerId, artifactoryConnectionId);
    return ApiStatusDTO.fromStatus(status);
  }

  private void checkArtifactoryIntegrationEnabled() {
    if (!SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId()
          + " feature is disabled");
    }
  }

  @Override
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @Audited(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION)
  public void updateOwnerArtifactoryConnectionStatus(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiArtifactoryConnectionStatusRequestDTO artifactoryConnectionStatusDTO)
  {
    checkArtifactoryIntegrationEnabled();
    artifactoryConnectionService
        .updateOwnerArtifactoryConnectionStatus(ownerType, internalOwnerId, artifactoryConnectionStatusDTO);
  }
}
