/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanHandlerTest
    extends AbstractComponentTest
{
  @Inject
  private InsightWork work;

  @Inject
  private ScanHandler scanHandler;

  @Mock
  private HdsClient hdsClient;

  @Mock
  private ThirdPartyScanService thirdPartyScanService;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    binder.bind(ThirdPartyScanService.class).toInstance(thirdPartyScanService);
    super.configure(binder);
  }

  @Test
  public void testHandle_SonatypeScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(hdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), anyMap())) //
        .thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).isFile().usingCharset(StandardCharsets.UTF_8).hasContent(scanFileContent);
  }

  @Test
  public void testHandle_SonatypeThirdPartyScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(thirdPartyScanService.filterAndUpload(any(File.class), any(Application.class), eq(null), eq(null), eq(null)))
        .thenReturn(scanReceipt);
    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    verify(thirdPartyScanService, times(1))
        .filterAndUpload(any(File.class), any(Application.class), eq(null), eq(null), eq(null));
  }

  @Test
  public void testHandle_SendClientUserAgentToHds_SonatypeThirdPartyScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("test-scan-Id");
    String testClientUserAgent = "client_user_agent";

    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(servletRequest.getHeader(DefaultHdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn(testClientUserAgent);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(thirdPartyScanService.filterAndUpload(any(File.class), any(Application.class), eq(null),
        clientUserAgentArgCaptor.capture(), eq(null))) //
            .thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY);

    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testHandle_SendAnalyticsToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("test-scan-Id");

    HdsClientAnalytics expectedAnalyticsData = HdsClientAnalytics.forOwner(app);

    ServletInputStream stream = mock(ServletInputStream.class);
    when(stream.read(any(byte[].class))).thenReturn(-1);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(stream);

    ArgumentCaptor<HdsClientAnalytics> analyticsArg = ArgumentCaptor.forClass(HdsClientAnalytics.class);
    when(hdsClient.put(analyticsArg.capture(), eq(ScanReceipt.class), eq(null), any(String.class), any(File.class),
        anyMap())) //
        .thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);

    HdsClientAnalytics analytics = analyticsArg.getValue();
    assertThat(analytics).isEqualTo(expectedAnalyticsData);
  }

  @Test
  public void testHandle_SendClientUserAgentToHds_SonatypeScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("test-scan-Id");
    String testClientUserAgent = "client_user_agent";

    ServletInputStream stream = mock(ServletInputStream.class);
    when(stream.read(any(byte[].class))).thenReturn(-1);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(stream);
    when(servletRequest.getHeader(DefaultHdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn(testClientUserAgent);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(hdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), clientUserAgentArgCaptor.capture(),
        any(String.class), any(File.class), anyMap(), any(String[].class))) //
            .thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);

    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testHandle_ApplicationDoesNotExist() throws Exception {
    String appPublicId = "NoSuchAppPublicID";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      scanHandler.handle(servletRequest, appPublicId, ClientScanType.SONATYPE);
    }).withMessage("Could not find an application with public ID NoSuchAppPublicID.");
  }

  @Test
  public void testHandle_FailedUpload_DeletesScanFile() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(hdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(File.class), anyMap())) //
        .thenThrow(new RuntimeException("test"));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> scanHandler.handle(servletRequest, application.getPublicId(), ClientScanType.SONATYPE))
        .withMessage("test");

    assertThat(work.getScanDir(application.getId()).listFiles()).isEmpty();
  }

  @Test
  public void testHandle_FailedScanSave_DeletesScanFile() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenThrow(new RuntimeException("test"));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> scanHandler.handle(servletRequest, application.getPublicId(), ClientScanType.SONATYPE))
        .withMessage("test");

    assertThat(work.getScanDir(application.getId()).listFiles()).isEmpty();
  }
}
