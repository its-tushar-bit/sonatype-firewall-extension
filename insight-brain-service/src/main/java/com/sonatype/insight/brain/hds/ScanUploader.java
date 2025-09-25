/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

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
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ItemContentType;

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

  private final CpeMatchingConfigurationService cpeMatchingConfigurationService;

  private final ProductLicense productLicense;

  @Inject
  public ScanUploader(
      final HdsClient client,
      final Configuration configuration,
      final CpeMatchingConfigurationService cpeMatchingConfigurationService,
      final ProductLicense productLicense)
  {
    this.client = client;
    this.configuration = configuration;
    this.cpeMatchingConfigurationService = cpeMatchingConfigurationService;
    this.productLicense = productLicense;
  }

  /**
   * Uploads an existing scan file to the HDS server.
   *
   * @since 1.8
   */
  ScanReceipt upload(
      ScanEntity scanEntity,
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

    boolean isCpeDataMatchingEnabled = false;

    if (thirdPartyScanContext != null
        && thirdPartyScanContext.getContainerItemContentType() == ItemContentType.CONTAINER_URI_SONATYPE
        && SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled()
        && productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)) {
      isCpeDataMatchingEnabled = true;
    }
    else if (thirdPartyScanContext == null || thirdPartyScanContext.getContainerItemContentType() == null) {
      isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(application.getId());
    }

    uploadMetadata.put("enableCpeDataMatching", Boolean.toString(isCpeDataMatchingEnabled));
    ScanReceipt receipt =
        client.put(analytics, ScanReceipt.class, clientUserAgent, HDS_PATH, scanEntity, uploadMetadata);
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
      String reportUrl = StageTypes.PROXY.getId().equals(stageTypeId)
          ? UserInterfaceLinksHelper.getFirewallContainerImageEvaluationReportUrl(
              applicationPublicId, receipt.getScanId())
          : UserInterfaceLinksHelper.getReportUrl(applicationPublicId, receipt.getScanId());
      receipt.setReportUrl(reportUrl);
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
