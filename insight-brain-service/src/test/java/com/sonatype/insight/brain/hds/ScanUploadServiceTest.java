/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultsProcessor;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanUploadServiceTest
    extends AbstractBrainServiceIntegrationTest
{
  private ScanUploader scanUploader;

  private InsightWork work;

  private ScanUploadService service;

  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessorMock;

  private ThirdPartyScanDAO thirdPartyScanDAO;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("ScanUploadServiceTest-PublicId", "ScanUploadServiceTest-Id");
    scanUploader = mock(ScanUploader.class);
    thirdPartyScanDAO = spy(lookup(ThirdPartyScanDAO.class));
    thirdPartySbomMetadataDAO = spy(lookup(ThirdPartySbomMetadataDAO.class));
    thirdPartyScanResultsProcessorMock = mock(ThirdPartyScanResultsProcessor.class);
    work = getCLMServer().getInstance(InsightWork.class);
    service = new ScanUploadService(thirdPartyScanResultsProcessorMock, scanUploader, thirdPartyScanDAO,
        thirdPartySbomMetadataDAO, work);
  }

  @Test
  public void testFilterAndUpload() throws Exception {
    Stage stage = new Stage(ReleaseStageType.ID);
    String scanId = "ScanUploadServiceTest_scanId";
    File scanFile = createScanFile(app, scanId);
    String scanRequestId = "scanRequestId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(eq(scanFile), any(File.class),
        any(File.class), eq(null), eq(null), eq(app.getId()), eq(stage.getStageTypeId())))
        .thenReturn(scanRequestId);
    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    String testClientUserAgent = "client_user_agent";
    when(scanUploader.upload(any(File.class), eq(app), eq(stage.getStageTypeId()), clientUserAgentArgCaptor.capture()))
        .thenReturn(scanReceipt);

    service.filterAndUpload(scanFile, app, stage.getStageTypeId(), testClientUserAgent, null, null);

    verify(thirdPartyScanResultsProcessorMock, times(1))
        .filterAndSaveData(eq(scanFile), any(File.class), any(File.class), eq(null), eq(null), eq(app.getId()),
            eq(stage.getStageTypeId()));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, scanId);

    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testFilterAndUpload_FileProcessingError() {
    Stage stage = new Stage(ReleaseStageType.ID);
    String scanId = "ScanUploadServiceTest_scanId";
    File scanFile = createScanFile(app, scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(eq(scanFile), any(File.class), any(File.class),
        eq(null), eq(null), eq(app.getId()), eq(stage.getStageTypeId())))
        .thenThrow(new IllegalArgumentException("error"));

    ThrowingCallable throwable = () -> service.filterAndUpload(scanFile,
        app, stage.getStageTypeId(), null, null, null);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(throwable);
  }

  @Test
  public void testUpload_NonThirdParty() throws Exception {
    String stageTypeId = ReleaseStageType.ID;
    File scanFile = createScanFile(app, RandomStringUtils.randomAlphanumeric(10));

    service.upload(scanFile, app, stageTypeId, null, null, null, null);

    verify(scanUploader, times(1)).upload(eq(scanFile), eq(app), eq(stageTypeId), eq(null));
    verify(thirdPartyScanDAO, never()).updateScanIdForScanRequest(any(), any());
  }

  @Test
  public void testUpload_BinaryWithNoThirdParty_ComplianceStage() throws Exception {
    File scanFile = createScanFile(app, RandomStringUtils.randomAlphanumeric(10));
    String stageTypeId = ComplianceStageType.ID;
    String scanId = RandomStringUtils.randomAlphanumeric(10);
    String scanRequestId = RandomStringUtils.randomAlphanumeric(10);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile("filename");
    tempEntity.newThirdPartyScan(scanRequestId, scanId, tpFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), SbomStatus.PENDING.toString(), "filename");
    sbomMetadata.setScanType(SbomScanType.BINARY.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ScanReceipt mockReceipt = new ScanReceipt();
    mockReceipt.setScanId(scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(any(File.class), any(File.class), any(File.class),
        any(), any(), any(), any())).thenReturn(scanRequestId);
    when(scanUploader.upload(any(File.class), eq(app), eq(stageTypeId), eq(null))).thenReturn(mockReceipt);

    ScanReceipt uploadReceipt =
        service.upload(scanFile, app, stageTypeId, ClientScanType.SONATYPE, null, null, scanRequestId);

    verify(scanUploader, times(1)).upload(any(File.class), eq(app), eq(stageTypeId), eq(null));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, mockReceipt.getScanId());
    assertThat(uploadReceipt).isEqualTo(mockReceipt);
  }

  @Test
  public void testUpload_BinaryWithThirdParty_ComplianceStage() throws Exception {
    File scanFile = createScanFile(app, RandomStringUtils.randomAlphanumeric(10));
    String stageTypeId = ComplianceStageType.ID;
    String scanId = RandomStringUtils.randomAlphanumeric(10);
    String scanRequestId = RandomStringUtils.randomAlphanumeric(10);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile("filename");
    tempEntity.newThirdPartyScan(scanRequestId, scanId, tpFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), SbomStatus.PENDING.toString(), "filename");
    sbomMetadata.setScanType(SbomScanType.BINARY.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ScanReceipt mockReceipt = new ScanReceipt();
    mockReceipt.setScanId(scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(any(File.class), any(File.class), any(File.class),
        any(), any(), any(), any())).thenReturn(scanRequestId);
    when(scanUploader.upload(any(File.class), eq(app), eq(stageTypeId), eq(null))).thenReturn(mockReceipt);

    ScanReceipt uploadReceipt =
        service.upload(scanFile, app, stageTypeId, ClientScanType.SONATYPE_THIRD_PARTY, null, null, scanRequestId);

    verify(scanUploader, times(1)).upload(any(File.class), eq(app), eq(stageTypeId), eq(null));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, mockReceipt.getScanId());
    assertThat(uploadReceipt).isEqualTo(mockReceipt);
  }

  @Test
  public void testUpload_MissingRequiredArguments() {
    File scanFile = createScanFile(app, RandomStringUtils.randomAlphanumeric(10));
    String stageTypeId = ComplianceStageType.ID;
    //null app
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
        service.upload(scanFile, null, stageTypeId, null, null, null, null));
    //null scanFile
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
        service.upload(null, app, stageTypeId, null, null, null, null));
  }

  private void assertFilteredScanFile(final String scanRequestId, final String applicationId) {
    ThirdPartyScan tpScan = thirdPartyScanDAO.getSingleByScanRequestId(scanRequestId);
    String filteredScanFile = tpScan.getFilteredScanFile();
    assertThat(filteredScanFile).isNotNull();
    File filteredScan = new File(work.getScanDir(applicationId), filteredScanFile);
    assertThat(filteredScan).exists();
  }

  private File createScanFile(Application app, String scanId) {
    File scanFile = work.getScanFile(app.getId(), scanId);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return scanFile;
  }
}
