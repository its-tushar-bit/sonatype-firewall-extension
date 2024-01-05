/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.version.VersionService;

@Named
public class PdfGeneratorService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final BaseUrl baseUrl;

  private final VersionService versionService;

  private final ReportService reportService;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public PdfGeneratorService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final BaseUrl baseUrl,
      final VersionService versionService,
      final ReportService reportService,
      final ApiReportDataServiceV2 apiReportDataServiceV2,
      final ClusterLockManager clusterLockManager)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.baseUrl = baseUrl;
    this.versionService = versionService;
    this.reportService = reportService;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.clusterLockManager = clusterLockManager;
  }

  @Authorize(permission = Permission.READ)
  public Response printReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      String scanId) throws IOException
  {
    AuditData.get().setReportId(scanId);
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    File pdfFile = generateReport(app, scanId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    String stageName = StageTypes.getById(policyEvaluation.getStageTypeId()).getName();
    String filename = app.getName() + "-" + stageName + "-" +
        new SimpleDateFormat("yyyyMMdd-HHmmss").format(policyEvaluation.getTime()) + ".pdf";
    ResponseBuilder responseBuilder = Response.ok()
        .lastModified(policyEvaluation.getTime()).expires(new Date())
        .type("application/pdf; charset=UTF-8")
        .encoding("UTF-8")
        .header(HttpHeaders.CONTENT_LENGTH, pdfFile.length())
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename))
        .entity(pdfFile);

    return responseBuilder.build();
  }

  public File generateReport(Application app, String scanId) throws IOException {
    File pdfFile = PdfGenerator.getPdfFile(reportService.getReport(app.getId(), scanId));
    PdfData pdfData = new PdfData();
    pdfData.baseUrl = getBaseUrl();
    pdfData.productVersion = versionService.getShortVersion();
    pdfData.policyData = apiReportDataServiceV2.getPolicyViolationsDataNoAuth(app.getPublicId(), scanId);
    pdfData.rawData = apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), scanId, true);
    augmentEmptyLicensesAsNotProvided(pdfData.rawData);
    try (ClusterLock clusterLock = clusterLockManager.createForPdfGeneration(app, scanId)) {
      clusterLock.lock(LockType.SHARED);
      if (isGenerated(pdfFile)) {
        return pdfFile;
      }
    }
    // The pdf file has not been generated so try to generate it
    try (ClusterLock clusterLock = clusterLockManager.createForPdfGeneration(app, scanId)) {
      clusterLock.lock();
      generate(pdfFile, pdfData);
    }
    return pdfFile;
  }

  // Visible for testing
  boolean isGenerated(File file) {
    return file.exists() && file.length() > 0;
  }

  // Visible for testing
  void generate(File pdfFile, PdfData pdfData) throws IOException {
    PdfGenerator.generate(pdfFile, pdfData);
  }

  // Visible for testing
  void augmentEmptyLicensesAsNotProvided(ApiReportRawDataDTOV2 rawData) {
    for (ApiReportComponentDTOV2 component : rawData.components) {
      if (component.licenseData == null) {
        continue;
      }
      if (component.licenseData.effectiveLicenses.isEmpty() && component.licenseData.declaredLicenses.isEmpty() &&
          component.licenseData.observedLicenses.isEmpty()) {
        ApiLicenseDTO license = new ApiLicenseDTO();
        license.licenseName = "Not Provided";
        component.licenseData.effectiveLicenses.add(license);
        component.licenseData.declaredLicenses.add(license);
        component.licenseData.observedLicenses.add(license);
      }
    }
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
