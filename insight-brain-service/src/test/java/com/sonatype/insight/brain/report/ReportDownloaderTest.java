/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportDownloaderTest
    extends AbstractComponentTest
{
  @Inject
  private ReportDownloader reportDownloader;

  @Inject
  private InsightWork work;

  @Rule
  public LogOutput logOutput = new LogOutput(ReportDownloader.class);

  private HdsClient mockHdsClient;

  @Override
  public void configure(Binder binder) {
    mockHdsClient = mock(DefaultHdsClient.class);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testDownloadReport_NonExistentScanId_DoesNotCreateParentDir() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "NonExistentScanId";

    NotFoundException expectedException = new NotFoundException("test");
    when(mockHdsClient.get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId)).thenThrow(expectedException);

    File reportFile = work.getReportFile(app.getId(), scanId);
    boolean rc = reportDownloader.downloadReport(scanId, reportFile, 0, 0);
    assertThat(rc).isFalse();
    assertThat(reportFile.getParentFile()).doesNotExist();
    assertThat(logOutput).atErrorLevel().contains(ReportDownloader.timeoutExceptionMessage(scanId));
  }

  @Test
  public void testDownloadReport_ReportNotFoundWithNoReportTimeout_DoesNotRetry() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";

    NotFoundException expectedException = new NotFoundException("test");
    when(mockHdsClient.get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId)).thenThrow(expectedException);

    File reportFile = work.getReportFile(app.getId(), scanId);
    boolean rc = reportDownloader.downloadReport(scanId, reportFile, 0, 0);
    assertThat(rc).isFalse();
    // only the initial download request is made
    verify(mockHdsClient).get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId);
  }

  @Test(timeout = 5000)
  public void testDownloadReport_ReportNotFoundWithReportTimeout_Retries() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";

    NotFoundException expectedException = new NotFoundException("test");
    when(mockHdsClient.get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId)).thenThrow(expectedException);

    File reportFile = work.getReportFile(app.getId(), scanId);
    long startTime = System.currentTimeMillis();
    boolean rc = reportDownloader.downloadReport(scanId, reportFile, 3, 2);
    long totalTime = System.currentTimeMillis() - startTime;
    assertThat(rc).isFalse();
    // 1 initial download request + 2 retries = 3 download requests
    verify(mockHdsClient, times(3)).get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId);
    // since we 'sleep' 1 time at 2000ms and another time at 1000ms = total sleep of 3000ms,
    // the test should not run quicker than 3000ms, but allow a variance in execution on the max
    assertThat(totalTime).isBetween(3000L, 3200L);
  }

  @Test
  public void testDownloadReport_RetryOnBadGatewayException() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";

    BadGatewayException expectedException = new BadGatewayException("test");
    when(mockHdsClient.get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId)).thenThrow(expectedException);

    File reportFile = work.getReportFile(app.getId(), scanId);
    boolean rc = reportDownloader.downloadReport(scanId, reportFile, 1, 0);
    assertThat(rc).isFalse();

    verify(mockHdsClient, times(ReportDownloader.BAD_GATEWAY_RETRY_LIMIT))
        .get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId);
  }

  @Test
  public void testDownloadReport_CanDownloadAfterABadGatewayRetry() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "scanId";

    BadGatewayException initialException = new BadGatewayException("test");
    InputStream finalReport = new ByteArrayInputStream("report".getBytes());
    when(mockHdsClient.get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId))
        .thenThrow(initialException)
        .thenReturn(finalReport);

    File reportFile = work.getReportFile(app.getId(), scanId);
    boolean rc = reportDownloader.downloadReport(scanId, reportFile, 1, 0);
    assertThat(rc).isTrue();

    verify(mockHdsClient, times(2)).get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId);
  }
}
