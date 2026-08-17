/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.BulkWaivePage;
import com.sonatype.clm.testing.playwright.pages.BulkWaivePageAssertions;
import com.sonatype.clm.testing.playwright.pages.WaiverConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.WaiverConfigurationPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WaiverConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "pw-bulk-waive-org";

  private static final String APP_NAME_PREFIX = "pw-bulk-waive-app";

  private static final String APP_ID_PREFIX = "pw-bw-app";

  private static final String CANNED_REPORT_CLASSPATH_DIR = "/canned-reports/transitive-violations-report";

  private static final String SCAN_ID = "pw-bulk-waive-scan-01";

  private static final String POLICY_NAME = "pw-bw-policy";

  private static final int POLICY_THREAT_LEVEL = 7;

  private static final String CUSTOM_EXPIRY_OPTION_VALUE = "custom";

  private static final String INVALID_PAST_DATE = "2000-01-01";

  private static final String VALID_PRESET_EXPIRY_VALUE = "30";

  private static final String ENTERPRISE_BANNER_TEXT = "Efficiently manage multiple policy violations";

  private WaiverConfigurationPage configPage;

  private WaiverConfigurationPageAssertions configAssertions;

  private BulkWaivePage bulkWaivePage;

  private BulkWaivePageAssertions bulkAssertions;

  private Application application;

  @BeforeEach
  public void seedEntitiesAndLogin() throws IOException {
    configPage = new WaiverConfigurationPage();
    configAssertions = new WaiverConfigurationPageAssertions(configPage);
    bulkWaivePage = new BulkWaivePage();
    bulkAssertions = new BulkWaivePageAssertions(bulkWaivePage);

    seedDb();
    playwrightRefreshOrOpen(BulkWaivePage.url(application.getPublicId(), SCAN_ID));
    playwrightLogin();
  }

  @AfterEach
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Test
  @Tag("regression")
  public void testWaiverConfiguration_redirectGuard_noSelectionsRedirectsToBulkWaive() {
    String directUrl = WaiverConfigurationPage.url(application.getPublicId(), SCAN_ID);
    playwrightRefreshOrOpen(directUrl);
    page.waitForURL("**/bulkWaive");
    assertThat(bulkWaivePage.container()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testWaiverConfiguration_expiryValidation_nextDisabledUntilExpirySelected() {
    navigateThroughBulkWaivePage();
    configAssertions.shouldBeVisible();

    configAssertions.shouldShowAllFormFields();
    configAssertions.shouldHaveNextButtonDisabled();

    assertThat(configPage.expirationDaysDiffMessage()).isHidden();

    configPage.expirySelect().selectOption(CUSTOM_EXPIRY_OPTION_VALUE);

    assertThat(configPage.customExpiryDateInput()).isVisible();

    configPage.customExpiryDateInput().fill(INVALID_PAST_DATE);
    configAssertions.shouldHaveNextButtonDisabled();

    configPage.expirySelect().selectOption(VALID_PRESET_EXPIRY_VALUE);
    configAssertions.shouldHaveNextButtonEnabled();

    assertThat(configPage.expirationDaysDiffMessage()).isVisible();

    configPage.nextButton().click();
    page.waitForURL("**/waiverConfirmation");
    configPage.confirmationPageContainer().waitFor();

    configPage.confirmationPageBackButton().click();
    page.waitForURL("**/waiverConfiguration");
    configAssertions.shouldBeVisible();

    assertThat(configPage.expirySelect()).hasValue(VALID_PRESET_EXPIRY_VALUE);
    configAssertions.shouldHaveNextButtonEnabled();
  }

  @Test
  @Tag("regression")
  public void testWaiverConfiguration_backButton_returnsToViolationSelection() {
    navigateThroughBulkWaivePage();
    configAssertions.shouldBeVisible();

    configPage.backButton().click();
    page.waitForURL("**/bulkWaive");
    bulkAssertions.shouldBeVisible();

    bulkAssertions.shouldHaveNextButtonEnabled();
    assertThat(bulkWaivePage.selectionCountLabel()).containsText("1 violation selected");
  }

  @Test
  @Tag("regression")
  public void testWaiverConfiguration_cancel_exitsFlowAndClearsState() {
    navigateThroughBulkWaivePage();
    configAssertions.shouldBeVisible();

    configPage.expirySelect().selectOption("30");

    configPage.cancelButton().click();

    page.waitForURL("**/applicationReport/**");
    assertThat(configPage.container()).isHidden();

    playwrightRefreshOrOpen(BulkWaivePage.url(application.getPublicId(), SCAN_ID));
    bulkAssertions.shouldBeVisible();
    assertThat(bulkWaivePage.nextButton()).isDisabled();

    bulkWaivePage.selectFirstViolationAndClickNext();
    configAssertions.shouldBeVisible();
    assertThat(configPage.expirySelect()).hasValue("");
    configAssertions.shouldHaveNextButtonDisabled();
  }

  @Test
  @Tag("regression")
  public void testWaiverConfiguration_enterpriseBanner_shownWhenBulkWaiversNotLicensed() {
    setMissingFeature(LicensedFeature.BULK_WAIVERS);

    navigateThroughBulkWaivePage();
    configAssertions.shouldBeVisible();

    configAssertions.shouldShowEnterpriseBannerWithText(ENTERPRISE_BANNER_TEXT);
  }

  private void navigateThroughBulkWaivePage() {
    String bulkWaiveUrl = BulkWaivePage.url(application.getPublicId(), SCAN_ID);
    playwrightRefreshOrOpen(bulkWaiveUrl);
    bulkWaivePage.selectFirstViolationAndClickNext();
  }

  private void seedDb() throws IOException {
    String suffix = TemporaryEntity.uuid();
    String orgName = ORG_NAME_PREFIX + "-" + suffix;
    String appName = APP_NAME_PREFIX + "-" + suffix;
    String appPublicId = APP_ID_PREFIX + "-" + suffix;

    Organization org = tempEntity.newOrganization(orgName);
    application = tempEntity.newApplication(appName, appPublicId, org.getId());

    Policy policy = new Policy(null, POLICY_NAME + "-" + suffix);
    policy.setThreatLevel(POLICY_THREAT_LEVEL);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "pw-bw-constraint", LogicalOperator.AND);
    constraint.setConditions(Collections.singletonList(
        new Condition("MatchState", "is", "exact")));
    policy.setConstraints(Collections.singletonList(constraint));
    tempEntity.newPolicy(policy);

    URL zippedReport = ReportHelper.zipReport(CANNED_REPORT_CLASSPATH_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(application, SCAN_ID, zippedReport, baseUrlFromTest, work)
        .evaluatePolicy();
  }
}
