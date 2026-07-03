/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** Regression tests for Application Report back-button origin-param behavior. */
public class ApplicationReportRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private static final String APP_PREFIX = "AppReportRegressionApp";

  private static final String USER_PREFIX = "appReportRegressionUser";

  private static final String BACK_BUTTON_FIREWALL_DASHBOARD = "Back to Firewall Dashboard";

  private static final String BACK_BUTTON_REPOSITORY_RESULTS = "Back to Repository Results";

  private static final String ORIGIN_FIREWALL_CONTAINERS = "firewall.firewallPage.containers";

  private static final String ORIGIN_REPOSITORY_RESULTS = "firewall.containerRepositoryResults";

  private static final Pattern FIREWALL_CONTAINERS_URL_PATTERN = Pattern.compile(".*firewall.*containers.*");

  private static final Pattern CONTAINER_REPOSITORY_URL_PATTERN = Pattern.compile(".*container.*repository.*");

  private Application app;

  @Before
  public void seedAndOpen() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);

    String suffix = TemporaryEntity.uuid();
    String appName = APP_PREFIX + "-" + suffix;
    String username = USER_PREFIX + "-" + suffix;
    String email = username + "@example.com";

    Organization org = tempEntity.newOrganization();
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    tempEntity.newUser(username, "Test", "User", email);
    app = tempEntity.newApplication(appName, appName, org.getId(), username);

    URL zippedReport = ReportHelper.zipReport(REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work, Stage.ID_BUILD)
        .evaluatePolicy();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    playwrightLogin();
    new ApplicationReportPageAssertions(new ApplicationReportPage()).shouldBeVisible();
  }

  /** Back button shows "Back to Firewall Dashboard" when origin=firewall.firewallPage.containers. */
  @Test
  @Category(RegressionTest.class)
  public void testApplicationReport_backButton_firewallDashboardContext_showsCorrectLabel() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    playwrightRefreshOrOpen(
        ApplicationReportPage.url(app, SCAN_ID) + "?origin=" + ORIGIN_FIREWALL_CONTAINERS);
    reportAssertions.shouldBeVisible();
    reportPage.waitForLoadingSpinnerHidden();

    reportAssertions.shouldShowBackButtonWithText(BACK_BUTTON_FIREWALL_DASHBOARD);
    reportPage.backButton().click();
    assertThat(page).hasURL(FIREWALL_CONTAINERS_URL_PATTERN);
  }

  /** Back button shows "Back to Repository Results" when origin=firewall.containerRepositoryResults. */
  @Test
  @Category(RegressionTest.class)
  public void testApplicationReport_backButton_repositoryResultsContext_showsCorrectLabel() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    playwrightRefreshOrOpen(
        ApplicationReportPage.url(app, SCAN_ID) + "?origin=" + ORIGIN_REPOSITORY_RESULTS);
    reportAssertions.shouldBeVisible();
    reportPage.waitForLoadingSpinnerHidden();

    reportAssertions.shouldShowBackButtonWithText(BACK_BUTTON_REPOSITORY_RESULTS);
    reportPage.backButton().click();
    assertThat(page).hasURL(CONTAINER_REPOSITORY_URL_PATTERN);
  }
}
