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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.sourcecontrol.ApiSourceControlRepositoryUserDTO;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * @since 1.66
 */
@Named
@Timed
@Path(value = PublicApiPaths.SOURCE_CONTROL_PATH_V2)
public class ApiSourceControlResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  /* paths are package private for use in tests */
  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  private final ApiSourceControlService sourceControlService;

  @Inject
  public ApiSourceControlResource(final ApiSourceControlService apiSourceControlService) {
    this.sourceControlService = apiSourceControlService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ApiSourceControlDTO getSourceControl(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    return sourceControlService.getSourceControlByOwner(ownerType, internalOwnerId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_SOURCE_CONTROL)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ApiSourceControlDTO addSourceControl(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiSourceControlDTO sourceControl)
  {
    return sourceControlService.addSourceControlByOwner(ownerType, internalOwnerId, sourceControl);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_SOURCE_CONTROL)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ApiSourceControlDTO updateSourceControl(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      ApiSourceControlDTO sourceControl)
  {
    return sourceControlService.updateSourceControlByOwner(ownerType, internalOwnerId, sourceControl);
  }

  @DELETE
  @Path(BY_OWNER)
  @Audited(AuditEvent.DELETE_SOURCE_CONTROL)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public void deleteSourceControl(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    sourceControlService.deleteSourceControlByOwner(ownerType, internalOwnerId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.AUTO_CREATE_SOURCE_CONTROL)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Hidden
  public ApiSourceControlDTO addOrUpdateSourceControl(
      @QueryParam("publicId") final String publicId,
      @QueryParam("repositoryUrl") final String repositoryUrl,
      final ApiSourceControlRepositoryUserDTO apiSourceControlRepoUserDTO)
  {
    return sourceControlService.addOrUpdateSourceControl(publicId,
        repositoryUrl, apiSourceControlRepoUserDTO);
  }
}
