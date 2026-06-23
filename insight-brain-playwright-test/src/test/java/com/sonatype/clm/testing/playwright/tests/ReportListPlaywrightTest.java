/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPageAssertions;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

  @Before
  public void seedReportsAndOpenAsAdmin() throws IOException {
    seedOrgAppUserAndReports();

    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
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
  @Category(SanityTest.class)
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
  @Category(SanityTest.class)
  public void testHeadersOrder() {
    ReportListPage reportList = new ReportListPage();

    Assertions.assertThat(reportList.headerTexts(EXPECTED_HEADERS.size()))
        .as("report list table header order")
        .isEqualTo(EXPECTED_HEADERS);
  }

  @Test
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
  public void testDeveloperPriorities_pageTitleIsPriorities() {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    playwrightRefreshOrOpen(ReportListPage.developerPrioritiesUrl());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldBeVisible();
    assertions.shouldShowPageTitle(ReportListPage.DEVELOPER_TITLE);
  }

  @Test
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
  public void testSort_indicatorReflectsAscendingDescending() {
    ReportListPage reportList = new ReportListPage();
    reportList.waitForFullHeaderRow(EXPECTED_HEADERS.size());

    Locator appSortButton = reportList.sortButtonOf(reportList.applicationHeaderCell());

    // RSC NxTableCell.tsx accessible-name format: `<headerText> <ariaSort>`.
    assertThat(appSortButton).hasAccessibleName("Application unsorted");
    reportList.clickApplicationSort();
    assertThat(appSortButton).hasAccessibleName("Application ascending");
    reportList.clickApplicationSort();
    assertThat(appSortButton).hasAccessibleName("Application descending");
  }

  @Test
  @Category(RegressionTest.class)
  public void testStageCell_threatCountsRendered() {
    ReportListPage reportList = new ReportListPage();

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();
    Locator buildCell = reportList.buildCellOf(firstRow);
    assertThat(reportList.criticalCounterIn(buildCell)).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testStageCell_sourcePendingState() {
    tempEntity.newSourceControlEvaluationEvent(app);

    playwrightRefreshOrOpen(ReportListPage.url());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    assertions.shouldShowSourcePendingState(reportList.firstRow());
  }

  @Test
  @Category(RegressionTest.class)
  public void testStageCellLinks_dualMode_showsReportAndPriorities() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    assertions.shouldShowBothReportAndPrioritiesLinks(firstRow);
  }

  @Test
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
  public void testSort_disabledWhenNoData() {
    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);

    reportList.typeFilter(NO_MATCH_FILTER);
    assertions.shouldShowEmptyMessage();

    reportList.clickApplicationSort();

    assertions.shouldHaveAriaSort(reportList.applicationHeaderCell(), "none");
  }

  private static final String REFERENCE_POLICIES_RESOURCE = "/reference-policies-v3.json";

  private void seedOrgAppUserAndReports() throws IOException {
    URL referencePolicyUrl = getClass().getResource(REFERENCE_POLICIES_RESOURCE);
    if (referencePolicyUrl == null) {
      throw new IllegalStateException("Missing classpath resource: " + REFERENCE_POLICIES_RESOURCE);
    }
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);

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
}
