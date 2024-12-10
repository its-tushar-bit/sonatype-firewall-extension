/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.FileUtils;

import static com.sonatype.insight.brain.report.ReportPdf.REPORT_FILE_NAME;

@Singleton
public class FileReportDataStore
    implements ReportDataStore
{
  private final InsightWork insightWork;

  private final ReportDownloader reportDownloader;

  private final Configuration configuration;

  @Inject
  public FileReportDataStore(
      final InsightWork insightWork,
      final ReportDownloader reportDownloader,
      final Configuration configuration)
  {
    this.insightWork = insightWork;
    this.reportDownloader = reportDownloader;
    this.configuration = configuration;
  }

  @Override
  public ApplicationReport downloadReport(
      final String applicationId,
      final String scanId,
      final DownloadReportPostAction downloadReportPostAction) throws IOException, NotFoundException
  {
    ApplicationReport applicationReport = getApplicationReport(applicationId, scanId);
    if (!applicationReport.exists()) {
      int reportTimeoutInSeconds = configuration.getReportTimeoutInSeconds();
      ApplicationReport tempApplicationReport = tempReport(applicationReport);
      if (!reportDownloader.downloadReport(scanId, tempApplicationReport, reportTimeoutInSeconds, 5)) {
        throw new NotFoundException("Could not download the report for scan ID " + scanId);
      }
      downloadReportPostAction.apply(scanId, tempApplicationReport, applicationId);
      rename(tempApplicationReport, applicationReport);
    }
    return applicationReport;
  }

  @Override
  public ApplicationReport getApplicationReport(final String appId, final String scanId) {
    return new FileReportEntity(insightWork.getReportFile(appId, scanId));
  }

  @Override
  public FileReportEntity getReportEntityByName(final String applicationId, final String scanId, final String name) {
    return new FileReportEntity(new File(insightWork.getReportDir(applicationId, scanId), name));
  }

  @Override
  public ReportPdf getReportPdf(final String appId, final String scanId) {
    return getReportEntityByName(appId, scanId, REPORT_FILE_NAME);
  }

  /**
   * Moved temporarily from FileReportDataStore because this is an implementation detail of how we download the report
   * zip, and not a DataStore responsibility
   *
   * @param reportFile
   * @return
   */
  private FileReportEntity tempReport(final ApplicationReport reportFile) {
    final File tempFile =
        FileUtils.createTempFile("temp-", ".zip", ((FileReportEntity) reportFile).getFile().getParentFile());
    return new FileReportEntity(tempFile);
  }

  /**
   * Moved temporarily from FileReportDataStore because this is an implementation detail of how we download the report
   * zip, and not a DataStore responsibility
   *
   * @param tempFile
   * @param reportFile
   * @throws IOException
   */
  private void rename(final ApplicationReport tempFile, final ApplicationReport reportFile) throws IOException {
    FileUtils.rename(((FileReportEntity) tempFile).getFile(), ((FileReportEntity) reportFile).getFile());
  }
}
