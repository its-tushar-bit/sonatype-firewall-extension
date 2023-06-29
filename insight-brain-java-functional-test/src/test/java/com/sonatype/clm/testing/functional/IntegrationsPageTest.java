/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional;

import java.time.Instant;
import java.util.Date;

import com.sonatype.clm.testing.functional.pages.IntegrationsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class IntegrationsPageTest extends AbstractFunctionalTest
{
  private static final String DONUT_TEST_ID = "iq-integrations-cicard__donut";

  private static final String CI_USAGE_APP_TABLE_TEST_ID = "iq-integrations-apps-without-recent-usage-preview";

  private static final String CI_USAGE_PERCENT_SELECTOR = ".iq-integrations-cicard__donut-col";

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

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testAppsWithoutCiIntegrationsPage() {
    tempEntity.newApplicationWithParent("appId", "appName");

    refreshOrOpen(IntegrationsPage.urlAppsWithoutCiIntegrations());

    appsWithoutCiIntegrationsTable().shouldBe(visible);

    applicationName().shouldBe(visible).shouldHave(text("appName"));

    eyesWatcher.eyesCheck();
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

  private SelenideElement applicationName() {
    return appsWithoutCiIntegrationsTable().$(".iq-integrations-applications-table__name-cell");
  }
}
