/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScanUploader
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploader.class);

  private static final String HDS_PATH = "rest/application/analysis";

  private final HdsClient client;

  private final InsightConfig insightConfig;

  @Inject
  public ScanUploader(final HdsClient client, final InsightConfig insightConfig) {
    this.client = client;
    this.insightConfig = insightConfig;
  }

  /**
   * Uploads an existing scan file to the HDS server.
   *
   * @since 1.8
   */
  public ScanReceipt upload(File scanFile, Application application) throws IOException {
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(application);

    ScanReceipt receipt = client.put(analytics, ScanReceipt.class, HDS_PATH, scanFile);

    augmentScanReceipt(application.getPublicId(), receipt);

    return receipt;
  }

  void augmentScanReceipt(String applicationPublicId, ScanReceipt receipt) {
    log.debug("Successfully uploaded scan id {}", receipt.getScanId());
    AuditData.get().setScanId(receipt.getScanId());

    // HDS knows nothing about where CLM Server stores reports, add this info to the receipt.
    receipt.setReportUrl(UserInterfaceLinksResource.getReportUrl(applicationPublicId, receipt.getScanId()));
    receipt.setPdfUrl(UserInterfaceLinksResource.getPdfUrl(applicationPublicId, receipt.getScanId()));
    receipt.setDataUrl(ApiReportDataResourceV2.getDataUrl(applicationPublicId, receipt.getScanId()));
    receipt.setReportTimeoutInSeconds(insightConfig.getReportTimeoutInSeconds());
  }
}
