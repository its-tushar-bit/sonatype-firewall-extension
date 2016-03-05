/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanUploaderTest
    extends AbstractComponentTest
{
  @Inject
  private ScanUploader scanUploader;

  @Mock
  private HdsClient hdsClient;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(HdsClient.class).toInstance(hdsClient);
  }

  @Test
  public void testAugmentScanReceipt() {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan id");
    scanUploader.augmentScanReceipt("app id", receipt);
    assertThat(receipt.getReportUrl(), is("ui/links/application/app%20id/report/scan%20id"));
    assertThat(receipt.getPdfUrl(), is("ui/links/application/app%20id/report/scan%20id/pdf"));
    assertThat(receipt.getDataUrl(), is("api/v2/applications/app%20id/reports/scan%20id"));
  }

  @Test
  public void testUpload_SendAnalyticsToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scanId");

    HdsClientAnalytics expectedAnalyticsData = HdsClientAnalytics.forApplication(app.getId());

    ArgumentCaptor<HdsClientAnalytics> analyticsArg = ArgumentCaptor.forClass(HdsClientAnalytics.class);
    when(
        hdsClient.put(analyticsArg.capture(), eq(ScanReceipt.class), any(String.class), any(File.class),
            (String[]) anyVararg())).thenReturn(receipt);

    scanUploader.upload(tempDir.newFile(), app);
    HdsClientAnalytics analytics = analyticsArg.getValue();
    assertThat(analytics, is(equalTo(expectedAnalyticsData)));

    ServletInputStream stream = mock(ServletInputStream.class);
    when(stream.read(any(byte[].class))).thenReturn(-1);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(stream);

    analyticsArg = ArgumentCaptor.forClass(HdsClientAnalytics.class);
    when(
        hdsClient.get(eq(servletRequest), analyticsArg.capture(), eq(ScanReceipt.class), any(String.class),
            eq((Map<String, String>) null), (String[]) anyVararg())).thenReturn(receipt);

    scanUploader.upload(servletRequest, app.getPublicId());
    analytics = analyticsArg.getValue();
    assertThat(analytics, is(equalTo(expectedAnalyticsData)));
  }
}
