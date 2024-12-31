/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiAutoPolicyWaiverExclusionService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.license.model.LicensedFeature;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH)
@Tag(name = "Auto Policy Waiver Exclusions",
    description = "Use this REST API to create and delete auto policy waiver exclusions.")
@ProductLicenseEnforcementPoint(LicensedFeature.DEVELOPER_DASHBOARD)
public class ApiAutoPolicyWaiverExclusionResource
{
  private final ApiConfigFeaturesService apiConfigFeaturesService;

  private final ApiAutoPolicyWaiverExclusionService apiAutoPolicyWaiverExclusionService;

  static final String OWNERS_PATH = "{ownerType: application|organization}/{ownerId}";

  static final String BY_AUTO_POLICY_WAIVER_ID_PATH = OWNERS_PATH + "/{autoPolicyWaiverId}";

  static final String BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH =
      OWNERS_PATH + "/{autoPolicyWaiverId}/{autoPolicyWaiverExclusionId}";

  @Inject
  public ApiAutoPolicyWaiverExclusionResource(
      ApiConfigFeaturesService apiConfigFeaturesService,
      ApiAutoPolicyWaiverExclusionService apiAutoPolicyWaiverExclusionService)
  {
    this.apiConfigFeaturesService = apiConfigFeaturesService;
    this.apiAutoPolicyWaiverExclusionService = apiAutoPolicyWaiverExclusionService;
  }

  @POST
  @Path(OWNERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION)
  @Operation(
      description = "Use this method to create an auto policy waiver exclusion for a specified " +
          "auto policy waiver." +
          "\n" +
          "\n" +
          "Permissions required: Waive Policy Violations",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Auto policy waiver exclusion has been created successfully.",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiAutoPolicyWaiverExclusionResponseDTO addAutoPolicyWaiveExclusion(
      @Parameter(description = "Enter the ownerType to specify which resource type owns the auto waiver you want to " +
          "apply a exclusion to. Possible values are application, organization.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.", required = true)
      @PathParam("ownerId") String ownerId,
      @RequestBody(
          description = "The request JSON can include the fields" +
              "<ol>" +
              "<li>applicationPublicId</li>" +
              "<li>ownerId - ID of the application or organization which will own the auto waiver exclusion</li>" +
              "<li>policyViolationId - ID of the policy violation which the exclusion will apply to</li>" +
              "<li>autoPolicyWaiverId - ID of the auto waiver you want to apply a exclusion to</li>" +
              "<li>scanId - ID of the scan which the violation being waived appeared in</li>" +
              "<li>matchStrategy (enumeration, required) can have values EXACT_COMPONENT, " +
              "ALL_VERSIONS, POLICY_VIOLATION. </li>" +
              "</ol>",
          required = true
      ) final ApiAutoPolicyWaiverExclusionRequestDTO autoPolicyWaiverExclusionDTO)
  {
    checkAutoPolicyWaiversFeatureEnabled();
    return apiAutoPolicyWaiverExclusionService
        .addAutoPolicyWaiverExclusion(ownerType, ownerId, autoPolicyWaiverExclusionDTO);
  }

  @DELETE
  @Path(BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
  @Audited(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION)
  @Operation(description = "Use this method to delete an auto policy waiver exclusion, " +
      "specified by the autoPolicyWaiverExclusionId." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "Auto policy waiver exclusion has been deleted successfully.",
              useReturnTypeSchema = true
          )
      })
  public void deleteAutoPolicyWaiverExclusion(
      @Parameter(description = "Enter the ownerType to specify the scope. A waiver exclusion " +
          "corresponding to the autoPolicyWaiverExclusionId provided and within the scope " +
          "specified will be deleted.",
          required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.", required = true)
      @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the relevant Auto Policy Waiver ID.", required = true)
      @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId,
      @Parameter(description = "Enter the autoPolicyWaiverId to be deleted")
      @PathParam("autoPolicyWaiverExclusionId") String autoPolicyWaiverExclusionId)
  {
    checkAutoPolicyWaiversFeatureEnabled();
    apiAutoPolicyWaiverExclusionService
        .deleteAutoPolicyWaiverExclusion(ownerType, ownerId, autoPolicyWaiverExclusionId);
  }

  @GET
  @Path(BY_AUTO_POLICY_WAIVER_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public List<ApiAutoPolicyWaiverExclusionResponseDTO> getAutoPolicyWaiverExclusions(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId,
      @DefaultValue("1") @QueryParam("page") int page,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize)
  {
    checkAutoPolicyWaiversFeatureEnabled();
    return apiAutoPolicyWaiverExclusionService.getAutoPolicyWaiverExclusions(
        ownerType,
        ownerId,
        autoPolicyWaiverId,
        page,
        pageSize
    );
  }

  private void checkAutoPolicyWaiversFeatureEnabled() {
    if (!apiConfigFeaturesService.isFeatureEnabled(SystemConfigurationPropertyFeature.AUTO_WAIVERS)) {
      throw new BadRequestException("Auto Policy Waivers feature is not enabled");
    }
  }
}
