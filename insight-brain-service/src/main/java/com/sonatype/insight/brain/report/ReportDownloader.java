/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ReportDownloader
{
  private static final Logger log = LoggerFactory.getLogger(ReportDownloader.class);

  static final String HDS_PATH = "rest/application/analysis/{scanId}";

  private final HdsClient client;

  private final LifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Inject
  public ReportDownloader(
      final HdsClient client,
      final LifecycleReportPersistenceService lifecycleReportPersistenceService)
  {
    this.client = client;
    this.lifecycleReportPersistenceService = lifecycleReportPersistenceService;
  }

  /**
   * Downloads a report for a scan.
   *
   * @param lifecycleReport to save report to
   * @param reportTimeoutInSeconds time to wait before the report times out - 0 will not make retry attempts
   * @return true if the report was downloaded, false otherwise.
   */
  public boolean downloadReport(
      final LifecycleReport lifecycleReport,
      final int reportTimeoutInSeconds,
      final int retryIntervalInSeconds)
  {
    String ownerId = lifecycleReport.getOwner().getId();
    String scanId = lifecycleReport.getScanId();

    log.debug("Downloading report for scan {} with timeout {} s", scanId, reportTimeoutInSeconds);
    try {
      Retry retryConfig = new Retry(
          HDS_PATH,
          4,
          Duration.ofSeconds(reportTimeoutInSeconds),
          BadGatewayException.class::isInstance,
          NotFoundException.class::isInstance,
          i -> Duration.ofSeconds(retryIntervalInSeconds));
      try (InputStream is = client.get(retryConfig, InputStream.class, HDS_PATH, null, scanId)) {
        lifecycleReportPersistenceService.saveOriginalReport(ownerId, scanId, is);
        return true;
      }
      catch (NotFoundException e) {
        throw new NotFoundException(timeoutExceptionMessage(scanId));
      }
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return false;
  }

  static String timeoutExceptionMessage(String scanId) {
    return "Report timeout exceeded for scan id: " + scanId + System.lineSeparator() +
        "Consider one of the following:" + System.lineSeparator() +
        "- Reduce the size of the scan." + System.lineSeparator() +
        "- If you have proprietary JARs, make sure to configure them properly via Proprietary Component Configuration.";
  }
}
