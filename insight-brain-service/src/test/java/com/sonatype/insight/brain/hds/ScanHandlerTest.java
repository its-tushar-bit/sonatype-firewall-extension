/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.inject.Inject;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.inject.Binder;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
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
  private ScanUploadService scanUploadService;

  @Override
  public void configure(Binder binder) {
    binder.bind(ScanUploadService.class).toInstance(scanUploadService);
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
    when(scanUploadService.upload(any(File.class), any(Application.class), any(), eq(ClientScanType.SONATYPE),
        any(), any(), any())).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).isFile().usingCharset(StandardCharsets.UTF_8).hasContent(scanFileContent);
  }

  @Test
  public void testHandle_SbomManagerBinaryScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    String scanRequestId = "scan-request-id";
    scanReceipt.setScanId(scanId);
    File scanDir = work.getScanDir(app.getId());
    Files.createDirectories(scanDir.toPath());
    File scanFile = FileUtils.createTempFile("temp-", ".xml.gz", scanDir);
    String scanFileContent = "test scan file content";
    Files.writeString(scanFile.getAbsoluteFile().toPath(), scanFileContent);

    when(scanUploadService.upload(any(File.class), any(Application.class), any(), eq(ClientScanType.SONATYPE),
        any(), any(), any())).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(scanFile, app, ClientScanType.SONATYPE, null, ComplianceStageType.ID,
        null , scanRequestId);

    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
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
    when(scanUploadService.upload(any(File.class), any(Application.class), eq(null), any(), eq(null), eq(null), any()))
        .thenReturn(scanReceipt);
    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    verify(scanUploadService, times(1))
        .upload(any(File.class), any(Application.class), eq(null), any(), eq(null), eq(null), any());
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
    when(servletRequest.getHeader(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn(testClientUserAgent);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(scanUploadService.upload(any(File.class), any(Application.class), eq(null), any(ClientScanType.class),
        clientUserAgentArgCaptor.capture(), eq(null), any())) //
            .thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY);

    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
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
    when(servletRequest.getHeader(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn(testClientUserAgent);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(scanUploadService.upload(any(File.class), any(Application.class), any(), eq(ClientScanType.SONATYPE),
        clientUserAgentArgCaptor.capture(), any(), any())).thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);

    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testHandle_ApplicationDoesNotExist() {
    String appPublicId = "NoSuchAppPublicID";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scanHandler.handle(servletRequest, appPublicId, ClientScanType.SONATYPE))
        .withMessage("Could not find an application with public ID NoSuchAppPublicID.");
  }

  @Test
  public void testHandle_FailedUpload_DeletesScanFile() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(scanUploadService.upload(any(File.class), any(Application.class), any(), eq(ClientScanType.SONATYPE),
        any(), any(), any())).thenThrow(new RuntimeException("test"));

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
