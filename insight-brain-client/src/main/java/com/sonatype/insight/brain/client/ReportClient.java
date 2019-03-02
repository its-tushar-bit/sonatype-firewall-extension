/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;

public final class ReportClient
    extends AbstractRequestClient
{
  private final String serverUrl;

  private final String appId;

  private final String scanId;

  public ReportClient(final Configuration config, final String appId, final String scanId) {
    super(config);

    if (scanId == null || scanId.trim().isEmpty()) {
      throw new IllegalArgumentException("Cannot create a ReportClient without a scanId");
    }
    this.serverUrl = config.getServerUrl();
    this.appId = UrlUtils.encodeUrlComponent(appId);
    this.scanId = UrlUtils.encodeUrlComponent(scanId);
  }

  public String linkToReport() {
    return UrlUtils.appendUrlPaths(serverUrl, "ui/links/application", appId, "report", scanId);
  }

  /**
   * Download the self-contained ZIP bundle of the specified report for use by 3rd-party integrators like HP Fortify.
   *
   * @since 1.10
   */
  public void downloadBundle(File bundleFile) throws IOException {
    final Result result = path("rest/report", appId, scanId, "downloadBundle").get();
    verifyStatusCode(result);

    byte[] data = result.data();
    try (FileOutputStream fos = new FileOutputStream(bundleFile)) {
      fos.write(data);
    }
  }

  /**
   * Prepares the report for an expanded coverage scan to be available when the customer loads it in a browser.
   * It waits for the report to become available on the HDS.
   * 
   * @since 1.37
   */
  public void prepareExpandedCoverageReport() throws IOException {
    Result result = path("rest/report", appId, scanId, "prepareReport").post(null /* entity */);
    verifyStatusCode(result);
  }
}
