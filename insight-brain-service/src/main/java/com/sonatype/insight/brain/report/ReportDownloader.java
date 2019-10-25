/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ReportDownloader
{
  private static final Logger log = LoggerFactory.getLogger(ReportDownloader.class);

  static final String HDS_PATH = "rest/application/analysis/{scanId}";

  static final int BAD_GATEWAY_RETRY_LIMIT = 5;

  private final HdsClient client;

  private final FileCleaner fileCleaner;

  @Inject
  public ReportDownloader(final HdsClient client, final FileCleaner fileCleaner) {
    this.client = client;
    this.fileCleaner = fileCleaner;
  }

  /**
   * Downloads a report for a scan.
   * @param scanId of the report
   * @param reportFile to save report to
   * @param reportTimeoutInSeconds time to wait before the report times out - 0 will not make retry attempts
   * @return true if the report was downloaded, false otherwise.
   */
  public boolean downloadReport(final String scanId,
                                final File reportFile,
                                final int reportTimeoutInSeconds,
                                final int retryIntervalInSeconds)
  {
    log.debug("Downloading report for scan {} with timeout {} s", scanId, reportTimeoutInSeconds);
    final long endTime = System.currentTimeMillis() + reportTimeoutInSeconds * 1000;
    int badGatewayRetryCount = 0;
    try {
      do {
        InputStream is = null;
        OutputStream os = null;

        try {
          is = client.get(InputStream.class, HDS_PATH, null, scanId);
          // Create the parent dir after the client returns with success
          // to ensure dir is not created for unknown scanId (or other errors)
          reportFile.getAbsoluteFile().getParentFile().mkdirs();
          os = new BufferedOutputStream(new FileOutputStream(reportFile));
          IOUtil.copy(is, os);
          return true;
        }
        catch (NotFoundException e) {
          long currentTime = System.currentTimeMillis();
          if (currentTime >= endTime) {
            throw new NotFoundException(timeoutExceptionMessage(scanId));
          }
          Thread.sleep(Math.min(retryIntervalInSeconds * 1000, endTime - currentTime));
        }
        catch (BadGatewayException e) {
          if (++badGatewayRetryCount >= BAD_GATEWAY_RETRY_LIMIT) {
            throw e;
          }
          Thread.sleep(retryIntervalInSeconds * 1000);
        }
        finally {
          IOUtil.close(is);
          IOUtil.close(os);
        }
      }
      while (true);
    }
    catch (final Exception e) {
      // don't leave an incomplete file around
      log.error(e.getMessage(), e);
      try {
        fileCleaner.delete(reportFile);
      }
      catch (FileDeletionException fde) {
        log.error("Could not delete incomplete report: {}", reportFile, fde);
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
