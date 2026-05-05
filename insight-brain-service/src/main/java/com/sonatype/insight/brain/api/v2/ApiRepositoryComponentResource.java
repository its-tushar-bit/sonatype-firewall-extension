/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API for hosted repository component cleanup operations.
 * <p>
 * Provides endpoints for NXRM to remove hosted repository components from IQ
 * when artifacts are deleted or monitoring is disabled.
 *
 * @since 1.203
 */
@Named
@Singleton
@Path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
@Tag(name = "Repository Components", description = "Hosted repository component management")
@Timed
public class ApiRepositoryComponentResource
{
  private static final String DELETE_COMPONENTS_PATH =
      "{repositoryManagerInstanceId}/components";

  private static final String DELETE_REPOSITORIES_PATH =
      "{repositoryManagerInstanceId}/repositories";

  private final ApiRepositoryComponentService apiRepositoryComponentService;

  @Inject
  public ApiRepositoryComponentResource(final ApiRepositoryComponentService apiRepositoryComponentService) {
    this.apiRepositoryComponentService = apiRepositoryComponentService;
  }

  /**
   * Delete specific hosted repository components by their IDs.
   */
  @DELETE
  @Path(DELETE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.REMOVE_REPOSITORY)
  @Operation(
      description = "Delete specific hosted repository components from IQ by their component IDs. " +
          "Cancels pending scan queue jobs and removes the components and all associated data. " +
          "<p>" +
          "Permissions Required: Configure IQ Server",
      responses = {
        @ApiResponse(responseCode = "204", description = "Components deleted"),
        @ApiResponse(responseCode = "404", description = "Repository manager or component not found")
      })
  public void deleteComponents(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      final List<String> componentIds)
  {
    apiRepositoryComponentService.deleteComponents(repositoryManagerInstanceId, componentIds);
  }

  /**
   * Delete all components for the specified hosted repositories.
   */
  @DELETE
  @Path(DELETE_REPOSITORIES_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.REMOVE_REPOSITORY)
  @Operation(
      description = "Delete all components from the specified hosted repositories in IQ. " +
          "Cancels all pending scan queue jobs and removes all components and associated data. " +
          "<p>" +
          "Permissions Required: Configure IQ Server",
      responses = {
        @ApiResponse(responseCode = "204", description = "All components deleted for specified repositories"),
        @ApiResponse(responseCode = "404", description = "Repository manager or repository not found")
      })
  public void deleteRepositoryComponents(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      final List<String> repositoryPublicIds)
  {
    apiRepositoryComponentService.deleteRepositoryComponents(repositoryManagerInstanceId, repositoryPublicIds);
  }
}
