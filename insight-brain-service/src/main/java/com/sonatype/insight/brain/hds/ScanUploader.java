/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScanUploader
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploader.class);

  public static final String HDS_PATH = "rest/application/analysis";

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
  ScanReceipt upload(
      File scanFile,
      Application application,
      String stageTypeId,
      String clientUserAgent,
      ThirdPartyScanContext thirdPartyScanContext)
      throws IOException
  {
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(application);
    String uploadId = UUID.randomUUID().toString().replace("-", "");
    Map<String, String> uploadMetadata = new HashMap<>();
    // enable HDS to reason about retries
    uploadMetadata.put("uploadId", uploadId);
    // Third party scan uploads which have no stage and our own scan
    // uploads which do have a stage both use this function.
    if (stageTypeId != null && !stageTypeId.isEmpty()) {
      uploadMetadata.put("stageTypeId", stageTypeId);
    }
    Map<String, String> matcherConfiguration = configuration.getMatcherConfiguration();
    if (MapUtils.isNotEmpty(matcherConfiguration)) {
      uploadMetadata.putAll(matcherConfiguration);
    }
    ScanReceipt receipt = client.put(analytics, ScanReceipt.class, clientUserAgent, HDS_PATH, scanFile, uploadMetadata);
    augmentScanReceipt(application.getPublicId(), receipt, stageTypeId, thirdPartyScanContext);
    return receipt;
  }

  void augmentScanReceipt(
      String applicationPublicId,
      ScanReceipt receipt,
      String stageTypeId,
      ThirdPartyScanContext thirdPartyScanContext)
  {
    log.debug("Successfully uploaded scan id {} for stageType {}", receipt.getScanId(), stageTypeId);
    AuditData.get().setScanId(receipt.getScanId());

    // HDS knows nothing about where CLM Server stores reports, add this info to the receipt.
    if (StageTypes.COMPLIANCE.getId().equals(stageTypeId) && thirdPartyScanContext.getApplicationVersion() != null) {
      updateReceiptForCompliance(applicationPublicId, receipt, thirdPartyScanContext);
    }
    else {
      receipt.setReportUrl(UserInterfaceLinksHelper.getReportUrl(applicationPublicId, receipt.getScanId()));
      receipt.setPdfUrl(UserInterfaceLinksHelper.getPdfUrl(applicationPublicId, receipt.getScanId()));
      receipt.setDataUrl(ApiReportDataResourceV2.getDataUrl(applicationPublicId, receipt.getScanId()));
      receipt.setPrioritiesUrl(UserInterfaceLinksHelper.getPrioritiesUrl(applicationPublicId, receipt.getScanId()));
      receipt.setIntegrationsPrioritiesUrl(
          UserInterfaceLinksHelper.getIntegrationsPrioritiesUrl(applicationPublicId, receipt.getScanId()));
      receipt.setReportTimeoutInSeconds(configuration.getReportTimeoutInSeconds());
    }
  }

  private void updateReceiptForCompliance(
      String applicationPublicId,
      ScanReceipt receipt,
      ThirdPartyScanContext thirdPartyScanContext)
  {
    String bomPath = UserInterfaceLinksHelper.getSBOMBillOfMaterialPath(applicationPublicId,
        thirdPartyScanContext.getApplicationVersion());
    receipt.setReportUrl(bomPath);
    receipt.setPdfUrl(bomPath + "/pdf");
  }
}
