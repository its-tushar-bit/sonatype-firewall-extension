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
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
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
 * HRC-scoped sibling of {@link ApiReachabilityEvidenceResource}. Delegates to the same
 * {@link ApiReachabilityEvidenceService} via its Owner-typed overload.
 */
@Named
@Timed
@Path(PublicApiPaths.HOSTED_REPOSITORY_COMPONENT_REACHABILITY_EVIDENCE_RESOURCE_PATH)
@Tag(name = "Reachability Evidence",
    description = "Use this REST API to retrieve reachability evidence showing call paths to vulnerable methods "
        + "for a hosted repository component.")
@ProductLicenseEnforcementPoint(LicensedFeature.CALL_FLOW_ANALYSIS)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class ApiHostedRepositoryComponentReachabilityEvidenceResource
{
  private final ApiReachabilityEvidenceService evidenceService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public ApiHostedRepositoryComponentReachabilityEvidenceResource(
      final ApiReachabilityEvidenceService evidenceService,
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.evidenceService = evidenceService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  @GET
  @Path("{vulnerabilityId}/reachability-evidence")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.EXPORT_REACHABILITY_EVIDENCE)
  @Operation(
      description = "Get reachability evidence for a specific vulnerability showing call paths from entry points "
          + "to the vulnerable method, for a hosted repository component."
          + "\n\nPermissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Returns reachability evidence with call paths for the specified vulnerability.",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "404",
            description = "Hosted repository component, report, or vulnerability evidence not found.")
      })
  public ApiReachabilityEvidenceResponse getReachabilityEvidence(
      @Parameter(description = "The hosted repository component ID (UUID)",
          required = true) @AuthzContext(AuthzContext.Key.HOSTED_REPOSITORY_COMPONENT_ID) @PathParam("hrcId") String hrcId,
      @Parameter(description = "The report/scan ID", required = true) @PathParam("reportId") String reportId,
      @Parameter(description = "The vulnerability ID (e.g., CVE-2023-35116)",
          required = true) @PathParam("vulnerabilityId") String vulnerabilityId) throws IOException
  {
    AuditData.get().setReportId(reportId);

    HostedRepositoryComponent hrc = hostedRepositoryComponentDAO.getByIdNotNull(hrcId);

    ApiReachabilityEvidenceResponse response =
        evidenceService.getEvidenceForVulnerability(hrc, reportId, vulnerabilityId);

    if (response == null) {
      throw new NotFoundException(
          String.format("Reachability evidence not found for vulnerability %s in report %s for HRC %s",
              vulnerabilityId, reportId, hrcId));
    }

    return response;
  }
}
