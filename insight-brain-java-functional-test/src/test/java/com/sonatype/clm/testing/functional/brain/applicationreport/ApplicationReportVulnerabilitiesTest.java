/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportVulnerabilitiesPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportVulnerabilitiesPage.VulnerabilityRow;
import com.sonatype.clm.testing.functional.pages.ApplicationReportVulnerabilitiesPage.VulnerabilityTable;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportVulnerabilitiesTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  public static final String EMPTY_RESULTS_SCAN_ID = "e16caf35769f4b3186a7e416d34c2798";

  private final ApplicationReportPage applicationReportPage = new ApplicationReportPage();

  private final ApplicationReportVulnerabilitiesPage vulnerabilitiesPage = new ApplicationReportVulnerabilitiesPage();

  private Application app;

  private Organization org;

  private final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  private LegacyViolationService legacyViolationService;

  @Before
  public void starts() throws IOException {
    policyDAO = lookup(PolicyDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

    legacyViolationService = testCLMServer.getCLMServer().getInstance(LegacyViolationService.class);
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportVulnerabilitiesTest", "ApplicationReportVulnerabilitiesTest",
        org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);

    Policy securityLow = policyDAO.getByName("Security-Low").get(0);
    Policy securityHigh = policyDAO.getByName("Security-High").get(0);

    // grant legacy status to the security-low policy
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());

    // waive a few violations, one of which is also in legacy status
    tempEntity.newWaiver("1e48256a2341047e7d72", securityLow.getId(), app.getId()); // commons-fileupload
    tempEntity.newWaiver("edde2efe828f5890f57a", securityHigh.getId(), app.getId()); // spring-expression

    evaluator.evaluatePolicy();

    setupWebDriver();
    refreshOrOpen(ApplicationReportVulnerabilitiesPage.url(app, SCAN_ID));
    loginAsAdmin();
  }

  @After
  public void after() {
    hardreset();
  }

  @Test
  public void testHeader() {
    String expectedDate = DateTime.now().toString("yyyy-MM-dd");
    String expectedTitle = "Vulnerabilities for " + app.getName() + " Build Report";

    vulnerabilitiesPage.shouldBe(visible);
    vulnerabilitiesPage.title().shouldHave(text(expectedTitle));
    vulnerabilitiesPage.subtitle().shouldHave(text(expectedDate));
    NxBackButton backButton = vulnerabilitiesPage.backButton();
    backButton.shouldHave(text("Back to Application Report"));

    backButton.click();
    applicationReportPage.should(appear);
  }

  @Test
  public void testResults() {
    VulnerabilityTable vulnerabilityTable = vulnerabilitiesPage.table();
    vulnerabilityTable.shouldBe(visible);

    vulnerabilityTable.rows().shouldHave(size(59));

    VulnerabilityRow jacksonDatabindRow = vulnerabilityTable.row(2);
    checkRow(jacksonDatabindRow, "com.fasterxml.jackson.core : jackson-databind : 2.0.4", "CVE-2017-7525", "9.8", "9",
        false, false);

    eyesWatcher.eyesCheck("Test Raw Data View");

    VulnerabilityRow angularRow = vulnerabilityTable.row(59);
    ScrollUtil.scrollIntoView(angularRow.getElement());
    checkRow(angularRow, "angular 1.2.17", "sonatype-2014-0059", "3.6", "0", false, true);

    VulnerabilityRow waivedRow = vulnerabilityTable.row(20);
    ScrollUtil.scrollIntoView(waivedRow.getElement());
    checkRow(waivedRow, "org.springframework : spring-expression : 3.2.4.RELEASE", "CVE-2018-1270", "9.8", "0", true,
        false);

    VulnerabilityRow waivedLegacyRow = vulnerabilityTable.row(21);
    ScrollUtil.scrollIntoView(waivedLegacyRow.getElement());
    checkRow(waivedLegacyRow, "commons-fileupload : commons-fileupload : 1.2.2", "CVE-2013-0248", "3.3", "0", true,
        true);
  }

  @Test
  public void testEmptyResults() throws Exception {
    Application emptyResultsApp = tempEntity.newApplication("ApplicationReportVulnerabilitiesTest-testEmptyResults",
        "ApplicationReportVulnerabilitiesTest-testEmptyResults", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/empty-report", tempDir);
    TestReportEvaluator evaluator = new TestReportEvaluator(emptyResultsApp, EMPTY_RESULTS_SCAN_ID, zippedReport,
        baseUrlFromTest, work);

    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportVulnerabilitiesPage.url(emptyResultsApp, EMPTY_RESULTS_SCAN_ID));

    VulnerabilityTable vulnerabilityTable = vulnerabilitiesPage.table();
    vulnerabilityTable.shouldBe(visible);

    vulnerabilityTable.rows().shouldHave(size(1));
    vulnerabilityTable.row(1).shouldHave(text("no vulnerabilities"));
  }

  // NOTE This test does not pass in headless mode (e.g. with -Dselenide.headless=true)
  @Test
  public void testSecurityIssueLink() {
    final String refId = "CVE-2016-1000031";
    String detailsUrl = VulnerabilitySearchPage.url(refId);

    VulnerabilityTable vulnerabilityTable = vulnerabilitiesPage.table();
    VulnerabilityRow vulnerabilityRow = vulnerabilityTable.row(1);
    vulnerabilityRow.detailsLink().shouldHave(attribute("href", detailsUrl));
    vulnerabilityRow.detailsLink().shouldHave(text(refId)).click();

    waitUntilUrl(detailsUrl);
  }

  private void checkRow(
      final VulnerabilityRow row,
      final String componentName,
      final String securityIssue,
      final String cvssScore,
      final String policyThreatLevel,
      final boolean waived,
      final boolean legacy)
  {
    row.component().shouldHave(text(componentName));
    row.securityIssue().shouldHave(exactText(securityIssue));
    row.cvssScore().shouldHave(exactText(cvssScore));
    row.policyThreatLevel().shouldHave(exactText(policyThreatLevel));

    if (waived) {
      row.waived().shouldBe(visible);
    }
    else {
      row.waived().shouldNotBe(visible);
    }

    if (legacy) {
      row.legacyViolationGranted().shouldBe(visible);
    }
    else {
      row.legacyViolationGranted().shouldNotBe(visible);
    }
  }
}
