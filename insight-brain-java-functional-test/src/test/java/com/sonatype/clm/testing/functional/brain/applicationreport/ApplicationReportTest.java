/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.ViolationStateFilter;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxDropdown;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationLatestEvaluationsPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.AppReportHeaders;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQCoverageIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQLegacyViolationsIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.ResultRow;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportVulnerabilitiesPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.utils.WaiverApplierForReport;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockServer;
import com.sonatype.insight.model.HasStringId;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.files.FileFilters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.DIRECT_DEPENDENCY_CLASS;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.INNER_SOURCE_DEPENDENCY_CLASS;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.TRANSITIVE_DEPENDENCY_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final int EXPECTED_TOTAL_ROWS_COUNT = 103;

  private static final int EXPECTED_VIOLATIONS_COUNT = 65;

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private LegacyViolationService legacyViolationService;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    applicationDAO = lookup(ApplicationDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);

    legacyViolationService = testCLMServer.getCLMServer().getInstance(LegacyViolationService.class);
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @After
  public void afterEachTestEnds() {
    if (reportPage.filterPanel().getElement().is(visible)) {
      reportPage.filterPanel().closeButton().click();
    }
  }

  @Test
  public void testSummary() throws Exception {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    Date policyEvaluationTime = policyEvaluation.getTime();

    String policyEvaluationTimeStr = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z")
        .print(policyEvaluationTime.getTime());
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    reportPage.policyTypeFilterWarning().shouldNot(exist);

    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
    reportPage.reportApplicationRiskScore().shouldBe(visible);
    reportPage.threatIndicators().critical().shouldHave(text("22"));
    reportPage.threatIndicators().severe().shouldHave(text("39"));
    reportPage.threatIndicators().moderate().shouldHave(text("4"));
    reportPage.threatIndicators().caption().shouldHave(exactText("65 Violations"));
    reportPage.threatIndicators().subCaption().shouldHave(exactText("Affecting 27 components"));

    IQCoverageIndicator coverageIndicator = reportPage.coverageIndicator();
    coverageIndicator.caption().shouldHave(exactText("64 COMPONENTS"));
    coverageIndicator.subCaption().shouldHave(exactText("98% of all components identified"));
    coverageIndicator.donutChart().shouldBe(visible);

    IQLegacyViolationsIndicator legacyViolationsIndicator = reportPage.legacyViolationsIndicator();
    legacyViolationsIndicator.caption().shouldHave(exactText("0 LEGACY VIOLATIONS"));

    activateLegacyViolations();

    legacyViolationsIndicator = reportPage.legacyViolationsIndicator();
    legacyViolationsIndicator.caption().shouldHave(exactText("46 LEGACY VIOLATIONS"));

    // The activateLegacyViolations above re-evals and refreshes the page
    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    policyEvaluationTime = policyEvaluation.getTime();
    policyEvaluationTimeStr =
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z").print(policyEvaluationTime.getTime());
    refresh();
    reportPage.shouldBe(visible);
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    // Update the policy evaluation to look like it was for monitoring
    policyEvaluation = updateForMonitoring(policyEvaluation);
    policyEvaluationTime = policyEvaluation.getTime();
    policyEvaluationTimeStr =
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z").print(policyEvaluationTime.getTime());
    refresh();
    reportPage.shouldBe(visible);
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));
  }

  @Test
  public void testSummaryWithDeveloperDashboardEnabled() throws Exception {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.POLICY_GRANDFATHERING,
        LicensedFeature.APPLICATION_REPORTS, LicensedFeature.SUCCESS_METRICS, LicensedFeature.APPLICATION_EVALUATION);
    refresh();

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    Date policyEvaluationTime = policyEvaluation.getTime();

    String policyEvaluationTimeStr = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z")
        .print(policyEvaluationTime.getTime());
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    reportPage.policyTypeFilterWarning().shouldNot(exist);

    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
    reportPage.reportApplicationRiskScore().shouldBe(visible);
    reportPage.threatIndicators().critical().shouldHave(text("22"));
    reportPage.threatIndicators().severe().shouldHave(text("39"));
    reportPage.threatIndicators().moderate().shouldHave(text("4"));
    reportPage.threatIndicators().caption().shouldHave(exactText("65 Violations"));
    reportPage.threatIndicators().subCaption().shouldHave(exactText("Affecting 27 components"));

    eyesWatcher.eyesCheck();

    IQCoverageIndicator coverageIndicator = reportPage.coverageIndicator();
    coverageIndicator.caption().shouldHave(exactText("64 COMPONENTS"));
    coverageIndicator.subCaption().shouldHave(exactText("98% of all components identified"));
    coverageIndicator.donutChart().shouldBe(visible);

    IQLegacyViolationsIndicator legacyViolationsIndicator = reportPage.legacyViolationsIndicator();
    legacyViolationsIndicator.caption().shouldHave(exactText("0 LEGACY VIOLATIONS"));

    activateLegacyViolations();

    legacyViolationsIndicator = reportPage.legacyViolationsIndicator();
    legacyViolationsIndicator.caption().shouldHave(exactText("46 LEGACY VIOLATIONS"));

    // The activateLegacyViolations above re-evals and refreshes the page
    policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    policyEvaluationTime = policyEvaluation.getTime();
    policyEvaluationTimeStr =
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z").print(policyEvaluationTime.getTime());
    refresh();
    reportPage.shouldBe(visible);
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    // Update the policy evaluation to look like it was for monitoring
    policyEvaluation = updateForMonitoring(policyEvaluation);
    policyEvaluationTime = policyEvaluation.getTime();
    policyEvaluationTimeStr =
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z").print(policyEvaluationTime.getTime());
    refresh();
    reportPage.shouldBe(visible);
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));
  }

  private PolicyEvaluation updateForMonitoring(PolicyEvaluation policyEvaluation) {
    policyEvaluation.setForMonitoring(true);
    policyEvaluation.setTime(new Date(policyEvaluation.getTime().getTime() + 1000));
    detachEntity(policyEvaluation);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  private <E extends HasStringId> void detachEntity(E entity) {
    PersistenceCapable pc = (PersistenceCapable) entity;
    pc.pcSetDetachedState(null);
    pc.pcReplaceStateManager(null);
    entity.setId(null);
  }

  @Test
  public void testOptionsMenu() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown.menu().shouldBe(visible).entries().shouldHave(size(7));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testDownloadPdf() throws Exception {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();

    SelenideElement selenideElement = optionsDropdown
        .menu()
        .shouldBe(visible)
        .entries()
        .shouldHave(size(7))
        .first()
        .shouldHave(text("Export PDF"));

    // A file filter is necessary here to ensure Selenide does not try to download the wrong file.
    // In particular, without the filter, it may try to download a ".com.google.Chrome.XXXXXX" type file.
    // This is a cache/partial download file and is deleted by Chrome when the download completes.
    // If Selenide targets this file it can cause a "WebDriverException: Cannot find file", which will cause it
    // to retry the download, but at this point the link has already been clicked and the menu is closed,
    // so this will further cause a "ElementNotFound" error since it can't click the link again.
    File downloadedPdf = selenideElement.download(Duration.ofSeconds(20).toMillis(), FileFilters.withExtension("pdf"));

    byte[] fileBeginning = new byte[4];
    try (FileInputStream stream = new FileInputStream(downloadedPdf)) {
      stream.read(fileBeginning);
    }

    // detect PDF magic number ("%PDF")
    assertThat(fileBeginning).isEqualTo(new byte[]{0x25, 0x50, 0x44, 0x46});
  }

  @Test
  public void testViewSbom() throws Exception {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();

    File downloadedSbom = optionsDropdown.menu().entries().get(1).shouldHave(text("Export CycloneDX")).download();

    byte[] fileBeginning = new byte[5];
    try (FileInputStream stream = new FileInputStream(downloadedSbom)) {
      stream.read(fileBeginning);
    }

    // similar to the PDF test, the content of the file is checked to see if it's XML
    assertThat(new String(fileBeginning)).isEqualTo("<?xml");
  }

  @Test
  public void testLatestEvaluationsLink() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(4).shouldHave(text("View Latest Evaluations")).click();

    waitUntilUrl(ApplicationLatestEvaluationsPage.url(app, Stage.ID_BUILD));
    new ApplicationLatestEvaluationsPage().title().shouldHave(text(app.getName() + " Latest Evaluations"));
  }

  @Test
  public void testRawDataLink() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(5).shouldHave(text("View raw data")).click();

    waitUntilUrl(ApplicationReportRawDataPage.url(app, SCAN_ID));
    new ApplicationReportRawDataPage().reportTitle().shouldHave(text(app.getName()));
  }

  @Test
  public void testVulnerabilitiesLink() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(6).shouldHave(text("View vulnerabilities")).click();

    waitUntilUrl(ApplicationReportVulnerabilitiesPage.url(app, SCAN_ID));
    new ApplicationReportVulnerabilitiesPage().title().shouldHave(text(app.getName()));
  }

  @Test
  public void testTextIndicators() throws Exception {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    Policy licenseBanned = policyDAO.getByName("License-Banned").get(0);
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    reportPage.resultRows().shouldHave(size(2));
    reportPage.resultRow(1).waiverIndicator().shouldBe(hidden);
    reportPage.resultRow(2).waiverIndicator().shouldBe(hidden);

    PolicyWaiver waiver = tempEntity.newWaiver(licenseBanned.getId(), app.getId());
    evaluator.reevaluatePolicy();
    refresh();

    // test that indicators are shown when aggregating
    InputUtils.clearInput(reportPage.headers().policyNameFilterInput());
    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(visible);
    reportPage.resultRow(1).waiverIndicator().shouldHave(text("1 Waived Violation"));
    reportPage.resultRow(1).legacyViolationIndicator().shouldNotBe(visible);
    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(visible);
    reportPage.resultRow(1).waiverIndicator().shouldHave(text("1 Waived Violation"));
    reportPage.resultRow(1).legacyViolationIndicator().shouldNotBe(visible);

    // test that indicators are shown when not aggregating
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.resultRows().shouldHave(size(2));
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(2).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).legacyViolationIndicator().shouldNotBe(visible);
    reportPage.resultRow(2).legacyViolationIndicator().shouldNotBe(visible);

    activateLegacyViolations();

    // now the legacy violation indicator should appear
    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(visible);
    reportPage.resultRow(1).waiverIndicator().shouldHave(text("1 Waived Violation"));
    reportPage.resultRow(1).legacyViolationIndicator().shouldBe(visible);

    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(visible);
    reportPage.resultRow(1).waiverIndicator().shouldHave(text("1 Waived Violation"));
    reportPage.resultRow(1).legacyViolationIndicator().shouldBe(visible);

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.resultRows().shouldHave(size(2));
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).legacyViolationIndicator().shouldBe(visible);

    eyesWatcher.eyesCheck();

    // a test to catch CLM-12064. When aggregating, the policy name for these rows should change back to None
    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    InputUtils.clearInput(reportPage.headers().policyNameFilterInput());
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).policyName().shouldHave(text(licenseBanned.getName()));
    reportPage.resultRow(1).threatNumber().shouldHave(text("10"));
    reportPage.aggregateByComponentToggle().shouldBeOff().click();
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).policyName().shouldHave(text("None"));
    reportPage.resultRow(1).threatNumber().shouldHave(text("0"));

    policyWaiverDAO.delete(waiver);
    evaluator.reevaluatePolicy();
    refresh();

    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).legacyViolationIndicator().shouldBe(visible);

    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).legacyViolationIndicator().shouldBe(visible);

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.resultRows().shouldHave(size(2));
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).legacyViolationIndicator().shouldBe(visible);
  }

  @Test
  public void testEllipsisInPolicyName() {
    reportPage.headers().policyNameFilterInput().click();
    reportPage.headers().policyNameFilterInput().setValue("License-Threat");
    SelenideElement policyName = reportPage.getColFromResultRow(1, 2);
    policyName.lastChild().shouldHave(cssValue("text-overflow", "ellipsis"));
    policyName.hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("License-Threat Not Assigned"));

    // We need to ensure by visual tests that the ellipsis is shown, there is no way
    // to see if the ellipsis is applied by looking to the HTML, attributes or innerText,
    // the text look the same with or without ellipsis to selenium/selenide accessors.
    eyesWatcher.eyesCheck("Showing ellipsis in overflown policy names.", false, false);
  }

  @Test
  public void testEllipsisInComponentName() {
    reportPage.headers().componentNameFilterInput().click();
    reportPage.headers().componentNameFilterInput()
        .setValue("org.springframework.security : spring-security-config : 3.2.4.RELEASE");
    reportPage.getColFromResultRow(1, 3).shouldHave(cssValue("text-overflow", "ellipsis"));

    // We need to ensure by visual tests that the ellipsis is shown, there is no way
    // to see if the ellipsis is applied by looking to the HTML, attributes or innerText,
    // the text look the same with or without ellipsis to selenium/selenide accessors.
    eyesWatcher.eyesCheck("Showing ellipsis in overflown component names");
  }

  @Test
  public void testInnerSourceTransitiveViolationsCount() {
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.resultRow(16).shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"))
        .transitiveViolationsCount().shouldHave(size(1))
        .get(0).shouldHave(text("2 transitive violations"));
    reportPage.aggregateByComponentToggle().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.resultRow(21).shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"))
        .transitiveViolationsCount().shouldHave(size(0));
  }

  @Test
  public void testDependencyIndicators() {
    reportPage.rowsWithDependencyInfo().shouldHave(size(6));
    reportPage.resultRow(5).shouldHave(text("apache-httpclient : commons-httpclient : 3.1"))
        .dependencyIndicators().shouldHave(size(1)).get(0).shouldHave(DIRECT_DEPENDENCY_CLASS);
    ResultRow resultRow = reportPage.resultRow(6).shouldHave(text("apache-taglibs : standard : 1.1.2"));
    ElementsCollection dependencyIndicators = resultRow.dependencyIndicators().shouldHave(size(2));
    dependencyIndicators.get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS).shouldHave(text("T"));
    dependencyIndicators.get(1).shouldHave(INNER_SOURCE_DEPENDENCY_CLASS).shouldHave(text("IS")).hover();
    Tooltip.get().shouldBe(visible)
        .shouldHave(text("This component was brought in by the following InnerSource component:"))
        .shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"));
    dependencyIndicators = reportPage.resultRow(16)
        .shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"))
        .dependencyIndicators().shouldHave(size(2));
    dependencyIndicators.get(0).shouldHave(DIRECT_DEPENDENCY_CLASS).shouldHave(text("D")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Direct Dependency"));
    dependencyIndicators.get(1).shouldHave(INNER_SOURCE_DEPENDENCY_CLASS).shouldHave(text("IS")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("InnerSource"));
    reportPage.resultRow(26).shouldHave(text("org.springframework : spring-core : 3.2.8.RELEASE"))
        .dependencyIndicators().shouldHave(size(1)).get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS);
    reportPage.resultRow(58).shouldHave(text("org.springframework : spring-aop : 3.2.8.RELEASE"))
        .dependencyIndicators().shouldHave(size(1)).get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS);
    reportPage.resultRow(59).shouldHave(text("org.springframework : spring-beans : 3.2.4.RELEASE"))
        .dependencyIndicators().shouldHave(size(1)).get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS);
  }

  @Test
  public void testAggregation() {
    // Aggregate by Component toggle
    reportPage.aggregateByComponentToggle().label().shouldHave(text("Aggregate by component"));
    reportPage.aggregateByComponentToggle().label().hover();
    NxTooltip aggregateByComponentToggleToolTip = new NxTooltip();
    aggregateByComponentToggleToolTip.getElement()
        .shouldHave(text("By default the Application Report aggregates violations by component. " +
            "To see all violations not Aggregated by Component, please switch the toggle off."));

    int expectedNoneThreatLevelResults = 37;

    // By default the "Aggregate by Component" toggle should be ON
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.resultRows().shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    reportPage.getThreatBars("critical").shouldHave(size(17));
    reportPage.getThreatBars("severe").shouldHave(size(9));
    reportPage.getThreatBars("moderate").shouldHave(size(1));
    reportPage.getThreatBars("low").shouldHave(size(1));
    reportPage.getThreatBars("none").shouldHave(size(expectedNoneThreatLevelResults));
    reportPage.headers().componentNameFilterInput().setValue("commons-fileupload");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.getThreatBars("critical").shouldHave(size(1));

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();

    reportPage.resultRows().shouldHave(size(6));
    reportPage.getThreatBars("critical").shouldHave(size(4));
    reportPage.getThreatBars("severe").shouldHave(size(1));
    reportPage.getThreatBars("moderate").shouldHave(size(1));

    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.resultRows().shouldHave(size(EXPECTED_TOTAL_ROWS_COUNT));
    reportPage.getThreatBars("critical").shouldHave(size(22));
    reportPage.getThreatBars("severe").shouldHave(size(39));
    reportPage.getThreatBars("moderate").shouldHave(size(4));
    reportPage.getThreatBars("low").shouldHave(size(1));
    reportPage.getThreatBars("none").shouldHave(size(expectedNoneThreatLevelResults));
  }

  @Test
  public void testAggregationMultipleWaivers() {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    // By default the "Aggregate by Component" toggle should be ON
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.headers().componentNameFilterInput().setValue("commons-fileupload");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(hidden);
    Policy securityHigh = policyDAO.getByName("Security-High").get(0);
    //
    tempEntity.newWaiver(securityHigh.getId(), app.getId());

    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
    FormMask.seeAndWaitForDismissal();

    reportPage.headers().componentNameFilterInput().shouldHave(value("commons-fileupload"));
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(visible);
    reportPage.resultRow(1).waiverIndicator().shouldHave(text("4 Waived Violations"));
    eyesWatcher.eyesCheck("Multiple waived violations in aggregated view");
  }

  @Test
  public void testSorting() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();
    // reduce the result set so we don't need to scroll around
    headers.componentNameFilterInput().setValue("com.");
    violations.shouldHave(size(8));

    // by threat level
    headers.threatHeader().sortArrows().shouldBeDown();
    violations.shouldHave(texts("10", "10", "9", "9", "0", "0", "0", "0"));
    // check that entries have also been sorted by component name
    checkSecondarySortByNameDescending(violations);
    // reverse threat level
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeUp();
    violations.shouldHave(texts("0", "0", "0", "0", "9", "9", "10", "10"));
    // the secondary sort should remain unchanged
    checkSecondarySortByNameDescending(violations);

    // by policy name
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrows().shouldBeUp();
    violations.shouldHave(
        texts("License-Banned", "License-Banned", "None", "None", "None", "None", "Security-High", "Security-High"));
    checkSecondarySortByNameDescending(violations);
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrows().shouldBeDown();
    violations.shouldHave(
        texts("Security-High", "Security-High", "None", "None", "None", "None", "License-Banned", "License-Banned"));
    checkSecondarySortByNameDescending(violations);

    // by component name
    reportPage.aggregateByComponentToggle().shouldBeOn().click(); // un-aggregate in order to check secondary sort
    reportPage.aggregateByComponentToggle().shouldBeOff();
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrows().shouldBeUp();
    String[] componentNamesAlpha = {
        "com.adobe.acrobat", "com.adobe.pdf", "com.fasterxml.jackson.core : jackson-annotations",
        "com.fasterxml.jackson.core : jackson-core", "com.fasterxml.jackson.core : jackson-core",
        "com.fasterxml.jackson.core : jackson-databind", "com.mycila", "com.palominolabs.metrics",
        "com.vaadin.addon"
    };
    violations.shouldHave(texts(componentNamesAlpha));
    // secondary sort by threat level descending
    violations.filterBy(text("jackson-core")).shouldHave(texts("9", "7"));
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrows().shouldBeDown();
    ArrayUtils.reverse(componentNamesAlpha);
    violations.shouldHave(texts(componentNamesAlpha));
    // secondary sort should remain unchanged
    violations.filterBy(text("jackson-core")).shouldHave(texts("9", "7"));
  }

  @Test
  public void testFilteringNotPersisting() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();

    headers.policyNameFilterInput().setValue("unk");
    violations.shouldHave(size(1));
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    SidebarNavigation.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();

    headers.policyNameFilterInput().shouldBe(empty);
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));

    headers.componentNameFilterInput().setValue("Reg");
    violations.shouldHave(size(1));
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    SidebarNavigation.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();

    headers.componentNameFilterInput().shouldBe(empty);
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
  }

  @Test
  public void testFiltering_Scroll() {
    reportPage.filterToggle().click();

    reportPage.filterPanel().proprietaryFilter().twisty().click();
    reportPage.filterPanel().matchStateFilter().twisty().click();
    reportPage.filterPanel().violationStateFilter().twisty().click();
    reportPage.filterPanel().dependencyTypeFilter().twisty().click();

    PolicyTypeFilter policyTypeFilter = reportPage.filterPanel().policyTypeFilter();
    policyTypeFilter.twisty().click();
    ScrollUtil.scrollIntoView(policyTypeFilter.twisty());
    policyTypeFilter.other().shouldBe(visible);
  }

  @Test
  public void testFiltering_violationState() throws Exception {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    ElementsCollection violations = reportPage.resultRows();
    reportPage.filterToggle().click();

    ViolationStateFilter violationStateFilter = reportPage.filterPanel().violationStateFilter();
    violationStateFilter.counter().shouldHave(exactText("4"));
    violationStateFilter.twisty().click();
    violationStateFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    violationStateFilter.open().click();
    violationStateFilter.open().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("1 of 4"));
    violations.shouldHave(size(28));
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    WaiverApplierForReport.waiveReportRow(reportPage, 0);

    reportPage.shouldBe(visible);
    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
    FormMask.seeAndWaitForDismissal();
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));

    reportPage.filterToggle().click();
    violationStateFilter = reportPage.filterPanel().violationStateFilter();
    violationStateFilter.twisty().click();
    violationStateFilter.multiSelectList().shouldHave(size(5));
    violationStateFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    violationStateFilter.open().click();
    violationStateFilter.open().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("1 of 4"));
    // waived violation filtered out
    violations.shouldHave(size(27));

    // now add waived violations
    violationStateFilter.waived().click();
    violationStateFilter.waived().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("2 of 4"));
    violations.shouldHave(size(28));

    // at this point, the mycila violation is visible but is way down at the "None" part of the list because we are
    // in the aggregated view
    violations.first().shouldHave(text("com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1"));

    // switch to non-aggregated view to get the actual waived violation,
    // back in its original place at the top of the list
    reportPage.filterPanel().closeButton().click();
    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    violations.shouldHave(size(66));
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    activateLegacyViolations();

    // activateLegacyViolations refreshes the page so we need to put the filter back how we had it
    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.filterToggle().click();
    violationStateFilter.twisty().click();
    violationStateFilter.open().click();
    violationStateFilter.waived().click();

    // legacy violations not visible
    violations.shouldHave(size(21));

    // legacy violations now visible
    violationStateFilter.legacyViolations().click();
    violationStateFilter.legacyViolations().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHave(size(66));

    // the waived violation also has legacy violation status, so no difference.
    violationStateFilter.waived().click();
    violations.shouldHave(size(66));

    violationStateFilter.notViolating().click();
    violationStateFilter.notViolating().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHave(size(EXPECTED_TOTAL_ROWS_COUNT));

    // all boxes checked - again no difference in count because the waived violation also has legacy violation status
    violationStateFilter.waived().click();
    violationStateFilter.counter().shouldHave(exactText("4 of 4"));
    violations.shouldHave(size(EXPECTED_TOTAL_ROWS_COUNT));

    // no boxes checked
    violationStateFilter.allItems().shouldBe(selected).click();
    violationStateFilter.allItems().shouldNotBe(selected);
    violationStateFilter.notViolating().shouldNotBe(selected);
    violationStateFilter.open().shouldNotBe(selected);
    violationStateFilter.waived().shouldNotBe(selected);
    violationStateFilter.legacyViolations().shouldNotBe(selected);
    violationStateFilter.counter().shouldHave(exactText("4"));
    violations.shouldHave(size(EXPECTED_TOTAL_ROWS_COUNT));

    reportPage.filterPanel().closeButton().click();
  }

  @Test
  public void testReevaluate() {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    Policy licenseBanned = policyDAO.getByName("License-Banned").get(0);
    tempEntity.newWaiver(licenseBanned.getId(), app.getId());

    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldNotBe(visible);

    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
    reportPage.seeReevaluationStatusModalAndWaitForDismissal();

    reportPage.headers().componentNameFilterInput().shouldHave(value("mycila"));
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).waiverIndicator().shouldBe(visible);
    reportPage.resultRow(1).waiverIndicator().shouldHave(text("1 Waived Violation"));
  }

  @Test
  public void testFilterReset() {
    AppReportHeaders headers = reportPage.headers();

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrows().shouldBeUp();

    reportPage.filterToggle().click();
    reportPage.filterPanel().proprietaryFilter().twisty().click();
    reportPage.filterPanel().proprietaryFilter().nonProprietary().click();
    reportPage.filterPanel().proprietaryFilter().nonProprietary().shouldBe(selected);

    // navigate elsewhere and then back to this report, without triggering a full refresh
    SidebarNavigation.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();

    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.aggregateByComponentToggle().shouldBeOn();
    headers.policyNameHeader().sortArrows().shouldNotBeUp();
    reportPage.filterToggle().click();
    reportPage.filterPanel().proprietaryFilter().twisty().click();
    reportPage.filterPanel().proprietaryFilter().nonProprietary().shouldNotBe(selected);
    reportPage.filterPanel().closeButton();
  }

  @Test
  public void testEmbeddable() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID) + "?embeddable");

    // test that the header is not present but that the data and sidebar are
    MainHeader.get().shouldNot(exist);
    reportPage.resultRows().shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    reportPage.aggregateByComponentToggle().shouldBeOn();
  }

  @Test
  public void testPolicyTypeFilterDisabledInV3Report() throws IOException {
    // Setup
    final String SCAN_ID2 = "e16caf35769f4b3186a7e3476d34c2798";
    Application app2 = tempEntity.newApplicationWithParent();
    URL zippedReport = ReportHelper.zipReport("/canned-reports/evaluated-v3-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app2.getId(), SCAN_ID2);
    FileUtils.copyURLToFile(zippedReport, reportDestination);
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, SCAN_ID2);
    refreshOrOpen(ApplicationReportPage.url(app2, SCAN_ID2));
    refresh(); // Extra refresh as a workaround until CLM-34495 is fixed.

    // Assertions
    reportPage.filterToggle().click();
    PolicyTypeFilter policyTypeFilter = reportPage.filterPanel().policyTypeFilter();
    ElementsCollection violations = reportPage.resultRows();

    reportPage.policyTypeFilterWarning().shouldBe(visible);
    policyTypeFilter.counter().shouldHave(exactText("4"));
    policyTypeFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    violations.shouldHave(size(63));
    policyTypeFilter.hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Reevaluate the report in order to enable Policy Types filter"));

    policyTypeFilter.multiSelectList().shouldHave(size(0));
    policyTypeFilter.twisty().shouldBe(disabled);

    violations.shouldHave(size(63));

    reportPage.filterPanel().closeButton().click();
  }

  @Test
  public void testViewVulnerabilitiesOptionDisabledInV4Report() throws IOException {
    // Setup
    final String SCAN_ID2 = "e16caf35769f4b3186a7e3476d34c2798";
    Application app2 = tempEntity.newApplicationWithParent();
    URL zippedReport = ReportHelper.zipReport("/canned-reports/evaluated-v4-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app2.getId(), SCAN_ID2);
    FileUtils.copyURLToFile(zippedReport, reportDestination);
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, SCAN_ID2);
    refreshOrOpen(ApplicationReportPage.url(app2, SCAN_ID2));
    refresh(); // Extra refresh as a workaround until CLM-34495 is fixed.

    reportPage.shouldBe(visible);
    // Assertions
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(6).shouldHave(DISABLED).click();
    // should remain on report page
    reportPage.shouldBe(visible);
  }

  @Test
  public void testUnscannedComponentsModal() throws IOException {
    // Setup
    final String SCAN_ID2 = "e16caf35769f4b3186a7e3476d34c2798";
    Application app2 = tempEntity.newApplicationWithParent(
        "UnscannedComponentsAppReportTest", "UnscannedComponentsAppReportTest"
    );
    URL zippedReport = ReportHelper.zipReport("/canned-reports/evaluated-v4-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app2.getId(), SCAN_ID2);
    FileUtils.copyURLToFile(zippedReport, reportDestination);
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, SCAN_ID2);
    refreshOrOpen(ApplicationReportPage.url(app2, SCAN_ID2));
    refresh(); // Extra refresh as a workaround until CLM-34495 is fixed.

    reportPage.shouldBe(visible);
    reportPage.viewUnscannedComponentsButton().shouldBe(visible);

    eyesWatcher.eyesCheck();

    reportPage.viewUnscannedComponentsButton().click();
    reportPage.unscannedComponentsModal().shouldBe(visible);
    reportPage.closeUnscannedComponentsModalButton().shouldBe(visible);

    eyesWatcher.eyesCheck();

    reportPage.closeUnscannedComponentsModalButton().click();
    reportPage.unscannedComponentsModal().shouldNotBe(visible);
  }

  @Test
  public void testBackNavigation() {
    // Test fully reopened page
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.backButton().click();
    waitUntilUrl(ReportListPage.url());

    /// Test when navigating from all reports page
    ReportListPage.firstRow().shouldBe(visible);
    ReportListPage.firstRow().buildReportLink().click();
    reportPage.backButton().click();
    waitUntilUrl(ReportListPage.url());
  }

  private void checkSecondarySortByNameDescending(final ElementsCollection violations) {
    violations.filterBy(text("License-Banned")).shouldHave(texts("com.mycila", "com.vaadin"));
    violations.filterBy(text("Security-High")).shouldHave(
        texts("com.fasterxml.jackson.core : jackson-core : 2.0.4",
            "com.fasterxml.jackson.core : jackson-databind : 2.0.4"));
    violations.filterBy(text("None"))
        .shouldHave(texts("com.adobe.acrobat", "com.adobe.pdf", "com.fasterxml", "com.palominolabs"));
  }

  private void activateLegacyViolations() throws Exception {
    Policy licenseBanned = policyDAO.getByName("License-Banned").get(0);

    app.setLegacyViolationEnabled(true);
    licenseBanned.setLegacyViolationAllowed(true);
    applicationDAO.update(app);
    policyDAO.update(licenseBanned);
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
    evaluator.reevaluatePolicy();
    refresh();
  }
}
