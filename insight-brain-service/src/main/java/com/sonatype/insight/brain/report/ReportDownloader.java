/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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

  @Inject
  public ReportDownloader(final SaasClient client) {
    this.client = client;
  }

  public boolean downloadReport(final String scanId, final File reportFile, final int retryAttempts,
      final int retryIntervalInSeconds)
  {
    reportFile.getAbsoluteFile().getParentFile().mkdirs();
    try {
      for (int i = 0; i < (retryAttempts + 1); i++) {
        InputStream is = null;
        OutputStream os = null;

        try {
          os = new BufferedOutputStream(new FileOutputStream(reportFile));
          Map<String, String> queryParams = new HashMap<String, String>();
          queryParams.put("scanId", scanId);
          is = client.get(InputStream.class, "rest/ci/report", queryParams);
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
      reportFile.delete();
    }

    return false;
  }

  public static class ReportDownloadReponse
  {
    private int statusCode;

    private Map<String, String> headers = new LinkedHashMap<String, String>();

    private byte[] data;

    public int getStatusCode() {
      return statusCode;
    }

    void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    void setHeader(String name, String value) {
      if (value == null) {
        headers.remove(name);
      }
      else {
        headers.put(name, value);
      }
    }

    public byte[] getData() {
      return data;
    }

    void setData(byte[] data) {
      this.data = data;
    }
  }
}