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

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Singleton
public class ReportDataStore
{
  @FunctionalInterface
  interface DownloadReportPostAction
  {
    void apply(String scanId, ApplicationReport tempApplicationReport, String appId) throws IOException;
  }

  private final ReportDownloader reportDownloader;

  private final Configuration configuration;

  private final ApplicationReportPersistenceService applicationReportPersistenceService;

  @Inject
  public ReportDataStore(
      final ReportDownloader reportDownloader,
      final Configuration configuration,
      final ApplicationReportPersistenceService applicationReportPersistenceService)
  {
    this.reportDownloader = reportDownloader;
    this.configuration = configuration;
    this.applicationReportPersistenceService = applicationReportPersistenceService;
  }

  public ApplicationReport downloadReport(
      final Application application,
      final String scanId,
      final DownloadReportPostAction downloadReportPostAction) throws IOException, NotFoundException
  {
    ApplicationReport applicationReport = getApplicationReport(application, scanId);
    if (!applicationReport.exists()) {
      int reportTimeoutInSeconds = configuration.getReportTimeoutInSeconds();
      if (!reportDownloader.downloadReport(applicationReport, reportTimeoutInSeconds, 5)) {
        throw new NotFoundException("Could not download the report for scan ID " + scanId);
      }
      downloadReportPostAction.apply(scanId, applicationReport, application.getId());
    }
    return applicationReport;
  }

  public ApplicationReport getApplicationReport(final Application application, final String scanId) {
    return new ApplicationReport(applicationReportPersistenceService, application, scanId);
  }

  public ReportPdfEntity getReportPdf(final String appId, final String scanId) {
    return applicationReportPersistenceService.getPdfEntity(appId, scanId);
  }

  public BaseReportEntity getVulnerabilitySignatureJson(final String applicationId, final String scanId) {
    return applicationReportPersistenceService.getVulnerabilitySignaturesEntity(applicationId, scanId);
  }

  public void deleteReportPdf(final String appId, final String scanId) throws IOException {
    applicationReportPersistenceService.getPdfEntity(appId, scanId).deleteIfExists();
  }

  public void moveApplicationReport(
      final String appId,
      final String sourceScanId,
      final String destinationScanId) throws IOException
  {
    applicationReportPersistenceService.moveReport(appId, sourceScanId, destinationScanId);
  }
}
