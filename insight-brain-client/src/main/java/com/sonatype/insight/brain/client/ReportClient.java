/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;

import org.apache.http.client.HttpResponseException;
import org.codehaus.plexus.util.IOUtil;

public final class ReportClient
    extends AbstractClient
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
    final int status = result.status();
    if (status >= 300) {
      throw new HttpResponseException(status, result.message());
    }

    byte[] data = result.data();
    FileOutputStream fos = new FileOutputStream(bundleFile);
    try {
      IOUtil.copy(data, fos);
    } finally {
      fos.close();
    }
  }
}
