/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.report.pdf.PdfGenerator.Context;
import com.sonatype.insight.brain.sbom.export.SbomExportParams;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExporterProvider;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class PdfGeneratorService
{
  private final ApplicationDAO applicationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final BaseUrl baseUrl;

  private final VersionService versionService;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final ClusterLockManager clusterLockManager;

  private final SbomExporterProvider sbomExporterProvider;

  private final ReportDataStore reportDataStore;

  @Inject
  public PdfGeneratorService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final BaseUrl baseUrl,
      final VersionService versionService,
      final ApiReportDataServiceV2 apiReportDataServiceV2,
      final ClusterLockManager clusterLockManager,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final SbomExporterProvider sbomExporterProvider,
      final ReportDataStore reportDataStore)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.baseUrl = baseUrl;
    this.versionService = versionService;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.clusterLockManager = clusterLockManager;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.sbomExporterProvider = sbomExporterProvider;
    this.reportDataStore = reportDataStore;
  }

  @Authorize(permission = Permission.READ)
  public Response printReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      String scanId) throws IOException
  {
    AuditData.get().setReportId(scanId);
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    ReportPdfEntity reportPdf = generateReport(app, scanId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    String stageName = StageTypes.getById(policyEvaluation.getStageTypeId()).getName();
    String filename = app.getName() + "-" + stageName + "-" +
        new SimpleDateFormat("yyyyMMdd-HHmmss").format(policyEvaluation.getTime()) + ".pdf";
    return buildPdfResponse(reportPdf, policyEvaluation.getTime(), filename);
  }

  public Response printSbomReport(
      final String applicationIdOrPublicId,
      final String sbomVersion) throws IOException
  {
    return printSbomReport(applicationDAO.getByIdOrPublicIdNotNull(applicationIdOrPublicId), sbomVersion);
  }

  @Authorize(permission = Permission.READ)
  public Response printSbomReport(
      @AuthzContext(Key.APPLICATION) final Application application,
      final String sbomVersion) throws IOException
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(), sbomVersion);
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(
          String.format("SBOM version '%s' not found for application '%s'.", sbomVersion, application.getPublicId()));
    }
    ThirdPartyScan thirdPartyScan =
        thirdPartyScanDAO.getByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    ReportPdfEntity sbomReportPdf = generateSbomReport(application, thirdPartyScan.getScanId(), sbomVersion);

    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), thirdPartyScan.getScanId());
    String filename = application.getName() + "-" + sbomVersion + ".pdf";
    return buildPdfResponse(sbomReportPdf, policyEvaluation.getTime(), filename);
  }

  public ReportPdfEntity generateReport(Application app, String scanId) throws IOException {
    PdfData pdfData = PdfData.createPdfData(
        getBaseUrl(),
        versionService.getShortVersion(),
        apiReportDataServiceV2.getPolicyViolationsDataNoAuth(app.getPublicId(), scanId, false),
        augmentEmptyLicensesAsNotProvided(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), scanId, true)));
    return generateReport(app, scanId, pdfData, false, Context.LIFECYCLE);
  }

  public ReportPdfEntity generateSbomReport(Application app, String scanId, String sbomVersion) throws IOException {
    ApiReportRawDataDTOV2 reportRawData =
        augmentEmptyLicensesAsNotProvided(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), scanId, true));
    PdfData pdfData = sbomExporterProvider.get(getSbomExportParams(app, sbomVersion, reportRawData)).exportPdf();
    return generateReport(app, scanId, pdfData, true, Context.SBOM);
  }

  private SbomExportParams getSbomExportParams(
      Application app,
      String sbomVersion,
      final ApiReportRawDataDTOV2 reportRawData)
  {
    ThirdPartySbomMetadata sbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(app.getId(), sbomVersion);
    return SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withReportRawData(reportRawData)
        .withExportSpecification(ExportSpecification.PDF);
  }

  private Response buildPdfResponse(ReportPdfEntity reportPdf, Date lastModified, String filename) throws IOException {
    ResponseBuilder responseBuilder = Response.ok()
        .lastModified(lastModified)
        .expires(new Date())
        .type("application/pdf; charset=UTF-8")
        .header(HttpHeaders.CONTENT_LENGTH, reportPdf.length())
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename))
        .entity(reportPdf.getInputStream());

    return responseBuilder.build();
  }

  public ReportPdfEntity generateReport(
      Application app,
      String scanId,
      PdfData pdfData,
      boolean overwrite,
      Context productContext) throws IOException
  {
    ReportPdfEntity reportPdf = reportDataStore.getReportPdf(app.getId(), scanId);

    if (!overwrite) {
      try (ClusterLock clusterLock = clusterLockManager.createForPdfGeneration(app, scanId)) {
        clusterLock.lock(LockType.SHARED);
        if (isGenerated(reportPdf)) {
          return reportPdf;
        }
      }
    }
    // The pdf file has not been generated so try to generate it
    try (ClusterLock clusterLock = clusterLockManager.createForPdfGeneration(app, scanId)) {
      clusterLock.lock();
      if (overwrite) {
        reportDataStore.deleteReportPdf(app.getId(), scanId);

      }
      generate(reportPdf, pdfData, productContext);
    }
    return reportPdf;
  }

  // Visible for testing
  boolean isGenerated(ReportPdfEntity reportPdf) throws IOException {
    return reportPdf.exists() && reportPdf.length() > 0;
  }

  // Visible for testing
  void generate(ReportPdfEntity reportPdf, PdfData pdfData, Context productContext) throws IOException {
    PdfGenerator.generate(reportPdf, pdfData, productContext);
  }

  // Visible for testing
  ApiReportRawDataDTOV2 augmentEmptyLicensesAsNotProvided(ApiReportRawDataDTOV2 rawData) {
    for (ApiReportComponentDTOV2 component : rawData.components) {
      if (component.licenseData == null) {
        continue;
      }
      if (component.licenseData.effectiveLicenses.isEmpty() && component.licenseData.declaredLicenses.isEmpty() &&
          component.licenseData.observedLicenses.isEmpty())
      {
        ApiLicenseDTO license = new ApiLicenseDTO();
        license.licenseName = "Not Provided";
        component.licenseData.effectiveLicenses.add(license);
        component.licenseData.declaredLicenses.add(license);
        component.licenseData.observedLicenses.add(license);
      }
    }
    return rawData;
  }

  private String getBaseUrl() {
    try {
      return baseUrl.getConfigured();
    }
    catch (IllegalStateException e) {
      return null;
    }
  }
}
