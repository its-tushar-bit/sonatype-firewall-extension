/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

public class PolicyEvaluateServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyEvaluateService policyEvaluateService;

  private MockReportDownloader mockReportDownloader;

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader();
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    binder.bind(TelemetrySender.class).toInstance(mock(TelemetrySender.class));
    binder.bind(ScanHandler.class).toInstance(mock(ScanHandler.class));

    super.configure(binder);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluate_Unauthenticated() throws Exception {
    policyEvaluateService.evaluate(app.getPublicId(), "scanId", new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test
  public void testEvaluate_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    String scanId = mockReportDownloader.mockDownloadReport("/PolicyEvaluateServiceTest/report");
    ScanHelper.createDummyScanFile(lookup(InsightWork.class), app.getId(), scanId);

    policyEvaluateService.evaluate(app.getPublicId(), scanId, new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluate_Unauthorized() throws Exception {
    login();
    policyEvaluateService.evaluate(app.getPublicId(), "scanId", new Stage(BuildStageType.ID),
        ScanTriggerType.CLI);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateWithPolling_Unauthenticated() throws Exception {
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test
  public void testEvaluateWithPolling_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateWithPolling_Unauthorized() throws Exception {
    login();
    policyEvaluateService.evaluateWithPolling(IntegrationType.CLI, app.getPublicId(), ClientScanType.SONATYPE, null,
        new Stage(BuildStageType.ID));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPollEvaluationResult_Unauthenticated() {
    policyEvaluateService.pollEvaluationResult(app.getPublicId(), "statusId");
  }

  @Test
  public void testPollEvaluationResult_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      policyEvaluateService.pollEvaluationResult(app.getPublicId(), "statusId");
    }).withMessage("Policy evaluation status with id %s for public application id %s was not found.", "statusId",
        app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testPollEvaluationResult_Unauthorized() {
    login();
    policyEvaluateService.pollEvaluationResult(app.getPublicId(), "statusId");
  }
}
