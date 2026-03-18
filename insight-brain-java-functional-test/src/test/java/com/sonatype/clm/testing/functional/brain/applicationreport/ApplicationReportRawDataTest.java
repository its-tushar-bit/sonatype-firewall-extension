/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IqVulnerabilityModal;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.ResultRow;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage.ResultTable;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.SelenideElement;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportRawDataTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final int EXPECTED_ROWS_COUNT = 101;

  private final ApplicationReportPage applicationReportPage = new ApplicationReportPage();

  private final ApplicationReportRawDataPage rawDataPage = new ApplicationReportRawDataPage();

  private PolicyEvaluationDAO policyEvaluationDAO;

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void starts() throws IOException {
    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);

    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportRawDataTest", "ApplicationReportRawDataTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportRawDataPage.url(app, SCAN_ID));
  }

  @Test
  public void testHeader() {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    Date policyEvaluationTime = policyEvaluation.getTime();

    String policyEvaluationTimeStr = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        .print(policyEvaluationTime.getTime());
    String expectedTitle = "Raw Data for " + app.getName() + " Build Report";

    rawDataPage.shouldBe(visible);
    rawDataPage.reportTitle().shouldHave(text(expectedTitle));
    rawDataPage.description().shouldHave(text(policyEvaluationTimeStr));
    NxBackButton backButton = rawDataPage.backButton();
    backButton.shouldHave(text("Back to Application Report"));

    backButton.click();
    applicationReportPage.should(appear);
  }

  @Test
  public void testResults() {
    ResultTable resultTable = rawDataPage.resultTable();
    resultTable.shouldBe(visible);
    resultTable.resultRows().shouldHave(size(EXPECTED_ROWS_COUNT));

    ResultRow springSecurity = resultTable.resultRow(95);
    ScrollUtil.scrollIntoView(springSecurity.getElement());
    checkRawDataRow(springSecurity, "org.springframework.security : spring-security-web : 3.2.4.release", "Apache-2.0",
        null, "sonatype-2017-0507", "5.0");
    springSecurity.declaredLicenses().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Declared: Apache-2.0 Observed: Apache-2.0"));

    ResultRow resultRowXpp3 = resultTable.resultRow(EXPECTED_ROWS_COUNT);
    ScrollUtil.scrollIntoView(resultRowXpp3.getElement());
    checkRawDataRow(resultRowXpp3, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", "XPP-1.2",
        "", "");
    resultRowXpp3.declaredLicenses().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Declared: Non-Standard, Public Domain, XPP-1.1.1 Observed: XPP-1.2"));

    // ensure that the entire (filled) part of the license column has the tooltip
    resultRowXpp3.cvssScore().hover();
    Tooltip.get().shouldNotBe(visible);
    resultRowXpp3.observedLicenses().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Declared: Non-Standard, Public Domain, XPP-1.1.1 Observed: XPP-1.2"));

    eyesWatcher.eyesCheck("Test Raw Data License Tooltip", false, false);
  }

  @Test
  public void testVulnerabilityModal() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");

    ResultTable resultTable = rawDataPage.resultTable();
    ResultRow springSecurity = resultTable.resultRow(95);
    ScrollUtil.scrollIntoView(springSecurity.getElement());
    springSecurity.securityIssue().shouldHave(exactText("sonatype-2017-0507"));
    springSecurity.securityIssueLink().click();

    IqVulnerabilityModal vulnerabilityModal = rawDataPage.vulnerabilityModal();
    vulnerabilityModal.shouldBe(visible);
    vulnerabilityModal.header().shouldHave(text("Vulnerability Information"));
    SelenideElement vulnerabilityDetails = vulnerabilityModal.vulnerabilityDetails();
    vulnerabilityDetails.shouldHave(text("sonatype-2017-0507"));
    vulnerabilityDetails.shouldHave(text("Sonatype CVSS 35.4"));
    vulnerabilityDetails.shouldHave(text("Sonatype Data Research"));
    vulnerabilityDetails.shouldHave(text("There is no non vulnerable version of this package. We recommend " +
        "investigating alternative components or a potential mitigating control."));

    // test that component-specific fields are present
    vulnerabilityDetails.shouldHave(text("Root Cause " +
        "org.webjars:bootstrap:3.1.1META-INF/resources/webjars/bootstrap/3.1.1/js/bootstrap.js[3.1.1-1,3.1.1-2]"));

    vulnerabilityModal.closeButton().shouldHave(text("Close")).click();
    vulnerabilityModal.shouldNot(exist);
  }

  @Test
  public void testSorting() {
    ResultTable resultTable = rawDataPage.resultTable();
    resultTable.resultRows().shouldHave(size(EXPECTED_ROWS_COUNT));

    // starts off sorting by component name ascending
    ResultRow firstRow = resultTable.resultRow(1);
    ResultRow lastRow = resultTable.resultRow(EXPECTED_ROWS_COUNT);
    checkRawDataRow(firstRow, "angular 1.2.17", "MIT", "Not Supported", "sonatype-2014-0015", "5.4");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", "XPP-1.2", "",
        "");

    // sort by component descending
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().componentHeader().click();
    checkRawDataRow(firstRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", "XPP-1.2", "",
        "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "angular 1.2.17", "MIT", "Not Supported", "sonatype-2017-0486", "4.3");

    // sort by license (ascending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().licensesHeader().click();
    checkRawDataRow(firstRow, "full.jar", null, null, "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "aopalliance : aopalliance : 1.0", "Public Domain", "No Source License", "", "");

    // sort by license (descending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().licensesHeader().click();
    checkRawDataRow(firstRow, "aopalliance : aopalliance : 1.0", "Public Domain", "No Source License", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "regexmatch.dll", null, null, "", "");

    // sort by security (ascending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().securityIssueHeader().click();
    checkRawDataRow(firstRow, "aopalliance : aopalliance : 1.0", "Public Domain", "No Source License", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "org.springframework.security : spring-security-web : 3.2.4.release", "Apache-2.0", null,
        "sonatype-2017-0507", "5.0");

    // sort by security (descending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().securityIssueHeader().click();
    checkRawDataRow(firstRow, "org.springframework.security : spring-security-web : 3.2.4.release", "Apache-2.0", null,
        "sonatype-2017-0507", "5.0");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", "XPP-1.2", "",
        "");

    // sort by cvss (descending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().cvssScoreHeader().click();
    checkRawDataRow(firstRow, "com.fasterxml.jackson.core : jackson-databind : 2.0.4", "Apache-2.0 or LGPL-2.1",
        "Non-Standard", "CVE-2017-7525", "9.8");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "xpp3 : xpp3_min : 1.1.4c", "Non-Standard, Public Domain, XPP-1.1.1", "XPP-1.2", "",
        "");

    // sort by cvss (ascending)
    ScrollUtil.scrollIntoView(rawDataPage.headers().getElement());
    rawDataPage.headers().cvssScoreHeader().click();
    checkRawDataRow(firstRow, "aopalliance : aopalliance : 1.0", "Public Domain", "No Source License", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "org.springframework : spring-expression : 3.2.4.release", "Apache-2.0", null,
        "CVE-2018-1270", "9.8");
  }

  @Test
  public void testFiltering() {
    ResultTable resultTable = rawDataPage.resultTable();
    resultTable.resultRows().shouldHave(size(EXPECTED_ROWS_COUNT));

    // filter component name
    rawDataPage.headers().componentFilterInput().setValue("java");
    resultTable.resultRows().shouldHave(size(6));
    ResultRow firstRow = resultTable.resultRow(1);
    ResultRow lastRow = resultTable.resultRow(6);
    checkRawDataRow(firstRow, "java2html : j2h : 1.3.1", "Not Declared", "No Sources", "", "");
    ScrollUtil.scrollIntoView(lastRow.getElement());
    checkRawDataRow(lastRow, "javax.transaction : jta : 1.0.1b", "Not Declared", "No Sources", "", "");
    rawDataPage.headers().clearFilterField(rawDataPage.headers().componentFilterInput());

    // filter license
    rawDataPage.headers().licenseFilterInput().setValue("declared");
    resultTable.resultRows().shouldHave(size(11));
    firstRow = resultTable.resultRow(1);
    lastRow = resultTable.resultRow(11);
    checkRawDataRow(firstRow, "apache-collections : commons-collections : 3.1", "Not Declared", "No Sources",
        "sonatype-2015-0002", "9.0");
    checkRawDataRow(lastRow, "javax.transaction : jta : 1.0.1b", "Not Declared", "No Sources", "", "");
    rawDataPage.headers().clearFilterField(rawDataPage.headers().licenseFilterInput());

    // filter security issue
    rawDataPage.headers().securityCodeFilterInput().setValue("005");
    resultTable.resultRows().shouldHave(size(6));
    firstRow = resultTable.resultRow(1);
    lastRow = resultTable.resultRow(6);
    checkRawDataRow(firstRow, "angular 1.2.17", "MIT", "Not Supported", "sonatype-2014-0058", "3.6");
    checkRawDataRow(lastRow, "org.springframework : spring-web : 3.2.4.release", "Apache-2.0", null, "CVE-2014-0054",
        "6.8");

    // intersection of multiple filters
    rawDataPage.headers().licenseFilterInput().setValue("Apache");
    resultTable.resultRows().shouldHave(size(2));
    firstRow = resultTable.resultRow(1);
    lastRow = resultTable.resultRow(2);
    checkRawDataRow(firstRow, "commons-fileupload : commons-fileupload : 1.2.2", "Apache-2.0", null, "CVE-2014-0050",
        "7.5");
    checkRawDataRow(lastRow, "org.springframework : spring-web : 3.2.4.release", "Apache-2.0", null, "CVE-2014-0054",
        "6.8");
    rawDataPage.headers().clearFilterField(rawDataPage.headers().licenseFilterInput());
    rawDataPage.headers().clearFilterField(rawDataPage.headers().securityCodeFilterInput());

    // filter cvssScore
    rawDataPage.headers().cvssMinFilterInput().setValue("XX");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(empty);
    rawDataPage.headers().cvssMaxFilterInput().setValue("XX");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(empty);

    rawDataPage.headers().cvssMinFilterInput().setValue("0");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(value("0"));
    rawDataPage.headers().cvssMinFilterInput().val("1.2");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(value("1.2"));
    rawDataPage.headers().cvssMaxFilterInput().setValue("4.5");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(value("4.5"));
    rawDataPage.headers().cvssMinFilterInput().setValue("0.0");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(value("0.0"));
    rawDataPage.headers().cvssMaxFilterInput().setValue("10");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(value("10"));
    rawDataPage.headers().cvssMaxFilterInput().setValue("10");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(value("10"));
    rawDataPage.headers().cvssMinFilterInput().setValue("3.");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(value("3."));
    rawDataPage.headers().cvssMaxFilterInput().setValue("9.");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(value("9."));
    rawDataPage.headers().cvssMinFilterInput().setValue("23");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(value("2"));
    rawDataPage.headers().cvssMaxFilterInput().setValue("23");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(value("2"));
    rawDataPage.headers().cvssMinFilterInput().setValue("5X");
    rawDataPage.headers().cvssMinFilterInput().shouldBe(value("5"));
    rawDataPage.headers().cvssMaxFilterInput().setValue("1");
    rawDataPage.headers().cvssMaxFilterInput().shouldBe(value("1"));

    rawDataPage.headers().cvssMinFilterInput().setValue("9");
    rawDataPage.headers().cvssMaxFilterInput().setValue("9.5");

    resultTable.resultRows().shouldHave(size(2));
    firstRow = resultTable.resultRow(1);
    lastRow = resultTable.resultRow(2);
    checkRawDataRow(firstRow, "apache-collections : commons-collections : 3.1", "Not Declared", "No Sources",
        "sonatype-2015-0002", "9.0");
    checkRawDataRow(lastRow, " hsqldb : hsqldb : 1.8.0.7", "BSD-3-Clause", "BSD", "CVE-2007-4575", "9.3");
    rawDataPage.headers().securityCodeFilterInput().clear();

    // make sure no results row shows up if all results are filtered out
    rawDataPage.headers().licenseFilterInput().setValue("Garbage");
    resultTable.resultRows().shouldHave(size(1));
    rawDataPage.noResultsRow().shouldHave(exactText("No Data Available"));
  }

  private void checkRawDataRow(
      final ResultRow row,
      final String componentName,
      final String declaredLicenses,
      final String observedLicenses,
      final String securityIssue,
      final String cvssScore)
  {
    row.component().shouldHave(exactText(componentName));
    if (observedLicenses == null && declaredLicenses == null) {
      row.license().shouldBe(empty);
    }
    else {
      row.declaredLicenses().shouldHave(exactText(declaredLicenses));
      if (observedLicenses == null) {
        row.observedLicenses().shouldNot(exist);
      }
      else {
        row.observedLicenses().shouldHave(exactText(observedLicenses));
      }
    }

    row.securityIssue().shouldHave(exactText(securityIssue));
    row.cvssScore().shouldHave(exactText(cvssScore));
  }
}
