/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.mock.hds.HdsMockServer.RestHandler;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PolicyMonitorAuditTest
    extends AbstractAuditTest
{
  private PolicyMonitor policyMonitor;

  private Application app;

  private Stage stage;

  @Before
  public void setup() {
    policyMonitor = getCLMServer().getInjector().getInstance(PolicyMonitor.class);
    app = tempEntity.newApplicationWithParent("MonitoredApp");
    stage = new Stage(ReleaseStageType.ID);
    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());
  }

  @Test
  public void testRunEvaluation_AppWithMonitoring() {
    createScanFile(app.getId(), RestHandler.SCAN_ID);
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), RestHandler.SCAN_ID);

    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogMessages(EVALUATION_AUDIT_LOGGER, 1).get(0), null, app.getId(), app.getPublicId(),
        app.getName(), ReleaseStageType.ID, scanId2, false, is(MDCUsernameScope.SYSTEM), is(nullValue()),
        is(nullValue()));
  }

  @Test
  public void testRunEvaluation_AppWithMonitoring_WithNoLastPrimaryEvaluation() {
    policyMonitor.run();

    awaitLogMessages(EVALUATION_AUDIT_LOGGER, 0);
  }

  @Test
  public void testRunEvaluation_AppWithMonitoring_WhenNoScanFileFound() {
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), RestHandler.SCAN_ID);

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogMessages(EVALUATION_AUDIT_LOGGER, 1).get(0), "server-error", app.getId(),
        app.getPublicId(), app.getName(), null, null, null, is(MDCUsernameScope.SYSTEM), is(nullValue()),
        is(nullValue()));
  }

  private void mockScanReceiptAndReport(String scanId) {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    mockScanReceipt(scanReceipt);
    mockReport(scanId, "/PolicyMonitorTest/report.zip");
  }
}
