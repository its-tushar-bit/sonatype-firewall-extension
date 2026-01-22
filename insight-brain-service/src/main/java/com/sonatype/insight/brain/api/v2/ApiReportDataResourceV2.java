/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportViolationsDiffService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 *
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
@Tag(name = "Application Report Data",
    description = "Use this REST API to retrieve the data from an application composition report, " +
        "that is generated after an evaluation.")
public class ApiReportDataResourceV2
{
  public static final String SCAN_PATH = "{scanId}";

  public static final String RAW_DATA_PATH = "raw";

  public static final String POLICY_DATA_PATH = "policy";

  public static final String VIOLATION_DIFF_PATH = "policyViolations/diff";

  public static final String DEPENDENCY_TREE_PATH = "dependencyTree";

  private final ApiReportDataServiceV2 reportDataService;

  private final BaseUrl baseUrl;

  private final ApiReportViolationsDiffService apiReportViolationsDiffService;

  @Inject
  public ApiReportDataResourceV2(
      final ApiReportDataServiceV2 reportDataService,
      final BaseUrl baseUrl,
      final ApiReportViolationsDiffService apiReportViolationsDiffService)
  {
    this.reportDataService = reportDataService;
    this.baseUrl = baseUrl;
    this.apiReportViolationsDiffService = apiReportViolationsDiffService;
  }

  /**
   * NOTE: prior to IQ 63, this endpoint was the actual implementation that is now at the RAW_DATA_PATH, rather than a
   * redirect
   */
  @GET
  @Path(SCAN_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  @Operation(
      description = "This is an older version of the endpoint. This call will now be redirected to " +
          "/api/v2/applications/{applicationPublicId}/reports/{scanId}/raw.",
      responses = {
          @ApiResponse(
              responseCode = "307",
              description = "Temporary redirect to the new endpoint."
          )
      }
  )
  public Response getData(
      @Parameter(description = "Enter the applicationPublicId for the evaluated application.",
          required = true)
      @PathParam("applicationPublicId") String applicationPublicId,
      @Parameter(description = "Enter the scanId (reportId) of the application report created after the evaluation. ",
          required = true)
      @PathParam("scanId") String scanId) throws Exception
  {
    return Response.temporaryRedirect(new URI(baseUrl.get()).resolve(getDataUrl(applicationPublicId, scanId))).build();
  }

  /**
   * Gets the JSON data for the report of the given application and scan.
   *
   * @since 1.63
   */
  @GET
  @Path(SCAN_PATH + "/" + RAW_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  @Operation(
      description = "Use this method to retrieve the 'raw' data generated as a result of an application evaluation." +
          " 'raw' data includes: the components identified in the application, and the licenses and vulnerabilities " +
          "associated with the identified components." +
          "/n" +
          "/n" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response fields contain the 'raw' data for the reportId (scanId) specified " +
                  "in the method call. The fields corresponding to 'dependencyData' will indicate if the " +
                  "component is a direct dependency (true/false), an InnerSource component" +
                  "(true/false), the associated parentComponentPurls (package URLs of the parent component " +
                  "ownerApplicationName (name of the owner application), ownerApplicatonId (internal ID " +
                  "of the owner application, innerSourceComponentPurl (the package URL of the InnerSource" +
                  "Component.)",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiReportRawDataDTOV2 getRawData(
      @Parameter(description = "Enter the applicationPublicId (assigned at the time of creating a new application.) ",
          required = true) @PathParam("applicationPublicId") String applicationPublicId,
      @Parameter(description = "Enter the reportId (scanId) created at the time of evaluating the application. " +
          "application.") @PathParam("scanId") String scanId) throws Exception
  {
    AuditData.get().setReportId(scanId);
    return reportDataService.getRawData(applicationPublicId, scanId);
  }

  /**
   * Gets the JSON data for the policy violations in the report of the given application and scan.
   *
   * @since 1.64
   */
  @GET
  @Path(SCAN_PATH + "/" + POLICY_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_VIOLATIONS)
  @Operation(
      description = "Use this method to retrieve the policy violation data generated as a result of an" +
          " application evaluation, for each component identified in the application evaluation." +
          "/n" +
          "/n" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response fields contain the policy violation data for the reportId (scanId) " +
                  "specified in the method call. The fields corresponding to 'violations' include the " +
                  "violation details for each policy, for the component.",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiReportPolicyDataDTOV2 getPolicyViolations(
      @Parameter(description = "Enter the applicationPublicId created at the time of creating the " +
          "application.", required = true) @PathParam("applicationPublicId") String applicationPublicId,
      @Parameter(description = "Enter the reportId (scanId) created at the time of evaluating the " +
          "application.", required = true) @PathParam("scanId") String scanId,
      @Parameter(description = "Set to true to include policy violation times (open, legacy, waived, fixed) in the" +
          " response if set.")
      @QueryParam("includeViolationTimes") @DefaultValue("false") boolean includeViolationTimes) throws Exception
  {
    AuditData.get().setReportId(scanId);
    return reportDataService.getPolicyViolationsData(applicationPublicId, scanId, includeViolationTimes);
  }

  @GET
  @Path(SCAN_PATH + "/" + DEPENDENCY_TREE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  @Operation(
      description = "Use this method to retrieve the dependencies related to the component identified at the " +
          "time of application evaluation. This is currently available only for Java (Maven) and NPM applications." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response fields contain the 'Dependency Tree' data  under the 'children' section. " +
                  "The 'children' section may contain more tree nodes. " +
                  "Every direct dependency can have zero or more transitive dependencies. " +
                  "Each tree node contains the packageUrl, component identifier and a dependency tree node (if it " +
                  "exists.) The component identifier section contains the format and coordinates for the component.",
              useReturnTypeSchema = true),
          @ApiResponse(
              responseCode = "400",
              description = "Missing or invalid parameter."
          ),
          @ApiResponse(
              responseCode = "404",
              description = "The requested dependency tree was not found."
          )
      }
  )
  public ApiDependencyTreeResponseDTO getDependencyTree(
      @Parameter(description = "Enter the applicationPublicId created at the time of creating the " +
          "application.", required = true) @PathParam("applicationPublicId") String applicationPublicId,
      @Parameter(description = " Enter the reportId (scanId) created at the time of evaluating the " +
          "application.", required = true) @PathParam("scanId") String scanId) throws Exception
  {
    AuditData.get().setReportId(scanId);
    return reportDataService.getDependencyTree(applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to this REST resource for the given application and scan.
   */
  public static String getDataUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(ApiReportDataResourceV2.SCAN_PATH)
        .path(ApiReportDataResourceV2.RAW_DATA_PATH)
        .build(applicationPublicId, scanId).toString();
  }

  @GET
  @Path(VIOLATION_DIFF_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_VIOLATIONS)
  @Operation(
      description = "By configuring Lifecycle with SCM, policy evaluations can be linked to the Git commit hash. " +
          "Use this method to compare the violations between policy evaluations for 2 commits, " +
          "by providing the linked commit hashes." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains the violation details grouped under addedViolations, " +
                  "sameViolations and removedViolations for the two policy evaluations being compared.",
              useReturnTypeSchema = true),
          @ApiResponse(
              responseCode = "400",
              description = "Missing or invalid parameter. Check if the policy evaluations are still available, " +
                  "based on the Data Retention Policies."
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Policy violation diff could not be determined for the given request."
          )
      }
  )
  public ApiPolicyViolationDiffDTO getPolicyViolationDiff(
      @Parameter(description = "Enter the applicationPublicId, created at the time of creating " +
          "the application", required = true) @PathParam("applicationPublicId") final String applicationPublicId,
      @Parameter(description = "Enter the commit hash linked to the earlier policy evaluation.", required = true)
      @QueryParam("fromCommit") final String fromCommit,
      @Parameter(description = "Enter the commit hash linked to the other (later) policy evaluation to compare.",
          required = true)
      @QueryParam("toCommit") final String toCommit,
      @Parameter(description = "Enter the policy evaluation Id linked to the earlier policy evaluation to compare")
      @QueryParam("fromPolicyEvaluationId") final String fromPolicyEvaluationId,
      @Parameter(description = "Enter the policy evaluation Id linked to the other (later) policy evaluation " +
          "to compare")
      @QueryParam("toPolicyEvaluationId") final String toPolicyEvaluationId,
      @Parameter(description = "Set to true to include policy violation times (open, legacy, waived, fixed) in the" +
          " response if set.")
      @QueryParam("includeViolationTimes") @DefaultValue("false") boolean includeViolationTimes)
  {
    return apiReportViolationsDiffService
        .getPolicyViolationDiff(applicationPublicId, fromCommit, toCommit, fromPolicyEvaluationId,
            toPolicyEvaluationId, includeViolationTimes);
  }
}
