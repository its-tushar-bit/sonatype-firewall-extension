/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultsProcessor;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static org.apache.commons.lang3.RandomStringUtils.secure;
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

@Category(SlowTest.class)
public class ScanUploadServiceTest
    extends AbstractBrainServiceIntegrationTest
{
  private ScanUploader scanUploader;

  private InsightWork work;

  private ScanUploadService service;

  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessorMock;

  private ThirdPartyScanDAO thirdPartyScanDAO;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private ScanPersistenceService scanPersistenceService;

  private Application app;

  private TelemetryData thirdPartyScanTelemetryData;

  private TelemetrySender telemetrySender;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("ScanUploadServiceTest-PublicId", "ScanUploadServiceTest-Id");
    scanUploader = mock(ScanUploader.class);
    thirdPartyScanDAO = spy(lookup(ThirdPartyScanDAO.class));
    thirdPartySbomMetadataDAO = spy(lookup(ThirdPartySbomMetadataDAO.class));
    thirdPartyScanResultsProcessorMock = mock(ThirdPartyScanResultsProcessor.class);
    work = lookup(InsightWork.class);
    scanPersistenceService = lookup(ScanPersistenceService.class);
    telemetrySender = mock(TelemetrySender.class);
    service = new ScanUploadService(thirdPartyScanResultsProcessorMock, scanUploader, thirdPartyScanDAO,
        thirdPartySbomMetadataDAO, scanPersistenceService, telemetrySender);
    thirdPartyScanTelemetryData = buildThirdPartyScanTelemetryData();
  }

  @Test
  public void testUpload() throws Exception {
    ScanEntity scanEntity = createScanFile(app, TemporaryEntity.uuid().substring(0, 10));
    String stageTypeId = ComplianceStageType.ID;
    String scanId = TemporaryEntity.uuid().substring(0, 10);
    String scanRequestId = TemporaryEntity.uuid().substring(0, 10);
    String sbomVersion = TemporaryEntity.uuid().substring(0, 10);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile("filename");
    tempEntity.newThirdPartyScan(scanRequestId, scanId, tpFile);

    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), sbomVersion,
            ThirdPartySbomMetadataStatus.PENDING, "filename", "CycloneDx", "XML", "1.5", new Date(), false);
    sbomMetadata.setScanType(SbomScanType.SBOM.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ScanReceipt mockReceipt = new ScanReceipt();
    mockReceipt.setScanId(scanId);
    when(scanUploader.upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false)))
        .thenReturn(mockReceipt);
    ScanReceipt uploadReceipt =
        service.upload(scanEntity, app, stageTypeId, null, null, thirdPartyScanTelemetryData, scanRequestId, false);

    verify(scanUploader, times(1)).upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, mockReceipt.getScanId());
    assertThat(uploadReceipt).isEqualTo(mockReceipt);
    assertThat(thirdPartyScanTelemetryData.getAttributes()).extracting("scan_file_type")
        .isEqualTo(SbomScanType.SBOM.name());
    verify(telemetrySender, times(1)).send(thirdPartyScanTelemetryData);
  }

  @Test
  public void testFilterAndUpload() throws Exception {
    Stage stage = new Stage(ReleaseStageType.ID);
    String scanId = "ScanUploadServiceTest_scanId";
    ScanEntity scanEntity = createScanFile(app, scanId);
    String scanRequestId = "scanRequestId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    ThirdPartyScanContext mockContext =
        new ThirdPartyScanContext(scanRequestId, app.getId(), SbomScanType.SBOM, scanEntity, stage.getStageTypeId());
    mockContext.markSbomSavedForScan();

    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(eq(scanEntity), any(ScanEntity.class), eq(mockContext),
        eq(null))).thenReturn(scanRequestId);
    when(scanUploader.upload(any(ScanEntity.class), eq(app), eq(stage.getStageTypeId()), eq(null),
        eq(mockContext), eq(false)))
            .thenReturn(scanReceipt);

    ArgumentCaptor<ScanEntity> scanEntityCaptor = ArgumentCaptor.forClass(ScanEntity.class);

    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    String testClientUserAgent = "client_user_agent";

    when(scanUploader.upload(scanEntityCaptor.capture(), eq(app), eq(stage.getStageTypeId()),
        clientUserAgentArgCaptor.capture(),
        eq(mockContext), eq(false))).thenReturn(scanReceipt);

    service.filterAndUpload(scanEntity, app, stage.getStageTypeId(), testClientUserAgent, mockContext,
        null, false);

    verify(thirdPartyScanResultsProcessorMock, times(1))
        .filterAndSaveData(eq(scanEntity), any(ScanEntity.class), eq(mockContext), eq(null));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, scanId);
    assertThat(scanEntityCaptor.getValue().getAppId()).isEqualTo(app.getId());
    assertThat(scanEntityCaptor.getValue().getName()).isNotEqualTo(scanEntity.getName());
    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
    verify(telemetrySender, never()).send(thirdPartyScanTelemetryData);
  }

  @Test
  public void testSaveFilteredScanFileIfNeeded() {
    Stage stage = new Stage(ComplianceStageType.ID);
    String scanId = "ScanUploadServiceTest_scanId";
    ScanEntity filteredScanEntity = createScanFile(app, scanId);
    String scanRequestId = secure().next(10);
    ThirdPartyScan tpScan = tempEntity.newThirdPartyScan(scanRequestId, scanId);
    ThirdPartyScanContext tpContext =
        new ThirdPartyScanContext(scanRequestId, app.getId(), SbomScanType.SBOM, filteredScanEntity,
            stage.getStageTypeId());
    tpContext.markSbomSavedForScan();
    tpContext.setThirdPartyScanId(tpScan.getId());

    service.saveFilteredScanFileIfNeeded(tpContext, filteredScanEntity);

    ThirdPartyScan tpScan1 = thirdPartyScanDAO.getSingleByScanRequestId(scanRequestId);
    String filteredScanFile1 = tpScan1.getFilteredScanFile();
    assertThat(filteredScanFile1).isNotNull();
    File filteredScan = new File(work.getScanDir(app.getId()), filteredScanFile1);
    assertThat(filteredScan).exists();
  }

  @Test
  public void testSaveContainerUriPaths() {
    Stage stage = new Stage(ComplianceStageType.ID);
    String scanId = "ScanUploadServiceTest_scanId";
    ScanEntity filteredScanEntity = createScanFile(app, scanId);
    String scanRequestId = secure().next(10);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile("filename");
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), PENDING, "filename");
    sbomMetadata.setScanType(SbomScanType.BINARY.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    ThirdPartyScan tpScan = tempEntity.newThirdPartyScan(scanRequestId, scanId, tpFile);
    ThirdPartyScanContext tpContext =
        new ThirdPartyScanContext(scanRequestId, app.getId(), SbomScanType.SBOM, filteredScanEntity,
            stage.getStageTypeId());
    tpContext.markSbomSavedForScan();
    tpContext.setThirdPartyScanId(tpScan.getId());
    tpContext.setSbomMetadataId(sbomMetadata.getId());
    tpContext.addContainerUriPath("container:alpine:3.0");
    tpContext.addContainerUriPath("container:alpine:3.6");

    service.saveContainerUriPaths(stage.getStageTypeId(), tpContext);

    sbomMetadata = thirdPartySbomMetadataDAO.getById(tpContext.getSbomMetadataId());

    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getOriginalBinaryFileName())
        .isNotNull()
        .isEqualTo(tpContext.getContainerUriPaths().get(0) + "," + tpContext.getContainerUriPaths().get(1));
  }

  @Test
  public void testFilterAndUpload_FileProcessingError() {
    Stage stage = new Stage(ReleaseStageType.ID);
    String scanId = "ScanUploadServiceTest_scanId";
    ScanEntity scanEntity = createScanFile(app, scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(eq(scanEntity), any(ScanEntity.class), eq(null),
        eq(null))).thenThrow(new IllegalArgumentException("error"));

    ThrowingCallable throwable = () -> service.filterAndUpload(scanEntity,
        app, stage.getStageTypeId(), null, null, null, false);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(throwable);
  }

  @Test
  public void testUpload_NonThirdParty() throws Exception {
    String stageTypeId = ReleaseStageType.ID;
    ScanEntity scanEntity = createScanFile(app, TemporaryEntity.uuid().substring(0, 10));
    service.upload(scanEntity, app, stageTypeId, null, null, thirdPartyScanTelemetryData, null, false);

    verify(scanUploader, times(1)).upload(eq(scanEntity), eq(app), eq(stageTypeId), eq(null), any(), eq(false));
    verify(thirdPartyScanDAO, never()).updateScanIdForScanRequest(any(), any());
    assertThat(thirdPartyScanTelemetryData.getAttributes()).extracting("scan_file_type")
        .isEqualTo(SbomScanType.SBOM.name());
    verify(telemetrySender, never()).send(thirdPartyScanTelemetryData);
  }

  @Test
  public void testUpload_BinaryWithNoThirdParty_ComplianceStage() throws Exception {
    ScanEntity scanEntity = createScanFile(app, TemporaryEntity.uuid().substring(0, 10));
    String stageTypeId = ComplianceStageType.ID;
    String scanId = TemporaryEntity.uuid().substring(0, 10);
    String scanRequestId = TemporaryEntity.uuid().substring(0, 10);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile("filename");
    tempEntity.newThirdPartyScan(scanRequestId, scanId, tpFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.PENDING,
            "filename");
    sbomMetadata.setScanType(SbomScanType.BINARY.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ScanReceipt mockReceipt = new ScanReceipt();
    mockReceipt.setScanId(scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(any(ScanEntity.class), any(ScanEntity.class),
        any(), any())).thenReturn(scanRequestId);
    when(scanUploader.upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false)))
        .thenReturn(mockReceipt);

    ScanReceipt uploadReceipt =
        service.upload(scanEntity, app, stageTypeId, ClientScanType.SONATYPE, null, thirdPartyScanTelemetryData,
            scanRequestId, false);

    verify(scanUploader, times(1)).upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, mockReceipt.getScanId());
    assertThat(uploadReceipt).isEqualTo(mockReceipt);
    assertThat(thirdPartyScanTelemetryData.getAttributes()).extracting("scan_file_type")
        .isEqualTo(SbomScanType.BINARY.name());
    verify(telemetrySender, times(1)).send(thirdPartyScanTelemetryData);
  }

  @Test
  public void testUpload_BinaryWithThirdParty_ComplianceStage() throws Exception {
    ScanEntity scanEntity = createScanFile(app, TemporaryEntity.uuid().substring(0, 10));
    String stageTypeId = ComplianceStageType.ID;
    String scanId = TemporaryEntity.uuid().substring(0, 10);
    String scanRequestId = TemporaryEntity.uuid().substring(0, 10);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile("filename");
    tempEntity.newThirdPartyScan(scanRequestId, scanId, tpFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.PENDING,
            "filename");
    sbomMetadata.setScanType(SbomScanType.BINARY.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ScanReceipt mockReceipt = new ScanReceipt();
    mockReceipt.setScanId(scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(any(ScanEntity.class), any(ScanEntity.class),
        any(), any())).thenReturn(scanRequestId);
    when(scanUploader.upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false)))
        .thenReturn(mockReceipt);

    ScanReceipt uploadReceipt =
        service.upload(scanEntity, app, stageTypeId, ClientScanType.SONATYPE_THIRD_PARTY, null,
            thirdPartyScanTelemetryData, scanRequestId, false);

    verify(scanUploader, times(1)).upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false));
    verify(thirdPartyScanDAO, times(1)).updateScanIdForScanRequest(scanRequestId, mockReceipt.getScanId());
    assertThat(uploadReceipt).isEqualTo(mockReceipt);
    assertThat(thirdPartyScanTelemetryData.getAttributes()).extracting("scan_file_type")
        .isEqualTo(SbomScanType.BINARY.name());
    verify(telemetrySender, never()).send(thirdPartyScanTelemetryData);
  }

  @Test
  public void testUpload_WithScanContext() throws Exception {
    ScanEntity scanEntity = createScanFile(app, TemporaryEntity.uuid().substring(0, 10));
    String stageTypeId = ComplianceStageType.ID;
    String scanId = TemporaryEntity.uuid().substring(0, 10);
    String scanRequestId = TemporaryEntity.uuid().substring(0, 10);

    ScanReceipt mockReceipt = new ScanReceipt();
    mockReceipt.setScanId(scanId);

    ArgumentCaptor<ThirdPartyScanContext> contextCaptor = ArgumentCaptor.forClass(ThirdPartyScanContext.class);
    when(scanUploader.upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), contextCaptor.capture(),
        eq(false))).thenReturn(mockReceipt);

    ScanContext scanContext = new ScanContext.Builder()
        .containerImageSbomSpecification(SbomSpecification.CYCLONEDX)
        .build();

    ScanReceipt uploadReceipt =
        service.upload(scanEntity, app, stageTypeId, null, null, thirdPartyScanTelemetryData, scanRequestId,
            scanContext, false);

    verify(scanUploader, times(1)).upload(any(ScanEntity.class), eq(app), eq(stageTypeId), eq(null), any(), eq(false));
    assertThat(uploadReceipt).isEqualTo(mockReceipt);
    assertThat(contextCaptor.getValue().getContainerImageSbomSpecification())
        .isEqualTo(SbomSpecification.CYCLONEDX);
  }

  @Test
  public void testUpload_MissingRequiredArguments() {
    ScanEntity scanEntity = createScanFile(app, TemporaryEntity.uuid().substring(0, 10));
    String stageTypeId = ComplianceStageType.ID;
    // null app
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> service.upload(scanEntity, null, stageTypeId, null, null, null, null, false));
    // null scanFile
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> service.upload(null, app, stageTypeId, null, null, null, null, false));
  }

  private ScanEntity createScanFile(Application app, String scanId) {
    File scanFile = work.getScanFile(app.getId(), scanId);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.writeString(scanFile.toPath(), "test");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new FileScanEntity(scanFile.toPath(), app.getId());
  }

  private TelemetryData buildThirdPartyScanTelemetryData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    telemetryData.setAttributes(new HashMap<>());
    return telemetryData;
  }
}
