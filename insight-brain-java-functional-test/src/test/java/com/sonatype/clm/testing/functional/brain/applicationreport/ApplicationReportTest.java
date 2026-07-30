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
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQCoverageIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQLegacyViolationsIndicator;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer;
import com.sonatype.insight.model.HasStringId;

import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;

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
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndScanId(app.getId(), SCAN_ID);
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
    policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndScanId(app.getId(), SCAN_ID);
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
    // With jOOQ, entities are plain POJOs and don't need to be detached from any persistence context.
    // Just reset the ID to allow re-insertion.
    entity.setId(null);
  }

  @Test
  public void testAggregation() {
    // Aggregate by Component toggle
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
