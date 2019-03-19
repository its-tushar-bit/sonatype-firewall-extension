/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportServiceTest
    extends AbstractComponentTest
{
  @Inject
  private InsightWork insightWork;

  private Application app;

  private String scanId = "ReportServiceTestScanId";

  private PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  @Inject
  private InsightConfig insightConfig;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Before
  public void before() throws Exception {
    app = tempEntity.newApplicationWithParent("testAppPublicId");
  }

  @Test
  public void testFetchReport_Exists() throws Exception {
    createReportFile();
    ReportDownloader reportDownloader = null;

    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
  }

  @Test
  public void testFetchReport_DoesNotExistAndEvaluationExist() throws Exception {
    ReportDownloader reportDownloader = mock(ReportDownloader.class);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), scanId);
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    }).withMessageContaining("report for application ID " + app.getId() + " and scan ID " + scanId + " does not exist")
        .withMessageContaining("purged to the trash");
  }

  @Test
  public void testFetchReport_WithWaitForReport_DoesNotExist() throws Exception {
    MockReportDownloader mockReportDownloader = new MockReportDownloader();
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report.zip");

    ReportService reportService = new ReportService(insightWork, mockReportDownloader.getMock(), policyEvaluationDAO,
        insightConfig, applicationDAO);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, true /* waitForReport */);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
    verify(mockReportDownloader.getMock()).downloadReport(eq(scanId), any(File.class), eq(900), eq(5));
  }

  @Test
  public void testFetchReport_WithoutWaitingForReport_DoesNotExist() throws Exception {
    MockReportDownloader mockReportDownloader = new MockReportDownloader();
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report.zip");

    ReportService reportService = new ReportService(insightWork, mockReportDownloader.getMock(), policyEvaluationDAO,
        insightConfig, applicationDAO);
    File report = reportService.fetchReport(insightWork, app.getId(), scanId, false /* waitForReport */);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
    verify(mockReportDownloader.getMock()).downloadReport(eq(scanId), any(File.class), eq(0), eq(5));
  }

  @Test
  public void testGetReport_Exists() throws Exception {
    createReportFile();
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);
    File report = reportService.getReport(insightWork, app.getId(), scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
  }

  @Test
  public void testGetReport_DoesNotExist() throws Exception {
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);
    File report = reportService.getReport(insightWork, app.getId(), scanId);
    assertThat(report).isNull();
  }

  @Test
  public void testGetReportMetadata() throws Exception {
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";

    // ReportResource.fetchReport requires a report.zip to exist when evaluations exist
    createReportFile(app.getId(), scanId1, zipReportDir("/ReportResourceTest/report-expanded_coverage_false"));
    // use an older data.json to make sure they still work
    createReportFile(app.getId(), scanId2, zipReportDir("/ReportResourceTest/report"));

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId2);

    ReportDownloader reportDownloader = mock(ReportDownloader.class);
    when(reportDownloader.downloadReport(eq("12345678"), (File) any(), anyInt(), anyInt())).then(new Answer<Boolean>()
    {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return false;
      }
    });
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);

    // Verify Response for scan 1
    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId1);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval1.getTime());

    // Verify Response for scan 2
    metadata = reportService.getReportMetadata(app.getPublicId(), scanId2);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Release Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval2.getTime());

    // Unknown scan id
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      reportService.getReportMetadata(app.getPublicId(), "12345678");
    }).withMessage("Could not download the report for scan ID 12345678");
  }

  @Test
  public void testGetReportMetadata_expandedCoverage() throws Exception {
    createReportFile(app.getId(), scanId, zipReportDir("/ReportResourceTest/report-expanded_coverage"));
    ReportDownloader reportDownloader = null;
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);

    // Verify Response for scan
    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId);
    assertThat(metadata).isNotNull();
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getReportTitle()).isEqualTo("Expanded Coverage Report");
    assertThat(metadata.getReportTime().getTime()).isEqualTo(1503511338632L);
  }

  @Test
  public void testPrepareExpandedCoverageReport() throws Exception {
    HdsClient hdsClient = mock(HdsClient.class);
    Map<String, String> queryParams = null;
    // The tested method is supposed to wait for the report to become available on the HDS.
    // We verify that it waits by returning NotFound on the first attempt to download the report for HDS.
    // Only the second attempt is successful. If the tested method does not wait, then it fails on the first attempt.
    when(hdsClient.get(eq(InputStream.class), eq(ReportDownloader.HDS_PATH), eq(queryParams), eq(scanId)))
        .thenThrow(new NotFoundException("test exception"))
        .thenReturn(new FileInputStream(zipReportDir("/ReportResourceTest/report-expanded_coverage")));
    ReportDownloader reportDownloader = new ReportDownloader(hdsClient, new FileCleaner());
    ReportService reportService = new ReportService(insightWork, reportDownloader, policyEvaluationDAO, insightConfig,
        applicationDAO);

    File reportFile = insightWork.getReportFile(app.getId(), scanId);
    assertThat(reportFile).doesNotExist();

    reportService.prepareExpandedCoverageReport(app.getPublicId(), scanId);
    
    assertThat(reportFile).isFile();
  }

  private void createReportFile() throws IOException {
    FileUtils.copyURLToFile(getClass().getResource("/ReportServiceTest/report.zip"),
        insightWork.getReportFile(app.getId(), scanId));
  }

  private void createReportFile(String appId, String scanId, File reportFile) throws IOException {
    FileUtils.copyFile(reportFile, insightWork.getReportFile(appId, scanId));
  }

  private File zipReportDir(String resourceName) {
    try {
      URL resourceUrl = getClass().getResource(resourceName);
      File resourceDir = new File(resourceUrl.toURI());
      File reportZipFile = new File(tempDir.getRoot(), getClass().getSimpleName() + "-" + UUID.randomUUID() + ".zip");
      Zipper.zip(resourceDir, reportZipFile);
      return reportZipFile;
    }
    catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
