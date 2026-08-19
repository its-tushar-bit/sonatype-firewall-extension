/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.LatestReportInformation;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.report.pdf.PdfGeneratorService;
import com.sonatype.insight.brain.sbom.policy.SbomPolicyService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * HRC-scoped sibling of {@link ReportResource}. Read handlers and {@link #reevaluatePolicy}
 * dispatch through the same Owner-scoped service methods as the App path.
 * Authorization for the read handlers lives on the service methods they delegate to, not on the
 * handlers themselves: the Owner-typed {@code ReportService} and {@code PdfGeneratorService} read
 * methods carry {@code @Authorize(READ)} with {@code @AuthzContext(Key.OWNER)}.
 * {@link #reevaluatePolicy} is the exception and carries its own handler-level {@code @Authorize} —
 * see that method for why.
 */
@Named
@Singleton
@Timed
@Path(HostedRepositoryComponentReportResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class HostedRepositoryComponentReportResource
{
  public static final String RESOURCE_PATH = "rest/report/hostedRepositoryComponent/{hrcId}";

  public static final String BROWSE_PATH = "{scanId}/browseReport";

  public static final String METADATA_PATH = "{scanId}/metadata";

  public static final String PRINT_PATH = "{scanId}/printReport";

  public static final String SBOM_PRINT_PATH = "sbom/{sbomVersion}/printReport";

  public static final String SBOM_POLICY_VIOLATION_REPORT = "sbom/{sbomVersion}/sbomPolicyViolationReport";

  public static final String AUDIT_LOG_PATH = "{scanId}/auditLog/{path}";

  public static final String LATEST_REPORT_INFO_PATH = "{stageTypeId}/latestReportInformation";

  public static final String REEVALUATE_PATH = "{scanId}/reevaluatePolicy";

  private final ReportService reportService;

  private final PdfGeneratorService pdfGeneratorService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private final AuditLogReader auditLogReader;

  private final SbomPolicyService sbomPolicyService;

  private final ApplicationService applicationService;

  @Inject
  public HostedRepositoryComponentReportResource(
      ReportService reportService,
      PdfGeneratorService pdfGeneratorService,
      HostedRepositoryComponentDAO hostedRepositoryComponentDAO,
      AuditLogReader auditLogReader,
      SbomPolicyService sbomPolicyService,
      ApplicationService applicationService)
  {
    this.reportService = reportService;
    this.pdfGeneratorService = pdfGeneratorService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
    this.auditLogReader = auditLogReader;
    this.sbomPolicyService = sbomPolicyService;
    this.applicationService = applicationService;
  }

  @GET
  @Path(BROWSE_PATH + "/{path:.*}")
  @Audited(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public ReportEntry browseReport(
      @PathParam("hrcId") final String hrcId,
      @PathParam("scanId") final String scanId,
      @PathParam("path") final String path)
  {
    return reportService.processBrowseReport(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), scanId, path);
  }

  @GET
  @Path(METADATA_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public ReportMetadataDTO getReportMetadata(
      @PathParam("hrcId") final String hrcId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    return reportService.getReportMetadata(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), scanId);
  }

  @GET
  @Path(PRINT_PATH)
  @Audited(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  @Produces("application/pdf")
  public Response printReport(
      @PathParam("hrcId") final String hrcId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    return pdfGeneratorService.printReport(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), scanId);
  }

  @GET
  @Path(SBOM_PRINT_PATH)
  @Audited(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces("application/pdf")
  public Response printSbomReport(
      @PathParam("hrcId") final String hrcId,
      @PathParam("sbomVersion") final String sbomVersion) throws IOException
  {
    return pdfGeneratorService.printSbomReport(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), sbomVersion);
  }

  @GET
  @Path(AUDIT_LOG_PATH)
  public Response auditLog(
      @PathParam("hrcId") final String hrcId,
      @PathParam("path") final String path,
      @QueryParam("key") final String encodedKey) throws IOException
  {
    return auditLogReader.readAuditLog(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), path, encodedKey);
  }

  @GET
  @Path(SBOM_POLICY_VIOLATION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  public Response getSbomPolicyViolationReport(
      @PathParam("hrcId") final String hrcId,
      @PathParam("sbomVersion") final String sbomVersion,
      @QueryParam("componentRef") final String componentRef,
      @QueryParam("fileCoordinateId") final String fileCoordinateId,
      @QueryParam("hash") final String hash,
      @Context final HttpServletRequest httpRequest) throws IOException
  {
    HostedRepositoryComponent hrc = hostedRepositoryComponentDAO.getByIdNotNull(hrcId);
    ReportEntry policyThreatsReportEntry = sbomPolicyService.getPolicyViolationsReportEntry(hrc, sbomVersion);

    if (policyThreatsReportEntry == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    if (!StringUtils.isAllBlank(fileCoordinateId, componentRef, hash)) {
      JsonNode jsonNode = sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(hrc, sbomVersion,
          componentRef, fileCoordinateId, hash, policyThreatsReportEntry, null);

      if (jsonNode != null) {
        ResponseBuilder response = Response.ok(jsonNode);
        response.lastModified(new Date(policyThreatsReportEntry.time));
        response.type(httpRequest.getServletContext().getMimeType(policyThreatsReportEntry.name));
        return response.build();
      }
      else {
        // There are no policy violations for the given component
        return Response.ok(JsonNodeFactory.instance.objectNode()).type(MediaType.APPLICATION_JSON_TYPE).build();
      }
    }
    else {
      ResponseBuilder response = Response.ok(policyThreatsReportEntry.buf);
      response.lastModified(new Date(policyThreatsReportEntry.time));
      response.type(httpRequest.getServletContext().getMimeType(policyThreatsReportEntry.name));
      return response.build();
    }
  }

  @GET
  @Path(LATEST_REPORT_INFO_PATH)
  public LatestReportInformation getLatestReportInformation(
      @PathParam("hrcId") final String hrcId,
      @PathParam("stageTypeId") final String stageTypeId)
  {
    return applicationService.getLatestReportInformation(
        hostedRepositoryComponentDAO.getByIdNotNull(hrcId), stageTypeId);
  }

  /**
   * Re-evaluates a hosted-repository component's scan against current policy.
   * <p>
   * The permission is enforced here rather than on the service method, gating on the raw
   * {@code hrcId} via {@link Key#HOSTED_REPOSITORY_COMPONENT_ID} so the check runs <em>before</em>
   * {@code getByIdNotNull} — an unauthorized caller gets {@code 403} rather than a {@code 404} that
   * would disclose whether the component exists.
   */
  @POST
  @Path(REEVALUATE_PATH)
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
  public Response reevaluatePolicy(
      @AuthzContext(Key.HOSTED_REPOSITORY_COMPONENT_ID) @PathParam("hrcId") final String hrcId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    reportService.reevaluateHostedComponent(hostedRepositoryComponentDAO.getByIdNotNull(hrcId).getId(), scanId);
    return Response.ok().build();
  }
}
