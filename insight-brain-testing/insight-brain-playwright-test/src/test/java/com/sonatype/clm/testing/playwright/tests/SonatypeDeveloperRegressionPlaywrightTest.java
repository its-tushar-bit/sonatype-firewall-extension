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
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperRegressionAssertions;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperRegressionPage;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for the Sonatype Developer Dashboard and Priorities.
 * Divergence: tabs replaced by integration cards; Issue Tracking has no card equivalent.
 */
public class SonatypeDeveloperRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String BUILD_SCAN_ID = "DEV_REGRESSION_BUILD_SCAN";

  private Application seedApp;

  /** Trailing {@code *} covers the {@code ?timestamp=} cache-buster added by the Axios interceptor. */
  private static final String PRODUCT_FEATURES_PATTERN = "**/rest/product/features*";

  private static final Pattern CI_CD_URL_PATTERN =
      Pattern.compile(".*" + SonatypeDeveloperRegressionPage.CI_CD_URL_SEGMENT + ".*");

  private static final Pattern SCM_URL_PATTERN =
      Pattern.compile(".*" + SonatypeDeveloperRegressionPage.SCM_URL_SEGMENT + ".*");

  private static final Pattern IDE_URL_PATTERN =
      Pattern.compile(".*" + SonatypeDeveloperRegressionPage.IDE_URL_SEGMENT + ".*");

  @AfterEach
  public void unrouteAll() {
    page.unrouteAll();
  }

  /**
   * Developer Dashboard renders with heading and all three integration cards (CI-CD, SCM, IDE) with Learn
   * More links (divergence: tabs replaced by cards).
   */
  @Test
  @Tag("regression")
  public void testDeveloperDashboard_allIntegrationCardsWithLinks() {
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();

    SonatypeDeveloperRegressionPage regressionPage = new SonatypeDeveloperRegressionPage();
    SonatypeDeveloperPageAssertions assertions = new SonatypeDeveloperPageAssertions(regressionPage);
    SonatypeDeveloperRegressionAssertions regressionAssertions =
        new SonatypeDeveloperRegressionAssertions(regressionPage);

    assertions.shouldShowContainer();
    assertions.shouldShowPageTitle(SonatypeDeveloperPage.DASHBOARD_HEADING);
    assertions.shouldShowAllIntegrationCards();
    regressionAssertions.shouldShowCiCdCardLearnMoreLink();
    regressionAssertions.shouldShowScmCardLearnMoreLink();
    regressionAssertions.shouldShowIdeCardLearnMoreLink();
  }

  /** CI-CD card Learn More link navigates to /ci-cd (divergence: tab replaced by card). */
  @Test
  @Tag("regression")
  public void testDeveloperDashboard_ciCdCardLearnMoreLink_navigates() {
    SonatypeDeveloperRegressionPage regressionPage = navigateToDeveloperDashboard();
    regressionPage.ciCdCardLearnMoreLink().click();
    assertThat(page).hasURL(CI_CD_URL_PATTERN);
  }

  /** SCM card Learn More link navigates to /scm (divergence: tab replaced by card). */
  @Test
  @Tag("regression")
  public void testDeveloperDashboard_scmCardLearnMoreLink_navigates() {
    SonatypeDeveloperRegressionPage regressionPage = navigateToDeveloperDashboard();
    regressionPage.scmCardLearnMoreLink().click();
    assertThat(page).hasURL(SCM_URL_PATTERN);
  }

  /** IDE card Learn More link navigates to /ide (divergence: tab replaced by card). */
  @Test
  @Tag("regression")
  public void testDeveloperDashboard_ideCardLearnMoreLink_navigates() {
    SonatypeDeveloperRegressionPage regressionPage = navigateToDeveloperDashboard();
    regressionPage.ideCardLearnMoreLink().click();
    assertThat(page).hasURL(IDE_URL_PATTERN);
  }

  private SonatypeDeveloperRegressionPage navigateToDeveloperDashboard() {
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();
    return new SonatypeDeveloperRegressionPage();
  }

  /** "View Priorities" link on Developer Priorities list navigates to the per-app priorities detail page. */
  @Test
  @Tag("regression")
  public void testDeveloperPriorities_viewPrioritiesLink_navigatesToPerAppPrioritiesPage() throws IOException {
    seedBuildEvaluation();

    playwrightRefreshOrOpen(ReportListPage.developerPrioritiesUrl());
    playwrightLogin();

    ReportListPage reportList = new ReportListPage();
    SonatypeDeveloperRegressionPage regressionPage = new SonatypeDeveloperRegressionPage();
    SonatypeDeveloperRegressionAssertions regressionAssertions =
        new SonatypeDeveloperRegressionAssertions(regressionPage);

    Locator appRow = reportList.rows().filter(new Locator.FilterOptions().setHasText(seedApp.getName()));
    assertThat(appRow).isVisible();

    Locator viewPrioritiesLink = reportList.buildDeveloperOnlyPrioritiesLinkOf(appRow);
    assertThat(viewPrioritiesLink).isVisible();
    viewPrioritiesLink.click();

    assertThat(page).hasURL(Pattern.compile(
        ".*developer/priorities/" + seedApp.getPublicId() + "/" + BUILD_SCAN_ID));
    regressionAssertions.shouldShowPrioritiesPageSummary();
  }

  /**
   * stubbing GET /rest/product/features with 500 forces NxLoadWrapper into error state;
   * NxStatefulErrorAlert and Retry button are visible.
   * Stub is registered after login so that login-phase dispatches complete against the real
   * endpoint first; a subsequent reload drives a single clean fetch cycle through the stub.
   */
  @Test
  @Tag("regression")
  public void testDeveloperDashboard_loadError_alertShownWithRetryButton() {
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();

    page.route(PRODUCT_FEATURES_PATTERN, route -> route.fulfill(
        new Route.FulfillOptions().setStatus(500)));
    page.reload();
    page.waitForLoadState();

    SonatypeDeveloperRegressionPage regressionPage = new SonatypeDeveloperRegressionPage();
    SonatypeDeveloperRegressionAssertions regressionAssertions =
        new SonatypeDeveloperRegressionAssertions(regressionPage);

    regressionAssertions.shouldShowLoadError();
    assertThat(regressionPage.retryButton()).isVisible();
  }

  private void seedBuildEvaluation() throws IOException {
    Organization org = tempEntity.newOrganization();
    seedApp = tempEntity.newApplication(org.getId());
    URL zippedReport = ReportHelper.zipReport(SmallReportFixture.CANNED_REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(seedApp, BUILD_SCAN_ID, zippedReport, baseUrlFromTest, work, Stage.ID_BUILD)
        .evaluatePolicy();
  }
}
