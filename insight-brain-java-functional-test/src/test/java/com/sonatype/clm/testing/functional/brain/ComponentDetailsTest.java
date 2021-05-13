/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ComponentDetailsTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  public static final String HASH = "fa78f54738ccf77379d1";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testComponentDetailsEnabled() {
    try {
      refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
      reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

      ElementsCollection violations = reportPage.resultRows();
      SelenideElement firstViolation = violations.first();
      firstViolation.click();

      waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, "fa78f54738ccf77379d1"));
      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      componentDetailsPage.title().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
      componentDetailsPage.tabs().shouldHaveSize(6);
      SelenideElement backButton = componentDetailsPage.backButton();
      backButton.shouldHave(text("Back to Application Report"));
      backButton.click();

      waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    }
    finally {
      if (reportPage.filterPanel().getElement().is(visible)) {
        reportPage.filterPanel().closeButton().click();
      }
    }
  }

  @Test
  public void testComponentDetailsRemediationDefaultTab() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    waitUntilUrl(ComponentDetailsPage.urlToRemediation(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetailsTabNavigation() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    waitUntilUrl(ComponentDetailsPage.urlToRemediation(app, SCAN_ID, HASH));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    componentDetailsPage.componentInfoTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToComponentInfo(app, SCAN_ID, HASH));

    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, HASH));

    componentDetailsPage.securityTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, HASH));

    componentDetailsPage.legalTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToLegal(app, SCAN_ID, HASH));

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    componentDetailsPage.remediationTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToRemediation(app, SCAN_ID, HASH));
  }
}
