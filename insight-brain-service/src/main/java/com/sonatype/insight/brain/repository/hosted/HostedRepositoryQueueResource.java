/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.ApiRepositoryComponentsService;
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiQueueStatsDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import com.codahale.metrics.annotation.Timed;

@Named
@Singleton
@Timed
@Path("api/v2/repositories/{repositoryManagerId}/{repositoryId}/queue")
@Produces(MediaType.APPLICATION_JSON)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class HostedRepositoryQueueResource
{
  private final ApiRepositoryComponentsService apiRepositoryComponentsService;

  @Inject
  public HostedRepositoryQueueResource(final ApiRepositoryComponentsService apiRepositoryComponentsService) {
    this.apiRepositoryComponentsService = apiRepositoryComponentsService;
  }

  @GET
  @Path("stats")
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ApiQueueStatsDTO getQueueStats(
      @PathParam("repositoryManagerId") String rmId,
      @PathParam("repositoryId") String repoId)
  {
    return apiRepositoryComponentsService.getQueueStats(rmId, repoId);
  }
}
