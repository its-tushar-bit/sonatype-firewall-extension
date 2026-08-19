/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerListDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API for Lifecycle operations
 *
 * @since 1.198
 */
@Named
@Singleton
@Path("/api/v2/lifecycle")
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
@Tag(name = "Lifecycle", description = "Lifecycle operations")
public class ApiLifecycleResource
{
  private static final String REPOSITORY_MANAGERS_PATH = "/repositoryManagers";

  private final ApiLifecycleService apiLifecycleService;

  @Inject
  public ApiLifecycleResource(final ApiLifecycleService apiLifecycleService) {
    this.apiLifecycleService = apiLifecycleService;
  }

  /**
   * Get all repository managers with hosted repository counts
   *
   * @return List of repository managers with connection status
   */
  @GET
  @Path(REPOSITORY_MANAGERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  @Operation(
      description = "Retrieve all configured Nexus Repository Manager instances with their hosted repository counts." +
          "\n\n" +
          "**Connection Status:**\n" +
          "- `CONNECTED`: Repository manager is configured (IQ Server connection established in NXRM)\n" +
          "- `DISCONNECTED`: Repository manager is not configured (no IQ Server connection in NXRM)\n" +
          "\n" +
          "Note: This is a system-level read-only operation that requires authentication.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "List of repository managers retrieved successfully",
            useReturnTypeSchema = true)
      })
  public ApiLifecycleRepositoryManagerListDTO getRepositoryManagers() {
    return apiLifecycleService.getRepositoryManagers();
  }
}
