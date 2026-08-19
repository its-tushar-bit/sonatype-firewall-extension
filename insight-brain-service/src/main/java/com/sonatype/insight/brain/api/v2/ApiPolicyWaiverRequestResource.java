/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.RequiresEntitlement;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.ScanSource;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.POLICY_WAIVER_REQUEST_PATH)
@Tag(name = "Policy Waiver Requests", description = "Use this REST API to manage policy waiver requests.")
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_WAIVERS)
public class ApiPolicyWaiverRequestResource
{
  private final ApiPolicyWaiverRequestService apiPolicyWaiverRequestService;

  private static final String OWNERS_PATH =
      "{ownerType: application|organization|repository|repository_manager|repository_container}/{ownerId}";

  static final String POLICY_VIOLATION_ID_PATH = OWNERS_PATH + "/policyViolation/{policyViolationId}";

  static final String POLICY_WAIVER_REQUEST_ID_PATH = OWNERS_PATH + "/{policyWaiverRequestId}";

  static final String POLICY_WAIVER_REQUEST_REVIEW_PATH = OWNERS_PATH + "/review/{policyWaiverRequestId}";

  @Inject
  public ApiPolicyWaiverRequestResource(ApiPolicyWaiverRequestService apiPolicyWaiverRequestService) {
    this.apiPolicyWaiverRequestService = apiPolicyWaiverRequestService;
  }

  @RequiresEntitlement(LicensedFeature.WAIVER_REQUEST_WORKFLOW)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WAIVER_REQUEST)
  @Path(POLICY_VIOLATION_ID_PATH)
  @Operation(
      description = "Use this method to create a policy waiver request." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200", description = "The new policy waiver request.",
            useReturnTypeSchema = true)
      })
  public ApiPolicyWaiverRequestDTO addPolicyWaiverRequestByPolicyViolationId(
      @Context HttpHeaders headers,
      @Parameter(description = "The scope of the policy waiver request. Possible values are application, " +
          "organization, repository, repository_manager, repository_container.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "The id for the ownerType provided above. E.g. applicationId if the " +
          "ownerType is application.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "The policyViolationId for the policy violation on which you want to create a policy "
          + "waiver request. Use the Policy Violation REST API or Reports REST API to obtain the policyViolationId.") @PathParam("policyViolationId") String policyViolationId,
      @RequestBody(description = "The request JSON can include the fields<ol>"
          + "<li>comment (optional, to indicate the reason of the waiver) default value is null</li>"
          + "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, "
          + "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>"
          + "<li>expiryTime (default null) to set the datetime when the waiver expires.</li>"
          + "<li>expireWhenRemediationAvailable (default false) to expire the waiver when a remediation is available."
          + "</li>"
          + "<li>noteToReviewer (optional) to add a note to the reviewer</li></ol>",
          required = true) ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO)
  {
    ScanSource scanSource =
        ScanSource.fromHeader(headers.getHeaderString(PublicApiPaths.X_SCAN_SOURCE_HEADER));
    return apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(ownerType, ownerId,
        policyViolationId, apiPolicyWaiverRequestOptionsDTO, scanSource);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.REVIEW_WAIVER_REQUEST)
  @RequiresEntitlement(LicensedFeature.WAIVER_REQUEST_WORKFLOW)
  @Path(POLICY_WAIVER_REQUEST_REVIEW_PATH)
  @Operation(
      description = "Use this method to approve or reject a policy waiver request." + //
          "\n" + //
          "\n" + //
          "Permissions required: Waive Policy Violations",
      responses = {@ApiResponse(responseCode = "200", description = "The updated policy waiver request.",
          useReturnTypeSchema = true)})
  public ApiPolicyWaiverRequestDTO reviewPolicyWaiverRequest(
      @Parameter(
          description = "The scope of the policy waiver request. Possible values are application, "
              + "organization, repository, repository_manager, repository_container.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "The id for the ownerType provided above. E.g. applicationId if the "
          + "ownerType is application.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "The policyWaiverRequestId for the policy waiver request to be approved or rejected.") //
      @PathParam("policyWaiverRequestId") String policyWaiverRequestId,
      @RequestBody(description = "The request JSON can include the fields<ol>"
          + "<li>status. Can be APPROVED or REJECTED</li>"
          + "<li>rejectionReason (optional). A text explaining the reason for the rejection., "
          + "<li>comment (optional, to indicate the reason of the waiver) default value is null</li>"
          + "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, "
          + "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>"
          + "<li>expiryTime (default null) to set the datetime when the waiver expires.</li></ol>"
          + "<li>expireWhenRemediationAvailable (default false) to expire the waiver when a remediation is available."
          + "</li>",
          required = true) //
      ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO) {
    return apiPolicyWaiverRequestService.reviewPolicyWaiverRequest(ownerType, ownerId, policyWaiverRequestId,
        apiPolicyWaiverRequestReviewDTO);
  }

  @GET
  @Path(OWNERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresEntitlement(LicensedFeature.WAIVER_REQUEST_WORKFLOW)
  @Operation(
      description = "Use this method to list all policy waiver requests for the given owner." +
          "\n\nPermissions required: View IQ Elements",
      responses = {@ApiResponse(responseCode = "200", description = "The list of policy waiver requests.",
          useReturnTypeSchema = true)})
  public List<ApiPolicyWaiverRequestDTO> getPolicyWaiverRequests(
      @Parameter(description = "The owner type. Possible values are application, organization, repository, "
          + "repository_manager, repository_container.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "The id for the ownerType.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "Optional filter for repository format. Use 'docker' to return only container "
          + "repository waiver requests, or 'component' to return only non-container repository waiver requests. "
          + "Omit to return all.") @QueryParam("repositoryFormat") String repositoryFormat)
  {
    return apiPolicyWaiverRequestService.getPolicyWaiverRequests(ownerType, ownerId, repositoryFormat);
  }

  @GET
  @Path(POLICY_WAIVER_REQUEST_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresEntitlement(LicensedFeature.WAIVER_REQUEST_WORKFLOW)
  @Operation(
      description = "Use this method to retrieve policy waiver request details for the policyWaiverRequestId specified."
          + "\n" //
          + "\n" //
          + "Permissions required: View IQ Elements",
      responses = {@ApiResponse(responseCode = "200", description = "The requested policy waiver request.",
          useReturnTypeSchema = true)})
  public ApiPolicyWaiverRequestDTO getPolicyWaiverRequest(
      @Parameter(description = """
          The scope of the policy waiver request. Possible values are application,
          organization, repository, repository_manager, repository_container.""",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "The id for the ownerType provided above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "The policyWaiverRequestId for which you want to retrieve the details.",
          required = true) @PathParam("policyWaiverRequestId") String policyWaiverRequestId)
  {
    return apiPolicyWaiverRequestService.getPolicyWaiverRequest(ownerType, ownerId, policyWaiverRequestId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_WAIVER_REQUEST)
  @RequiresEntitlement(LicensedFeature.WAIVER_REQUEST_WORKFLOW)
  @Path(POLICY_WAIVER_REQUEST_ID_PATH)
  @Operation(
      description = "Use this method to update a policy waiver request." //
          + "\n" //
          + "\n" //
          + "Permissions required: View IQ Elements",
      responses = {@ApiResponse(responseCode = "200", description = "The updated policy waiver request.",
          useReturnTypeSchema = true)})
  public ApiPolicyWaiverRequestDTO updatePolicyWaiverRequest(
      @Parameter(
          description = "The scope of the policy waiver request. Possible values are application, "
              + "organization, repository, repository_manager, repository_container.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "The id for the ownerType provided above. E.g. applicationId if the "
          + "ownerType is application.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "The id of the policy waiver request to be updated.",
          required = true) @PathParam("policyWaiverRequestId") String policyWaiverRequestId,
      @RequestBody(description = "The request JSON can include the fields<ol>"
          + "<li>comment (optional, to indicate the reason of the waiver) default value is null</li>"
          + "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, "
          + "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>"
          + "<li>expiryTime (default null) to set the datetime when the waiver expires.</li>"
          + "<li>expireWhenRemediationAvailable (default false) to expire the waiver when a remediation is available."
          + "</li>" + "<li>noteToReviewer (optional) to add a note to the reviewer</li></ol>",
          required = true) ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO)
  {
    return apiPolicyWaiverRequestService.updatePolicyWaiverRequest(ownerType, ownerId, policyWaiverRequestId,
        apiPolicyWaiverRequestOptionsDTO);
  }

  @DELETE
  @Path(POLICY_WAIVER_REQUEST_ID_PATH)
  @Audited(AuditEvent.WITHDRAW_WAIVER_REQUEST)
  @RequiresEntitlement(LicensedFeature.WAIVER_REQUEST_WORKFLOW)
  @Operation(
      description = "Use this method to withdraw a pending policy waiver request that you originally submitted."
          + "\n" //
          + "\n" //
          + "The request row is removed; this is intended for cleaning up requests created by mistake "
          + "(wrong scope, wrong rationale, etc.). The action is recorded in the audit log so the "
          + "withdrawal remains traceable. Only the original requester may withdraw, and only while "
          + "the request is in the REQUESTED state. Once a request has been APPROVED or REJECTED, "
          + "withdraw is rejected — those terminal states must be handled by a reviewer through the "
          + "review endpoint." //
          + "\n" //
          + "\n" //
          + "Permissions required: View IQ Elements (the caller must also be the requester of the "
          + "waiver request; other callers receive 404).",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "The policy waiver request was withdrawn."),
        @ApiResponse(responseCode = "400",
            description = "The policy waiver request is not in the REQUESTED state."),
        @ApiResponse(responseCode = "404",
            description = "The policy waiver request does not exist or the caller is not its requester.")
      })
  public void withdrawPolicyWaiverRequest(
      @Parameter(
          description = "The scope of the policy waiver request. Possible values are application, "
              + "organization, repository, repository_manager, repository_container.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "The id for the ownerType provided above. E.g. applicationId if the "
          + "ownerType is application.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "The id of the policy waiver request to be withdrawn.",
          required = true) @PathParam("policyWaiverRequestId") String policyWaiverRequestId)
  {
    apiPolicyWaiverRequestService.withdrawPolicyWaiverRequest(ownerType, ownerId, policyWaiverRequestId);
  }
}
