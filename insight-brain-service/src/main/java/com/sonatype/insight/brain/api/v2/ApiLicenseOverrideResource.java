/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiAppliedLicenseOverridesDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.license.LicenseOverrideService;
import com.sonatype.insight.brain.license.LicenseOverrideUtil;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import java.io.IOException;

import static com.sonatype.insight.brain.audit.AuditEvent.UPDATE_COMPONENT_LICENSE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
@Named
@Timed
@Path(PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2)
@Tag(name = "License Overrides",
    description = "Use this REST API to manage license overrides for components in your applications" +
        "organizations and repositories.")
public class ApiLicenseOverrideResource
{
  static final String LEGAL_REVIEWER_PATH = "/legalReviewer";

  private final LicenseOverrideService licenseOverrideService;

  @Inject
  public ApiLicenseOverrideResource(final LicenseOverrideService licenseOverrideService) {
    this.licenseOverrideService = licenseOverrideService;
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Audited(UPDATE_COMPONENT_LICENSE)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
  @Operation(description = "Use this method to add or update a license override to a component " +
      "for a given owner scope.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains the same license override information that was" +
                  " added." +
                  "\n" +
                  "\n" +
                  "Permissions required: Change Licenses"
          )
      }
  )
  public ApiLicenseOverrideDTO addLicenseOverride(
      @Parameter(description = "Select the `ownerType` scope for which you want to add or " +
          "update a license override",
          required =
          true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the id of the application, organization or the repository.", required = true)
      @PathParam("ownerId") final String ownerId,
      @QueryParam("where") String where,
      @RequestBody(
          description =
              "Enter the license override details to add or update a license override for a " +
                  "component." +
                  "\n" +
              "The request body should contain the following fields:" +
                  "\n" +
                  " - `ownerId`: Enter the id of the application, organization or the repository." +
                  "\n" +
                  " - `comment`: Enter a comment for the license override." +
                  "\n" +
                  " - `licenseIds`: Enter the license ids for the license override." +
                  "\n" +
                  " - `componentIdentifier`: Enter the componentIdentifier consisting of format and " +
                  "coordinates." +
                  "\n" +
                  " - `status`: Enter the status of the license override. The possible values are " +
                    "`OPEN`, `ACKNOWLEDGED`, `OVERRIDDEN`, `SELECTED`, and `CONFIRMED`.",
          required = true)
      final ApiLicenseOverrideDTO licenseOverrideDTO,
      @Context final HttpServletRequest request) throws IOException
  {
    LicenseOverride addedLicenseOverride = licenseOverrideService.addLicenseOverride(ownerType,
        ownerId, LicenseOverrideUtil.toInternalLicenseOverride(licenseOverrideDTO), where,
        request);
    return LicenseOverrideUtil.toApiLicenseOverrideDTO(addedLicenseOverride);
  }

  @DELETE
  @Path("{licenseOverrideId}")
  @Audited(UPDATE_COMPONENT_LICENSE)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
  @Operation(description = "Use this method to delete a license override for a component.",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "The license override was deleted successfully." +
                  "\n" +
                  "\n" +
                  "Permissions required: Change Licenses"
          )
      }
  )
  public void deleteLicenseOverride(
      @Parameter(description = "Select the `ownerType` scope for which you want to delete license" +
          " override",
          required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the id of the application, organization or the repository.", required = true)
      @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the id of the license override you want to delete.",
          required = true)
      @PathParam("licenseOverrideId") final String licenseOverrideId,
      @QueryParam("where") String where,
      @Context final HttpServletRequest request) throws IOException
  {
    licenseOverrideService.deleteLicenseOverride(ownerType, ownerId, licenseOverrideId,
        where, request);
  }

  @GET
  @Path(LEGAL_REVIEWER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public LicenseOverrideService.AppliedLicenseOverrides getAppliedLicenseOverridesForLegalReviewer(
      @Parameter(description = "Select the `ownerType` for which you want to retrieve the applied " +
          "license overrides.",
          required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id of the application, organization or the repository.", required = true)
      @PathParam("ownerId") String ownerId,
      @Parameter(name = "componentIdentifier",
          description = "Enter the componentIdentifier consisting of format and " +
              "coordinates as a JSON " +
              "e.g., `?componentIdentifier={\"format\":\"maven\",\"coordinates\":\"{...}}\"}",
          required = true)
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier)
  {
    return licenseOverrideService.getAppliedLicenseOverridesForLegalReviewer(ownerType, ownerId,
        componentIdentifier);
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the applied license overrides for a " +
      "component.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains the license overrides for the component." +
                  "\n" +
                  "\n" +
                  "Permissions required: View IQ Elements"
          )
      }
  )
  public ApiAppliedLicenseOverridesDTO getAppliedLicenseOverrides(
      @Parameter(description = "Select the `ownerType` for which you want to retrieve the applied " +
          "license overrides.",
          required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the id of the application, organization or the repository.", required = true)
      @PathParam("ownerId") final String ownerId,
      @Parameter(name = "componentIdentifier",
          description = "Enter the componentIdentifier consisting of format and " +
          "coordinates as a JSON " +
              "e.g., `?componentIdentifier={\"format\":\"maven\",\"coordinates\":\"{...}}\"}",
          required = true)
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier)
  {
    return licenseOverrideService.getAppliedLicenseOverridesForRead(ownerType, ownerId,
        componentIdentifier).toDto();
  }
}
