/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private InsightWork insightWork;

  private Application app;

  private String scanId = "ReportServiceTestScanId";

  @Before
  public void before() throws Exception {
    app = tempEntity.newApplicationWithParent("testAppPublicId");

    File sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
  }

  @Test
  public void testFetchReport_Exists() throws Exception {
    createReportFile();
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
  }

  @Test
  public void testFetchReport_DoesNotExist() throws Exception {
    ReportDownloader reportDownloader = mock(ReportDownloader.class);
    when(reportDownloader.downloadReport(eq(scanId), (File) any(), anyInt(), anyInt())).then(new Answer<Boolean>()
    {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        File reportFile = (File) invocation.getArguments()[1];
        FileUtils.copyURLToFile(getClass().getResource("/ReportServiceTest/report.zip"), reportFile);
        return true;
      }
    });

    ReportService reportService = new ReportService(insightWork, reportDownloader);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
  }

  @Test
  public void testGetReport_Exists() throws Exception {
    createReportFile();
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader);
    File report = reportService.getReport(insightWork, app.getId(), scanId);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
  }

  @Test
  public void testGetReport_DoesNotExist() throws Exception {
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader);
    File report = reportService.getReport(insightWork, app.getId(), scanId);
    assertThat(report, nullValue());
  }

  private void createReportFile() throws IOException {
    FileUtils.copyURLToFile(getClass().getResource("/ReportServiceTest/report.zip"),
        insightWork.getReportFile(app.getId(), scanId));
  }
}
