/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import jakarta.inject.Inject;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ComponentH2Test
public class ScanHandlerTest
    extends AbstractComponentH2Test
{
  @Inject
  private InsightWork work;

  @Inject
  private ScanHandler scanHandler;

  @Mock
  private ScanUploadService scanUploadService;

  @Test
  public void testHandle_SonatypeScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    stubRequestHeader(servletRequest, "test-user-agent");
    when(scanUploadService.upload(any(ScanEntity.class), any(Application.class), eq((String) null),
        eq(ClientScanType.SONATYPE), eq("test-user-agent"), eq((TelemetryData) null), eq((String) null),
        any(ScanContext.class), eq(false))).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).isFile().usingCharset(StandardCharsets.UTF_8).hasContent(scanFileContent);
  }

  @Test
  public void testHandle_SbomManagerBinaryScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt mockScanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    String scanRequestId = "scan-request-id";
    mockScanReceipt.setScanId(scanId);
    File scanDir = work.getScanDir(app.getId());
    Files.createDirectories(scanDir.toPath());
    File tempFile = FileUtils.createTempFile("temp-", ".xml.gz", scanDir);
    Files.writeString(tempFile.getAbsoluteFile().toPath(), "test scan file content");
    ScanEntity scanEntity = new FileScanEntity(tempFile.toPath(), app.getId());

    when(scanUploadService.upload(eq(scanEntity), eq(app), eq(ComplianceStageType.ID),
        eq(ClientScanType.SONATYPE), eq("test-client-user-agent"), eq((TelemetryData) null),
        eq(scanRequestId), any(ScanContext.class), eq(false))).thenReturn(mockScanReceipt);

    ScanReceipt scanReceipt = scanHandler.handle(ScanHandler.ScanRequest.builder()
        .scanEntity(scanEntity)
        .application(app)
        .clientScanType(ClientScanType.SONATYPE)
        .thirdPartyScanTelemetryData(null)
        .stageTypeId(ComplianceStageType.ID)
        .clientUserAgent("test-client-user-agent")
        .scanRequestId(scanRequestId)
        .httpRequest(null)
        .build());
    verify(scanUploadService, times(1))
        .upload(eq(scanEntity), eq(app), eq(ComplianceStageType.ID), eq(ClientScanType.SONATYPE),
            eq("test-client-user-agent"), eq((TelemetryData) null), eq(scanRequestId),
            eq((ScanContext) new ScanContext.Builder().isValid(true).build()), eq(false));
    assertThat(mockScanReceipt).isEqualTo(scanReceipt);
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
    stubRequestHeader(servletRequest, "test-user-agent");
    when(scanUploadService.upload(any(ScanEntity.class), any(Application.class), eq((String) null),
        eq(ClientScanType.SONATYPE_THIRD_PARTY), eq("test-user-agent"), eq((TelemetryData) null), eq((String) null),
        any(ScanContext.class), eq(false))).thenReturn(scanReceipt);
    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE_THIRD_PARTY);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    verify(scanUploadService, times(1))
        .upload(any(ScanEntity.class), any(Application.class), eq((String) null),
            eq(ClientScanType.SONATYPE_THIRD_PARTY),
            eq("test-user-agent"), eq((TelemetryData) null), eq((String) null), any(ScanContext.class), eq(false));
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
    stubRequestHeader(servletRequest, testClientUserAgent);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(scanUploadService.upload(any(ScanEntity.class), any(Application.class), eq((String) null),
        eq(ClientScanType.SONATYPE_THIRD_PARTY), clientUserAgentArgCaptor.capture(), eq((TelemetryData) null),
        eq((String) null), any(ScanContext.class), eq(false))).thenReturn(scanReceipt);

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
    stubRequestHeader(servletRequest, testClientUserAgent);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    when(scanUploadService.upload(any(ScanEntity.class), any(Application.class), eq((String) null),
        eq(ClientScanType.SONATYPE), clientUserAgentArgCaptor.capture(), eq((TelemetryData) null),
        eq((String) null), any(ScanContext.class), eq(false))).thenReturn(scanReceipt);

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
    stubRequestHeader(servletRequest, "test-user-agent");
    when(scanUploadService.upload(any(ScanEntity.class), any(Application.class), eq((String) null),
        eq(ClientScanType.SONATYPE), eq("test-user-agent"), eq((TelemetryData) null), eq((String) null),
        any(ScanContext.class), eq(false))).thenThrow(new RuntimeException("test"));

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

  private void stubRequestHeader(HttpServletRequest request, String clientUserAgent) {
    when(request.getHeader(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).thenReturn(clientUserAgent);
  }
}
