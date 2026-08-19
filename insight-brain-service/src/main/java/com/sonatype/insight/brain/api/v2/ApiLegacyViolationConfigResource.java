/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiLegacyViolationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_GRANDFATHERING)
@Tag(
    name = "Legacy Violations Configuration",
    description = "Use this REST API to view and update legacy-violation configuration for an application or organization.")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiLegacyViolationConfigResource
{
  public static final String OWNER_PATH = "{ownerType: application|organization}/{ownerId}";

  private final ApiLegacyViolationService apiLegacyViolationService;

  @Inject
  public ApiLegacyViolationConfigResource(final ApiLegacyViolationService apiLegacyViolationService) {
    this.apiLegacyViolationService = apiLegacyViolationService;
  }

  @GET
  @Path(OWNER_PATH)
  @Operation(description = "Use this method to retrieve the legacy-violation configuration for an owner." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "Successfully retrieved legacy-violation configuration.",
      useReturnTypeSchema = true)
  public ApiLegacyViolationStatusDTO getConfig(
      @Parameter(description = "Owner type. Allowed values: `application`, `organization`.",
          required = true) @PathParam("ownerType") final String ownerTypeParam,
      @Parameter(description = "Public id of the application, or id of the organization.",
          required = true) @PathParam("ownerId") final String ownerId)
  {
    OwnerType ownerType = parseOwnerType(ownerTypeParam);
    return apiLegacyViolationService.getConfig(ownerType, ownerId);
  }

  @PUT
  @Path(OWNER_PATH)
  @Audited(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS)
  @Operation(description = "Use this method to update the legacy-violation configuration for an owner." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "Successfully updated legacy-violation configuration.",
      useReturnTypeSchema = true)
  public ApiLegacyViolationStatusDTO setConfig(
      @Parameter(description = "Owner type. Allowed values: `application`, `organization`.",
          required = true) @PathParam("ownerType") final String ownerTypeParam,
      @Parameter(description = "Public id of the application, or id of the organization.",
          required = true) @PathParam("ownerId") final String ownerId,
      final ApiLegacyViolationStatusDTO request)
  {
    if (request == null) {
      throw new BadRequestException("Request body is required.");
    }
    OwnerType ownerType = parseOwnerType(ownerTypeParam);
    return apiLegacyViolationService.setConfig(ownerType, ownerId, request);
  }

  private static OwnerType parseOwnerType(String raw) {
    if ("application".equals(raw)) {
      return OwnerType.APPLICATION;
    }
    if ("organization".equals(raw)) {
      return OwnerType.ORGANIZATION;
    }
    throw new BadRequestException("Invalid owner type: " + raw);
  }
}
