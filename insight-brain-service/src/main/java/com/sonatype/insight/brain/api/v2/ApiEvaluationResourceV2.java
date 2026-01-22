/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentEvaluationServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiPromoteScanServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlEvaluationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2)
@Tag(name = "Policy Evaluation", description = "Use this REST API to perform an application " +
    "policy evaluation. Policy evaluations are executed asynchronously." +
    "<p>" +
    "This is a 2-step process that involves: \n" +
    "1. Requesting a policy evaluation (POST) \n" +
    "2. Checking the status and response of the evaluation request (GET)"
)
public class ApiEvaluationResourceV2
{
  public static final String PROMOTE_SCAN_PATH = "{applicationId}/promoteScan";

  public static final String SOURCE_CONTROL_EVALUATION_PATH = "{applicationId}/sourceControlEvaluation";

  private final ApiComponentEvaluationServiceV2 componentEvaluationService;

  private final ApiPromoteScanServiceV2 promoteScanService;

  private final ApiSourceControlEvaluationService sourceControlEvaluationService;

  @Inject
  public ApiEvaluationResourceV2(
      final ApiComponentEvaluationServiceV2 componentEvaluationService,
      final ApiPromoteScanServiceV2 apiPromoteScanServiceV2,
      ApiSourceControlEvaluationService sourceControlEvaluationService)
  {
    this.componentEvaluationService = componentEvaluationService;
    this.promoteScanService = apiPromoteScanServiceV2;
    this.sourceControlEvaluationService = sourceControlEvaluationService;
  }

  @Path("{applicationId}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_AD_HOC)
  @Operation(description = "Use this method to request a component evaluation. This is step 1 of the 2 step " +
      "policy evaluation for components process." +
      "\n" +
      "\n" +
      "Permissions Required: Evaluate Components",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description =
                  "The JSON response contains resultId that will be assigned to the evaluation results, " +
                      "timestamp when the component evaluation was requested, " +
                      "the applicationId of the " +
                      "component and the results URL. The resultId obtained from here can be used " +
                      "to retrieve the evaluation result using the REST API or the result URL can be used in cURL. ",
              useReturnTypeSchema = true)
      }
  )
  @ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
  public ApiComponentEvaluationTicketDTOV2 evaluateComponents(
      @Parameter(description = "Enter the internal applicationId. Use the Applications REST API to retrieve the " +
          "internal applicationId.", required = true)
      @PathParam("applicationId") final String applicationId,
      @RequestBody(
          description = "The request JSON should contain component coordinates " +
              "or the hash (SHA1) for each component. You can provide the packageURL instead of component information" +
              " or hash."
      ) final ApiComponentEvaluationRequestDTOV2 evaluationRequest)
  {
    return componentEvaluationService.evaluateComponents(applicationId, evaluationRequest);
  }

  @Path("{applicationId}/results/{resultId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_EVALUATION_AD_HOC)
  @Operation(description = "This is step 2 of the policy evaluation process for components. Use the " +
      "resultId obtained from the POST response for the corresponding applicationId. " +
      "\n" +
      "\n" +
      "Permissions Required: Evaluate Components ",
      responses = {
          @ApiResponse(
              responseCode = "404",
              description = "Response not ready "
          ),
          @ApiResponse(
              responseCode = "200",
              description = "The response contains details for the policy evaluation request including " +
                  "submitted date, evaluation date, applicationId and the results of the evaluation for " +
                  "the component(s).",
              useReturnTypeSchema = true
          )
      })
  @ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
  public ApiComponentEvaluationResultDTOV2 getComponentEvaluation(
      @Parameter(description = "Enter the internal applicationId (same as that sent in the POST request (step 1))",
          required = true)
      @PathParam("applicationId") final String applicationId,
      @Parameter(description = "Enter the resultId obtained from the POST response (step 1) used for component " +
          "evaluation.", required = true)
      @PathParam("resultId") final String resultId)
      throws IOException
  {
    return componentEvaluationService.getComponentEvaluation(applicationId, resultId);
  }

  @Path(PROMOTE_SCAN_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(value = AuditEvent.EVALUATE_APPLICATION)
  @Operation(description = "Use this method to rescan older scans. This is done when the binaries of a " +
      "previous build are now moving to a new stage in the production pipeline. Using this method, you can " +
      "avoid rebuilding the application and reuse the scan metadata at the newer stage. This new evaluation will " +
      "evaluate the most recent security and license data against your current policies. " +
      "\n" +
      "\n" +
      "Permissions Required: Evaluate Applications",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response will contain the statusUrl to view the evaluation result using the " +
                  "GET method (step 2)",
              useReturnTypeSchema = true
          )
      }
  )
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
  public ApiApplicationEvaluationStatusDTOV2 promoteScan(
      @Parameter(description = "Enter the internal applicationId. Use the Applications REST API to retrieve the " +
          "internal applicationId.", required = true)
      @PathParam("applicationId") final String applicationId,
      @RequestBody(description = "You can provide either the scanId (reportId) of " +
          "the previous scan OR the source stageId (possible values " +
          "are 'build', 'stage-release', 'release' or 'operate'). When using the stageId, the latest scanId " +
          "for the application will be used. Enter the targetStageId for the new stage you want your scan to be " +
          "promoted to (possible values are 'build', 'stage-release', 'release' or 'operate'). Using the same value " +
          "for source and target stage will resubmit the latest scan report.")
      final ApiPromoteScanRequestDTOV2 promoteScanRequest,
      @Context HttpServletRequest request)
  {
    return promoteScanService.promoteScan(applicationId, promoteScanRequest, HdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.101
   */
  @Path(SOURCE_CONTROL_EVALUATION_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(value = AuditEvent.EVALUATE_APPLICATION)
  @Operation(description = "Use this method to request a source control evaluation for a specific application. " +
      "This is step 1 of the 2 step source control evaluation process. " +
      "\n" +
      "\n" +
      "Permissions Required: Evaluate Applications",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description =
                  "The response contains statusUrl. Use this statusUrl to check the evaluation status using " +
                      "the GET method (step 2 of the evaluation process). ",
              useReturnTypeSchema = true
          )
      }
  )
  @ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
  public ApiApplicationEvaluationStatusDTOV2 evaluateSourceControl(
      @Parameter(description = "Enter the internal applicationId. Use the Applications REST API to retrieve the " +
          "internal applicationId.", required = true)
      @PathParam("applicationId") String applicationId,
      @RequestBody(description = "The request JSON should include the 1. branch name (name of the target branch in " +
          "the source control repository, 2. stageId (recommended values are 'develop' for feature branches, and " +
          "'source' for default branches. " +
          "Other stageIds that can be used are 'build', 'stage-release', 'release', 'operate' " +
          "but are not recommended), 3. scanTargets (optional, specify one or more paths inside the repository. " +
          "If not specified, the entire repository will be evaluated by default). Ensure that the repository paths " +
          "are not relative and do not contain '../' or '..\\'."
      )
      ApiSourceControlEvaluationRequestDTO sourceControlEvaluationRequest,
      @Context HttpServletRequest request)
  {
    return sourceControlEvaluationService.evaluateSourceControl(applicationId, sourceControlEvaluationRequest,
        HdsClient.getClientUserAgent(request));
  }

  @GET
  @Path("{applicationId}/status/{statusId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "This is step 2 of the policy evaluation process. Use the statusUrl obtained " +
      "from the POST response for the corresponding applicationId. " +
      "\n" +
      "\n" +
      "Permissions Required: Evaluate Applications",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response will include one of the 3 possible status values: PENDING (indicates that " +
                  "the evaluation is still in progress), FAILED or COMPLETED. For completed evaluations, " +
                  "the response " +
                  "will contain the URLs for evaluation report to view the evaluation results.",
              useReturnTypeSchema = true
          )
      })
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION) 
  public ApiApplicationEvaluationResultDTOV2 getApplicationEvaluationStatus(
      @Parameter(description = "Enter the applicationId, for the which policy evaluation was requested.",
          required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the statusId value obtained as response of the POST call in step 1.")
      @PathParam("statusId") String statusId)
  {
    return promoteScanService.getApplicationEvaluationStatus(applicationId, statusId);
  }
}
