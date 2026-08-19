/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationChangeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiLegacyViolationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_GRANDFATHERING)
@Tag(
    name = "Legacy Violations",
    description = "Use this REST API to list, grant, and revoke legacy status for policy violations of an application.")
@Produces(MediaType.APPLICATION_JSON)
public class ApiLegacyViolationResource
{
  public static final String APPLICATION_PATH = "application/{applicationPublicId}";

  public static final String GRANT_PATH = "application/{applicationPublicId}/grant";

  public static final String REVOKE_PATH = "application/{applicationPublicId}/revoke";

  private final ApiLegacyViolationService apiLegacyViolationService;

  @Inject
  public ApiLegacyViolationResource(final ApiLegacyViolationService apiLegacyViolationService) {
    this.apiLegacyViolationService = apiLegacyViolationService;
  }

  @GET
  @Path(APPLICATION_PATH)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  @Operation(description = "Use this method to retrieve all legacy policy violations for an application." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "Successfully retrieved legacy violations.",
      useReturnTypeSchema = true)
  public List<ApiPolicyViolationDTOV2> listLegacyViolations(
      @Parameter(description = "The public id of the application.",
          required = true) @PathParam("applicationPublicId") final String applicationPublicId,
      @Parameter(description = "Optional policy id filter.") @QueryParam("policyId") final String policyId,
      @Parameter(
          description = "Optional component identifier filter, expressed as a package URL (purl).") @QueryParam("componentIdentifier") final String componentIdentifierPurl)
  {
    ComponentIdentifier componentIdentifierFilter = null;
    if (componentIdentifierPurl != null && !componentIdentifierPurl.isEmpty()) {
      try {
        componentIdentifierFilter = new PackageUrlIdentifier(componentIdentifierPurl).toComponentIdentifier();
      }
      catch (InvalidPackageURLException | IllegalArgumentException e) {
        throw new BadRequestException("Invalid componentIdentifier: " + e.getMessage());
      }
    }
    return apiLegacyViolationService.listLegacyViolations(applicationPublicId, policyId, componentIdentifierFilter);
  }

  @POST
  @Path(GRANT_PATH)
  @Audited(AuditEvent.APPLY_LEGACY_VIOLATION_STATUS)
  @Operation(description = "Use this method to grant legacy status to all eligible policy violations of an application."
      +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "Successfully granted legacy status.",
      useReturnTypeSchema = true)
  public ApiLegacyViolationChangeResponseDTO grant(
      @Parameter(description = "The public id of the application.",
          required = true) @PathParam("applicationPublicId") final String applicationPublicId)
  {
    return apiLegacyViolationService.grant(applicationPublicId);
  }

  @POST
  @Path(REVOKE_PATH)
  @Audited(AuditEvent.REVOKE_LEGACY_VIOLATION_STATUS)
  @Operation(
      description = "Use this method to revoke legacy status from all legacy policy violations of an application."
          +
          "\n" +
          "\n" +
          "Permissions required: Edit IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "Successfully revoked legacy status.",
      useReturnTypeSchema = true)
  public ApiLegacyViolationChangeResponseDTO revoke(
      @Parameter(description = "The public id of the application.",
          required = true) @PathParam("applicationPublicId") final String applicationPublicId)
  {
    return apiLegacyViolationService.revoke(applicationPublicId);
  }
}
