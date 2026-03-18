/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.UUID;

import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockReportDownloader
{
  private final ReportDownloader reportDownloader = mock(ReportDownloader.class);

  private InsightWork insightWork;

  private final TemporaryFolder tempDir;

  public MockReportDownloader(TemporaryFolder tempDir) {
    this.tempDir = tempDir;
  }

  /**
   * Simulates that a report (based on the specified resource) exists.
   *
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   *
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  public String mockDownloadReport(String reportResourceName) {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    mockDownloadReport(scanId, reportResourceName);
    return scanId;
  }

  public void setInsightWork(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  /**
   * Simulates that a report (based on the specified resource) associated with the specified scan ID exists.
   *
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   */
  public void mockDownloadReport(String scanId, String reportResourceName) {
    ArgumentMatcher<ApplicationReport> appReportMatcher = (appReport) -> {
      return appReport != null && appReport.getScanId().equals(scanId);
    };

    when(reportDownloader.downloadReport(argThat(appReportMatcher), anyInt(), anyInt())).then(new Answer<Boolean>()
    {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        ApplicationReport report = invocation.getArgument(0, ApplicationReport.class);
        ReportHelper.saveMockReport(
            insightWork,
            tempDir,
            reportResourceName,
            report.getApplication().getId(),
            report.getScanId());

        return true;
      }
    });
  }

  public ReportDownloader getMock() {
    return reportDownloader;
  }
}
