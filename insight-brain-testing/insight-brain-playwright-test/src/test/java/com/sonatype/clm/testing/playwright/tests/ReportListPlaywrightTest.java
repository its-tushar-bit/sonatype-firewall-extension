/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Per-test UUID-seeded org + app + canned BUILD / STAGE-RELEASE evaluations. */
public class ReportListPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORGANIZATION_NAME_PREFIX = "ApplicationReportTestOrg";

  private static final String APPLICATION_NAME_PREFIX = "ApplicationReportTest";

  private static final String USER_PREFIX = "reportListUser";

  private static final String USER_FIRST_NAME = "reallylongfirst";

  private static final String USER_LAST_NAME = "even longer last name junior senior";

  private static final String USER_EMAIL_DOMAIN = "example.com";

  private static final String BUILD_SCAN_ID = "BUILD_SCAN_ID";

  private static final String STAGE_SCAN_ID = "STAGE_SCAN_ID";

  private static final String BUILD_REPORT_DIR = "/canned-reports/large-report";

  private static final String STAGE_REPORT_DIR = SmallReportFixture.CANNED_REPORT_DIR;

  private static final String NO_MATCH_FILTER = "zzzz-no-app-matches-this";

  private static final String CONTACT_ENDPOINT_GLOB = "**/rest/application/services/summary/*";

  private static final List<String> EXPECTED_HEADERS =
      List.of("APPLICATION", "ORGANIZATION", "SOURCE", "BUILD", "STAGE RELEASE", "RELEASE");

  private static final String EXPECTED_FIRST_ROW_BUILD_THREAT_CATEGORY = "Critical";

  /** Slice's page-1 size threshold for triggering "more results exist". */
  private static final int LOAD_MORE_PAGE_SIZE = 50;

  private Application app;

  @BeforeEach
  public void seedReportsAndOpenAsAdmin() throws IOException {
    seedOrgAppUserAndReports();

    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void testReportLinks() {
    ReportListPage reportList = new ReportListPage();
    ApplicationReportPage appReport = new ApplicationReportPage();

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    Locator buildLink = reportList.buildReportLinkOf(firstRow);
    assertThat(buildLink).isVisible();
    buildLink.click();

    assertThat(appReport.appReportMain()).isVisible();
  }

  @Test
  @Tag("sanity")
  public void testChiclets() {
    ReportListPage reportList = new ReportListPage();

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    Locator buildCell = reportList.buildCellOf(firstRow);
    Locator criticalCounter = reportList.criticalCounterIn(buildCell);
    assertThat(criticalCounter).isVisible();
    assertThat(reportList.counterCategoryIn(criticalCounter))
        .hasText(EXPECTED_FIRST_ROW_BUILD_THREAT_CATEGORY);
  }

  @Test
  @Tag("sanity")
  public void testHeadersOrder() {
    ReportListPage reportList = new ReportListPage();

    Assertions.assertThat(reportList.headerTexts(EXPECTED_HEADERS.size()))
        .as("report list table header order")
        .isEqualTo(EXPECTED_HEADERS);
  }

  @Test
  @Tag("regression")
  public void testReportsPage_titleAndContainerVisible() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldBeVisible();
    assertions.shouldShowPageTitle(ReportListPage.LIFECYCLE_TITLE);
    assertions.shouldShowFilterInput();
  }

  /**
   * Title is gated on {@code DEVELOPER_DASHBOARD}; reached via direct URL since the Solution Switcher opens in a new
   * tab.
   * <p>
   * Note: {@code setFeatures} replaces the full feature set, leaving only {@code DEVELOPER_DASHBOARD} licensed
   * for this test. The minimal-license scenario is intentional — only the page title is asserted, so a
   * partial render (or 402s on feature-gated API calls) does not affect the assertion.
   */
  @Test
  @Tag("regression")
  public void testDeveloperPriorities_pageTitleIsPriorities() {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    playwrightRefreshOrOpen(ReportListPage.developerPrioritiesUrl());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldBeVisible();
    assertions.shouldShowPageTitle(ReportListPage.DEVELOPER_TITLE);
  }

  @Test
  @Tag("regression")
  public void testFilter_realTimeFiltersTable_clearRestoresAll() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldHaveRowCount(1);

    reportList.typeFilter(NO_MATCH_FILTER);
    assertions.shouldShowEmptyMessage();

    reportList.clearFilter();
    assertions.shouldHaveRowCount(1);
  }

  /** Only App and Org headers are sortable; Source-stage is checked as a non-sortable guard. */
  @Test
  @Tag("regression")
  public void testSort_applicationAndOrganizationHeaders_toggleAriaSort() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    reportList.waitForFullHeaderRow(EXPECTED_HEADERS.size());

    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "none");
    assertions.shouldHaveAriaSort(reportList.organizationHeaderCell(), "none");

    reportList.clickApplicationSort();
    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "ascending");
    reportList.clickApplicationSort();
    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "descending");

    reportList.clickOrganizationSort();
    assertions.shouldHaveAriaSort(reportList.organizationHeaderCell(), "ascending");
    reportList.clickOrganizationSort();
    assertions.shouldHaveAriaSort(reportList.organizationHeaderCell(), "descending");

    assertions.shouldNotBeSortable(reportList.sourceColumnHeader());
  }

  /** Stage columns aren't sortable; click must be a no-op for aria-sort on every header. */
  @Test
  @Tag("regression")
  public void testSort_stageColumnHeader_isNotSortable() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    reportList.waitForFullHeaderRow(EXPECTED_HEADERS.size());

    assertions.shouldNotBeSortable(reportList.sourceColumnHeader());
    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "none");

    reportList.sourceColumnHeader().click();

    assertions.shouldNotBeSortable(reportList.sourceColumnHeader());
    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "none");
    assertions.shouldHaveAriaSort(reportList.organizationHeaderCell(), "none");
  }

  /** Asserts the AT-facing accessible name; the sibling test covers aria-sort on the th cell. */
  @Test
  @Tag("regression")
  public void testSort_indicatorReflectsAscendingDescending() {
    ReportListPage reportList = new ReportListPage();
    reportList.waitForFullHeaderRow(EXPECTED_HEADERS.size());

    Locator appSortButton = reportList.sortButtonOf(reportList.applicationHeaderCell());

    // RSC NxTableCell.tsx accessible-name format: `<headerText> <ariaSort>`.
    assertThat(appSortButton).hasAccessibleName("Application unsorted",
        new LocatorAssertions.HasAccessibleNameOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    reportList.clickApplicationSort();
    assertThat(appSortButton).hasAccessibleName("Application ascending",
        new LocatorAssertions.HasAccessibleNameOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    reportList.clickApplicationSort();
    assertThat(appSortButton).hasAccessibleName("Application descending",
        new LocatorAssertions.HasAccessibleNameOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  @Test
  @Tag("regression")
  public void testStageCell_threatCountsRendered() {
    ReportListPage reportList = new ReportListPage();

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();
    Locator buildCell = reportList.buildCellOf(firstRow);
    assertThat(reportList.criticalCounterIn(buildCell)).isVisible();
  }

  @Test
  @Tag("regression")
  public void testStageCell_sourcePendingState() {
    tempEntity.newSourceControlEvaluationEvent(app);

    playwrightRefreshOrOpen(ReportListPage.url());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldShowSourcePendingState(reportList.firstRow());
  }

  @Test
  @Tag("regression")
  public void testStageCellLinks_dualMode_showsReportAndPriorities() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    assertions.shouldShowBothReportAndPrioritiesLinks(firstRow);
  }

  @Test
  @Tag("regression")
  public void testStageCellLinks_developerMode_onlyDeveloperPrioritiesLink() {
    playwrightRefreshOrOpen(ReportListPage.developerPrioritiesUrl());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    assertions.shouldShowOnlyDeveloperPrioritiesLink(firstRow);
  }

  /** N+1 apps so page-1 fills to {@link #LOAD_MORE_PAGE_SIZE}; page-2's single row hides the button. */
  @Test
  @Tag("regression")
  public void testLoadMoreResults_buttonAppendsNextPage() {
    int totalApps = LOAD_MORE_PAGE_SIZE + 1;
    tempEntity.newApplications(app.getOrganizationId(), totalApps - 1);

    playwrightRefreshOrOpen(ReportListPage.url());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldHaveRowCount(LOAD_MORE_PAGE_SIZE);
    assertions.shouldShowLoadMoreButton();

    reportList.loadButton().click();

    assertions.shouldHaveRowCount(totalApps);
    assertThat(reportList.loadButton()).hasCount(0);
  }

  /** Show Contact lazy-loads the contact-info via the application services-summary endpoint. */
  @Test
  @Tag("regression")
  public void testContactColumn_loadedState_displaysName() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    Locator firstRow = reportList.firstRow();
    assertions.shouldShowShowContactButton(firstRow);
    reportList.showContactButtonOf(firstRow).click();

    assertions.shouldShowLoadedContactName(firstRow, USER_FIRST_NAME + " " + USER_LAST_NAME);
  }

  /** Latch holds the response so the spinner is observable; avoids the banned {@code page.waitForTimeout}. */
  @Test
  @Tag("regression")
  public void testContactColumn_loadingState_showsSpinner() {
    CountDownLatch release = new CountDownLatch(1);
    page.route(CONTACT_ENDPOINT_GLOB, route -> {
      try {
        release.await(15, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      route.resume();
    });
    try {
      ReportListPage reportList = new ReportListPage();
      ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

      Locator firstRow = reportList.firstRow();
      reportList.showContactButtonOf(firstRow).click();
      assertions.shouldShowContactLoadingSpinner(firstRow);
    }
    finally {
      release.countDown();
      page.unrouteAll();
    }
  }

  /** 500 → {@code reportsSlice.loadContactNameRejected} → renders the error-icon block. */
  @Test
  @Tag("regression")
  public void testContactColumn_errorState_showsErrorIconAndText() {
    page.route(CONTACT_ENDPOINT_GLOB,
        route -> route.fulfill(new Route.FulfillOptions().setStatus(500)));
    try {
      ReportListPage reportList = new ReportListPage();
      ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

      Locator firstRow = reportList.firstRow();
      reportList.showContactButtonOf(firstRow).click();
      assertions.shouldShowContactErrorState(firstRow);
    }
    finally {
      page.unrouteAll();
    }
  }

  /** {@code doSort} short-circuits on an empty result set so {@code aria-sort} stays "none". */
  @Test
  @Tag("regression")
  public void testSort_disabledWhenNoData() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    reportList.typeFilter(NO_MATCH_FILTER);
    assertions.shouldShowEmptyMessage();

    reportList.clickApplicationSort();

    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "none");
  }

  /**
   * Aborting the primary data fetch ({@code /rest/application/services/summary}) and the
   * stage-type fetch ({@code /rest/policy/stages}) causes {@code reportsSlice.loadStagesAndReports}
   * to reject, setting {@code loadError} in Redux. {@code NxLoadWrapper} then renders its error
   * state containing a Retry button.
   * <p>
   * Both endpoints must be intercepted: {@code loadStagesFulfilled} sets {@code loadError = null}
   * unconditionally, so aborting only the summary endpoint causes the error to be cleared when
   * the stage-type request later resolves successfully. The container is asserted visible before
   * the routes are registered so that in-flight API calls from {@code @Before} have settled.
   * {@code page.unrouteAll()} in finally cleans up state for sibling tests.
   */
  @Test
  @Tag("regression")
  public void testReportsPage_networkErrorOnDataLoad_showsRetryButton() {
    try {
      assertThat(new ReportListPage().container()).isVisible(
          new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
      page.route(Pattern.compile(".*/rest/application/services/summary.*"), Route::abort);
      page.route(Pattern.compile(".*/rest/policy/stages.*"), Route::abort);
      playwrightRefreshOrOpen(ReportListPage.url());

      ReportListPage reportList = new ReportListPage();
      Locator retryButton = reportList.container()
          .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Retry"));
      assertThat(retryButton).isVisible(
          new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    }
    finally {
      page.unrouteAll();
    }
  }

  /**
   * Reports page renders only {@code NxSmallThreatCounter} — no per-action icon — so the
   * action's persistence on the policy is the only verifiable claim here.
   */
  @Test
  @Tag("regression")
  public void testFailActionPolicy_persistsOnBuildStage() {
    Policy policy = tempEntity.newPolicy(
        app.getOrganizationId(), "Fail Action Policy-" + TemporaryEntity.uuid(),
        10, "fail", Stage.ID_BUILD, null);

    Policy reloaded = lookup(PolicyDAO.class).getById(policy.getId());
    Assertions.assertThat(reloaded.getActions())
        .as("seeded Fail action persists on the policy for the build stage")
        .containsEntry(Stage.ID_BUILD, "fail");
  }

  /** Same UI-rendering caveat as {@link #testFailActionPolicy_persistsOnBuildStage}. */
  @Test
  @Tag("regression")
  public void testWarnActionPolicy_persistsOnBuildStage() {
    Policy policy = tempEntity.newPolicy(
        app.getOrganizationId(), "Warn Action Policy-" + TemporaryEntity.uuid(),
        6, "warn", Stage.ID_BUILD, null);

    Policy reloaded = lookup(PolicyDAO.class).getById(policy.getId());
    Assertions.assertThat(reloaded.getActions())
        .as("seeded Warn action persists on the policy for the build stage")
        .containsEntry(Stage.ID_BUILD, "warn");
  }

  @Test
  @Tag("regression")
  public void testNotifyOnlyPolicy_carriesNoActionEntries() {
    Policy policy = tempEntity.newPolicy(
        app.getOrganizationId(), "Notify Only Policy-" + TemporaryEntity.uuid(), 4);

    Policy reloaded = lookup(PolicyDAO.class).getById(policy.getId());
    Assertions.assertThat(reloaded.getActions())
        .as("notify-only policy carries no action entries")
        .doesNotContainKey(Stage.ID_BUILD);
  }

  /**
   * Separate app from the class-level seed so "Stage Release / Release columns empty" is
   * legitimate (the @Before seeds STAGE_RELEASE on its own app).
   */
  @Test
  @Tag("regression")
  public void testStageCells_buildOnlyShowsOtherStagesEmpty() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORGANIZATION_NAME_PREFIX + "-buildOnly-" + suffix);
    Application buildOnlyApp = tempEntity.newApplication(
        APPLICATION_NAME_PREFIX + "-buildOnly-" + suffix,
        APPLICATION_NAME_PREFIX + "-buildOnly-" + suffix,
        org.getId());
    lookup(PolicyImportExport.class).importOrganization(org, loadReferencePolicies());
    evaluateApp(buildOnlyApp, "BUILD_ONLY_SCAN_" + suffix, BUILD_REPORT_DIR, Stage.ID_BUILD);

    playwrightRefreshOrOpen(ReportListPage.url());
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);
    reportList.typeFilter(buildOnlyApp.getPublicId());

    Locator row = reportList.rowForApp(buildOnlyApp.getPublicId());
    assertThat(row).isVisible();
    assertions.shouldShowThreatCounterIn(reportList.buildCellOf(row));
    assertions.shouldShowEmptyStageCell(reportList.stageReleaseCellOf(row));
    assertions.shouldShowEmptyStageCell(reportList.releaseCellOf(row));
  }

  /** Fresh app with BUILD + RELEASE evaluations — the class-level seed covers BUILD + STAGE_RELEASE. */
  @Test
  @Tag("regression")
  public void testStageCells_buildAndReleaseBothPopulated() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORGANIZATION_NAME_PREFIX + "-buildRelease-" + suffix);
    Application multiStageApp = tempEntity.newApplication(
        APPLICATION_NAME_PREFIX + "-buildRelease-" + suffix,
        APPLICATION_NAME_PREFIX + "-buildRelease-" + suffix,
        org.getId());
    lookup(PolicyImportExport.class).importOrganization(org, loadReferencePolicies());
    evaluateApp(multiStageApp, "MULTI_BUILD_" + suffix, BUILD_REPORT_DIR, Stage.ID_BUILD);
    evaluateApp(multiStageApp, "MULTI_RELEASE_" + suffix, BUILD_REPORT_DIR, Stage.ID_RELEASE);

    playwrightRefreshOrOpen(ReportListPage.url());
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);
    reportList.typeFilter(multiStageApp.getPublicId());

    Locator row = reportList.rowForApp(multiStageApp.getPublicId());
    assertThat(row).isVisible();
    assertions.shouldShowThreatCounterIn(reportList.buildCellOf(row));
    assertions.shouldShowThreatCounterIn(reportList.releaseCellOf(row));
  }

  private void evaluateApp(Application targetApp, String scanId, String reportDir, String stageId) throws IOException {
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator.seedEvaluation(targetApp, scanId, reportDir, tempDir, baseUrlFromTest, work, stageId);
  }

  @Test
  @Tag("regression")
  public void testStageCells_samePolicyDifferentApps() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORGANIZATION_NAME_PREFIX + "-crossApp-" + suffix);
    Application highSevApp = tempEntity.newApplication(
        APPLICATION_NAME_PREFIX + "-high-" + suffix,
        APPLICATION_NAME_PREFIX + "-high-" + suffix,
        org.getId());
    Application lowSevApp = tempEntity.newApplication(
        APPLICATION_NAME_PREFIX + "-low-" + suffix,
        APPLICATION_NAME_PREFIX + "-low-" + suffix,
        org.getId());
    lookup(PolicyImportExport.class).importOrganization(org, loadReferencePolicies());
    evaluateApp(highSevApp, "CROSS_HI_" + suffix, BUILD_REPORT_DIR, Stage.ID_BUILD);
    evaluateApp(lowSevApp, "CROSS_LO_" + suffix, STAGE_REPORT_DIR, Stage.ID_BUILD);

    playwrightRefreshOrOpen(ReportListPage.url());
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    // rowForApp avoids the firstRow()-after-typeFilter race where the previous app's row
    // briefly matches before the server-side filter applies.
    reportList.typeFilter(highSevApp.getPublicId());
    Locator highRow = reportList.rowForApp(highSevApp.getPublicId());
    assertThat(highRow).isVisible();
    assertions.shouldShowThreatCounterIn(reportList.buildCellOf(highRow));

    reportList.clearFilter();
    reportList.typeFilter(lowSevApp.getPublicId());
    Locator lowRow = reportList.rowForApp(lowSevApp.getPublicId());
    assertThat(lowRow).isVisible();
    assertions.shouldShowThreatCounterIn(reportList.buildCellOf(lowRow));
  }

  private static final String REFERENCE_POLICIES_RESOURCE = "/reference-policies-v3.json";

  private void seedOrgAppUserAndReports() throws IOException {
    PolicyExportResult referencePolicies = loadReferencePolicies();

    String suffix = TemporaryEntity.uuid();
    String orgName = ORGANIZATION_NAME_PREFIX + "-" + suffix;
    String appName = APPLICATION_NAME_PREFIX + "-" + suffix;
    String username = USER_PREFIX + "-" + suffix;
    String email = username + "@" + USER_EMAIL_DOMAIN;

    Organization org = tempEntity.newOrganization(orgName);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    tempEntity.newUser(username, USER_FIRST_NAME, USER_LAST_NAME, email);
    app = tempEntity.newApplication(appName, appName, org.getId(), username);

    evaluate(BUILD_SCAN_ID, BUILD_REPORT_DIR, Stage.ID_BUILD);
    evaluate(STAGE_SCAN_ID, STAGE_REPORT_DIR, Stage.ID_STAGE_RELEASE);
  }

  private void evaluate(String scanId, String reportDir, String stageId) throws IOException {
    URL zippedReport = ReportHelper.zipReport(reportDir, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work, stageId)
        .evaluatePolicy();
  }

  /**
   * Loads the canned reference policies bundle from the classpath. Guarded so a missing resource
   * surfaces as a clear {@code IllegalStateException} instead of an NPE inside Jackson.
   * {@code JsonUtils.parse(InputStream, Class)} closes the stream via Jackson's default
   * {@code AUTO_CLOSE_SOURCE} feature, so an explicit try-with-resources at the call site is
   * redundant.
   */
  private PolicyExportResult loadReferencePolicies() throws IOException {
    InputStream is = getClass().getResourceAsStream(REFERENCE_POLICIES_RESOURCE);
    if (is == null) {
      throw new IllegalStateException("Missing classpath resource: " + REFERENCE_POLICIES_RESOURCE);
    }
    return JsonUtils.parse(is, PolicyExportResult.class);
  }
}
