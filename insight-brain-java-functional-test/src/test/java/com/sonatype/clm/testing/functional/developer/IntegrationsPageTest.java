/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.developer;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IntegrationsPage;
import com.sonatype.clm.testing.functional.pages.PrioritiesPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.back;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class IntegrationsPageTest
    extends AbstractFunctionalTest
{
  private static final int TOTAL_APPS_FOR_INTEGRATION_AND_RISKS = 20;

  private static final int TOTAL_APPS_PER_PAGE = 10;

  private static final String REPO_URL = "https://example.com/organization/project";

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  @Before
  public void before() {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.DASHBOARD, LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.APPLICATION_REPORTS);
    refreshOrOpen(IntegrationsPage.urlOverview());
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
  }

  @Test
  public void testNavigation() {
    refreshOrOpen(IntegrationsPage.urlOverview());
    overviewSection().shouldBe(visible);
  }

  @Test
  public void testIdeUsersCount() {
    // Imitate one user that has an IDE integration
    tempEntity.newUserIdePolicyEvaluation("test_user");

    refreshOrOpen(IntegrationsPage.urlOverview());

    overviewSection().shouldBe(visible);

    ideUserCount().shouldBe(visible).shouldHave(text("1"));

    // Imitate another user that has an IDE integration
    tempEntity.newUserIdePolicyEvaluation("test_user_2");

    refresh();
    ideUserCount().shouldBe(visible).shouldHave(text("2"));

    scrollIntoView(ideUserCount());
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testAppIntegrationsAndRiskTable_ShouldRenderRowsCorrectly() throws Exception {
    setUpAppsForIntegrationAndRisks();
    // Evaluations set up in setUpAppsForIntegrationAndRisks() have the current date
    final String lastEvaluationDateString = new SimpleDateFormat("MMMM d, yyyy").format(new Date());
    final Calendar oldEvaluationDate = Calendar.getInstance();
    oldEvaluationDate.add(Calendar.DATE, -100);
    final String oldEvaluationDateString = new SimpleDateFormat("MMMM d, yyyy").format(oldEvaluationDate.getTime());

    refreshOrOpen(IntegrationsPage.urlOverview());

    appIntegrationsAndRiskTable().shouldBe(visible);

    scrollIntoView(appIntegrationsAndRiskTable());
    appIntegrationsAndRiskTableDataRows().shouldHave(size(TOTAL_APPS_PER_PAGE));

    applicationName(0).shouldHave(text("appName19"));
    appIntegrationsCicdConfigureButton(0).shouldNotBe(visible);
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("January 25, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text(lastEvaluationDateString));
    prioritiesReport(0).shouldNotHave(text("N/A"));
    prioritiesReportViewLink(0).shouldBe(visible).shouldHave(text("View"));

    applicationName(7).shouldHave(text("appName12"));
    appIntegrationsCicdConfigureButton(7).shouldNotBe(visible);
    appIntegrationsScmConfigureButton(7).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(7).shouldBe(visible).shouldHave(text("February 1, 2023"));
    lastEvaluationDate(7).shouldBe(visible).shouldHave(text(lastEvaluationDateString));
    prioritiesReport(7).shouldNotHave(text("N/A"));
    prioritiesReportViewLink(7).shouldBe(visible).shouldHave(text("View"));

    Selenide.sleep(1000);
    // eyesWatcher.eyesCheck(); https://sonatype.atlassian.net/browse/CLM-30559

    prioritiesReportViewLink(9).click();

    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldBe(visible);
    prioritiesPage.summaryTile().shouldBe(visible);
    prioritiesPage.prioritiesTable().shouldBe(visible);
    back();

    // Showing all rows
    applicationFilterInput().clear();
    applicationFilterInput().sendKeys("a");
    Selenide.sleep(1000);
    appIntegrationsAndRiskTableDataRows().shouldHave(size(10));

    // Going to the second page
    appIntegrationPageButton(2).click();
    appIntegrationsAndRiskTableDataRows().shouldHave(size(10));

    applicationName(0).shouldHave(text("appName8"));
    appIntegrationsCicdConfigureButton(0).shouldNotBe(visible);
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 5, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text(lastEvaluationDateString));
    prioritiesReport(0).shouldNotHave(text("N/A"));
    prioritiesReportViewLink(0).shouldBe(visible).shouldHave(text("View"));

    applicationName(8).shouldHave(text("appName10"));
    appIntegrationsCicdConfigureButton(8).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(8).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(8).shouldBe(visible).shouldHave(text("February 3, 2023"));
    lastEvaluationDate(8).shouldBe(visible).shouldHave(text(oldEvaluationDateString));
    prioritiesReport(8).shouldNotHave(text("N/A"));
    prioritiesReportViewLink(8).shouldBe(visible).shouldHave(text("View"));

    applicationName(9).shouldHave(text("appName0"));
    appIntegrationsCicdConfigureButton(9).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(9).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(9).shouldBe(visible).shouldHave(text("None"));
    lastEvaluationDate(9).shouldBe(visible).shouldHave(text("None"));
    prioritiesReport(9).shouldHave(text("N/A"));
  }

  @Test
  public void testAppIntegrationsAndRiskTable_shouldCorrectlyShowCiCdAsConfiguredWhenThereIsAQualifyingEval() {
    final Date anyDateInThePastButLessThan3Months = new Date(System.currentTimeMillis() - 1000);

    final Application appWithQualifyingEval = tempEntity.newApplicationWithParent("app1", "app1");
    tempEntity.newPolicyEvaluation(
        appWithQualifyingEval.getId(),
        Stage.ID_BUILD,
        "scan-id-1",
        false,
        false,
        false,
        anyDateInThePastButLessThan3Months,
        "hash-1",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    refreshOrOpen(IntegrationsPage.urlOverview());

    appIntegrationsAndRiskTable().shouldBe(visible);
    scrollIntoView(appIntegrationsAndRiskTable());

    applicationName(0).shouldHave(text("app1"));
    cicdEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    appIntegrationsCicdConfigureButton(0).shouldNotBe(visible);
  }

  @Test
  public void testAppIntegrationsAndRiskTable_shouldCorrectlyShowCiCdAsNotConfiguredWhenThereIsNoQualifyingEval() {
    // there have not been any evals so this is not configured for cicd
    tempEntity.newApplicationWithParent("app1", "app1");

    refreshOrOpen(IntegrationsPage.urlOverview());

    appIntegrationsAndRiskTable().shouldBe(visible);
    scrollIntoView(appIntegrationsAndRiskTable());

    applicationName(0).shouldHave(text("app1"));
    appIntegrationsCicdConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));

    // Click cicd configure button
    appIntegrationsCicdConfigureButton(0).click();
    appIntegrationsConfigurationModal().shouldBe(visible);
    appIntegrationsConfigurationModalCloseButton().shouldBe(visible).shouldBe(enabled).click();
    appIntegrationsConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testAppIntegrationsAndRiskTable_shouldCorrectlyShowScmAsConfiguredWhenConfiguredFully() throws PlexusCipherException {
    final Application applicationWithScmConfigured = tempEntity.newApplicationWithParent("app1", "app1");

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null,
        (new DefaultPlexusCipher()).encrypt(ROOT_TOKEN, ENC),
        SourceControlProvider.GITHUB);

    tempEntity.newSourceControlDefaultBranchCommitHistory(applicationWithScmConfigured.getId(),
        "commit1", new Date(), null);
    tempEntity.newSourceControl(
        applicationWithScmConfigured.getId(),
        REPO_URL,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        true,
        true,
        "/target/*",
        true,
        true,
        false,
        false);

    refreshOrOpen(IntegrationsPage.urlOverview());

    appIntegrationsAndRiskTable().shouldBe(visible);
    scrollIntoView(appIntegrationsAndRiskTable());

    applicationName(0).shouldHave(text("app1"));
    scmFeedbackEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    appIntegrationsScmConfigureButton(0).shouldNotBe(visible);
  }

  @Test
  public void testAppIntegrationsAndRiskTable_shouldCorrectlyShowScmAsNotConfiguredWhenNotConfiguredFully() throws PlexusCipherException {
    final Application applicationWithScmConfigured = tempEntity.newApplicationWithParent("app1", "app1");

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null,
        (new DefaultPlexusCipher()).encrypt(ROOT_TOKEN, ENC),
        SourceControlProvider.GITHUB);

    tempEntity.newSourceControlDefaultBranchCommitHistory(applicationWithScmConfigured.getId(),
        "commit1", new Date(), null);

    refreshOrOpen(IntegrationsPage.urlOverview());

    appIntegrationsAndRiskTable().shouldBe(visible);
    scrollIntoView(appIntegrationsAndRiskTable());

    applicationName(0).shouldHave(text("app1"));
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));

    // Click scm configure button
    appIntegrationsScmConfigureButton(0).click();
    appIntegrationsConfigurationModal().shouldBe(visible);
    appIntegrationsConfigurationModalCloseButton().shouldBe(visible).shouldBe(enabled).click();
    appIntegrationsConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testAppIntegrationsAndRiskTable_shouldSortFilterAndSearchCorrectly() {
    setUpAppsForSorting();
    refreshOrOpen(IntegrationsPage.urlOverview());
    scrollIntoView(appIntegrationsAndRiskTable());

    // Sorting by app name
    applicationColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));

    // Sorting by last commit
    lastCommitColumnHeader().click();
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));

    // Sorting by last evaluation
    lastEvaluationColumnHeader().click();
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("February 24, 2023"));

    // Searching for application
    applicationFilterInput().sendKeys("appName5");
    applicationName(0).shouldHave(text("appName5"));
    appIntegrationsAndRiskTableDataRows().shouldHave(size(1));

    // Testing name filter working on different page than first
    applicationFilterInput().clear();
    applicationFilterInput().sendKeys("appName0");
    appIntegrationsAndRiskTableDataRows().shouldHave(size(1));
    applicationName(0).shouldHave(text("appName0"));
  }

  @Test
  public void testAdoptionGraph() {
    setUpAppsForAdoptionGraph();
    refreshOrOpen(IntegrationsPage.urlOverview());

    scrollIntoView(adoptionGraph());

    adoptionGraph().shouldBe(visible);

    adoptionGraph().hover();

    developerDashboardGraphTooltip().shouldBe(visible);

    // eyesWatcher.eyesCheck(null, false, false); https://sonatype.atlassian.net/browse/CLM-30559
  }

  @Test
  public void testRiskRemediationGraph() {
    setUpAppsForRiskRemediationGraph();
    refreshOrOpen(IntegrationsPage.urlOverview());

    scrollIntoView(riskRemediationGraph());

    riskRemediationGraph().shouldBe(visible);

    riskRemediationGraph().hover();

    developerDashboardGraphTooltip().shouldBe(visible);

    eyesWatcher.eyesCheck(null, false, false);
  }

  @Test
  public void testMTTRGraph() {
    setUpAppsForMTTRGraph();
    refreshOrOpen(IntegrationsPage.urlOverview());

    scrollIntoView(mttrGraph());

    mttrGraph().shouldBe(visible);

    mttrGraph().hover();

    developerDashboardGraphTooltip().shouldBe(visible);

    eyesWatcher.eyesCheck(null, false, false);
  }

  @Test
  public void testShowsFeatureDisabledMessageWhenDeveloperFeatureIsNotInLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    refreshOrOpen(IntegrationsPage.urlOverview());
    assertDisabled();
  }

  private void setUpAppsForSorting() {
    Calendar calendarForLastEval = Calendar.getInstance();
    calendarForLastEval.set(2023, Calendar.MARCH, 15);

    Calendar calendarForLastCommit = Calendar.getInstance();
    calendarForLastCommit.set(2023, Calendar.FEBRUARY, 12);

    IntStream.range(0, TOTAL_APPS_FOR_INTEGRATION_AND_RISKS)
        .forEach(i -> {
          final Application application = tempEntity.newApplicationWithParent("appId" + i, "appName" + i);
          final Policy policy = tempEntity.newPolicy(application);
          policy.setThreatLevel(i);
          final PolicyEvaluation policyEvaluation =
              tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-" + i,
                  calendarForLastEval.getTime());
          tempEntity.newPolicyViolation(policyEvaluation, policy);

          tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(),
              "commit1", calendarForLastCommit.getTime(), null);

          calendarForLastEval.add(Calendar.DATE, -1);
          calendarForLastCommit.add(Calendar.DATE, -1);
        });
  }

  private void setUpAppsForIntegrationAndRisks() {
    Calendar calendarForLastCommit = Calendar.getInstance();
    calendarForLastCommit.set(2023, Calendar.FEBRUARY, 12);

    // Set the date for an eval older than 3 months to spawn the CI/CD configure button
    Calendar calendarForOldEval = Calendar.getInstance();
    calendarForOldEval.add(Calendar.DATE, -100);

    for (int i = 0; i < TOTAL_APPS_FOR_INTEGRATION_AND_RISKS; i++) {
      final Application application = tempEntity.newApplicationWithParent("appId" + i, "appName" + i);

      // Skip evaluation of the first app
      if (i == 0) {
        continue;
      }

      // Evaluate all but app10 at the build stage
      final String stageId = i == 10 ? DevelopStageType.ID : BuildStageType.ID;

      // Evaluate an app and create a report zip for the priorities page to use
      evaluate(application, i, stageId);

      // Set total risk at the build stage
      final PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-" + i,
              calendarForOldEval.getTime());
      final Policy policy = tempEntity.newPolicy(application);
      policy.setThreatLevel(i);
      tempEntity.newPolicyViolation(policyEvaluation, policy);

      // Create commit
      tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(),
          "commit1", calendarForLastCommit.getTime(), null);
      calendarForLastCommit.add(Calendar.DATE, -1);
    }
  }

  private void evaluate(final Application application, final int scanNum, final String stageId) {
    final URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    final TestReportEvaluator evaluator =
        new TestReportEvaluator(application, "scan-" + scanNum, zippedReport, baseUrlFromTest, work, stageId);
    try {
      evaluator.evaluatePolicy();
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void setUpAppsForAdoptionGraph() {
    Calendar calendarForLastEval = Calendar.getInstance();
    Date now = new Date();
    IntStream.range(0, 10).forEach(i -> {
      Date xWeekAgo = Date.from(Instant.now().minus(i * 7, ChronoUnit.DAYS));

      tempEntity.newApplicationCountHistoryEntry(xWeekAgo, 100, 100 - (i * 10), 0, 0, 0);
    });

    tempEntity.newApplicationCountHistoryEntry(now, 100, 100, 0, 0, 0);

    IntStream.range(0, 100)
        .forEach(i -> {
          final Application application = tempEntity.newApplicationWithParent("appId" + i, "appName" + i);
          final Policy policy = tempEntity.newPolicy(application);
          policy.setThreatLevel(i);
          final PolicyEvaluation policyEvaluation =
              tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-" + i,
                  calendarForLastEval.getTime());
          tempEntity.newPolicyViolation(policyEvaluation, policy);

          if (i % 2 == 0) {
            tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-id-1",
                false, false, false,
                calendarForLastEval.getTime(), "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);
          }

          calendarForLastEval.add(Calendar.DATE, -1);
        });
  }

  private void setUpAppsForRiskRemediationGraph() {
    Date now = new Date();
    IntStream.range(0, 10).forEach(i -> {
      Date xWeekAgo = Date.from(Instant.now().minus(i * 7, ChronoUnit.DAYS));

      tempEntity.newApplicationCountHistoryEntry(xWeekAgo, 100, 0, 100 - (i * 10), 50 - (i * 5), 0);
    });

    tempEntity.newApplicationCountHistoryEntry(now, 100, 0, 10, 5, 0);
  }

  private static long daysToMilliseconds(double days) {
    // 1 day is equal to 24 hours, 60 minutes, 60 seconds, and 1000 milliseconds
    long millisecondsInOneDay = 24 * 60 * 60 * 1000;

    // Calculate the number of milliseconds
    long milliseconds = (long) (days * millisecondsInOneDay);

    return milliseconds;
  }

  private void setUpAppsForMTTRGraph() {
    Date now = new Date();
    IntStream.range(0, 10).forEach(i -> {
      Date xWeekAgo = Date.from(Instant.now().minus(i * 7, ChronoUnit.DAYS));

      tempEntity.newApplicationCountHistoryEntry(xWeekAgo, 100, 0, 0, 0, daysToMilliseconds(100 - (i * 10)));
    });

    tempEntity.newApplicationCountHistoryEntry(now, 100, 0, 0, 0, daysToMilliseconds(5));
  }

  private SelenideElement overviewSection() {
    return $("#iq-integrations-overview-section");
  }

  private SelenideElement ideUserCount() {
    return overviewSection().$(".iq-integrations-card-callout--count");
  }

  private SelenideElement applicationName(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(1)");
  }

  private SelenideElement cicdEnabledIcon(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(2) svg");
  }

  private SelenideElement scmFeedbackEnabledIcon(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(3) svg");
  }

  private SelenideElement appIntegrationsCicdConfigureButton(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(2) button");
  }

  private SelenideElement appIntegrationsScmConfigureButton(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(3) button");
  }

  private SelenideElement appIntegrationsConfigurationModalCloseButton() {
    return $(".iq-integrations-developer-configuration-close-button");
  }

  private SelenideElement appIntegrationsConfigurationModal() {
    return $("#iq-integrations-developer-configuration-modal");
  }

  private SelenideElement lastCommitDate(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(4)");
  }

  private SelenideElement lastEvaluationDate(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(5)");
  }

  private SelenideElement prioritiesReport(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(6)");
  }

  private SelenideElement prioritiesReportViewLink(int rowNum) {
    return prioritiesReport(rowNum).$(".nx-text-link");
  }

  private SelenideElement applicationFilterInput() {
    return appIntegrationsAndRiskTable().$(".nx-text-input__input");
  }

  private SelenideElement applicationColumnHeader() {
    return appIntegrationsAndRiskTable().$(".nx-cell--header:nth-child(1)");
  }

  private SelenideElement lastCommitColumnHeader() {
    return appIntegrationsAndRiskTable().$(".nx-cell--header:nth-child(4)");
  }

  private SelenideElement lastEvaluationColumnHeader() {
    return appIntegrationsAndRiskTable().$(".nx-cell--header:nth-child(5)");
  }

  private SelenideElement licenseFeatureMissingAlert() {
    return $("[data-testid='iq-integrations__missing-license']");
  }

  private SelenideElement appIntegrationsAndRiskTable() {
    return $("#iq-developer-app-integrations-and-risk-table");
  }

  private SelenideElement appIntegrationPageButton(int page) {
    return appIntegrationsAndRiskTable().$(String.format(".nx-btn--pagination:nth-child(%d)", page));
  }

  private ElementsCollection appIntegrationsAndRiskTableDataRows() {
    return appIntegrationsAndRiskTable().findAll(" tbody .nx-table-row");
  }

  private SelenideElement adoptionGraph() {
    return $(".iq-developer-dashboard-adoption-graph");
  }

  private SelenideElement riskRemediationGraph() {
    return $(".iq-developer-dashboard-risk-remediation-graph");
  }

  private SelenideElement mttrGraph() {
    return $(".iq-developer-dashboard-mttr-graph");
  }

  private SelenideElement developerDashboardGraphTooltip() {
    return $(".iq-developer-dashboard-graph-tooltip");
  }

  private void assertDisabled() {
    licenseFeatureMissingAlert().shouldBe(visible);
  }
}
