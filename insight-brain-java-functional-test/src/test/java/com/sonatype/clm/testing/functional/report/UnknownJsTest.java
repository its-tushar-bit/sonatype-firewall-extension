/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import java.util.UUID;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.pages.ReportPolicyPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;

public class UnknownJsTest
    extends AbstractFunctionalTest
{
  private static final String scanId = UUID.randomUUID().toString().replace("-", "");

  private static final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() {
    app = tempEntity.newApplicationWithParent(WaiverTest.class.getSimpleName());
    createPolicy(app.getId());
  }

  @Test
  public void testViewWaivedPolicyViolations() throws Exception {
    evaluator = new TestReportEvaluator(app, scanId, ReportHelper.zipReport("/UnknownJsTest", tempDir),
        baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ReportPage.url(app, scanId));

    ReportPage.policyTabButton().click();
    ReportPolicyPage.rows().shouldHave(texts("frontend.zip"));

    refreshOrOpen(ReportPage.url(app, scanId).replaceAll("index.html", "index.html?unknownjs=true"));

    ReportPage.policyTabButton().click();
    eyesWatcher.eyesCheck();
    ReportPolicyPage.rows().shouldHave(texts("frontend.zip", "ComponentDisplayModule.js"));
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
