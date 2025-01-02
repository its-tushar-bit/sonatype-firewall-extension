/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ReportDownloader
{
  private static final Logger log = LoggerFactory.getLogger(ReportDownloader.class);

  static final String HDS_PATH = "rest/application/analysis/{scanId}";

  private final HdsClient client;

  private final FileCleaner fileCleaner;

  @Inject
  public ReportDownloader(final HdsClient client, final FileCleaner fileCleaner) {
    this.client = client;
    this.fileCleaner = fileCleaner;
  }

  /**
   * Downloads a report for a scan.
   *
   * @param scanId                 of the report
   * @param tempApplicationReport             to save report to
   * @param reportTimeoutInSeconds time to wait before the report times out - 0 will not make retry attempts
   * @return true if the report was downloaded, false otherwise.
   */
  public boolean downloadReport(
      final String scanId,
      final ApplicationReport tempApplicationReport,
      final int reportTimeoutInSeconds,
      final int retryIntervalInSeconds)
  {
    log.debug("Downloading report for scan {} with timeout {} s", scanId, reportTimeoutInSeconds);
    try {
      try (InputStream is = client.get(
          new Retry(HDS_PATH, 4, Duration.ofSeconds(reportTimeoutInSeconds), BadGatewayException.class::isInstance,
              NotFoundException.class::isInstance, i -> Duration.ofSeconds(retryIntervalInSeconds)),
          InputStream.class,
          HDS_PATH, null, scanId)) {
        // Create the parent dir after the client returns with success
        // to ensure dir is not created for unknown scanId (or other errors)
        Files.createDirectories(
            ((FileApplicationReport) tempApplicationReport).getFile().getAbsoluteFile().getParentFile().toPath());
        try (OutputStream os = new BufferedOutputStream(
            Files.newOutputStream(((FileApplicationReport) tempApplicationReport).getFile().toPath()))) {
          IOUtils.copy(is, os);
          return true;
        }
      }
      catch (NotFoundException e) {
        throw new NotFoundException(timeoutExceptionMessage(scanId));
      }
    }
    catch (Exception e) {
      // don't leave an incomplete file around
      log.error(e.getMessage(), e);
      try {
        fileCleaner.delete(((FileApplicationReport) tempApplicationReport).getFile());
      }
      catch (FileDeletionException fde) {
        log.error("Could not delete incomplete report: {}", tempApplicationReport, fde);
      }
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
