/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SourceControlScanServiceAuditTest
    extends AbstractComponentAuditTest
{
  private SourceControlScanService sourceControlScanService;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private InsightWork insightWork;

  @Mock
  private GitApi mockGitApi;

  @Mock
  private GitApiFactory mockGitApiFactory;

  @Mock
  private ScanHandler mockScanHandler;

  private MockReportDownloader mockReportDownloader;

  @Before
  public void before() {
    sourceControlScanService = lookup(SourceControlScanService.class);
    mockReportDownloader.setInsightWork(insightWork);
  }

  @Override
  public void configure(Binder binder) {
    lenient().when(mockGitApiFactory.createGitApi(any(GitRepositoryInfo.class))).thenReturn(mockGitApi);
    binder.bind(GitApiFactory.class).toInstance(mockGitApiFactory);
    mockReportDownloader = new MockReportDownloader(tempDir);
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    binder.bind(TelemetrySender.class).toInstance(mock(TelemetrySender.class));
    binder.bind(ScanHandler.class).toInstance(mockScanHandler);

    super.configure(binder);
  }

  @Test
  public void testOnSourceControlScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    createRootOrgSourceControl();
    tempEntity.newSourceControl(app.getId(), "http://localhost/testorg/testproject");
    SourceControlEvent sourceControlEvent = new SourceControlEvent() //
        .forSourceControlEvaluation() //
        .setApplicationId(app.getId()) //
        .setStageTypeId(Stage.ID_BUILD) //
        .setStatusId("testStatusId") //
        .setBranchName("testBranchName") //
        .setUserAgent("testUserAgent") //
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);

    String scanId = mockReportDownloader.mockDownloadReport("/AbstractAuditTest/report");
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
            .thenReturn(scanReceipt);

    sourceControlScanService.onSourceControlScan(sourceControlEvent);
    
    assertEvaluationAuditLog(null /* error */, app, Stage.ID_BUILD, false /* isReevaluation */);
  }

  @Test
  public void testOnSourceControlScan_Error() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    createRootOrgSourceControl();
    tempEntity.newSourceControl(app.getId(), "http://localhost/testorg/testproject");
    SourceControlEvent sourceControlEvent = new SourceControlEvent() //
        .forSourceControlEvaluation() //
        .setApplicationId(app.getId()) //
        .setStageTypeId(Stage.ID_BUILD) //
        .setStatusId("testStatusId") //
        .setBranchName("testBranchName") //
        .setUserAgent("testUserAgent") //
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);

    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
            .thenThrow(new RuntimeException("test error"));

    sourceControlScanService.onSourceControlScan(sourceControlEvent);

    assertEvaluationAuditLog("server-error", app, Stage.ID_BUILD, null /* isReevaluation */);
  }

  @Test
  public void testDoSynchronousSourceControlScan() throws Exception {
    // Remove the subject and security manager for the current thread - i.e. make it run as system.
    tearDownSecurity();

    Application app = tempEntity.newApplicationWithParent();
    createRootOrgSourceControl();
    tempEntity.newSourceControl(app.getId(), "http://localhost/testorg/testproject");

    String scanId = mockReportDownloader.mockDownloadReport("/AbstractAuditTest/report");
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
            .thenReturn(scanReceipt);

    sourceControlScanService.doSynchronousSourceControlScan(app.getId(), new Stage(Stage.ID_BUILD), "testBranchName");

    assertEvaluationAuditLog(null /* error */, app, Stage.ID_BUILD, false /* isReevaluation */);
  }

  @Test
  public void testDoSynchronousSourceControlScan_Error() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    createRootOrgSourceControl();
    tempEntity.newSourceControl(app.getId(), "http://localhost/testorg/testproject");

    when(mockScanHandler.handle(any(ScanHandler.ScanRequest.class)))
            .thenThrow(new RuntimeException("test error"));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(
            () -> sourceControlScanService.doSynchronousSourceControlScan(app.getId(), new Stage(Stage.ID_BUILD),
                "testBranchName")).withMessage("test error");

    assertEvaluationAuditLog("server-error", app, Stage.ID_BUILD, null /* isReevaluation */);
  }

  private void assertEvaluationAuditLog(String error, Application app, String stageId, Boolean isReevaluation) {
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), error, app.getId(),
        app.getPublicId(), app.getName(), stageId, null /* scanId */, isReevaluation, SYSTEM_USER);
  }

  private void createRootOrgSourceControl() {
    SourceControl sourceControl = tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null,
        new String(passwordHandler.encryptPassword("token".toCharArray())), SourceControlProvider.GITHUB);
    sourceControl.setSourceControlEvaluationsEnabled(true);
    sourceControlDAO.update(sourceControl);
  }
}
