/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.report.pdf.PdfGeneratorService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

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

  public static final String REEVALUATE_PATH = "{scanId}/reevaluatePolicy";

  private final ReportService reportService;

  private final PdfGeneratorService pdfGeneratorService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public HostedRepositoryComponentReportResource(
      ReportService reportService,
      PdfGeneratorService pdfGeneratorService,
      HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.reportService = reportService;
    this.pdfGeneratorService = pdfGeneratorService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
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
