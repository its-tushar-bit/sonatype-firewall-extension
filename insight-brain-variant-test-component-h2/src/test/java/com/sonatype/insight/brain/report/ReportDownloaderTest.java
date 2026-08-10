/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static com.sonatype.insight.brain.utils.HttpHelper.createMockResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ComponentH2Test
public class ReportDownloaderTest
    extends AbstractComponentH2Test
{
  private ReportDownloader reportDownloader;

  @Rule
  public LogOutput logOutput = new LogOutput(ReportDownloader.class);

  @Inject
  private HdsClient hdsClient;

  private HdsClient spyHdsClient;

  @Inject
  private ReportDataStore reportDataStore;

  @Inject
  private FileLifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Inject
  private InsightWork insightWork;

  @BeforeEach
  public void before() {
    spyHdsClient = mockingDetails(hdsClient).isMock() ? hdsClient : spy(hdsClient);
    reset(spyHdsClient);
    reportDownloader = new ReportDownloader(spyHdsClient, lifecycleReportPersistenceService);
  }

  @Test
  public void testDownloadReport_NonExistentScanId_DoesNotCreateParentDir() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "NonExistentScanId";
    doReturn(createMockResponse(Status.NOT_FOUND)).when(spyHdsClient).getResponse(any());
    LifecycleReport lifecycleReport = reportDataStore.getLifecycleReport(app, scanId);

    boolean rc = reportDownloader.downloadReport(lifecycleReport, 0, 0);

    assertThat(rc).isFalse();
    assertThat(insightWork.getReportDir(app.getId(), scanId)).doesNotExist();
    assertThat(logOutput).atErrorLevel().contains(ReportDownloader.timeoutExceptionMessage(scanId));
  }

  @Test
  public void testDownloadReport_ReportNotFoundWithNoReportTimeout_DoesNotRetry() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";
    doReturn(createMockResponse(Status.NOT_FOUND)).when(spyHdsClient).getResponse(any());
    LifecycleReport lifecycleReport = reportDataStore.getLifecycleReport(app, scanId);

    boolean rc = reportDownloader.downloadReport(lifecycleReport, 0, 0);

    assertThat(rc).isFalse();
    // only the initial download request is made
    verify(spyHdsClient).get(any(), eq(InputStream.class), eq(ReportDownloader.HDS_PATH), eq(null), eq(scanId));
  }

  @Test
  @Timeout(10)
  public void testDownloadReport_ReportNotFoundWithReportTimeout_Retries() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";
    doReturn(createMockResponse(Status.NOT_FOUND)).when(spyHdsClient).getResponse(any());
    LifecycleReport lifecycleReport = reportDataStore.getLifecycleReport(app, scanId);
    long startTime = System.currentTimeMillis();

    boolean rc = reportDownloader.downloadReport(lifecycleReport, 3, 2);

    long totalTime = System.currentTimeMillis() - startTime;
    assertThat(rc).isFalse();
    // 1 initial download request + at least 2 retries
    verify(spyHdsClient, atLeast(3)).getResponse(any());
    // request 1 -> sleep 2000ms -> request 2 -> sleep 2000ms -> request 3
    // the test should not run quicker than 4000ms
    assertThat(totalTime).isGreaterThanOrEqualTo(4000L);
  }

  @Test
  public void testDownloadReport_RetryOnBadGatewayException() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";
    doReturn(createMockResponse(Status.BAD_GATEWAY)).when(spyHdsClient).getResponse(any());
    LifecycleReport lifecycleReport = reportDataStore.getLifecycleReport(app, scanId);

    boolean rc = reportDownloader.downloadReport(lifecycleReport, 1, 0);

    assertThat(rc).isFalse();
    verify(spyHdsClient, times(5)).getResponse(any());
  }

  @Test
  public void testDownloadReport_CanDownloadAfterABadGatewayRetry() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";
    InputStream finalReport = new ByteArrayInputStream("report".getBytes());
    doReturn(createMockResponse(Status.BAD_GATEWAY), createMockResponse(Status.OK, finalReport)).when(spyHdsClient)
        .getResponse(any());
    LifecycleReport lifecycleReport = reportDataStore.getLifecycleReport(app, scanId);

    boolean rc = reportDownloader.downloadReport(lifecycleReport, 1, 0);

    assertThat(rc).isTrue();
    verify(spyHdsClient, times(2)).getResponse(any());
  }
}
