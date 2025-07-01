/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ItemContentType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanUploaderTest
    extends AbstractComponentTest
{
  @Inject
  private ScanUploader scanUploader;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private Configuration mockConfiguration;

  @Mock
  private ThirdPartyScanContext thirdPartyScanContext;

  @Mock
  private CpeMatchingConfigurationService mockCpeMatchingConfigurationService;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(InsightConfig.class).toInstance(insightConfig);
    binder.bind(Configuration.class).toInstance(mockConfiguration);
    binder.bind(ThirdPartyScanContext.class).toInstance(thirdPartyScanContext);
    binder.bind(CpeMatchingConfigurationService.class).toInstance(mockCpeMatchingConfigurationService);
    super.configure(binder);
  }

  @Test
  public void testAugmentScanReceipt() {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(2100);
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");
    scanUploader.augmentScanReceipt("app id", receipt, StageTypes.RELEASE.getId(), thirdPartyScanContext);
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/application/app%20id/report/scan%20id");
    assertThat(receipt.getPdfUrl()).isEqualTo("ui/links/application/app%20id/report/scan%20id/pdf");
    assertThat(receipt.getDataUrl()).isEqualTo("api/v2/applications/app%20id/reports/scan%20id/raw");
    assertThat(receipt.getPrioritiesUrl()).isEqualTo("ui/links/developer/priorities/app%20id/scan%20id");
    assertThat(receipt.getIntegrationsPrioritiesUrl()).isEqualTo("ui/links/developer/integrations/app%20id/scan%20id/");
    assertThat(receipt.getReportTimeoutInSeconds()).isEqualTo(2100);
  }

  @Test
  public void testAugmentScanReceipt_SbomManager() {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");
    when(thirdPartyScanContext.getApplicationVersion()).thenReturn("version");
    scanUploader.augmentScanReceipt("app id", receipt, StageTypes.COMPLIANCE.getId(), thirdPartyScanContext);
    assertThat(receipt.getReportUrl()).isEqualTo(
        "ui/links/sbomManager/management/view/application/app%20id/bom/version");
    assertThat(receipt.getPdfUrl()).isEqualTo(
        "ui/links/sbomManager/management/view/application/app%20id/bom/version/pdf");
    assertThat(receipt.getPrioritiesUrl()).isNull();
    assertThat(receipt.getIntegrationsPrioritiesUrl()).isNull();
    assertThat(receipt.getDataUrl()).isNull();
    assertThat(receipt.getReportTimeoutInSeconds()).isNull();
  }

  @Test
  public void testAugmentScanReceipt_proxyStage() {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(2100);
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");
    scanUploader.augmentScanReceipt("app id", receipt, StageTypes.PROXY.getId(), thirdPartyScanContext);
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/malware-defense/containerReport/app%20id/report/scan%20id");
    assertThat(receipt.getPdfUrl()).isEqualTo("ui/links/application/app%20id/report/scan%20id/pdf");
    assertThat(receipt.getDataUrl()).isEqualTo("api/v2/applications/app%20id/reports/scan%20id/raw");
    assertThat(receipt.getPrioritiesUrl()).isEqualTo("ui/links/developer/priorities/app%20id/scan%20id");
    assertThat(receipt.getIntegrationsPrioritiesUrl()).isEqualTo("ui/links/developer/integrations/app%20id/scan%20id/");
    assertThat(receipt.getReportTimeoutInSeconds()).isEqualTo(2100);
  }

  @Test
  public void testUpload_SendAnalyticsToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");

    HdsClientAnalytics expectedAnalyticsData = HdsClientAnalytics.forOwner(app);

    ArgumentCaptor<HdsClientAnalytics> analyticsArg = ArgumentCaptor.forClass(HdsClientAnalytics.class);
    when(mockHdsClient.put(analyticsArg.capture(), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), anyMap(), any(String[].class))).thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, null, null, thirdPartyScanContext);
    HdsClientAnalytics analytics = analyticsArg.getValue();
    assertThat(analytics).isEqualTo(expectedAnalyticsData);
  }

  @Test
  public void testUpload_SendClientUserAgentToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    String testClientUserAgent = "client_user_agent";

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), clientUserAgentArgCaptor.capture(),
        any(String.class), any(File.class), anyMap(), any(String[].class))) //
        .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, null, testClientUserAgent, thirdPartyScanContext);
    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testUpload_SendMatcherConfigsToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    ImmutableMap<String, String> matcherConfigs = ImmutableMap.of("k1", "v1");
    when(mockConfiguration.getMatcherConfiguration()).thenReturn(matcherConfigs);
    ArgumentCaptor<Map<String, String>> metadataArgs = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), metadataArgs.capture(), any(String[].class))) //
        .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, null, null, thirdPartyScanContext);

    assertThat(metadataArgs.getValue()).containsAllEntriesOf(matcherConfigs);
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabled() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockCpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId())).thenReturn(true);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
        .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, null, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isTrue();
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, ProxyStageType.ID, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isTrue();
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingDisabledWithScannerNotSonatype()
      throws Exception
  {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, ProxyStageType.ID, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingDisabledForContainerScansNoScanner()
      throws Exception
  {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(mockCpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId())).thenReturn(false);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, ProxyStageType.ID, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages_NoProductLicense()
      throws Exception
  {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, ProxyStageType.ID, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages_NoFeatureFlag()
      throws Exception
  {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, ProxyStageType.ID, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages_NoProxyStage()
      throws Exception
  {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, null, null, thirdPartyScanContext);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }
}
