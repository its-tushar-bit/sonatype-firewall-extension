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

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Singleton
public class ReportDataStore
{
  @FunctionalInterface
  public interface DownloadReportPostAction
  {
    void apply(String scanId, LifecycleReport tempLifecycleReport, String appId) throws IOException;
  }

  private final ReportDownloader reportDownloader;

  private final Configuration configuration;

  private final LifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Inject
  public ReportDataStore(
      final ReportDownloader reportDownloader,
      final Configuration configuration,
      final LifecycleReportPersistenceService lifecycleReportPersistenceService)
  {
    this.reportDownloader = reportDownloader;
    this.configuration = configuration;
    this.lifecycleReportPersistenceService = lifecycleReportPersistenceService;
  }

  public LifecycleReport downloadReport(
      final Owner owner,
      final String scanId,
      final DownloadReportPostAction downloadReportPostAction) throws IOException, NotFoundException
  {
    LifecycleReport applicationReport = getLifecycleReport(owner, scanId);
    if (!applicationReport.exists()) {
      int reportTimeoutInSeconds = configuration.getReportTimeoutInSeconds();
      if (!reportDownloader.downloadReport(applicationReport, reportTimeoutInSeconds, 5)) {
        throw new NotFoundException("Could not download the report for scan ID " + scanId);
      }
      downloadReportPostAction.apply(scanId, applicationReport, owner.getId());
    }
    return applicationReport;
  }

  public LifecycleReport getLifecycleReport(final Owner owner, final String scanId) {
    return new LifecycleReport(lifecycleReportPersistenceService, owner, scanId);
  }

  public ReportPdfEntity getReportPdf(final String appId, final String scanId) {
    return lifecycleReportPersistenceService.getPdfEntity(appId, scanId);
  }

  public BaseReportEntity getVulnerabilitySignatureJson(final String applicationId, final String scanId) {
    return lifecycleReportPersistenceService.getVulnerabilitySignaturesEntity(applicationId, scanId);
  }

  public void saveReportFile(
      final String appId,
      final String scanId,
      final String name,
      final java.io.InputStream content) throws java.io.IOException
  {
    lifecycleReportPersistenceService.saveReportFile(appId, scanId, name, content);
  }

  public void deleteReportPdf(final String appId, final String scanId) throws IOException {
    lifecycleReportPersistenceService.getPdfEntity(appId, scanId).deleteIfExists();
  }

  public void moveLifecycleReport(
      final String appId,
      final String sourceScanId,
      final String destinationScanId) throws IOException
  {
    lifecycleReportPersistenceService.moveReport(appId, sourceScanId, destinationScanId);
  }
}
