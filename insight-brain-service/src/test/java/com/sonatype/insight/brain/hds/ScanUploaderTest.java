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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ScanUploaderTest
    extends AbstractComponentTest
{
  @Inject
  private ScanUploader scanUploader;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private Configuration mockConfiguration;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(InsightConfig.class).toInstance(insightConfig);
    binder.bind(Configuration.class).toInstance(mockConfiguration);
    super.configure(binder);
  }

  @Test
  public void testAugmentScanReceipt() {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(2100);
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");
    scanUploader.augmentScanReceipt("app id", receipt);
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/application/app%20id/report/scan%20id");
    assertThat(receipt.getPdfUrl()).isEqualTo("ui/links/application/app%20id/report/scan%20id/pdf");
    assertThat(receipt.getDataUrl()).isEqualTo("api/v2/applications/app%20id/reports/scan%20id/raw");
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

    scanUploader.upload(tempDir.newFile(), app, null, null);
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

    scanUploader.upload(tempDir.newFile(), app, null, testClientUserAgent);
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

    scanUploader.upload(tempDir.newFile(), app, null, null);

    assertThat(metadataArgs.getValue()).containsAllEntriesOf(matcherConfigs);
  }

  @Test
  public void testUpload_SendUploadIdToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> queryParamsCaptor = ArgumentCaptor.forClass(Map.class);

    when(mockHdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), queryParamsCaptor.capture(), any(String[].class))) //
        .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, null, null);

    assertThat(queryParamsCaptor.getValue().get("uploadId")).isNotBlank();
  }
}
