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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ReportDownloader
{
  private static final Logger log = LoggerFactory.getLogger(ReportDownloader.class);

  private final SaasClient client;
  
  private final FileCleaner fileCleaner;

  @Inject
  public ReportDownloader(final SaasClient client, final FileCleaner fileCleaner) {
    this.client = client;
    this.fileCleaner = fileCleaner;
  }

  public boolean downloadReport(final String scanId, final File reportFile, final int retryAttempts,
      final int retryIntervalInSeconds)
  {
    try {
      for (int i = 0; i < (retryAttempts + 1); i++) {
        InputStream is = null;
        OutputStream os = null;

        try {
          Map<String, String> queryParams = new HashMap<>();
          queryParams.put("scanId", scanId);
          is = client.get(InputStream.class, "rest/ci/report", queryParams);
          // Create the parent dir after the client returns with success
          // to ensure dir is not created for unknown scanId (or other errors)
          reportFile.getAbsoluteFile().getParentFile().mkdirs();
          os = new BufferedOutputStream(new FileOutputStream(reportFile));
          IOUtil.copy(is, os);
          return true;
        }
        catch (NotFoundException e) {
          if (retryAttempts == 0 || i >= retryAttempts) {
            throw e;
          }
          if (retryIntervalInSeconds > 0) {
            Thread.sleep(retryIntervalInSeconds * 1000);
          }
        }
        finally {
          IOUtil.close(is);
          IOUtil.close(os);
        }
      }
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
}
