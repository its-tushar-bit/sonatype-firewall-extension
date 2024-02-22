/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThirdPartyScanServiceTest
    extends AbstractBrainServiceIntegrationTest
{
  private ScanUploader scanUploader;

  private InsightWork work;

  private ThirdPartyScanService service;

  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessorMock;

  @Before
  public void before() {
    scanUploader = mock(ScanUploader.class);
    thirdPartyScanResultsProcessorMock = mock(ThirdPartyScanResultsProcessor.class);
    work = getCLMServer().getInstance(InsightWork.class);
    service = new ThirdPartyScanService(thirdPartyScanResultsProcessorMock, scanUploader, work);
  }

  @Test
  public void testFilterAndUpload() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("ThirdPartyScanServiceTest_App", org.getId());
    Stage stage = new Stage(ReleaseStageType.ID);
    String scanId = "ThirdPartyScanServiceTest_scanId";
    File scanFile = createScanFile(app, scanId);
    String scanRequestId = "scanRequestId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(eq(scanFile), any(File.class), any(File.class), eq(null),
        eq(app.getId())))
        .thenReturn(scanRequestId);
    ArgumentCaptor<String> clientUserAgentArgCaptor = ArgumentCaptor.forClass(String.class);
    String testClientUserAgent = "client_user_agent";
    when(scanUploader.upload(any(File.class), eq(app), eq(stage.getStageTypeId()), clientUserAgentArgCaptor.capture()))
        .thenReturn(scanReceipt);

    service.filterAndUpload(scanFile, app, stage.getStageTypeId(), testClientUserAgent, null);

    verify(thirdPartyScanResultsProcessorMock, times(1))
        .filterAndSaveData(eq(scanFile), any(File.class), any(File.class), eq(null), eq(app.getId()));
    verify(thirdPartyScanResultsProcessorMock, times(1))
        .postHandle(scanId, scanRequestId);
    assertThat(clientUserAgentArgCaptor.getValue()).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testFilterAndUpload_FileProcessingError() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("ThirdPartyScanServiceTest_App", org.getId());
    Stage stage = new Stage(ReleaseStageType.ID);
    String scanId = "ThirdPartyScanServiceTest_scanId";
    File scanFile = createScanFile(app, scanId);
    when(thirdPartyScanResultsProcessorMock.filterAndSaveData(eq(scanFile), any(File.class), any(File.class), eq(null),
        eq(app.getId())))
        .thenThrow(new IllegalArgumentException("error"));

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
        () -> service.filterAndUpload(scanFile, app, stage.getStageTypeId(), null, null));
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
