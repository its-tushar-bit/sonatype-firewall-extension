/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.DefaultApiReportDataResourceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScanUploader
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploader.class);

  public static final String HDS_PATH = "rest/application/analysis";

  public static final int BAD_GATEWAY_ATTEMPT_LIMIT = 5;

  public static final Duration BAD_GATEWAY_RETRY_WAIT = Duration.ofSeconds(1);

  private final HdsClient client;

  private final Configuration configuration;

  @Inject
  public ScanUploader(final HdsClient client, final Configuration configuration) {
    this.client = client;
    this.configuration = configuration;
  }

  /**
   * Uploads an existing scan file to the HDS server.
   *
   * @since 1.8
   */
  public ScanReceipt upload(File scanFile, Application application, String stageTypeId, String clientUserAgent)
      throws IOException
  {
    return upload(scanFile, application, BAD_GATEWAY_RETRY_WAIT, stageTypeId, clientUserAgent);
  }

  ScanReceipt upload(
      File scanFile,
      Application application,
      Duration retryWait,
      String stageTypeId,
      String clientUserAgent)
      throws IOException
  {
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(application);

    ScanReceipt receipt = null;
    int retryCount = 0;
    String uploadId = UUID.randomUUID().toString().replace("-", "");

    while (receipt == null) {
      try {
        Map<String, String> uploadMetadata = new HashMap<>();

        // enable HDS to reason about retries
        uploadMetadata.put("uploadId", uploadId);
        if (retryCount > 0) {
          uploadMetadata.put("retryCount", Integer.toString(retryCount));
        }

        // Third party scan uploads which have no stage and our own scan
        // uploads which do have a stage both use this function.
        if (stageTypeId != null && !stageTypeId.isEmpty()) {
          uploadMetadata.put("stageTypeId", stageTypeId);
        }

        Map<String, String> matcherConfiguration = configuration.getMatcherConfiguration();
        if (MapUtils.isNotEmpty(matcherConfiguration)) {
          uploadMetadata.putAll(matcherConfiguration);
        }

        receipt = client.put(analytics, ScanReceipt.class, clientUserAgent, HDS_PATH, scanFile, uploadMetadata);
      }
      catch (BadGatewayException e) {
        if (++retryCount >= BAD_GATEWAY_ATTEMPT_LIMIT) {
          throw e;
        }
        log.info("failed to upload scan, retrying", e);
        try {
          Thread.sleep(retryWait.toMillis());
        }
        catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
          //root cause is the BadGatewayException, let's throw that.
          throw e;
        }
      }
    }

    augmentScanReceipt(application.getPublicId(), receipt);

    return receipt;
  }

  void augmentScanReceipt(String applicationPublicId, ScanReceipt receipt) {
    log.debug("Successfully uploaded scan id {}", receipt.getScanId());
    AuditData.get().setScanId(receipt.getScanId());

    // HDS knows nothing about where CLM Server stores reports, add this info to the receipt.
    receipt.setReportUrl(UserInterfaceLinksHelper.getReportUrl(applicationPublicId, receipt.getScanId()));
    receipt.setPdfUrl(UserInterfaceLinksHelper.getPdfUrl(applicationPublicId, receipt.getScanId()));
    receipt.setDataUrl(DefaultApiReportDataResourceV2.getDataUrl(applicationPublicId, receipt.getScanId()));
    receipt.setReportTimeoutInSeconds(configuration.getReportTimeoutInSeconds());
  }
}
