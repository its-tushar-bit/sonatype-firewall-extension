/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.time.Duration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanUploaderTest
    extends AbstractComponentTest
{
  @Inject
  private ScanUploader scanUploader;

  @Mock
  private HdsClient hdsClient;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    super.configure(binder);
  }

  @Test
  public void testAugmentScanReceipt() {
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
    when(
        hdsClient.put(analyticsArg.capture(), eq(ScanReceipt.class), any(String.class), any(File.class),
            any(String[].class))).thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app);
    HdsClientAnalytics analytics = analyticsArg.getValue();
    assertThat(analytics).isEqualTo(expectedAnalyticsData);
  }

  @Test
  public void testUpload_RetryOnBadGatewayCanSucceed() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");

    when(
        hdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class), any(File.class),
            any(String[].class)))
        .thenThrow(new BadGatewayException("oops"))
        .thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app, Duration.ZERO);
    verify(hdsClient, times(2))
        .put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class), any(File.class),
            any(String[].class));
  }

  @Test
  public void testUpload_RetryOnBadGatewayErrorsOutEventually() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");

    when(
        hdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class), any(File.class),
            any(String[].class)))
        .thenThrow(new BadGatewayException("oops"));

    assertThatThrownBy(() -> scanUploader.upload(tempDir.newFile(), app, Duration.ZERO))
        .isInstanceOf(BadGatewayException.class);
    verify(hdsClient, times(ScanUploader.BAD_GATEWAY_ATTEMPT_LIMIT))
        .put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class), any(File.class),
            any(String[].class));
  }
}
