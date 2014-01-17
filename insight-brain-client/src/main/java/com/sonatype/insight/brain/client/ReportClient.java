/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.UrlUtils;

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
}
