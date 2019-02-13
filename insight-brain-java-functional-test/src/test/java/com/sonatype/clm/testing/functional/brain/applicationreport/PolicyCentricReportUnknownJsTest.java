/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.util.UUID;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;

public class PolicyCentricReportUnknownJsTest
    extends AbstractFunctionalTest
{
  private static final String scanId = UUID.randomUUID().toString().replace("-", "");

  private static final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() {
    app = tempEntity.newApplicationWithParent(PolicyCentricReportUnknownJsTest.class.getSimpleName(), "app");
    createPolicy(app.getId());
  }

  @Test
  public void testViewWaivedPolicyViolations() throws Exception {
    evaluator = new TestReportEvaluator(app, scanId, ReportHelper.zipReport("/UnknownJsTest", tempDir),
        Configuration.baseUrl, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, scanId));

    reportPage.resultRows().shouldHave(texts("frontend.zip"));

    refreshOrOpen(ApplicationReportPage.url(app, scanId) + "?unknownjs=true");

    eyesWatcher.eyesCheck();
    reportPage.resultRows().shouldHave(texts("frontend.zip", "ComponentDisplayModule.js"));
  }

  private void createPolicy(String ownerId) {
    // create policy
    Condition condition = new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId());
    Constraint constraint = new Constraint();
    constraint.setName("Match State Check");
    constraint.addCondition(condition);
    Policy policy = new Policy();
    policy.setName("Unmatched");
    policy.addConstraint(constraint);
    policy.setOwnerId(ownerId);

    // add policy
    tempEntity.newPolicy(policy);
  }
}
