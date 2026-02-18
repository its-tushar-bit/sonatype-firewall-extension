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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationResponseDto;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.ci.CiConfigurationService;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API resource for managing CI integration configurations.
 * Supports hierarchical configuration inheritance from the organization hierarchy.
 *
 * @since 1.201
 */
@Named
@Timed
@Path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "CI Configuration",
    description = """
        Use this REST API to manage CI integration configuration. Configurations can be set at organization or \
        application level and are merged from the organization hierarchy with lower levels taking precedence.""")
@Produces(MediaType.APPLICATION_JSON)
public class ApiCiConfigurationResource
{
  private final CiConfigurationService ciConfigurationService;

  @Inject
  public ApiCiConfigurationResource(final CiConfigurationService ciConfigurationService) {
    this.ciConfigurationService = ciConfigurationService;
  }

  @GET
  @Operation(
      description = """
          Use this method to retrieve CI integration configuration for the specified owner.

          Set the `direct` query parameter to `true` to retrieve only the configuration directly associated with \
          the specified owner. Set it to `false` (default) to retrieve the merged configuration from the \
          organization hierarchy, where configurations from parent organizations are combined with lower levels \
          taking precedence.

          The response includes a `source` map that indicates which owner (organization or application) \
          contributed each configuration field when using merged mode.

          Permissions required: View IQ Elements""")
  @ApiResponse(
      responseCode = "200",
      description = """
          The response contains:
          <ul>
          <li>`configuration` - the CI integration configuration as a JSON object</li>
          <li>`source` - a map of field names to owner IDs indicating provenance (empty for direct queries)</li>
          </ul>""",
      useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "404",
      description = "No CI configuration found for the specified owner or its hierarchy.")
  public ApiCiConfigurationResponseDto getConfiguration(
      @Parameter(description = "The owner type (application or organization)", required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal ID of the owner", required = true)
      @PathParam("ownerId") final String ownerId,
      @Parameter(description = """
          Set to true to retrieve only direct configuration, false (default) to retrieve merged configuration \
          from hierarchy""")
      @QueryParam("direct") @DefaultValue("false") final boolean direct)
  {
    return ciConfigurationService.getConfiguration(ownerType, ownerId, direct);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      description = """
          Use this method to create or update CI integration configuration for the specified owner.

          The configuration is stored as a JSON object to support various CI systems (GitHub Actions, \
          GitLab CI, etc.). String values must be non-empty.

          Permissions required: Edit IQ Elements""")
  @ApiResponse(
      responseCode = "200",
      description = "CI configuration was saved successfully.",
      useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "400",
      description = "Invalid configuration provided (e.g., empty strings).")
  @Audited(AuditEvent.UPDATE_CI_CONFIGURATION)
  public ApiCiConfigurationDto setConfiguration(
      @Parameter(description = "The owner type (application or organization)", required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal ID of the owner", required = true)
      @PathParam("ownerId") final String ownerId,
      @RequestBody(
          description = """
              Provide the CI integration configuration as a JSON object. The structure supports different \
              CI systems.""",
          useParameterTypeSchema = true)
      final ApiCiConfigurationDto configuration)
  {
    return ciConfigurationService.setConfiguration(ownerType, ownerId, configuration);
  }

  @DELETE
  @Operation(
      description = """
          Use this method to delete CI integration configuration for the specified owner.

          Permissions required: Edit IQ Elements""")
  @ApiResponse(
      responseCode = "204",
      description = "CI configuration was deleted successfully.")
  @ApiResponse(
      responseCode = "404",
      description = "CI configuration not found for the specified owner.")
  @Audited(AuditEvent.DELETE_CI_CONFIGURATION)
  public void deleteConfiguration(
      @Parameter(description = "The owner type (application or organization)", required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal ID of the owner", required = true)
      @PathParam("ownerId") final String ownerId)
  {
    ciConfigurationService.deleteConfiguration(ownerType, ownerId);
  }
}
