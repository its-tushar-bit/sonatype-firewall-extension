/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API endpoint for retrieving reachability evidence for vulnerabilities.
 *
 */
@Named
@Timed
@Path(PublicApiPaths.REACHABILITY_EVIDENCE_RESOURCE_PATH)
@Tag(name = "Reachability Evidence",
    description = "Use this REST API to retrieve reachability evidence showing call paths to vulnerable methods.")
@ProductLicenseEnforcementPoint(LicensedFeature.CALL_FLOW_ANALYSIS)
public class ApiReachabilityEvidenceResource
{

  private final ApiReachabilityEvidenceService evidenceService;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ApiReachabilityEvidenceResource(
      ApiReachabilityEvidenceService evidenceService,
      ApplicationDAO applicationDAO)
  {
    this.evidenceService = evidenceService;
    this.applicationDAO = applicationDAO;
  }

  @GET
  @Path("{vulnerabilityId}/reachability-evidence")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = com.sonatype.insight.brain.model.security.Permission.READ)
  @Audited(AuditEvent.EXPORT_REACHABILITY_EVIDENCE)
  @Operation(
      description = "Get reachability evidence for a specific vulnerability showing call paths from entry points to the vulnerable method."
          +
          "\\n\\nPermissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Returns reachability evidence with call paths for the specified vulnerability.",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "404",
            description = "Application, report, or vulnerability evidence not found.")
      })
  public ApiReachabilityEvidenceResponse getReachabilityEvidence(
      @Parameter(description = "The public ID of the application",
          required = true) @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @Parameter(description = "The report/scan ID", required = true) @PathParam("reportId") String reportId,
      @Parameter(description = "The vulnerability ID (e.g., CVE-2023-35116)",
          required = true) @PathParam("vulnerabilityId") String vulnerabilityId) throws IOException
  {
    AuditData.get().setReportId(reportId);

    Application application = applicationDAO.getByPublicId(applicationPublicId);
    if (application == null) {
      throw new NotFoundException("Application not found with public ID: " + applicationPublicId);
    }

    ApiReachabilityEvidenceResponse response = evidenceService.getEvidenceForVulnerability(
        application.getId(), reportId, vulnerabilityId);

    if (response == null) {
      throw new NotFoundException(
          String.format("Reachability evidence not found for vulnerability %s in report %s for application %s",
              vulnerabilityId, reportId, applicationPublicId));
    }

    return response;
  }
}
