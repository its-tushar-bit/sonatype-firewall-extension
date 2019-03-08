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
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.ResultRow;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.ResultTable;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.VulnerabilityModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
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

    ResultRow springSecurity = resultTable.resultRow(94);
    ScrollUtil.scrollIntoView(springSecurity.getElement());
    checkRawDataRow(springSecurity, "org.springframework.security : spring-security-web : 3.2.4.release", "Apache-2.0",
        null, "sonatype-2017-0507", "5.0");
    springSecurity.declaredLicenses().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Declared:Apache-2.0 Observed:Apache-2.0"));

    ResultRow resultRowXpp3 = resultTable.resultRow(100);
    ScrollUtil.scrollIntoView(resultRowXpp3.getElement());
    checkRawDataRow(resultRowXpp3, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", ", XPP-1.2",
        "", "");
    resultRowXpp3.declaredLicenses().hover();
    Tooltip.get().shouldBe(visible)
        .shouldHave(text("Declared:Non-Standard, Public Domain, XPP-1.1.1 Observed:XPP-1.2"));

    // ensure that the entire (filled) part of the license column has the tooltip
    resultRowXpp3.cvssScore().hover();
    Tooltip.get().shouldNotBe(visible);
    resultRowXpp3.observedLicenses().hover();
    Tooltip.get().shouldBe(visible)
        .shouldHave(text("Declared:Non-Standard, Public Domain, XPP-1.1.1 Observed:XPP-1.2"));
  }

  @Test
  public void testVulnerabilityModal() {
    testCLMServer.getHdsServer().setResponseForURI("rest/vulnerability/details/sonatype/sonatype-2017-0507",
        getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails2.json"), 200);

    ResultTable resultTable = rawDataPage.resultTable();
    ResultRow springSecurity = resultTable.resultRow(94);
    ScrollUtil.scrollIntoView(springSecurity.getElement());
    springSecurity.securityIssue().shouldHave(exactText("sonatype-2017-0507")).click();

    VulnerabilityModal vulnerabilityModal = rawDataPage.vulnerabilityModal();
    vulnerabilityModal.shouldBe(visible);
    vulnerabilityModal.header().shouldHave(text("Vulnerability Information"));
    vulnerabilityModal.content().$("#somedivfortest").shouldHave(text("sonatype-2017-0507"));
    vulnerabilityModal.closeButton().shouldHave(text("Close")).click();
    vulnerabilityModal.shouldNot(exist);
  }

  @Test
  public void testSorting() {
    ResultTable resultTable = rawDataPage.resultTable();
    resultTable.resultRows().shouldHaveSize(100);

    // starts off sorting by component name ascending
    ResultRow firstRow = resultTable.resultRow(1);
    ResultRow lastRow = resultTable.resultRow(100);
    checkRawDataRow(firstRow, "angular 1.2.17", "MIT", ", Not Supported", "sonatype-2014-0015", "5.4");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", ", XPP-1.2", "",
        "");

    // sort by component descending
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().componentHeader().click();
    checkRawDataRow(firstRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", ", XPP-1.2", "",
        "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "angular 1.2.17", "MIT", ", Not Supported", "sonatype-2017-0486", "4.3");

    // sort by license (ascending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().licensesHeader().click();
    checkRawDataRow(firstRow, "full.jar", "", null, "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "aopalliance : aopalliance : 1.0", "Public Domain", ", No Source License", "", "");

    // sort by license (descending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().licensesHeader().click();
    checkRawDataRow(firstRow, "aopalliance : aopalliance : 1.0", "Public Domain", ", No Source License", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "regexmatch.dll", "", null, "", "");

    // sort by security (ascending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().securityIssueHeader().click();
    checkRawDataRow(firstRow, "aopalliance : aopalliance : 1.0", "Public Domain", ", No Source License", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "org.springframework.security : spring-security-web : 3.2.4.release", "Apache-2.0", null,
        "sonatype-2017-0507", "5.0");

    // sort by security (descending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().securityIssueHeader().click();
    checkRawDataRow(firstRow, "org.springframework.security : spring-security-web : 3.2.4.release", "Apache-2.0", null,
        "sonatype-2017-0507", "5.0");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", ", XPP-1.2", "",
        "");

    // sort by cvss (descending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().cvssScoreHeader().click();
    checkRawDataRow(firstRow, "com.fasterxml.jackson.core : jackson-databind : 2.0.4", "Apache-2.0 or LGPL-2.1",
        ", Non-Standard", "CVE-2017-7525", "9.8");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", ", XPP-1.2", "",
        "");

    // sort by cvss (ascending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().cvssScoreHeader().click();
    checkRawDataRow(firstRow, "aopalliance : aopalliance : 1.0", "Public Domain", ", No Source License", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "org.springframework : spring-expression : 3.2.4.release", "Apache-2.0", null,
        "CVE-2018-1270", "9.8");
  }

  private void checkRawDataRow(final ResultRow row,
                               final String componentName,
                               final String declaredLicenses,
                               final String observedLicenses,
                               final String securityIssue,
                               final String cvssScore)
  {
    row.component().shouldHave(exactText(componentName));
    row.declaredLicenses().shouldHave(exactText(declaredLicenses));
    if (observedLicenses == null) {
      row.observedLicenses().shouldNot(exist);
    }
    else {
      row.observedLicenses().shouldHave(exactText(observedLicenses));
    }
    row.securityIssue().shouldHave(exactText(securityIssue));
    row.cvssScore().shouldHave(exactText(cvssScore));
  }
}
