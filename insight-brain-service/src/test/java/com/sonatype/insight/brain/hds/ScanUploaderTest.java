/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

  @Mock
  private IntegrationVersionCache mockIntegrationVersionCache;

  @Override
  public void configure(Binder binder) {
    // Setup default mock behavior to skip integration version validation (lenient for tests that don't use it)
    lenient().when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(null);

    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(InsightConfig.class).toInstance(insightConfig);
    binder.bind(Configuration.class).toInstance(mockConfiguration);
    binder.bind(ThirdPartyScanContext.class).toInstance(thirdPartyScanContext);
    binder.bind(CpeMatchingConfigurationService.class).toInstance(mockCpeMatchingConfigurationService);
    binder.bind(IntegrationVersionCache.class).toInstance(mockIntegrationVersionCache);
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
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/firewall/containerReport/app%20id/report/scan%20id");
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
        any(ScanEntity.class), anyMap(), any(String[].class))).thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, null, null, thirdPartyScanContext, false);
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
        any(String.class), any(ScanEntity.class), anyMap(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, null, testClientUserAgent, thirdPartyScanContext, false);
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
        any(ScanEntity.class), metadataArgs.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, null, null, thirdPartyScanContext, false);

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
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, null, null, thirdPartyScanContext, false);

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
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, ProxyStageType.ID, null, thirdPartyScanContext, false);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isTrue();
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingDisabledWithScannerNotSonatype() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, ProxyStageType.ID, null, thirdPartyScanContext, false);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingDisabledForContainerScansNoScanner() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(mockCpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId())).thenReturn(false);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, ProxyStageType.ID, null, thirdPartyScanContext, false);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages_NoProductLicense() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, ProxyStageType.ID, null, thirdPartyScanContext, false);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages_NoFeatureFlag() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, ProxyStageType.ID, null, thirdPartyScanContext, false);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isFalse();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SendUploadIdToHds_CpeDataMatchingEnabledWithFirewallForContainerImages_NoProxyStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    when(thirdPartyScanContext.getContainerItemContentType()).thenReturn(ItemContentType.CONTAINER_URI_SONATYPE);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(ScanEntity.class), queryParamsCaptor.capture(), any(String[].class))) //
            .thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    scanUploader.upload(scanEntity, app, null, null, thirdPartyScanContext, false);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
    assertThat(queryParamsCaptor.getValue().get("enableCpeDataMatching")).asBoolean().isTrue();
    verify(mockCpeMatchingConfigurationService, never()).isCpeDataMatchingEnabled(app.getId());
  }

  @Test
  public void testUpload_SkipsValidation_WhenSupportedVersionCountNotConfigured() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(null);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class),
        eq("Maven_Plugin/1.0.0 (Java 11.0.13; Linux)"),
        any(String.class), any(ScanEntity.class), anyMap(), any(String[].class))).thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    ScanReceipt result = scanUploader.upload(scanEntity, app, null, "Maven_Plugin/1.0.0 (Java 11.0.13; Linux)",
        thirdPartyScanContext, false);

    assertThat(result).isEqualTo(receipt);
    assertThat(result.getScanId()).isEqualTo(receipt.getScanId());
  }

  @Test
  public void testUpload_SkipsValidation_WhenWebUIRequest() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class),
        eq("Maven_Plugin/1.0.0 (Java 11.0.13; Linux)"),
        any(String.class), any(ScanEntity.class), anyMap(), any(String[].class))).thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    ScanReceipt result = scanUploader.upload(scanEntity, app, null, "Maven_Plugin/1.0.0 (Java 11.0.13; Linux)",
        thirdPartyScanContext, true);

    assertThat(result).isEqualTo(receipt);
    assertThat(result.getScanId()).isEqualTo(receipt.getScanId());
  }

  @Test
  public void testUpload_ValidatesIntegrationVersion_ForExternalIntegration() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(3);

    List<IqIntegrationVersion> supportedVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.2.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.1.0"));
    when(mockIntegrationVersionCache.get("Maven_Plugin", 3)).thenReturn(supportedVersions);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class),
        eq("Maven_Plugin/1.2.0 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)"),
        any(String.class), any(ScanEntity.class), anyMap(), any(String[].class))).thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());
    ScanReceipt result = scanUploader.upload(scanEntity, app, null,
        "Maven_Plugin/1.2.0 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)", thirdPartyScanContext, false);

    assertThat(result).isEqualTo(receipt);
    assertThat(result.getScanId()).isEqualTo(receipt.getScanId());
  }

  @Test
  public void testUpload_ThrowsException_ForUnsupportedIntegrationVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(3);

    List<IqIntegrationVersion> supportedVersions = List.of(
        new IqIntegrationVersion("Maven_Plugin", "1.3.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.2.0"),
        new IqIntegrationVersion("Maven_Plugin", "1.1.0"));
    when(mockIntegrationVersionCache.get("Maven_Plugin", 3)).thenReturn(supportedVersions);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());

    assertThatExceptionOfType(UnsupportedIntegrationVersionException.class)
        .isThrownBy(() -> scanUploader.upload(scanEntity, app, null,
            "Maven_Plugin/1.0.0 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)", thirdPartyScanContext, false))
        .withMessageContaining("The integration version 1.0.0 of Maven_Plugin is not supported")
        .withMessageContaining("Minimum supported version is 1.1.0.");
  }

  @Test
  public void testUpload_ThrowsException_ForMissingClientUserAgent() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(3);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> scanUploader.upload(scanEntity, app, null, null, thirdPartyScanContext, false))
        .withMessageContaining("Client user agent is required for integration version validation");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> scanUploader.upload(scanEntity, app, null, "", thirdPartyScanContext, false))
        .withMessageContaining("Client user agent is required for integration version validation");
  }

  @Test
  public void testUpload_ThrowsException_ForInvalidClientUserAgent() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(3);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> scanUploader.upload(scanEntity, app, null, "invalid-user-agent",
            thirdPartyScanContext, false))
        .withMessageContaining("Cannot parse client user agent: invalid-user-agent");
  }

  @Test
  public void testUpload_ThrowsException_ForInvalidSupportedVersionCount() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(-1);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> scanUploader.upload(scanEntity, app, null, "Maven_Plugin/1.2.0 (Java 11.0.13; Linux)",
            thirdPartyScanContext, false))
        .withMessageContaining("Invalid supported version count: -1");
  }

  @Test
  public void testUpload_SkipsVersionValidation_WhenNoIntegrationVersionsReturnedFromHDS() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    when(mockConfiguration.getIntegrationsSupportedVersionCount()).thenReturn(3);

    List<IqIntegrationVersion> emptyVersions = List.of();
    when(mockIntegrationVersionCache.get("Maven_Plugin", 3)).thenReturn(emptyVersions);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class),
        eq("Maven_Plugin/1.2.0 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)"),
        any(String.class), any(ScanEntity.class), anyMap(), any(String[].class))).thenReturn(receipt);

    ScanEntity scanEntity = new FileScanEntity(tempDir.newFile().toPath(), app.getId());

    ScanReceipt result = scanUploader.upload(scanEntity, app, null,
        "Maven_Plugin/1.2.0 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)", thirdPartyScanContext, false);

    assertThat(result).isEqualTo(receipt);
    assertThat(result.getScanId()).isEqualTo(receipt.getScanId());
  }
}
