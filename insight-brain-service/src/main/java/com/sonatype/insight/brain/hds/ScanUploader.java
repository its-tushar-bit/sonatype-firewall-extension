/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ItemContentType;

import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

  private final IntegrationVersionCache integrationVersionCache;

  @Inject
  public ScanUploader(
      final HdsClient client,
      final Configuration configuration,
      final CpeMatchingConfigurationService cpeMatchingConfigurationService,
      final ProductLicense productLicense,
      final IntegrationVersionCache integrationVersionCache)
  {
    this.client = client;
    this.configuration = configuration;
    this.cpeMatchingConfigurationService = cpeMatchingConfigurationService;
    this.productLicense = productLicense;
    this.integrationVersionCache = integrationVersionCache;
  }

  /**
   * Uploads an existing scan file to the HDS server on behalf of any {@link Owner}.
   *
   * @since 1.8
   */
  public ScanReceipt upload(
      ScanEntity scanEntity,
      Owner owner,
      String stageTypeId,
      String clientUserAgent,
      ThirdPartyScanContext thirdPartyScanContext,
      boolean isWebUIRequest) throws IOException
  {
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(owner);

    validateIntegrationVersion(clientUserAgent, isWebUIRequest);

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
        && productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION))
    {
      isCpeDataMatchingEnabled = true;
    }
    else if (thirdPartyScanContext == null || thirdPartyScanContext.getContainerItemContentType() == null) {
      isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(owner);
    }

    uploadMetadata.put("enableCpeDataMatching", Boolean.toString(isCpeDataMatchingEnabled));
    ScanReceipt receipt =
        client.put(analytics, ScanReceipt.class, clientUserAgent, HDS_PATH, scanEntity, uploadMetadata);

    log.debug("Successfully uploaded scan id {} for stageType {}", receipt.getScanId(), stageTypeId);
    AuditData.get().setScanId(receipt.getScanId());

    if (owner.getType() == OwnerType.APPLICATION || owner.getType() == OwnerType.HOSTED_REPOSITORY_COMPONENT) {
      augmentScanReceipt(owner, receipt, stageTypeId, thirdPartyScanContext);
    }
    return receipt;
  }

  void augmentScanReceipt(
      Owner owner,
      ScanReceipt receipt,
      String stageTypeId,
      ThirdPartyScanContext thirdPartyScanContext)
  {
    // HDS knows nothing about where CLM Server stores reports, add this info to the receipt.
    String scanId = receipt.getScanId();
    if (owner.getType() == OwnerType.HOSTED_REPOSITORY_COMPONENT) {
      receipt.setReportUrl(UserInterfaceLinksHelper.getHostedRepositoryComponentReportUrl(owner.getId(), scanId));
      receipt.setPdfUrl(UserInterfaceLinksHelper.getHostedRepositoryComponentPdfUrl(owner.getId(), scanId));
      receipt.setDataUrl(ApiReportDataResourceV2.getHostedRepositoryComponentDataUrl(owner.getId(), scanId));
      receipt.setReportTimeoutInSeconds(configuration.getReportTimeoutInSeconds());
      return;
    }

    String applicationPublicId = owner.getPublicId();
    if (StageTypes.COMPLIANCE.getId().equals(stageTypeId) && thirdPartyScanContext != null
        && thirdPartyScanContext.getApplicationVersion() != null)
    {
      updateReceiptForCompliance(applicationPublicId, receipt, thirdPartyScanContext);
    }
    else {
      String reportUrl = StageTypes.PROXY.getId().equals(stageTypeId)
          ? UserInterfaceLinksHelper.getFirewallContainerImageEvaluationReportUrl(applicationPublicId, scanId)
          : UserInterfaceLinksHelper.getReportUrl(applicationPublicId, scanId);
      receipt.setReportUrl(reportUrl);
      receipt.setPdfUrl(UserInterfaceLinksHelper.getPdfUrl(applicationPublicId, scanId));
      receipt.setDataUrl(ApiReportDataResourceV2.getDataUrl(applicationPublicId, scanId));
      receipt.setPrioritiesUrl(UserInterfaceLinksHelper.getPrioritiesUrl(applicationPublicId, scanId));
      receipt.setIntegrationsPrioritiesUrl(
          UserInterfaceLinksHelper.getIntegrationsPrioritiesUrl(applicationPublicId, scanId));
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
    receipt.setSbomVersion(thirdPartyScanContext.getApplicationVersion());
  }

  /**
   * Validates that the client integration version is supported according to configuration.
   * Skips validation for web UI requests or when validation is not configured.
   *
   * @param clientUserAgent the client user agent string containing integration name/version
   * @param isWebUIRequest true if this is a web UI scan request
   * @throws IllegalArgumentException if validation fails
   * @throws IllegalStateException if configuration is invalid
   */
  private void validateIntegrationVersion(final String clientUserAgent, final boolean isWebUIRequest) {
    // Skip validation for IQ server itself (web UI scans)
    if (isWebUIRequest) {
      log.debug("Skipping integration version validation for web UI request");
      return;
    }

    Integer supportedVersionCount = getSupportedVersionCount();
    if (supportedVersionCount == null) {
      return;
    }

    SonatypeUserAgentUtil.UserAgent userAgent = parseAndValidateUserAgent(clientUserAgent);
    String name = userAgent.product;
    String version = userAgent.version;

    validateVersionSupport(name, version, supportedVersionCount);
    log.debug("Integration version {} of {} is supported", version, name);
  }

  private Integer getSupportedVersionCount() {
    Integer supportedVersionCount = configuration.getIntegrationsSupportedVersionCount();
    if (supportedVersionCount == null) {
      log.debug("Integration version validation not configured");
      return null;
    }
    if (supportedVersionCount <= 0) {
      throw new IllegalStateException("Invalid supported version count: " + supportedVersionCount +
          ". Must be a positive integer.");
    }
    return supportedVersionCount;
  }

  private SonatypeUserAgentUtil.UserAgent parseAndValidateUserAgent(final String clientUserAgent) {
    if (StringUtils.isBlank(clientUserAgent)) {
      throw new IllegalArgumentException("Client user agent is required for integration version validation");
    }

    SonatypeUserAgentUtil.UserAgent userAgent = SonatypeUserAgentUtil.parse(clientUserAgent);
    if (userAgent == null) {
      throw new IllegalArgumentException("Cannot parse client user agent: " + clientUserAgent);
    }

    if (StringUtils.isBlank(userAgent.product) || StringUtils.isBlank(userAgent.version)) {
      throw new IllegalArgumentException("Integration name/version not found in client user agent: " + clientUserAgent);
    }
    return userAgent;
  }

  private void validateVersionSupport(final String name, final String version, final Integer supportedVersionCount) {
    List<IqIntegrationVersion> sortedReleases = integrationVersionCache.get(name, supportedVersionCount);

    if (sortedReleases.isEmpty()) {
      log.warn("No integration versions found for {}", name);
      return;
    }

    if (sortedReleases.stream().noneMatch(release -> version.equals(release.version()))) {
      throw new UnsupportedIntegrationVersionException(version, name,
          sortedReleases.stream().map(IqIntegrationVersion::version).toList());
    }
  }
}
