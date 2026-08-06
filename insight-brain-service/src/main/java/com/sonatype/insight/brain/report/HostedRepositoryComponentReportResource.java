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
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.report.pdf.PdfGeneratorService;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * HRC-scoped sibling of {@link ReportResource}. Read handlers dispatch through the same
 * Owner-scoped service methods as the App path. The write handlers (reevaluate) are added in
 * CLM-43710, when the reevaluation pipeline is rewritten onto {@code ScanPolicyEvaluator}.
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

  // TODO CLM-43710: add the HRC reevaluatePolicy / reevaluatePolicyStatus write handlers here.
  // The reevaluation pipeline is rewritten onto ScanPolicyEvaluator.evaluate(hrc, ...) in that
  // ticket; the HRC-scoped POST/status endpoints land alongside it.
}
