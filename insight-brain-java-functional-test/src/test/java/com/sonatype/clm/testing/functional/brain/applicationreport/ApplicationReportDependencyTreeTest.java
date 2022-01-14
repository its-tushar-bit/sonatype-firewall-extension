/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.DependencyTreePage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;

public class ApplicationReportDependencyTreeTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    assert referencePolicyUrl != null;
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
  }

  @Test
  public void testNoDependencyTreeAvailable() throws IOException {
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();

    refreshOrOpen(ApplicationReportPage.urlWithDepencyTreeEnabled(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.urlWithDepencyTreeEnabled(app, SCAN_ID));

    eyesWatcher.eyesCheck("go To Dependency Tree Button disabled");

    SelenideElement goToDependencyTreeButton = reportPage.goToDependencyTreeButton();

    goToDependencyTreeButton.hover();

    Tooltip.get().shouldBe(visible).shouldHave(text("Please re-scan the application"));

    goToDependencyTreeButton.click();
    DependencyTreePage dependencyTreePage = new DependencyTreePage();
    dependencyTreePage.shouldNotBe(visible);
  }

  @Test
  public void testNavigateToDependencyTree() throws IOException {
    URL zippedReport = ReportHelper.zipReport("/canned-reports/report-with-dependency-tree", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();

    refreshOrOpen(ApplicationReportPage.urlWithDepencyTreeEnabled(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.urlWithDepencyTreeEnabled(app, SCAN_ID));

    eyesWatcher.eyesCheck("go To Dependency Tree Button enabled");

    SelenideElement goToDependencyTreeButton = reportPage.goToDependencyTreeButton();
    goToDependencyTreeButton.shouldBe(visible);
    goToDependencyTreeButton.shouldNotHave(cssClass("disabled"));

    goToDependencyTreeButton.click();
    waitUntilUrl(DependencyTreePage.url(app, SCAN_ID));
    DependencyTreePage dependencyTreePage = new DependencyTreePage();
    dependencyTreePage.shouldBe(visible);
  }
}
