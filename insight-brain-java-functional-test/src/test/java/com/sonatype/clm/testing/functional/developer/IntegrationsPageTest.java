/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.developer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IntegrationsPage;
import com.sonatype.clm.testing.functional.pages.SastScanPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

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

public class IntegrationsPageTest extends AbstractFunctionalTest
{
  private static final int TOTAL_APPS_FOR_INTEGRATION_AND_RISKS = 20;

  private static final int TOTAL_APPS_PER_PAGE = 10;

  private static final String REPO_URL = "https://example.com/organization/project";

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  @Before
  public void before() {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.DASHBOARD);
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
    navigationTabs().shouldBe(visible);

    cicdTab().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlCiCd());
    ciCdSection().shouldBe(visible);

    scmTab().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlScm());
    scmSection().shouldBe(visible);

    issueTrackingTab().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlIssueTracking());
    issueTrackingSection().shouldBe(visible);

    ideTab().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlIde());
    ideSection().shouldBe(visible);

    overviewTab().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlOverview());
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
  public void testAppIntegrationsAndRiskTable() throws Exception {
    setUpAppsForIntegrationAndRisks();
    refreshOrOpen(IntegrationsPage.urlOverview());

    appIntegrationsAndRiskTable().shouldBe(visible);

    scrollIntoView(appIntegrationsAndRiskTable());
    appIntegrationsAndRiskTableDataRows().shouldHave(size(TOTAL_APPS_PER_PAGE));

    applicationName(0).shouldHave(text("appName10"));
    appIntegrationsCicdConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 2, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 5, 2023"));
    totalRisk(0).shouldHave(text("10"));
    sastReport(0).shouldHave(text("Not Available"));

    applicationName(9).shouldHave(text("appName1"));
    cicdEnabledIcon(9).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackEnabledIcon(9).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(9).shouldBe(visible).shouldHave(text("February 11, 2023"));
    lastEvaluationDate(9).shouldBe(visible).shouldHave(text("March 14, 2023"));
    totalRisk(9).shouldHave(text("1"));
    sastReport(9).shouldNotHave(text("Not Available"));
    sastReportViewLink(9).shouldBe(visible).shouldHave(text("View"));
    sastReport(9).shouldHave(text("a few seconds ago"));

    Selenide.sleep(1000);
    eyesWatcher.eyesCheck();

    sastReportViewLink(9).click();

    SastScanPage.title().shouldBe(visible);
    SastScanPage.triggeredOnDate().shouldBe(visible);
    SastScanPage.filterBySeverityDropdown().shouldBe(visible);
    SastScanPage.findingsTable().shouldBe(visible);
    back();

    // Click cicd configure button
    appIntegrationsCicdConfigureButton(0).click();
    appIntegrationsConfigurationModal().shouldBe(visible);
    appIntegrationsConfigurationModalCloseButton().shouldBe(visible).shouldBe(enabled).click();
    appIntegrationsConfigurationModal().shouldBe(hidden);

    // Click scm configure button
    appIntegrationsScmConfigureButton(0).click();
    appIntegrationsConfigurationModal().shouldBe(visible);
    appIntegrationsConfigurationModalCloseButton().shouldBe(visible).shouldBe(enabled).click();
    appIntegrationsConfigurationModal().shouldBe(hidden);

    // Sorting by total risk
    totalRiskColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    cicdEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));

    // Sorting by app name
    applicationColumnHeader().click();
    applicationName(0).shouldHave(text("appName9"));
    appIntegrationsCicdConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 3, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 6, 2023"));
    totalRisk(0).shouldHave(text("9"));

    totalRiskColumnHeader().click();

    // Sorting by last commit
    lastCommitColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    cicdEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));

    totalRiskColumnHeader().click();

    // Sorting by last evaluation
    lastEvaluationColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    cicdEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));

    // Searching for application
    applicationFilterInput().sendKeys("appName5");
    applicationName(0).shouldHave(text("appName5"));
    appIntegrationsCicdConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 7, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 10, 2023"));
    totalRisk(0).shouldHave(text("5"));
    appIntegrationsAndRiskTableDataRows().shouldHave(size(1));

    // Showing all rows
    applicationFilterInput().clear();
    applicationFilterInput().sendKeys("a");
    appIntegrationsAndRiskTableDataRows().shouldHave(size(10));

    // Going to the second page
    appIntegrationPageButton(2).click();
    appIntegrationsAndRiskTableDataRows().shouldHave(size(10));

    applicationName(0).shouldHave(text("appName10"));
    appIntegrationsCicdConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(0).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 2, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 5, 2023"));
    totalRisk(0).shouldHave(text("10"));

    applicationName(9).shouldHave(text("appName19"));
    appIntegrationsCicdConfigureButton(9).shouldHave(visible).shouldHave(text("Configure"));
    appIntegrationsScmConfigureButton(9).shouldHave(visible).shouldHave(text("Configure"));
    lastCommitDate(9).shouldBe(visible).shouldHave(text("January 24, 2023"));
    lastEvaluationDate(9).shouldBe(visible).shouldHave(text("February 24, 2023"));
    totalRisk(9).shouldHave(text("0"));

    // Testing name filter working on different page than first
    applicationFilterInput().clear();
    applicationFilterInput().sendKeys("appName0");
    appIntegrationsAndRiskTableDataRows().shouldHave(size(1));

    applicationName(0).shouldHave(text("appName0"));
    cicdEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackEnabledIcon(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));
  }

  @Test
  public void testAdoptionGraph() {
    setUpAppsForAdoptionGraph();
    refreshOrOpen(IntegrationsPage.urlOverview());

    scrollIntoView(adoptionGraph());

    adoptionGraph().shouldBe(visible);

    adoptionGraph().hover();

    developerDashboardGraphTooltip().shouldBe(visible);

    eyesWatcher.eyesCheck(null, false, false);
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
  public void testShowsFeatureDisabledMessageWhenFeatureIsNotInLicense() {
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    refreshOrOpen(IntegrationsPage.urlOverview());
    assertDisabled();

    refreshOrOpen(IntegrationsPage.urlCiCd());
    assertDisabled();

    refreshOrOpen(IntegrationsPage.urlScm());
    assertDisabled();

    refreshOrOpen(IntegrationsPage.urlIssueTracking());
    assertDisabled();

    refreshOrOpen(IntegrationsPage.urlIde());
    assertDisabled();
  }

  private void setUpAppsForIntegrationAndRisks() throws Exception {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null,
            (new DefaultPlexusCipher()).encrypt(ROOT_TOKEN, ENC),
            SourceControlProvider.GITHUB);

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

              if (i == 0 || i == 1) {
                tempEntity.newSourceControl(application.getId(), REPO_URL, null, null, null, null, false,
                        null, null, null, true,
                        true, "/target/*", true, true);
                tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-id-1",
                        false, false, false,
                        calendarForLastEval.getTime(), "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);
                tempEntity.newSastScan(application.getId());

              }

              calendarForLastEval.add(Calendar.DATE, -1);
              calendarForLastCommit.add(Calendar.DATE, -1);
            });
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

  private SelenideElement navigationTabs() {
    return $(".iq-integrations-content .nx-tab-list");
  }

  private SelenideElement overviewTab() {
    return $(".iq-integrations-content .nx-tab:nth-child(1)");
  }

  private SelenideElement cicdTab() {
    return $(".iq-integrations-content .nx-tab:nth-child(2)");
  }

  private SelenideElement scmTab() {
    return $(".iq-integrations-content .nx-tab:nth-child(3)");
  }

  private SelenideElement issueTrackingTab() {
    return $(".iq-integrations-content .nx-tab:nth-child(4)");
  }

  private SelenideElement ideTab() {
    return $(".iq-integrations-content .nx-tab:nth-child(5)");
  }

  private SelenideElement overviewSection() {
    return $("#iq-integrations-overview-section");
  }

  private SelenideElement ciCdSection() {
    return $("#iq-integrations-cicd-section");
  }

  private SelenideElement scmSection() {
    return $("#iq-integrations-scm-section");
  }

  private SelenideElement issueTrackingSection() {
    return $("#iq-integrations-issue-tracking-section");
  }

  private SelenideElement ideSection() {
    return $("#iq-integrations-ide-section");
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

  private SelenideElement totalRisk(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(6)");
  }

  private SelenideElement sastReport(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(7)");
  }

  private SelenideElement sastReportViewLink(int rowNum) {
    return sastReport(rowNum).$(".nx-text-link");
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

  private SelenideElement totalRiskColumnHeader() {
    return appIntegrationsAndRiskTable().$(".nx-cell--header:nth-child(6)");
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
