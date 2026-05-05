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
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.RequiresEntitlement;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.webhook.RequestPolicyWaiverEventService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService.MAX_BULK_WAIVER_VIOLATIONS;

/**
 * @since 1.90
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_WAIVER_PATH)
@Tag(name = "Policy Waivers",
    description = "Use this REST API to create and retrieve policy waivers.")
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_WAIVERS)
public class ApiPolicyWaiverResource
{
  private final ApiPolicyWaiverService apiPolicyWaiverService;

  private final RequestPolicyWaiverEventService requestPolicyWaiverEventService;

  static final String OWNERS_PATH =
      "{ownerType: application|organization|repository|repository_manager|repository_container}/{ownerId}";

  static final String BY_POLICY_WAIVER_ID_PATH = OWNERS_PATH + "/{policyWaiverId}";

  static final String BY_POLICY_VIOLATION_ID_PATH = OWNERS_PATH + "/{policyViolationId}";

  static final String TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH = "transitive/{ownerType: application}/{ownerId}/{scanId}";

  static final String TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH =
      "transitive/{ownerType: application|organization}/{ownerId}/stages/{stageId}";

  static final String REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH = "/waiverRequests/{policyViolationId}";

  @Inject
  public ApiPolicyWaiverResource(
      ApiPolicyWaiverService apiPolicyWaiverService,
      RequestPolicyWaiverEventService requestPolicyWaiverEventService)
  {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
    this.requestPolicyWaiverEventService = requestPolicyWaiverEventService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WAIVER)
  @Path(BY_POLICY_VIOLATION_ID_PATH)
  @Operation(
      description = "Use this method to create a policy waiver." +
          "\n" +
          "\n" +
          "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "No content. Indicates that the waiver has been created successfully.")
      }

  )
  public void addPolicyWaiverByPolicyViolationId(
      @Parameter(description = "Indicates the scope of the waiver. Possible values are application, " +
          "organization, repository, repository_manager, repository_container.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the ownerType provided above. E.g. applicationId if the " +
          "ownerType is application.", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the policyViolationId for the policy on which you want to create a waiver. " +
          "Use the Policy Violation REST API or Reports REST API to obtain the policyViolationId.") @PathParam("policyViolationId") String policyViolationId,
      @RequestBody(
          description = "The request JSON can include the fields" +
              "<ol>" +
              "<li>comment (optional, to indicate the reason of " +
              "the waiver) default value is null</li>" +
              "<li>applyToAllComponents (boolean, default 'false'),deprecated in favor of matcherStrategy. " +
              "If matcherStrategy is not set, 'true' means this will apply the waiver to all components, " +
              "'false' means this will apply to a specific component.</li>" +
              "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, " +
              "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>" +
              "<li>expiryTime (default null) to set the datetime when the waiver expires.</li>" +
              "</ol>",
          required = true) ApiWaiverOptionsDTO waiverOptionsDTO)
  {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(ownerType, ownerId, policyViolationId, waiverOptionsDTO);
  }

  @RequiresEntitlement(LicensedFeature.BULK_WAIVERS)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WAIVER)
  @Path(OWNERS_PATH)
  @Operation(
      description = "Use this method to create policy waivers for multiple policy violations." +
          "\n" +
          "\n" +
          "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "No content. Indicates that the waivers have been created successfully.")
      })
  public void addBulkPolicyWaivers(
      @Parameter(description = "Indicates the scope of the waiver. Possible values are application, " +
          "organization, repository, repository_manager, repository_container.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the ownerType provided above. E.g. applicationId if the " +
          "ownerType is application.", required = true) @PathParam("ownerId") String ownerId,
      @RequestBody(
          description = "The request JSON should include:" +
              "<ol>" +
              "<li>violationIds (required, list of policy violation IDs, maximum "
              + MAX_BULK_WAIVER_VIOLATIONS + ")</li>" +
              "<li>apiWaiverOptionsDTO (required) containing:" +
              "<ul>" +
              "<li>comment (optional, to indicate the reason of the waiver)</li>" +
              "<li>matcherStrategy (enumeration, required) can have values EXACT_COMPONENT or ALL_VERSIONS</li>" +
              "<li>expiryTime (optional) to set the datetime when the waiver expires</li>" +
              "<li>waiverReasonId (optional) waiver reason ID</li>" +
              "<li>expireWhenRemediationAvailable (optional boolean, default false) expire waiver when remediation " +
              "is available, can only be applied to Exact Components.</li>" +
              "</ul></li>" +
              "</ol>",
          required = true) ApiBulkWaiversDTO bulkWaiversDTO)
  {
    apiPolicyWaiverService.addBulkPolicyWaivers(ownerType, ownerId, bulkWaiversDTO);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_WAIVER)
  @Path(BY_POLICY_WAIVER_ID_PATH)
  @Operation(description = "Use this method to update an existing policy waiver." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "The policy waiver was updated successfully.")
      })
  public void updatePolicyWaiver(
      @Parameter(description = "Indicates the scope of the policy waiver. Possible values are application," +
          " organization, repository, repository_manager, and repository_container.",
          required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the id for the `ownerType` provided above. E.g. `applicationId` if the" +
          " `ownerType` is application.", required = true) @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the id for the policy waiver.",
          required = true) @PathParam("policyWaiverId") final String policyWaiverId,
      @RequestBody(description = "Enter the policy waiver details to update." +
          " Note that updating `matcherStrategy` is currently unsupported.",
          required = true) final ApiWaiverOptionsDTO dto)
  {
    apiPolicyWaiverService.updatePolicyWaiver(ownerType, ownerId, policyWaiverId, dto);
  }

  /**
   * @since 1.147
   */
  @GET
  @Audited(AuditEvent.VIEW_WAIVER)
  @Path(BY_POLICY_WAIVER_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve waiver details for the waiverId specified." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains waiver details corresponding to the policy waiverId specified.",
            useReturnTypeSchema = true)
      })
  public ApiPolicyWaiverDTO getPolicyWaiver(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain the details " +
          "for waivers within the scope.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the policyWaiverId for which you want to retrieve the waiver details.",
          required = true) @PathParam("policyWaiverId") String policyWaiverId)
  {
    return apiPolicyWaiverService.getPolicyWaiver(ownerType, ownerId, policyWaiverId);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_WAIVER)
  @Path(BY_POLICY_WAIVER_ID_PATH)
  @Operation(description = "Use this method to delete a waiver, specified by the policyWaiverId." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Waiver has been deleted successfully.")
      })
  public void deletePolicyWaiver(
      @Parameter(description = "Enter the ownerType to specify the scope. A waiver corresponding to the " +
          "policyWaiverId provided and within the scope specified will be deleted.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the policyWaiverId to be deleted.") @PathParam("policyWaiverId") String policyWaiverId)
  {
    apiPolicyWaiverService.deletePolicyWaiver(ownerType, ownerId, policyWaiverId);
  }

  @GET
  @Path(OWNERS_PATH)
  @Audited(AuditEvent.VIEW_WAIVER)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve waiver details for all policy waivers for the " +
      "scope specified. You can specify the scope by using the parameters ownerType and ownerId." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains waiver details for the specified ownerType and the corresponding " +
                "ownerId, grouped by the policyWaiverId. The response field 'matcherStrategy' indicates whether " +
                "the waiver " +
                "applies to a specific component, or all components that exist at that level of hierarchy" +
                " (root org, org " +
                "application), or all versions of the component (past, present, and future). " +
                "The response fields " +
                "associatedPackageUrl, displayName, and componentIdentifier are null for waivers on all " +
                "components and " +
                "unknown components.",
            useReturnTypeSchema = true)
      })
  public List<ApiPolicyWaiverDTO> getPolicyWaivers(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain " +
          "waivers that are within the scope specified.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId)
  {
    return apiPolicyWaiverService.getPolicyWaivers(ownerType, ownerId);
  }

  @POST
  @Path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
  @Audited(AuditEvent.CREATE_TRANSITIVE_POLICY_VIOLATIONS_WAIVER)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to create a policy waiver on a transitive component detected during " +
      "the specified scan. NOTE: Any one of the input parameters, i.e. component identifier, packageUrl or hash " +
      "is required. If more than one is provided, the system will pick them in the order specified here." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "No content. Indicates that the waiver has been created successfully.")
      })
  public void addWaiverToTransitivePolicyViolationsByAppScanComponent(
      @Parameter(description = "Indicates the scope of the waiver that will be created.",
          required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the scanId (reportId) of the evaluation report that shows the transitive " +
          "component.", required = true) @PathParam("scanId") final String scanId,
      @Parameter(description = "Enter the component identifier of the transitive component on which you want to " +
          "create a policy waiver.") @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL of the transitive component on which you want to create " +
          "a policy waiver.") @QueryParam("packageUrl") final String packageUrl,
      @Parameter(description = "Enter the hash of the transitive component on which you want to create a policy " +
          "waiver.") @QueryParam("hash") final String hash,
      @RequestBody(description = "The request JSON can include the fields" +
          "<ol>" +
          "<li>comment (optional, to indicate the reason of " +
          "the waiver) default value is null</li>" +
          "<li>applyToAllComponents (boolean, default 'false'),deprecated in favor of matcherStrategy. " +
          "If matcherStrategy is not set, 'true' means this will apply the waiver to all components, " +
          "'false' means this will apply to a specific component.</li>" +
          "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, " +
          "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>" +
          "<li>expiryTime (default null) to set the datetime when the waiver expires.</li>" +
          "</ol>") ApiWaiverOptionsDTO apiWaiverOptionsDTO)
  {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(ownerType, ownerId, scanId,
        componentIdentifier, packageUrl, hash, apiWaiverOptionsDTO);
  }

  @POST
  @Path(TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH)
  @Audited(AuditEvent.CREATE_TRANSITIVE_POLICY_VIOLATIONS_WAIVER)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to add a waiver for all transitive violations for a given component, " +
      "detected in the latest scan at the stage specified." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Waiver created successfully.")
      })
  public void addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
      @Parameter(description = "Indicates the scope of the waiver that will be created.",
          required = true) @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above. " +
          "E.g. applicationId for ownerType 'application' or organizationId for ownerType 'organization'.",
          required = true) @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the stageId corresponding to the evaluation stage at which you want to " +
          "create a waiver. " +
          "Possible values are 'develop', 'source', 'build', 'stage-release', 'release' and 'operate'.",
          required = true) @PathParam("stageId") final String stageId,
      @Parameter(description = "Enter the component identifier and coordinates of the component for which you want " +
          "to waive the transitive violations.") @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL of the component for which you want to waive the transitive " +
          "violations.") @QueryParam("packageUrl") final String packageUrl,
      @Parameter(description = "Enter the hash for the component for which you want to waive the transitive " +
          "violations ") @QueryParam("hash") final String hash,
      @RequestBody(description = "<ol>" +
          "<li>comment (optional, to indicate the reason of " +
          "the waiver) default value is null</li>" +
          "<li>applyToAllComponents (boolean, default 'false'),deprecated in favor of matcherStrategy. " +
          "If matcherStrategy is not set, 'true' means this will apply the waiver to all components, " +
          "'false' means this will apply to a specific component.</li>" +
          "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, " +
          "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>" +
          "<li>expiryTime (default null) to set the datetime when the waiver expires.</li>" +
          "</ol>",
          required = true) ApiWaiverOptionsDTO apiWaiverOptionsDTO)
  {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(ownerType, ownerId, stageId,
        componentIdentifier, packageUrl, hash, apiWaiverOptionsDTO);
  }

  @GET
  @Path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
  @Audited(AuditEvent.VIEW_WAIVER)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all waivers on policy violations due to transitive " +
      "dependencies for a specific component detected in a specific scan. Any one of the input parameters, i.e. " +
      "componentIdentifier, packageUrl or hash is required. If more than one is provided, the system will pick them " +
      "in the order specified here." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains a list of waivers on transitive policy violations for the " +
                "dependencies of the component specified, for the given scanId.",
            useReturnTypeSchema = true)
      })
  public ApiComponentPolicyWaiversDTO getTransitivePolicyWaiversByAppScanComponent(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain the " +
          "policy violations that are within the scope specified.") @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(
          description = "Enter the corresponding id for the ownerType specified above.") @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the scanId (reportId) of the scan for which you want to retrieve the " +
          "waivers on transitive policy violations occurring due the dependencies of a component.") @PathParam("scanId") final String scanId,
      @Parameter(description = "Enter the component identifier for the component for which you want to retrieve the " +
          "waivers on transitive policy violations, for the specified scanId.") @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL for the component for which you want to retrieve the " +
          "waivers on transitive policy violations, for the specified scanId.") @QueryParam("packageUrl") final String packageUrl,
      @Parameter(description = "Enter the hash for the component for which you want to retrieve the " +
          "waivers on transitive policy violations, for the specified scanId.") @QueryParam("hash") final String hash)
  {
    return apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(ownerType, ownerId, scanId,
        componentIdentifier, packageUrl, hash);
  }

  /**
   * @since 1.164
   * @deprecated since 1.192
   *             Workflow changed. Waiver reviewers now review existing waiver requests instead of creating new waivers.
   *             Please use {@link ApiPolicyWaiverRequestResource#addPolicyWaiverRequestByPolicyViolationId}
   */
  @Deprecated(since = "1.192")
  @POST
  @Path(REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Deprecated since IQ Server 1.192. Triggers a 'Waiver Request' webhook event. " +
          "Deprecated because the webhook event is now integrated into the policy waiver request process. " +
          "Please use `api/v2/policyWaiverRequests{ownerType}/policyViolation/{policyViolationId}` instead. " +
          "Scheduled for removal in December 2025.",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "Waiver request webhook triggered successfully")
      })
  public void requestPolicyWaiver(
      @Parameter(description = "Enter the policyViolationId for which you want to trigger the waiver request event.",
          required = true) @PathParam("policyViolationId") final String policyViolationId,
      @RequestBody(description = "The request JSON should contain" +
          "<ol>" +
          "<li>comment (optional, default null) to indicate the waiver request reason</li>" +
          "<li>policyViolationLink (link to the policy violation page in the Lifecycle UI)</li>" +
          "<li>addWaiverLink (link to the Add Waiver page in the Lifecycle UI)</li>" +
          "</ol>") ApiRequestPolicyWaiverDTO requestWaiverDTO)
  {
    requestPolicyWaiverEventService.postRequestPolicyWaiverEvent(policyViolationId, requestWaiverDTO);
  }
}
