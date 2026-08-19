/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.api.v2.service.autowaivers.ApiAutoPolicyWaiverExclusionService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.RequiresEntitlement;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;

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
  private final ApiAutoPolicyWaiverExclusionService apiAutoPolicyWaiverExclusionService;

  static final String OWNERS_PATH = "{ownerType: application|organization}/{ownerId}";

  static final String BY_AUTO_POLICY_WAIVER_ID_PATH = OWNERS_PATH + "/{autoPolicyWaiverId}";

  static final String BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH =
      OWNERS_PATH + "/{autoPolicyWaiverId}/{autoPolicyWaiverExclusionId}";

  @Inject
  public ApiAutoPolicyWaiverExclusionResource(ApiAutoPolicyWaiverExclusionService apiAutoPolicyWaiverExclusionService) {
    this.apiAutoPolicyWaiverExclusionService = apiAutoPolicyWaiverExclusionService;
  }

  @RequiresEntitlement(LicensedFeature.AUTO_WAIVER_MANAGEMENT)
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
            useReturnTypeSchema = true)
      })
  public ApiAutoPolicyWaiverExclusionResponseDTO addAutoPolicyWaiveExclusion(
      @Parameter(description = "Enter the ownerType to specify which resource type owns the auto waiver you want to " +
          "apply a exclusion to. Possible values are application, organization.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
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
          required = true) final ApiAutoPolicyWaiverExclusionRequestDTO autoPolicyWaiverExclusionDTO)
  {
    return apiAutoPolicyWaiverExclusionService
        .addAutoPolicyWaiverExclusion(ownerType, ownerId, autoPolicyWaiverExclusionDTO);
  }

  @RequiresEntitlement(LicensedFeature.AUTO_WAIVER_MANAGEMENT)
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
            useReturnTypeSchema = true)
      })
  public void deleteAutoPolicyWaiverExclusion(
      @Parameter(description = "Enter the ownerType to specify the scope. A waiver exclusion " +
          "corresponding to the autoPolicyWaiverExclusionId provided and within the scope " +
          "specified will be deleted.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the relevant Auto Policy Waiver ID.",
          required = true) @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId,
      @Parameter(
          description = "Enter the autoPolicyWaiverId to be deleted") @PathParam("autoPolicyWaiverExclusionId") String autoPolicyWaiverExclusionId)
  {
    apiAutoPolicyWaiverExclusionService
        .deleteAutoPolicyWaiverExclusion(ownerType, ownerId, autoPolicyWaiverExclusionId);
  }

  @GET
  @Path(BY_AUTO_POLICY_WAIVER_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve auto policy waiver exclusions " +
      "for the given owner and policy waiver." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the auto policy waiver exclusions.",
            useReturnTypeSchema = true)
      })
  public List<ApiAutoPolicyWaiverExclusionResponseDTO> getAutoPolicyWaiverExclusions(
      @Parameter(description = "Enter the owner type.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the owner id.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the id of the automatic policy waiver.") @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId,
      @Parameter(description = "Enter the page.") @DefaultValue("1") @QueryParam("page") int page,
      @Parameter(description = "Enter the page size.") @DefaultValue("10") @QueryParam("pageSize") int pageSize)
  {
    return apiAutoPolicyWaiverExclusionService.getAutoPolicyWaiverExclusions(
        ownerType,
        ownerId,
        autoPolicyWaiverId,
        page,
        pageSize);
  }
}
