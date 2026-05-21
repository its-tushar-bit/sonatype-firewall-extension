/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright test for the Report List page.
 * <p>
 * Each test follows a Given/When/Then shape:
 * <ul>
 * <li>{@link #seedReportsAndOpenAsAdmin()} seeds a per-test {@link Organization} + {@link Application}
 * (names are UUID-suffixed so parallel forks cannot collide), evaluates two canned reports
 * against the build and stage-release stages, then opens the Reports page logged-in as admin.</li>
 * <li>The test body interacts with the report list via {@link ReportListPage} locators.</li>
 * </ul>
 *
 * <p>
 * Selectors live in {@link ReportListPage} (and {@link ApplicationReportPage} for the destination
 * page after clicking a report link).
 */
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

  private static final String STAGE_REPORT_DIR = "/canned-reports/small-report";

  private static final List<String> EXPECTED_HEADERS =
      List.of("APPLICATION", "ORGANIZATION", "SOURCE", "BUILD", "STAGE RELEASE", "RELEASE");

  private static final String EXPECTED_FIRST_ROW_BUILD_THREAT_CATEGORY = "Critical";

  private Application app;

  // --------------- @Before ---------------

  @Before
  public void seedReportsAndOpenAsAdmin() throws IOException {
    seedOrgAppUserAndReports();

    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
  }

  // --------------- @Test methods ---------------

  @Test
  @Category(SanityTest.class)
  public void testReportLinks() {
    ReportListPage reportList = new ReportListPage();
    ApplicationReportPage appReport = new ApplicationReportPage();

    // Given: the report list is open (from @Before) and the seeded app row is present.
    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    // When: the user clicks the BUILD-stage "View Report" link in the first row.
    Locator buildLink = reportList.buildReportLinkOf(firstRow);
    assertThat(buildLink).isVisible();
    buildLink.click();

    // Then: navigation lands on the application report shell.
    assertThat(appReport.appReportMain()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testChiclets() {
    ReportListPage reportList = new ReportListPage();

    // Given: the seeded app row is rendered with its threat counters.
    Locator firstRow = reportList.firstRow();
    assertThat(firstRow).isVisible();

    // Then: the BUILD cell shows a "Critical" small-threat counter (canned large-report fixture
    // contains critical-severity violations at BUILD stage).
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

    // Then: column headers render in the documented order (App | Org | Source | Build | Stage Release | Release).
    Assertions.assertThat(reportList.headerTexts(EXPECTED_HEADERS.size()))
        .as("report list table header order")
        .isEqualTo(EXPECTED_HEADERS);
  }

  // --------------- Backend seed methods ---------------

  private void seedOrgAppUserAndReports() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
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
