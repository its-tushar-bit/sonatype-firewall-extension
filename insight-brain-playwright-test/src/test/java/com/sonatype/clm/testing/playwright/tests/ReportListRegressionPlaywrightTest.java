/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.ReportListPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression carve-out from {@link ReportListPlaywrightTest} for tests that need lighter,
 * per-test seeding than the sanity class's pre-seeded BUILD + STAGE_RELEASE evaluations.
 */
public class ReportListRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "ReportListRegressionOrg";

  private static final String APP_NAME_PREFIX = "ReportListRegressionApp";

  private static final String BUILD_SCAN_ID_PREFIX = "buildScan";

  private static final String BUILD_REPORT_DIR = "/canned-reports/large-report";

  @Before
  public void openReportsPageAsAdmin() {
    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testStageCells_unevaluatedApplicationShowsEmptyCells() {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORG_NAME_PREFIX + "-122-" + suffix);
    Application app = tempEntity.newApplication(
        APP_NAME_PREFIX + "-122-" + suffix, APP_NAME_PREFIX + "-122-" + suffix, org.getId());

    playwrightRefreshOrOpen(ReportListPage.url());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);
    reportList.typeFilter(app.getPublicId());

    // rowForApp avoids the firstRow()-after-typeFilter race where the previously-visible
    // row briefly matches before the server-side filter applies.
    Locator row = reportList.rowForApp(app.getPublicId());
    assertThat(row).isVisible();

    assertions.shouldShowEmptyStageCell(reportList.buildCellOf(row));
    assertions.shouldShowEmptyStageCell(reportList.stageReleaseCellOf(row));
    assertions.shouldShowEmptyStageCell(reportList.releaseCellOf(row));
  }

  @Test
  @Category(RegressionTest.class)
  public void testStageCells_evaluatedApplicationBuildCellNavigatesToReport() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORG_NAME_PREFIX + "-123-" + suffix);
    Application app = tempEntity.newApplication(
        APP_NAME_PREFIX + "-123-" + suffix, APP_NAME_PREFIX + "-123-" + suffix, org.getId());
    // Permissive policy ensures the BUILD evaluation produces a violation, which drives the
    // threat-counter chiclet + clickable link the test asserts on.
    tempEntity.newPolicy(org, 5, LogicalOperator.AND, new Condition("MatchState", "is", "exact"));
    evaluateBuild(app, BUILD_SCAN_ID_PREFIX + suffix);

    playwrightRefreshOrOpen(ReportListPage.url());

    ReportListPage reportList = new ReportListPage();
    ReportListPageAssertions assertions = new ReportListPageAssertions(reportList);
    reportList.typeFilter(app.getPublicId());

    // rowForApp avoids the firstRow()-after-typeFilter race where the previously-visible
    // row briefly matches before the server-side filter applies.
    Locator row = reportList.rowForApp(app.getPublicId());
    assertThat(row).isVisible();
    Locator buildCell = reportList.buildCellOf(row);
    assertions.shouldShowThreatCounterIn(buildCell);

    reportList.buildReportLinkOf(row).click();

    ApplicationReportPage appReport = new ApplicationReportPage();
    assertThat(appReport.appReportMain()).isVisible();
  }

  private void evaluateBuild(Application app, String scanId) throws IOException {
    URL zippedReport = ReportHelper.zipReport(BUILD_REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work, Stage.ID_BUILD)
        .evaluatePolicy();
  }
}
