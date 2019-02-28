/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.ResultRow;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.ResultTable;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportRawDataTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private final ApplicationReportPage applicationReportPage = new ApplicationReportPage();

  private final ApplicationReportRawDataPage rawDataPage = new ApplicationReportRawDataPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void starts() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportRawDataTest", "ApplicationReportRawDataTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportRawDataPage.url(app, SCAN_ID));
  }

  @Test
  public void testHeader() {
    String expectedDate = DateTime.now().toString("yyyy-MM-dd");
    String expectedTitle = "Raw Data for " + app.getName() + " Build Report - " + expectedDate;

    rawDataPage.shouldBe(visible);
    rawDataPage.reportTitle().shouldHave(text(expectedTitle));
    IqBackButton backButton = rawDataPage.backButton();
    backButton.shouldHave(text("Back to Application Report"));

    backButton.click();
    applicationReportPage.should(appear);
  }

  @Test
  public void testResults() {
    ResultTable resultTable = rawDataPage.resultTable();
    resultTable.shouldBe(visible);
    resultTable.resultRows().shouldHaveSize(100);

    ResultRow springSecurity = resultTable.resultRow(6);
    springSecurity.component()
        .shouldHave(exactText("org.springframework.security : spring-security-web : 3.2.4.release"));
    springSecurity.declaredLicenses().shouldHave(exactText("Apache-2.0"));
    springSecurity.observedLicenses().shouldNot(exist);
    springSecurity.securityIssue().shouldHave(exactText("sonatype-2017-0507"));
    springSecurity.cvssScore().shouldHave(exactText("5.0"));

    ResultRow resultRowXpp3 = resultTable.resultRow(7);
    resultRowXpp3.component().shouldHave(exactText("xpp3 : xpp3_min : 1.1.4c"));
    resultRowXpp3.declaredLicenses().shouldHave(exactText("Non-Standard, Public Domain, XPP-1.1.1"));
    resultRowXpp3.observedLicenses().shouldHave(exactText(", XPP-1.2"));
    resultRowXpp3.securityIssue().shouldBe(empty);
    resultRowXpp3.cvssScore().shouldBe(empty);
  }
}
