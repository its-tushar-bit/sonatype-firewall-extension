/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class LegacyReportViewTest
    extends AbstractFunctionalTest
{
  private Application app;

  private TestReportEvaluator evaluator;

  private static final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private final String scanId = "36520727d65449bdae17917da746637a";

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() {
    app = tempEntity.newApplicationWithParent();
    evaluator = new TestReportEvaluator(app, scanId,
        ReportHelper.zipReport("/canned-reports/report-without-resources", tempDir), Configuration.baseUrl, work);
  }

  @Test
  public void testViewWaivedPolicyViolations() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());
    evaluator.evaluatePolicy();
    refreshOrOpen(ReportPage.url(app, scanId));

    ReportPage.summaryTabButton().shouldBe(visible).click();
    ReportPage.coverageDonut().shouldBe(visible);
    //eyesWatcher.eyesCheck("Legacy View Summary Tab"); https://sonatype.atlassian.net/browse/CLM-30559

    ReportPage.policyTabButton().shouldBe(visible).click();
    ReportPage.componentContainer().shouldBe(visible);
    //eyesWatcher.eyesCheck("Legacy View Policy Tab"); https://sonatype.atlassian.net/browse/CLM-30559

    ReportPage.securityContainerButton().shouldBe(visible).click();
    ReportPage.securityTable().shouldBe(visible);
    //eyesWatcher.eyesCheck("Legacy View Security Container Tab"); https://sonatype.atlassian.net/browse/CLM-30559

    ReportPage.licenseContainerButton().shouldBe(visible).click();
    ReportPage.licenseContainer().shouldBe(visible);
    //eyesWatcher.eyesCheck("Legacy View License Container"); https://sonatype.atlassian.net/browse/CLM-30559
  }

  private void createGavViolatingPolicy(String ownerId) {
    // create policy
    Condition condition = new Condition(CoordinatesConditionType.ID, "match", "maven:*");
    Constraint constraint = new Constraint();
    constraint.setName("All coordinates");
    constraint.addCondition(condition);
    Policy policy = new Policy();
    policy.setName("All Components");
    policy.addConstraint(constraint);
    policy.setOwnerId(ownerId);

    // add policy
    tempEntity.newPolicy(policy);
  }
}
