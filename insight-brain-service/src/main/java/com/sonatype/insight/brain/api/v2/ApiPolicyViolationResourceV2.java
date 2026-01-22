/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestsApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestService;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationType;
import com.sonatype.insight.brain.api.v2.service.autowaivers.ApiAutoPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
@Tag(name = "Policy Violation Details",
    description = "Use this REST API to obtain the violation details, violation details across stages " +
        "(cross stage), violations occurring due to transitive dependencies and all waivers applicable to " +
        "a violation." +
        "\n" +
        "\n" +
        "Cross-stage policy violations are helpful in performance analysis like MTTR metrics."
)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_VIOLATIONS)
public class ApiPolicyViolationResourceV2
{
  public static final String CROSS_STAGE_POLICY_VIOLATION_SUBPATH = "crossStage";

  public static final String VIOLATIONID = "/{violationId}";

  public static final String APPLICABLE_WAIVERS_PATH = "/applicableWaivers";

  public static final String APPLICABLE_AUTO_WAIVER_PATH = "/applicableAutoWaiver";

  public static final String APPLICABLE_WAIVER_REQUESTS_PATH = "/applicableWaiverRequests";

  public static final String SIMILAR_WAIVERS_PATH = "/similarWaivers";

  public static final String TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH =
      "transitive/{ownerType: application|organization}/{ownerId}/stages/{stageId}";

  public static final String TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH =
      "transitive/{ownerType: application}/{ownerId}/{scanId}";

  private final ApiPolicyViolationServiceV2 apiPolicyViolationService;

  private final ApiCrossStageViolationService apiCrossStageViolationService;

  private final ApiPolicyWaiverService apiPolicyWaiverService;

  private final ApiAutoPolicyWaiverService apiAutoPolicyWaiverService;

  private final ApiPolicyWaiverRequestService apiPolicyWaiverRequestService;

  @Inject
  public ApiPolicyViolationResourceV2(
      final ApiPolicyViolationServiceV2 apiPolicyViolationService,
      final ApiCrossStageViolationService apiCrossStageViolationService,
      final ApiPolicyWaiverService apiPolicyWaiverService,
      final ApiAutoPolicyWaiverService apiAutoPolicyWaiverService,
      final ApiPolicyWaiverRequestService apiPolicyWaiverRequestService)
  {
    this.apiPolicyViolationService = apiPolicyViolationService;
    this.apiCrossStageViolationService = apiCrossStageViolationService;
    this.apiPolicyWaiverService = apiPolicyWaiverService;
    this.apiAutoPolicyWaiverService = apiAutoPolicyWaiverService;
    this.apiPolicyWaiverRequestService = apiPolicyWaiverRequestService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  @Operation(description = "Use this method to retrieve policy violation details for a policy/policies. " +
      "You will need the policyId(s) to retrieve the policy violations details. " +
      "policyId is available as the response field of the Policies REST API." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of the application that violates the policy/policies " +
                  "and violation details grouped under the policyIds provided. It contains:" +
                  "<ul>" +
                  "<li>`openTime` indicates the date and time when the violation was first detected.</li>" +
                  "<li>`waiveTime` indicates the date and time when the violation was waived.</li>" +
                  "<li>`legacyTime` indicates the date and time when the violation was assigned as a legacy" +
                  " violation.</li>" +
                  "<li>`reference` is the reference data that triggered the violation.</li>" +
                  "</ul>",
              useReturnTypeSchema = true)
      })
  public ApiApplicationViolationListDTOV2 getPolicyViolations(
      @Parameter(description = "Enter the policyIds to obtain the corresponding violation details", required = true)
      @QueryParam("p") final Set<String> policyIds,
      @Parameter(description = "Enter the date (format YYYY-MM-DD) from which you want to retrieve" +
          " the violation details") @QueryParam("openTimeAfter") final String openTimeAfter,
      @Parameter(description = "Enter the date (format YYYY-MM-DD) until which you want to retrieve" +
          " the violation details") @QueryParam("openTimeBefore") final String openTimeBefore,
      @Parameter(description = "Set one or more policy violation type (active, legacy, waived) to include")
      @QueryParam("type") @DefaultValue("ACTIVE") final Set<PolicyViolationType> violationTypes)
  {
    return apiPolicyViolationService.getPolicyViolations(policyIds, openTimeAfter, openTimeBefore,
        violationTypes);
  }

  /**
   * @since 1.86.0
   */
  @GET
  @Path(CROSS_STAGE_POLICY_VIOLATION_SUBPATH + VIOLATIONID)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  @Operation(description = "A cross-stage policy violation represents an aggregate of all violations of the " +
      "same policy, occurring at multiple stages for an application. " +
      "Cross-stage policy violations are helpful in performance analysis by determining " +
      "the time taken to remediate a violation across all stages where it was detected." + "\n" +
      "Use this method to retrieve cross-stage policy violations." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "The response contains violation details for all occurrences of the same policy violation " +
          "across multiple stages. `stageData` indicates the name of the stages where the violation" +
          "occurred, and `reportId` " +
          "where it was reported and the policy action triggered due to the violation.", useReturnTypeSchema = true)
  public ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationById(
      @Parameter(description = "Enter the policy `violationId`. Use the GET method described for the endpoint " +
          "/api/v2/policyViolations to obtain the policy violationId. ", required = true) @PathParam("violationId")
      final String violationId)
  {
    return apiCrossStageViolationService.getCrossStageViolationById(violationId);
  }

  @GET
  @Path(CROSS_STAGE_POLICY_VIOLATION_SUBPATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  @Operation(description = "A cross-stage policy violation represents an aggregate of all violations of a policy  " +
      "occurring across multiple stages of an application. Cross-stage policy violations are helpful in performance " +
      "analysis by determining the time taken to remediate a violation across all stages where it was detected." +
      "\n" +
      "Use this method to retrieve all cross-stage violations, irrespective of the time they were detected." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements"
  )
  @ApiResponse(responseCode = "200",
      description = "The response contains violation details for all occurrences of the same policy violation, " +
          "across multiple stages. stageData indicates the name of the stages where the violation occurred, " +
          "the scanId/reportId when it was reported and the policy action triggered due to the violation.",
      useReturnTypeSchema = true)
  public ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationByConstituentId(
      @Parameter(description = "Enter the violationId. Use the GET method described for the endpoint " +
          "/api/v2/policyViolations to obtain the policy violationId.", required = true) @QueryParam("constituentId")
      final String constituentId)
  {
    return apiCrossStageViolationService.getCrossStageViolationByConstituentId(constituentId);
  }

  /**
   * @since 1.98
   */
  @GET
  @Path(VIOLATIONID + APPLICABLE_WAIVERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS)
  @Operation(description = "Use this method to obtain all existing waivers that are applicable to a policy " +
      "violation. A waiver is considered as 'applicable' if it matches the following conditions:" +
      "<ul>" +
      "<li>The policyId for the policy violation matches the policyId associated with the waiver</li>" +
      "<li>The violated policy conditions match the policy conditions of the waiver/li>" +
      "<li>The waiver scope matches the violating component</li>" +
      "</ul>" +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements"
  )
  @ApiResponse(responseCode = "200",
      description = "The response contains details for all applicable waivers for the `violationId` specified. " +
          "It is grouped under 'activeWaivers' and 'expiredWaivers'. " +
          "`scope` indicates the scope of the applicable waiver. " +
          "Possible values for the enum field `matcherStrategy` are EXACT_COMPONENT, ALL_COMPONENTS," +
          " ALL_VERSIONS)." +
          "\n" +
          "\n" +
          "`reference` shows the reference data that triggered the violation. " +
          "`componentUpgradeAvailable` indicates if a non-violating version of the " +
          "component is available to remediate the violation.",
      useReturnTypeSchema = true)
  public ApiPolicyWaiversApplicableToViolationDTO getApplicableWaivers(
      @Parameter(description = "Enter the policy violationId for which you want to obtain the applicable waivers.",
          required = true) @PathParam("violationId") final String violationId)
  {
    return apiPolicyWaiverService.getApplicableWaivers(violationId);
  }

  @GET
  @Path(VIOLATIONID + APPLICABLE_WAIVER_REQUESTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to obtain all existing waiver requests that are applicable to a policy "
      + "violation. A waiver request is considered as 'applicable' if it matches the following conditions:" + "<ul>"
      + "<li>The policyId for the policy violation matches the policyId associated with the waiver request</li>"
      + "<li>The violated policy conditions match the policy conditions of the waiver request/li>"
      + "<li>The waiver request scope matches the violating component</li>" + "</ul>" + "\n" + "\n"
      + "Permissions required: View IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "The response contains details for all applicable waiver requests for the `violationId` specified. "
          + "It is grouped under 'activeWaiverRequests' and 'expiredWaiverRequests'. "
          + "`scope` indicates the scope of the applicable waiver request. "
          + "Possible values for the enum field `matcherStrategy` are EXACT_COMPONENT, ALL_COMPONENTS,"
          + " ALL_VERSIONS)." //
          + "\n" //
          + "\n" //
          + "`reference` shows the reference data that triggered the violation. "
          + "`componentUpgradeAvailable` indicates if a non-violating version of the "
          + "component is available to remediate the violation.",
      useReturnTypeSchema = true)
  public ApiPolicyWaiverRequestsApplicableToViolationDTO getApplicableWaiverRequests(
      @Parameter(
          description = "Enter the policy violationId for which you want to obtain the applicable waiver requests.",
          required = true) @PathParam("violationId") String violationId)
  {
    return apiPolicyWaiverRequestService.getApplicableWaiverRequests(violationId);
  }

  @GET
  @Path(VIOLATIONID + APPLICABLE_AUTO_WAIVER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS)
  @Operation(description = "Use this method to obtain the existing auto waiver applicable to a policy violation" +
      "violation." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements"
  )
  @ApiResponse(responseCode = "200",
      description = "The response contains details for applicable auto waiver for the `violationId` specified. ",
      useReturnTypeSchema = true)
  public ApiAutoPolicyWaiverDTO getApplicableAutoWaiver(
      @Parameter(description = "Enter the policy violationId for which you want to obtain the applicable auto policy " +
          "waiver ",
          required = true) @PathParam("violationId") final String violationId)
  {
    return apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violationId);
  }

  /**
   * @since 1.115
   */
  @GET
  @Path(VIOLATIONID + SIMILAR_WAIVERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve similar policy waivers for the given policy violation id." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "Successfully retrieved similar policy waivers for the given policy violation id.",
      useReturnTypeSchema = true)
  public List<ApiPolicyWaiverDTO> getSimilarWaivers(
      @Parameter(description = "Policy violation id to find similar waivers for.", required = true)
      @PathParam("violationId") final String violationId)
  {
    return apiPolicyWaiverService.getSimilarWaivers(violationId);
  }

  @GET
  @Path(TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS)
  @Operation(description = "Use this method to obtain all transitive policy violations for a given component in  " +
      "a specific stage. Transitive policy violations are violations caused by transitive dependencies." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements"
  )
  @ApiResponse(responseCode = "204",
      description = "The response contains all transitive violations detected for the component specified. " +
          "In addition to the policy violation details like the name/id of the policy violated, threat level " +
          "threat category, etc. the response also indicates if the violation is due to an 'InnerSource' component.",
      useReturnTypeSchema = true)
  public ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByOwnerStageComponent(
      @Parameter(description = "Possible values are 'application' or 'organization'", required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Possible values are applicationId, organizationId") @PathParam("ownerId")
      final String ownerId,
      @Parameter(description = "Possible values are 'develop', 'source', 'build', 'stage-release', 'release', and, " +
          "'operate'.")
      @PathParam("stageId") final String stageId,
      @Parameter(description = "Enter the component identifier and the coordinates of the component for which " +
          "you want to obtain the transitive violations. This is optional, not required if package URL or hash value " +
          "is provided.") @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL of the component. This is optional, not required if component " +
          "identifier or hash value is provided.") @QueryParam("packageUrl") final String packageUrl,
      @Parameter(description = "Enter the hash value of the component. This is optional, not required if component " +
          "identifier or package URL is provided.") @QueryParam("hash") final String hash)
  {
    apiPolicyViolationService.ensureInnerSourceTransitiveWaiverEnabled();
    return apiPolicyViolationService.getTransitivePolicyViolationsByOwnerStageComponent(ownerType, ownerId, stageId,
        componentIdentifier, packageUrl, hash);
  }

  /**
   * @since 1.117
   */
  @GET
  @Path(TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS)
  @Operation(description = "Use this method to retrieve transitive policy violations for a given component " +
      "in a specific scan." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements")
  @ApiResponse(responseCode = "200",
      description = "The response contains violation details for all transitive violations occurring in the " +
          "scan specified. The response also indicates if the violation is due to an 'InnerSource' component.",
      useReturnTypeSchema = true)
  public ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByAppScanComponent(
      @Parameter(description = "Enter the scope for this violation. " +
          "Possible values are 'application'", required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the identifier for the scope specified above. E.g. applicationId",
          required = true)
      @PathParam("ownerId") final String ownerId,
      @Parameter(description = "Enter the scanId/reportId corresponding to the scan.", required = true)
      @PathParam("scanId") final String scanId,
      @Parameter(description = "Enter the component identifier and the coordinates of the component for which you" +
          " want to retrieve the transitive policy violations. " +
          "This is optional, not required if package URL or hash value is provided.")
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL for the component for which you want to retrieve the " +
          "transitive policy violations in the specific scan.")
      @QueryParam("packageUrl") final String packageUrl,
      @Parameter(description = "Enter the hash value for the component for which you want to retrieve the " +
          "transitive policy violations in the specific scan.")
      @QueryParam("hash") final String hash)
  {
    apiPolicyViolationService.ensureInnerSourceTransitiveWaiverEnabled();
    return apiPolicyViolationService.getTransitivePolicyViolationsByAppScanComponent(ownerType, ownerId, scanId,
        componentIdentifier, packageUrl, hash);
  }
}
