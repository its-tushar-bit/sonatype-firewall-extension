/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.reports.LicenseCIP;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.AuditLogContent;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.utils.WaiverApplierForReport;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.matchText;
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

    Organization org = tempEntity.newOrganization("Test Organization");
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

      waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, HASH));
      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      componentDetailsPage.header().title().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
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
  public void testComponentDetailsHeaderAndFooter() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement directDependencyWithViolation = violations.get(4);
    directDependencyWithViolation.click();

    final String directDependencyHash = "f0776db1593e215146d2";
    waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, directDependencyHash));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    SelenideElement title = componentDetailsPage.header().title();
    title.shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    // Not comparing exact texts due to dynamic information (organization uuid, rpeort date)
    ElementsCollection reportInformationElements = componentDetailsPage.header().reportInformationElements();
    reportInformationElements.shouldHave(texts("Test Organization", "ApplicationReportTest", "Build Report "));

    ElementsCollection tags = componentDetailsPage.header().tags();
    tags.shouldHave(texts("maven", "Direct Dependency"));

    componentDetailsPage.footer().paginationCounter().shouldHave(text("5 of 64"));

    eyesWatcher.eyesCheck("component details header and footer");
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

    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

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

  @Test
  public void testPolicyViolationsTab_violationTableEntries() {
    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(5);
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')", "Unapplied Waiver"));
    eyesWatcher.eyesCheck("component details violations tab violation table unapplied waiver");

    // Reevaluate to apply the waiver and apply appropriate filter to show in the report
    componentDetailsPage.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    reportPage.filterToggle().click();
    reportPage.filterPanel().violationStateFilter().twisty().click();
    reportPage.filterPanel().violationStateFilter().waived().click();
    reportPage.filterPanel().closeButton().click();
    componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(5);
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')", "1 Active Waiver"));
    eyesWatcher.eyesCheck("component details violations tab violation table active waiver");

    testGrandfatheringIndicator(componentDetailsPage);
  }

  /* Part of testPolicyViolationsTab_violationTableEntries. */
  private void testGrandfatheringIndicator(final ComponentDetailsPage componentDetailsPage) {
    // Configure grandfathering indicator for the first violation in the report and reload it
    componentDetailsPage.backButton().click();
    activateGrandfathering();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    reportPage.aggregateByComponentToggle().click();
    SelenideElement firstGrandfatheredViolation = reportPage.resultRows().first();
    firstGrandfatheredViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToRemediation(app, SCAN_ID, HASH));

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.getRows().shouldHaveSize(1);
    SelenideElement indicatorsCell = policyViolationsTable.getRows().first().findAll(By.tagName("td")).last();
    indicatorsCell.shouldHave(text("Grandfathered"));
    eyesWatcher.eyesCheck("component details violations tab violation table grandfathered row");
  }

  private void activateGrandfathering() {
    Policy licenseBannedPolicy = new PolicyDAO().getByName("License-Banned").get(0);

    app.setPolicyViolationGrandfatheringEnabled(true);
    licenseBannedPolicy.setPolicyViolationGrandfatheringAllowed(true);
    new ApplicationDAO().update(app);
    new PolicyDAO().update(licenseBannedPolicy);
    PolicyViolationGrandfatheringService policyViolationGrandfatheringService =
        testCLMServer.getCLMServer().getInstance(PolicyViolationGrandfatheringService.class);
    policyViolationGrandfatheringService.grandfather(app.getPublicId());
    try {
      evaluator.reevaluatePolicy();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAuditLogTab_emptyMessage() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    auditLog.emptyMessage().shouldHave(text("No changes were found for this component"));
  }

  @Test
  public void testAuditLogTab_entries() {
    String dateRegex = "\\w{3} \\d{1,2}, \\d{4} \\d{1,2}:\\d{2}:\\d{2} (am|pm)";

    createAuditLogEntries();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    SelenideElement date = auditLog.dateFromRow(0);
    date.should(matchText(dateRegex));
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
  }

  @Test
  public void testAuditLogTab_sort() {
    createAuditLogEntries();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    // Sorted by time, descending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));

    ElementsCollection headers = auditLog.tableHeaders();
    headers.get(0).click();
    // Sorted by time, ascending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));

    headers.get(2).click();
    // Sorted by action, ascending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));

    headers.get(2).click();
    // Sorted by action, descending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));

    headers.get(4).click();
    // Sorted by comment, ascending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));

    headers.get(4).click();
    // Sorted by comment, descending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
  }

  private void createAuditLogEntries() {
    // Using the CIP to create log entries.
    // Would need to change this to the form in the Component Details Page once the license tab is implemented.
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    mockHdsResponseForFirstComponent();
    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(5).click();

    //Move some licenses' status so we can have some entries in audit log
    LicenseCIP.status().selectOption("Acknowledged");
    LicenseCIP.comment().setValue("AAAA");
    LicenseCIP.updateButton().shouldBe(enabled).click();
    // Navigate away and back
    LicenseCIP.status().selectOption("Open");
    LicenseCIP.comment().setValue("BBBB");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    cipModal.closeButton().click();
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private ComponentDetailsPage openComponentDetailsPageForFirstViolation() {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToRemediation(app, SCAN_ID, HASH));
    return new ComponentDetailsPage();
  }

  private void navigateToComponentDetailsPageViolationsTab(final ComponentDetailsPage componentDetailsPage) {
    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, HASH));
    componentDetailsPage.violationsTabContent().shouldBe(visible);
  }

  private void waiveFirstReportRow() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    WaiverApplierForReport.waiveReportRow(reportPage, 0);
  }
}
