/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

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

    super.configure(binder);
  }

  @Test
  public void testEvaluate_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    String scanId = mockReportDownloader.mockDownloadReport("/PolicyEvaluateServiceTest/report.zip");

    policyEvaluateService.evaluate(app.getPublicId(), scanId, new Stage(BuildStageType.ID));
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluate_Unauthorized() throws Exception {
    login();
    policyEvaluateService.evaluate(app.getPublicId(), "scanId", new Stage(BuildStageType.ID));
  }
}
