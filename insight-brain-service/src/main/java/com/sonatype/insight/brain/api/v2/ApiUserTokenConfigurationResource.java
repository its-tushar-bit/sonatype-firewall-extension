/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiUserTokenConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.198
 */
@Named
@Timed
@Path(PublicApiPaths.USER_TOKEN_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "User Token Configuration",
    description = "Use this REST API to manage user token expiration configuration.")
public class ApiUserTokenConfigurationResource
{
  private final ApiUserTokenConfigurationService service;

  @Inject
  public ApiUserTokenConfigurationResource(final ApiUserTokenConfigurationService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve user token configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(responseCode = "200", description = "Current user token configuration",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "401", description = "Authentication required")
  @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  public ApiUserTokenConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_PROPERTIES)
  @Operation(description = "Use this method to update user token configuration. " +
      "Null values are ignored (no change). " +
      "Returns the current configuration after applying changes." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(responseCode = "200", description = "Configuration updated successfully",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid configuration values")
  @ApiResponse(responseCode = "401", description = "Authentication required")
  @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  public ApiUserTokenConfigurationDTO updateConfiguration(final ApiUserTokenConfigurationDTO configuration) {
    service.updateConfiguration(configuration);
    return service.getConfiguration();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.DELETE_PROPERTIES)
  @Operation(description = "Use this method to reset user token configuration properties to system defaults. " +
      "Returns the current configuration after reset." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(responseCode = "200", description = "Configuration reset successfully",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "No properties specified or invalid properties")
  @ApiResponse(responseCode = "401", description = "Authentication required")
  @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  public ApiUserTokenConfigurationDTO resetConfiguration(@QueryParam("property") final Set<String> properties) {
    service.resetConfiguration(properties);
    return service.getConfiguration();
  }
}
