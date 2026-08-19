/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.containerimagewaiver.ApiContainerImageWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestService;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.PolicyContainerWaiverData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Singleton
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
@HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
@Path(PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallContainerImagePolicyWaiverResource
{
  static final String CONTAINER_IMAGE_ID = "/{containerImageId}";

  static final String POLICY_WAIVER = "/policyWaiver";

  static final String POLICY_WAIVER_REQUEST = "/policyWaiverRequest";

  private final ApiPolicyWaiverService apiPolicyWaiverService;

  private final ApiPolicyWaiverRequestService apiPolicyWaiverRequestService;

  @Inject
  public ApiFirewallContainerImagePolicyWaiverResource(
      final ApiPolicyWaiverService apiPolicyWaiverService,
      final ApiPolicyWaiverRequestService apiPolicyWaiverRequestService)
  {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
    this.apiPolicyWaiverRequestService = apiPolicyWaiverRequestService;
  }

  @POST
  @Path(CONTAINER_IMAGE_ID + POLICY_WAIVER)
  @Audited(AuditEvent.CREATE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to create a waiver for all policy violations of a container Image. " +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Waiver has been created successfully.")
      })
  public void addWaiver(
      @Parameter(description = "Enter the container image id.",
          required = true) @PathParam("containerImageId") final String containerImageId,
      @RequestBody(description = "The request JSON can include the fields" +
          "<ol>" +
          "<li>expiryTime (default null): Sets the datetime when the waiver expires.</li>" +
          "<li>waiverReasonId (default null): Sets the specific reason chosen for the waiver.</li>" +
          "<li>comment (default null): Further explanation about the waiver.</li>" +
          "</ol>") ApiContainerImageWaiverDTO waiverDTO)
  {
    apiPolicyWaiverService.addContainerImageWaiver(containerImageId, waiverDTO);
  }

  @POST
  @Path(CONTAINER_IMAGE_ID + POLICY_WAIVER_REQUEST)
  @Audited(AuditEvent.CREATE_WAIVER_REQUEST)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to request a waiver for all policy violations of a container image. " +
      "\n\nPermissions required: authenticated user (waiver request workflow must be enabled)",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Waiver request has been created successfully.")
      })
  public void requestWaiver(
      @Parameter(description = "The container image id.",
          required = true) @PathParam("containerImageId") final String containerImageId,
      @RequestBody(
          description = "Fields: comment, noteToReviewer, expiryTime, waiverReasonId") ApiContainerImageWaiverRequestDTO requestDTO)
  {
    apiPolicyWaiverRequestService.addContainerImagePolicyWaiverRequest(containerImageId, requestDTO);
  }

  @DELETE
  @Path(CONTAINER_IMAGE_ID + POLICY_WAIVER)
  @Audited(AuditEvent.DELETE_CONTAINER_IMAGE_POLICY_VIOLATIONS_WAIVER)
  @Operation(description = "Use this method to delete a container waiver, specified by the containerImageId." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Waiver has been deleted successfully.",
            useReturnTypeSchema = true)
      })
  public void deleteContainerImagePolicyWaiver(
      @Parameter(description = "Enter the container id.",
          required = true) @PathParam("containerImageId") String containerImageId)
  {
    apiPolicyWaiverService.deleteContainerImageWaiver(containerImageId);
  }

  private class PolicyContainerWaiverDataResult
      extends ApiPageResult<PolicyContainerWaiverData>
  {
  }

  @GET
  @Path(POLICY_WAIVER)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to get all policy waivers for container images. " +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Policy waivers for container images.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PolicyContainerWaiverDataResult.class)),
            headers = {
              @Header(name = "Link",
                  description = "Pagination links (first, last, next, prev)",
                  schema = @Schema(type = "string"))
            })
      })
  public Response getWaivers(
      @Context UriInfo uriInfo,
      @QueryParam("page") @Min(1) int page,
      @QueryParam("pageSize") @Min(1) @Max(100) int pageSize)
  {
    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize,
        apiPolicyWaiverService.getAllPolicyContainerWaivers(page, pageSize))
            .queryParameters(uriInfo.getQueryParameters())
            .build();
  }
}
