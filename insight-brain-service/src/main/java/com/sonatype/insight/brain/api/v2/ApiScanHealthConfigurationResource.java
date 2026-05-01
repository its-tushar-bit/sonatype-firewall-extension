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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.scanhealth.ScanHealthService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API resource for managing Scan Health configuration.
 * Supports hierarchical configuration inheritance from the organization hierarchy.
 */
@Named
@Timed
@Path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
@Tag(name = "Scan Health Configuration",
    description = """
        Use this REST API to manage Scan Health configuration. This includes settings for
        failing scans with zero components detected. Configurations can be set at organization
        or application level and are inherited from the organization hierarchy.""")
@Produces(MediaType.APPLICATION_JSON)
public class ApiScanHealthConfigurationResource
{
  private final ScanHealthService scanHealthService;

  @Inject
  public ApiScanHealthConfigurationResource(final ScanHealthService scanHealthService) {
    this.scanHealthService = scanHealthService;
  }

  @GET
  @Operation(
      description = """
          Use this method to retrieve Scan Health configuration for the specified owner.

          The response includes the `failOnZeroComponents` setting. A null value indicates
          the setting is inherited from the parent organization (or default disabled if no
          parent configuration exists).

          Permissions required: View IQ Elements""")
  @ApiResponse(
      responseCode = "200",
      description = "The Scan Health configuration for the specified owner.",
      useReturnTypeSchema = true)
  public ScanHealthConfigDTO getConfiguration(
      @Parameter(description = "The owner type (application or organization)",
          required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal ID of the owner",
          required = true) @PathParam("ownerId") final String ownerId)
  {
    return scanHealthService.getConfiguration(ownerType, ownerId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      description = """
          Use this method to create or update Scan Health configuration for the specified owner.

          Set `failOnZeroComponents` to `true` to fail scans that detect zero components.
          Set to `false` to explicitly disable this check. Set to `null` to inherit from
          the parent organization.

          Permissions required: Edit IQ Elements""")
  @ApiResponse(
      responseCode = "200",
      description = "Scan Health configuration was saved successfully.",
      useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "404",
      description = "Owner not found.")
  @Audited(AuditEvent.CONFIGURE_SCAN_HEALTH)
  public ScanHealthConfigDTO saveConfiguration(
      @Parameter(description = "The owner type (application or organization)",
          required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal ID of the owner",
          required = true) @PathParam("ownerId") final String ownerId,
      @RequestBody(
          description = "The Scan Health configuration to save.",
          useParameterTypeSchema = true) final ScanHealthConfigDTO config)
  {
    return scanHealthService.saveConfiguration(ownerType, ownerId, config);
  }

  @DELETE
  @Operation(
      description = """
          Use this method to delete Scan Health configuration for the specified owner.
          After deletion, the configuration will be inherited from the parent organization
          or default to disabled.

          Permissions required: Edit IQ Elements""")
  @ApiResponse(
      responseCode = "204",
      description = "Scan Health configuration was deleted successfully.")
  @ApiResponse(
      responseCode = "404",
      description = "Scan Health configuration not found for the specified owner.")
  @Audited(AuditEvent.DELETE_SCAN_HEALTH)
  public void deleteConfiguration(
      @Parameter(description = "The owner type (application or organization)",
          required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The internal ID of the owner",
          required = true) @PathParam("ownerId") final String ownerId)
  {
    scanHealthService.deleteConfiguration(ownerType, ownerId);
  }
}
