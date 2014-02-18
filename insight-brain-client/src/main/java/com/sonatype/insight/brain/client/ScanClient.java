/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

public class ScanClient
    extends AbstractClient
{
  private static final ContentType GZIP_CONTENT_TYPE = ContentType.create("application/x-gzip");

  private final String serverUrl;

  private final String appId;

  public ScanClient(final Configuration config, final String appId) {
    super(config);

    this.serverUrl = config.getServerUrl();
    this.appId = UrlUtils.encodeUrlComponent(appId);
  }

  public ScanReceipt uploadCiScan(final File scanFile) throws IOException {
    final Result result = path("rest/ci/scan", appId).put(new FileEntity(scanFile, GZIP_CONTENT_TYPE));
    return handleUpload(result);
  }

  public ScanReceipt uploadRepoManScan(final File scanFile) throws IOException {
    final Result result = path("rest/rm/scan", appId).put(new FileEntity(scanFile, GZIP_CONTENT_TYPE));
    return handleUpload(result);
  }

  private ScanReceipt handleUpload(Result result) throws IOException {
    final int status = result.status();
    final String text = result.text();
    if (status >= 300) {
      throw new HttpResponseException(status, (text == null || text.isEmpty()) ? result.reason() : text);
    }
    return JsonUtils.parse(text, ScanReceipt.class);
  }

  /**
   * Exports links to the results of the scan to the specified output JSON file for use by 3rd-party tools.
   * 
   * @since 1.10
   */
  public void saveResultData(File resultFile, ScanReceipt receipt) throws IOException {
    ResultData resultData = new ResultData();
    resultData.applicationId = appId;
    resultData.scanId = receipt.getScanId();
    resultData.reportHtmlUrl = receipt.resolveReportUrl(serverUrl);
    resultData.reportPdfUrl = receipt.resolvePdfUrl(serverUrl);
    resultData.reportDataUrl = receipt.resolveDataUrl(serverUrl);
    JsonUtils.write(resultFile, resultData);
  }
}
