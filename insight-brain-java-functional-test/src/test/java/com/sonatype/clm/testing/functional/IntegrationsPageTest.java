/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional;

import java.util.Calendar;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.pages.IntegrationsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class IntegrationsPageTest extends AbstractFunctionalTest
{
  private static final int TOTAL_APPS_FOR_INTEGRATION_AND_RISKS = 10;

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

    scrollIntoView(appIntegrationsAndRiskTable());

    appIntegrationsAndRiskTable().shouldBe(visible);

    appIntegrationsAndRiskTableDataRows().shouldHaveSize(TOTAL_APPS_FOR_INTEGRATION_AND_RISKS);

    applicationName(0).shouldHave(text("appName9"));
    cicdStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-disabled"));
    scmFeedbackStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-disabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 3, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 6, 2023"));
    totalRisk(0).shouldHave(text("9"));

    applicationName(9).shouldHave(text("appName0"));
    cicdStatus(9).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackStatus(9).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(9).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(9).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(9).shouldHave(text("0"));

    eyesWatcher.eyesCheck();

    //Sorting by total risk
    totalRiskColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    cicdStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));

    //Sorting by app name
    applicationColumnHeader().click();
    applicationName(0).shouldHave(text("appName9"));
    cicdStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-disabled"));
    scmFeedbackStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-disabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 3, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 6, 2023"));
    totalRisk(0).shouldHave(text("9"));

    totalRiskColumnHeader().click();

    //Sorting by last commit
    lastCommitColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    cicdStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));

    totalRiskColumnHeader().click();

    //Sorting by last evaluation
    lastEvaluationColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    cicdStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    scmFeedbackStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-enabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 12, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 15, 2023"));
    totalRisk(0).shouldHave(text("0"));

    //Searching for application
    applicationFilterInput().sendKeys("appName5");
    applicationName(0).shouldHave(text("appName5"));
    cicdStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-disabled"));
    scmFeedbackStatus(0).shouldBe(visible).shouldHave(cssClass("iq-integrations-and-risk-disabled"));
    lastCommitDate(0).shouldBe(visible).shouldHave(text("February 7, 2023"));
    lastEvaluationDate(0).shouldBe(visible).shouldHave(text("March 10, 2023"));
    totalRisk(0).shouldHave(text("5"));
    appIntegrationsAndRiskTableDataRows().shouldHaveSize(1);
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
              }

              calendarForLastEval.add(Calendar.DATE, -1);
              calendarForLastCommit.add(Calendar.DATE, -1);
            });
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

  private SelenideElement cicdStatus(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(2) svg");
  }

  private SelenideElement scmFeedbackStatus(int rowNum) {
    return appIntegrationsAndRiskTableDataRows().get(rowNum).$(".nx-cell:nth-child(3) svg");
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

  private ElementsCollection appIntegrationsAndRiskTableDataRows() {
    return appIntegrationsAndRiskTable().findAll(" tbody .nx-table-row");
  }

  private void assertDisabled() {
    licenseFeatureMissingAlert().shouldBe(visible);
  }
}
