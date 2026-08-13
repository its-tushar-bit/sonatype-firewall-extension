/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * HRC-scoped sibling of {@link ApiReportDataResourceV2}.
 * Provides report data for hosted repository components using the HRC row UUID.
 */
@Named
@Timed
@Path(PublicApiPaths.HOSTED_REPOSITORY_COMPONENT_REPORT_DATA_RESOURCE_PATH_V2)
@Tag(name = "Hosted Repository Component Report Data",
    description = "Use this REST API to retrieve the data from a hosted repository component report, " +
        "that is generated after an evaluation.")
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class ApiHostedRepositoryComponentReportDataResourceV2
{
  private final ApiReportDataServiceV2 reportDataService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public ApiHostedRepositoryComponentReportDataResourceV2(
      final ApiReportDataServiceV2 reportDataService,
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.reportDataService = reportDataService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  /**
   * Gets the JSON data for the report of the given hosted repository component and scan.
   */
  @GET
  @Path(ApiReportDataResourceV2.SCAN_PATH + "/" + ApiReportDataResourceV2.RAW_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(
      description = "Use this method to retrieve the 'raw' data generated as a result of a hosted repository " +
          "component evaluation. 'raw' data includes: the components identified in the scan, and the licenses " +
          "and vulnerabilities associated with the identified components. " +
          "Optionally set `includeCustomSecurityVulnerabilityData=true` to include any configured security " +
          "vulnerability custom data (remediation, cweId, cvssVector, cvssSeverity) for each securityIssue. " +
          "NOT to be confused with `SecurityVulnerabilityOverride` (hash-based violation-state override)." +
          "\n" +
          "\n" +
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
            useReturnTypeSchema = true)
      })
  public ApiReportRawDataDTOV2 getRawData(
      @Parameter(description = "Enter the hosted repository component ID (UUID).",
          required = true) @PathParam("hrcId") String hrcId,
      @Parameter(
          description = "Enter the reportId (scanId) created at the time of evaluating the hosted repository component.") @PathParam("scanId") String scanId,
      @Parameter(description = "Set to true to include security vulnerability custom data (remediation, cweId, "
          + "cvssVector, cvssSeverity) for each securityIssue. Defaults to false.") @QueryParam("includeCustomSecurityVulnerabilityData") @DefaultValue("false") boolean includeCustomSecurityVulnerabilityData) throws Exception
  {
    AuditData.get().setReportId(scanId);
    HostedRepositoryComponent hrc = hostedRepositoryComponentDAO.getByIdNotNull(hrcId);
    return reportDataService.getRawData(hrc, scanId, includeCustomSecurityVulnerabilityData);
  }

  /**
   * Gets the JSON data for the policy violations in the report of the given hosted repository component and scan.
   */
  @GET
  @Path(ApiReportDataResourceV2.SCAN_PATH + "/" + ApiReportDataResourceV2.POLICY_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(
      description = "Use this method to retrieve the policy violation data generated as a result of a " +
          "hosted repository component evaluation, for each component identified in the evaluation." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response fields contain the policy violation data for the reportId (scanId) " +
                "specified in the method call. The fields corresponding to 'violations' include the " +
                "violation details for each policy, for the component. " +
                "When 'page' and 'pageSize' are provided, the response includes 'page', 'pageSize', " +
                "'pageCount', and 'total' fields to support pagination.",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination parameters.")
      })
  public ApiReportPolicyDataDTOV2 getPolicyViolations(
      @Parameter(description = "Enter the hosted repository component ID (UUID).",
          required = true) @PathParam("hrcId") String hrcId,
      @Parameter(description = "Enter the reportId (scanId) created at the time of evaluation.",
          required = true) @PathParam("scanId") String scanId,
      @Parameter(description = "Set to true to include policy violation times (open, legacy, waived, fixed) in the"
          + " response if set.") @QueryParam("includeViolationTimes") @DefaultValue("false") boolean includeViolationTimes,
      @Parameter(description = "Page number (1-indexed). Must be provided together with 'pageSize'. "
          + "When omitted, all components are returned.") @QueryParam("page") @Min(1) Integer page,
      @Parameter(description = "Number of components per page (1-500). Must be provided together with 'page'. "
          + "When omitted, all components are returned.") @QueryParam("pageSize") @Min(1) @Max(500) Integer pageSize) throws Exception
  {
    AuditData.get().setReportId(scanId);
    HostedRepositoryComponent hrc = hostedRepositoryComponentDAO.getByIdNotNull(hrcId);
    return reportDataService.getPolicyViolationsData(hrc, scanId, includeViolationTimes, page, pageSize);
  }

  /**
   * Gets the dependency tree for the given hosted repository component and scan.
   */
  @GET
  @Path(ApiReportDataResourceV2.SCAN_PATH + "/" + ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(
      description = "Use this method to retrieve the dependencies related to the component identified at the " +
          "time of hosted repository component evaluation. This is currently available only for Java (Maven) " +
          "and NPM components." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response fields contain the 'Dependency Tree' data under the 'children' section. " +
                "The 'children' section may contain more tree nodes. " +
                "Every direct dependency can have zero or more transitive dependencies. " +
                "Each tree node contains the packageUrl, component identifier and a dependency tree node (if it " +
                "exists.) The component identifier section contains the format and coordinates for the component.",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "400",
            description = "Missing or invalid parameter."),
        @ApiResponse(
            responseCode = "404",
            description = "The requested dependency tree was not found.")
      })
  public ApiDependencyTreeResponseDTO getDependencyTree(
      @Parameter(description = "Enter the hosted repository component ID (UUID).",
          required = true) @PathParam("hrcId") String hrcId,
      @Parameter(description = " Enter the reportId (scanId) created at the time of evaluation.",
          required = true) @PathParam("scanId") String scanId) throws Exception
  {
    AuditData.get().setReportId(scanId);
    HostedRepositoryComponent hrc = hostedRepositoryComponentDAO.getByIdNotNull(hrcId);
    return reportDataService.getDependencyTree(hrc, scanId);
  }
}
