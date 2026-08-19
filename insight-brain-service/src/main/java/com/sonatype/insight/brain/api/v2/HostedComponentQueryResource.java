/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentListDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import com.codahale.metrics.annotation.Timed;

@Named
@Singleton
@Timed
@Path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class HostedComponentQueryResource
{
  private static final int MAX_PAGE_SIZE = 100;

  static final String COMPONENTS_PATH = "{repositoryManagerId}/{repositoryId}/components";

  private final ApiRepositoryComponentsService apiRepositoryComponentsService;

  @Inject
  public HostedComponentQueryResource(final ApiRepositoryComponentsService apiRepositoryComponentsService) {
    this.apiRepositoryComponentsService = apiRepositoryComponentsService;
  }

  @GET
  @Path(COMPONENTS_PATH)
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ApiHostedRepositoryComponentListDTO getComponents(
      @PathParam("repositoryManagerId") String rmId,
      @PathParam("repositoryId") String repoId,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("pageSize") @DefaultValue("25") int pageSize,
      @QueryParam("filter") String filter)
  {
    return apiRepositoryComponentsService.getComponents(rmId, repoId, page, Math.min(pageSize, MAX_PAGE_SIZE), filter);
  }

  @GET
  @Path(COMPONENTS_PATH + "/{componentId}")
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ApiHostedRepositoryComponentDTO getComponent(
      @PathParam("repositoryManagerId") String rmId,
      @PathParam("repositoryId") String repoId,
      @PathParam("componentId") String componentId)
  {
    return apiRepositoryComponentsService.getComponent(rmId, repoId, componentId);
  }

  @GET
  @Path(COMPONENTS_PATH + "/{componentId}/violations")
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ApiComponentViolationListDTO getViolations(
      @PathParam("repositoryManagerId") String rmId,
      @PathParam("repositoryId") String repoId,
      @PathParam("componentId") String componentId)
  {
    return apiRepositoryComponentsService.getViolations(rmId, repoId, componentId);
  }
}
