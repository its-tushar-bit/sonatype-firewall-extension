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

import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;

@Named
public class PdfGeneratorService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final BaseUrl baseUrl;

  private final ReportService reportService;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  public PdfGeneratorService(
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      BaseUrl baseUrl,
      ReportService reportService,
      ApiReportDataServiceV2 apiReportDataServiceV2)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.baseUrl = baseUrl;
    this.reportService = reportService;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
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
        .type("application/pdf")
        .header(HttpHeaders.CONTENT_LENGTH, pdfFile.length())
        .header("Content-Disposition", "attachment; filename=\"" + filename + '"')
        .entity(pdfFile);

    return responseBuilder.build();
  }

  public File generateReport(Application app, String scanId) throws IOException {
    File pdfFile = PdfGenerator.getPdfFile(reportService.getReport(app.getId(), scanId));
    PdfGenerator.generate(pdfFile, getBaseUrl(),
        apiReportDataServiceV2.getPolicyViolationsDataNoAuth(app.getPublicId(), scanId),
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), scanId));
    return pdfFile;
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
