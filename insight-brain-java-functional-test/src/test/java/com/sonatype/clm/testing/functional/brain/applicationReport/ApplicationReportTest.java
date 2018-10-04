/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationReport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private Policy policy;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    app = tempEntity.newApplicationWithParent("ApplicationReportTest", "ApplicationReportTest");
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    Constraint constraint = new Constraint("C1", "All coordinates", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:javancss*"));
    policy = tempEntity.newPolicy("ApplicationReportTest Policy", constraint);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testSummary() {
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDate().shouldHave(text(DateTime.now().toString("yyyy-MM-dd")));
    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
    reportPage.threatIndicators().critical().shouldHave(text("0"));
    reportPage.threatIndicators().severe().shouldHave(text("0"));
    reportPage.threatIndicators().moderate().shouldHave(text("0"));
    reportPage.threatIndicators().caption().shouldHave(text("0 Violations"));
    reportPage.threatIndicators().subCaption().shouldHave(text("Affecting 0 components"));
  }

  @Test
  public void testOptionsMenu() {
    IQDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown.menu().shouldBe(visible).entries()
        .shouldHave(texts("Re-Evaluate Report", "Generate PDF", "View raw data"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testResults() {
    reportPage.resultRows().shouldHaveSize(4);
    reportPage.resultRow(1).threatBar().shouldHave(cssClass("severe"));
    reportPage.resultRow(1).threatNumber().shouldHave(text("5"));
    reportPage.resultRow(1).policyName().shouldHave(text(policy.getName()));
    for (int i = 2; i <= 4; i++) {
      reportPage.resultRow(i).threatBar().shouldHave(cssClass("ignore"));
      reportPage.resultRow(i).threatNumber().shouldHave(text("0"));
      reportPage.resultRow(i).policyName().shouldHave(text("None"));
    }
    reportPage.resultRow(1).componentName().shouldHave(text("javancss : javancss : 29.50"));
    reportPage.resultRow(2).componentName().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    reportPage.resultRow(3).componentName().shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    reportPage.resultRow(4).componentName().shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
  }

  @Test
  public void testWaivedIndicator() throws Exception {
    reportPage.resultRow(1).threatNumber().shouldHave(text("5"));
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);

    tempEntity.newWaiver(policy.getId(), app.getId());
    evaluator.reevaluatePolicy();
    refresh();

    reportPage.resultRows().shouldHaveSize(5); // because we're not hiding waived violations ATM
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    eyesWatcher.eyesCheck();
  }
}
