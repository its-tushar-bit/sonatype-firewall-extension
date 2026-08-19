/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationRequestDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @since 1.205
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
public class ApiBulkComponentRemediationResource
{
  private final ApiComponentRemediationService remediationService;

  @Inject
  public ApiBulkComponentRemediationResource(ApiComponentRemediationService remediationService) {
    this.remediationService = remediationService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Bulk variant of the component remediation endpoint. Accepts a list of components " +
      "and returns a per-component result for each. This is intended for callers (such as the ServiceNow AppVR " +
      "integration) that need remediations for many components against the same owner/stage and would otherwise " +
      "have to make one call per component. The batch is capped at 200 components; larger requests return 400.")
  @ApiResponse(
      responseCode = "200", description = "The response returns a list of results, one per input component, in the " +
          "same order as the input. Each result echoes the input component and contains either a remediation " +
          "payload (same shape as the single-component endpoint's response) or, for per-component input-validation " +
          "failures (null entry, missing componentIdentifier/packageUrl, malformed identifier or purl, HDS-unknown " +
          "component), an error message. All other failures — batch-level input errors (missing components, " +
          "oversized batch, invalid stageId, invalid scanId for repositories), authorization/license issues, " +
          "downstream infrastructure failures, and batch-level deadline (HTTP 503) — return the usual error status " +
          "codes instead of a 200.")
  public ApiBulkComponentRemediationDTO getSuggestedRemediationForComponents(
      ApiBulkComponentRemediationRequestDTO request,
      @Parameter(
          description = "Possible values: application, organization, repository. ") @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(
          description = "Possible values: applicationId, organizationId or repositoryId.") @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the stageId to obtain next-non-failing and next-non-failing-with-dependencies " +
          "remediation types in the response. Possible values are develop, build, stage-release, release " +
          "and operate.") @QueryParam("stageId") String stageId,
      @Parameter(description = "Enter the identification source if you want the remediation result based" +
          " on third-party scan information (non-Sonatype). " +
          "The identification source can be obtained from the Component Details" +
          " Page in the UI.") @QueryParam("identificationSource") String identificationSource,
      @Parameter(description = "Enter the scanId (reportId) if you want the remediation result based on" +
          " third-party scan information (non-Sonatype).") @QueryParam("scanId") String scanId,
      @Parameter(
          description = "Enter true if you want to include parent remediation for transitive dependency in the response based"
              +
              " on your application policy scan.") @DefaultValue("false") @QueryParam("includeParentRemediation") Boolean includeParentRemediation)
  {
    return remediationService.getSuggestedRemediationForComponentsBulk(
        request == null ? null : request.components,
        ownerType, ownerId, stageId, identificationSource, scanId, includeParentRemediation);
  }
}
