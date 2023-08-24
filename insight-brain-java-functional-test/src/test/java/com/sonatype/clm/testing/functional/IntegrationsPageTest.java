/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

import com.sonatype.clm.testing.functional.pages.IntegrationsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class IntegrationsPageTest extends AbstractFunctionalTest
{
  private static final String DONUT_TEST_ID = "iq-integrations-cicard__donut";

  private static final String CI_USAGE_APP_TABLE_TEST_ID = "iq-integrations-apps-without-recent-usage-preview";

  private static final String CI_USAGE_PERCENT_SELECTOR = ".iq-integrations-cicard__donut-col";

  private static final int TOTAL_APPS_WITHOUT_CI_INTEGRATIONS = 10;

  private static final String REPO_URL = "https://example.com/organization/project";

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  @Before
  public void before() {
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
    sideNavigation().shouldBe(visible);

    sideCiCdLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlCiCd());
    ciCdSection().shouldBe(visible);

    sideScmLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlScm());
    scmSection().shouldBe(visible);

    sideIssueTrackingLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlIssueTracking());
    issueTrackingSection().shouldBe(visible);

    sideIdeLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlIde());
    ideSection().shouldBe(visible);
  }

  @Test
  public void testCiUsageIsShown() {
    final Organization givenOrg = tempEntity.newOrganization("Parent Org");

    final String appNameWithCiUsage = "App With Ci Scan";
    final String appNameWithoutCiUsage = "App With No Ci Scan";

    givenAppWithEvalFromCi(appNameWithCiUsage, "app-for-ci-scan", givenOrg);
    givenAppWithoutEvalFromCi(appNameWithoutCiUsage, "app-with-no-ci-scan", givenOrg);

    refreshOrOpen(IntegrationsPage.urlOverview());

    ciUsageDonut().shouldBe(visible);
    ciUsagePercentMessage().shouldHave(text("50% of your apps are not integrated with CI"));
    ciUsageAppTable().shouldBe(visible);
    ciUsageAppTable().shouldHave(text(appNameWithoutCiUsage));
    ciUsageAppTable().shouldNotHave(text(appNameWithCiUsage));

    viewAllAppsButton().shouldNotBe(visible);

    Arrays.asList("app1", "app2", "app3", "app4", "app5", "app6")
        .forEach(newApp -> givenAppWithoutEvalFromCi(newApp, newApp, givenOrg));

    refreshOrOpen(IntegrationsPage.urlOverview());
    viewAllAppsButton().shouldBe(visible).click();

    appsWithoutCiIntegrationsTable().shouldBe(visible);
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
  public void testAppsWithoutCiIntegrationsPage() {
    createAppsWithPolicyViolations(TOTAL_APPS_WITHOUT_CI_INTEGRATIONS);

    refreshOrOpen(IntegrationsPage.urlAppsWithoutCiIntegrations());

    appsWithoutCiIntegrationsTable().shouldBe(visible);

    appsWithoutCiIntegrationsTableDataRows().shouldHaveSize(TOTAL_APPS_WITHOUT_CI_INTEGRATIONS);

    applicationName(0).shouldHave(text("appName9"));
    totalRisk(0).shouldHave(text("9"));

    eyesWatcher.eyesCheck();

    //Sorting by total risk
    totalRiskColumnHeader().click();
    applicationName(0).shouldHave(text("appName0"));
    totalRisk(0).shouldHave(text("0"));

    //Sorting by app name
    applicationColumnHeader().click();
    applicationName(0).shouldHave(text("appName9"));
    totalRisk(0).shouldHave(text("9"));

    //Searching for application
    applicationFilterInput().sendKeys("appName5");
    applicationName(0).shouldHave(text("appName5"));
    totalRisk(0).shouldHave(text("5"));
    appsWithoutCiIntegrationsTableDataRows().shouldHaveSize(1);

    //Clicking back button
    backButton().shouldHave(text("Back to Overview")).click();
    appsWithoutCiIntegrationsTable().shouldNotBe(visible);

    waitUntilUrl(IntegrationsPage.urlOverview());
    overviewSection().shouldBe(visible);
  }

  @Test
  public void testAppsWithoutScmIntegrationsPage() throws Exception {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null,
            (new DefaultPlexusCipher()).encrypt(ROOT_TOKEN, ENC),
            SourceControlProvider.GITHUB);

    final Application configuredApp = tempEntity.newApplicationWithParent("app1", "Configured App");
    final Application unconfiguredApp = tempEntity.newApplicationWithParent("app2", "Unconfigured App");

    // Add a source control record for configuredApp with ASCF enabled, so it shouldn't be in the result list
    // unconfiguredApp has no source control record, so it should be in the result list
    tempEntity.newSourceControl(configuredApp.getId(), REPO_URL, null, null, null, null, false,
            null, null, null, true, true, "/target/*", true, true);

    refreshOrOpen(IntegrationsPage.urlOverview());

    appsWithoutScmIntegrationsTable().shouldBe(visible);

    appsWithoutScmIntegrationsTableDataRows().shouldHaveSize(1);

    applicationNameWithNoScm(0).shouldHave(text(unconfiguredApp.getName()));

    eyesWatcher.eyesCheck();
  }

  private void createAppsWithPolicyViolations(int numOfViolations) {
    IntStream.range(0, numOfViolations)
        .forEach(i -> {
          final Application application = tempEntity.newApplicationWithParent("appId" + i, "appName" + i);
          final Policy policy = tempEntity.newPolicy(application);
          policy.setThreatLevel(i);
          final PolicyEvaluation policyEvaluation =
                  tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-" + i,
                          new Date(System.currentTimeMillis() - 2000));
          tempEntity.newPolicyViolation(policyEvaluation, policy);
        });
  }

  private SelenideElement sideNavigation() {
    return $("#integrations-sidebar");
  }

  private SelenideElement sideCiCdLink() {
    return $("#integrations-sidebar__cicd-link");
  }

  private SelenideElement sideScmLink() {
    return $("#integrations-sidebar__scm-link");
  }

  private SelenideElement sideIssueTrackingLink() {
    return $("#integrations-sidebar__issue-tracking-link");
  }

  private SelenideElement sideIdeLink() {
    return $("#integrations-sidebar__ide-link");
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

  private SelenideElement ciUsageDonut() {
    return $(String.format("[data-testid='%s']", DONUT_TEST_ID));
  }

  private SelenideElement ciUsagePercentMessage() {
    return $(CI_USAGE_PERCENT_SELECTOR);
  }

  private SelenideElement ideUserCount() {
    return overviewSection().$(".nx-card__call-out");
  }

  private SelenideElement ciUsageAppTable() {
    return $(String.format("[data-testid='%s'", CI_USAGE_APP_TABLE_TEST_ID));
  }

  private SelenideElement viewAllAppsButton() {
    return overviewSection().$(".nx-btn");
  }

  private SelenideElement backButton() {
    return $("#menu-bar__back-button-container");
  }

  private void givenAppWithEvalFromCi(
      final String appName,
      final String publicId,
      final Organization organization)
  {
    final Application givenApp = tempEntity.newApplication(appName, publicId, organization.getId());

    final PolicyEvaluation givenEval = new PolicyEvaluation(
        givenApp.getId(),
        "random-stage-type-id",
        "random-scan-id",
        false,
        false,
        "random-initiator",
        ScanTriggerType.CONTINUOUS_INTEGRATION,
        ClientScanType.SONATYPE
    );
    givenEval.setForObsoleteScan(false);
    givenEval.setTime(Date.from(Instant.now()));

    tempEntity.insertPolicyEvaluation(givenEval);
  }

  private void givenAppWithoutEvalFromCi(
      final String appName,
      final String publicId,
      final Organization organization)
  {
    final Application givenApp = tempEntity.newApplication(appName, publicId, organization.getId());

    final PolicyEvaluation givenEval = new PolicyEvaluation(
        givenApp.getId(),
        "random-stage-type-id-not-ci",
        "random-scan-id-not-ci",
        false,
        true,
        "random-initiator",
        ScanTriggerType.SOURCE_CONTROL_API,
        ClientScanType.SONATYPE
    );
    givenEval.setTime(Date.from(Instant.now()));

    tempEntity.insertPolicyEvaluation(givenEval);
  }

  private SelenideElement appsWithoutCiIntegrationsTable() {
    return $("#iq-integrations-apps-without-ci-integrations-section-table");
  }

  private SelenideElement applicationName(int rowNum) {
    return appsWithoutCiIntegrationsTableDataRows().get(rowNum).$(".nx-cell:nth-child(1)");
  }

  private SelenideElement totalRisk(int rowNum) {
    return appsWithoutCiIntegrationsTableDataRows().get(rowNum).$(".nx-cell:nth-child(2)");
  }

  private ElementsCollection appsWithoutCiIntegrationsTableDataRows() {
    return appsWithoutCiIntegrationsTable().findAll(" tbody .nx-table-row");
  }

  private SelenideElement applicationFilterInput() {
    return appsWithoutCiIntegrationsTable().$(".nx-text-input__input");
  }

  private SelenideElement applicationColumnHeader() {
    return appsWithoutCiIntegrationsTable().$(".nx-cell--header:nth-child(1)");
  }

  private SelenideElement totalRiskColumnHeader() {
    return appsWithoutCiIntegrationsTable().$(".nx-cell--header:nth-child(2)");
  }

  private SelenideElement appsWithoutScmIntegrationsTable() {
    return $("#iq-integrations-apps-without-scm-integrations-section");
  }

  private ElementsCollection appsWithoutScmIntegrationsTableDataRows() {
    return appsWithoutScmIntegrationsTable().findAll(" tbody .nx-table-row");
  }

  private SelenideElement applicationNameWithNoScm(int rowNum) {
    return appsWithoutScmIntegrationsTableDataRows().get(rowNum).$(".nx-cell:nth-child(1)");
  }

  private SelenideElement applicationTotalRiskWithNoScm(int rowNum) {
    return appsWithoutScmIntegrationsTableDataRows().get(rowNum).$(".nx-cell:nth-child(1)");
  }
}
