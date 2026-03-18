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
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @since 1.64
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
public class ApiComponentRemediationResource
{
  private final ApiComponentRemediationService remediationService;

  @Inject
  public ApiComponentRemediationResource(ApiComponentRemediationService remediationService) {
    this.remediationService = remediationService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to obtain remediation suggestions for policy violations on " +
      "a component basis. Remediations obtained from this method are same as those appearing on " +
      "the Component Details Page in the UI.")
  @ApiResponse(
      responseCode = "200", description = "The response returns details for components that can be used for " +
          "remediation. Details for the recommended component are grouped under type in the response." +
          "<ul>" +
          "<li>Type <i>next-no-violations</i> indicates that the component version has no violations.</li>" +
          "<li>Type <i>next-non-failing</i> indicates that the component version does not fail policy violations. " +
          "The response will contain this type only if stageId is provided in the method call.</li>" +
          "<li>Type <i>next-no-violations-with-dependencies</i> indicates that the component, along-with its " +
          "dependencies does not any violate any policies.</li>" +
          "<li>Type <i>next-non-failing-with-dependencies</i> indicates that the component and its dependencies " +
          "will not fail a build for the stageId provided.</li>" +
          "</ul>" +
          "<p>" +
          "Hash values returned here are truncated and are not intended to be used as checksums. " +
          "They can be used as identifiers to pass to other REST API calls. "

  )
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 component,
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
    return remediationService
        .getSuggestedRemediationForComponent(component, ownerType, ownerId, stageId, identificationSource, scanId,
            includeParentRemediation);
  }
}
