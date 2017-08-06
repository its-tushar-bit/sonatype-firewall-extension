/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private InsightWork insightWork;

  private Application app;

  private String scanId = "ReportServiceTestScanId";

  private PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private InsightConfig insightConfig = new InsightConfig();

  @Before
  public void before() throws Exception {
    app = tempEntity.newApplicationWithParent("testAppPublicId");

    File sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
  }

  @Test
  public void testFetchReport_Exists() throws Exception {
    createReportFile();
    ReportDownloader reportDownloader = null;

    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
  }

  @Test
  public void testFetchReport_DoesNotExistAndEvaluationExist() throws Exception {
    ReportDownloader reportDownloader = mock(ReportDownloader.class);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), scanId);
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig);
    try {
      reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
      fail("IllegalStateException expected but not thrown");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage(),
          equalTo("The report file does not exist for application ID " + app.getId() + " and scan ID " + scanId + "."));
    }
  }

  @Test
  public void testFetchReport_WithWaitForReport_DoesNotExist() throws Exception {
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

    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
    verify(reportDownloader).downloadReport(eq(scanId), any(File.class), eq(900), eq(5));
  }

  @Test
  public void testFetchReport_WithoutWaitingForReport_DoesNotExist() throws Exception {
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

    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, false /* waitForReport */);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
    verify(reportDownloader).downloadReport(eq(scanId), any(File.class), eq(0), eq(5));
  }

  @Test
  public void testGetReport_Exists() throws Exception {
    createReportFile();
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig);
    File report = reportService.getReport(insightWork, app.getId(), scanId);
    assertThat(report, notNullValue());
    assertThat(report.exists(), is(true));
    assertThat(report.getName(), is("report.zip"));
  }

  @Test
  public void testGetReport_DoesNotExist() throws Exception {
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig);
    File report = reportService.getReport(insightWork, app.getId(), scanId);
    assertThat(report, nullValue());
  }

  private void createReportFile() throws IOException {
    FileUtils.copyURLToFile(getClass().getResource("/ReportServiceTest/report.zip"),
        insightWork.getReportFile(app.getId(), scanId));
  }
}
