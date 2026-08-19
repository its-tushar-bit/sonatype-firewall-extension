/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPolicyTabPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPolicyTabPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression carve-out from {@link ApplicationReportPolicyTabPlaywrightTest} for rows that need
 * custom seeding (no policies, large-report) instead of the sanity class's reference-policy
 * fixture.
 */
public class ApplicationReportPolicyTabRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "PolicyTabRegOrg";

  private static final String APP_NAME_PREFIX = "PolicyTabRegApp";

  private static final String SCAN_ID_PREFIX = "policyTabReg";

  private static final String LARGE_REPORT_DIR = "/canned-reports/large-report";

  private static final int LARGE_REPORT_MIN_VIOLATION_ROWS = 50;

  private Application app;

  private Organization organization;

  @BeforeEach
  public void seedAppAndLogin() {
    String suffix = TemporaryEntity.uuid();
    organization = tempEntity.newOrganization(ORG_NAME_PREFIX + "-" + suffix);
    app = tempEntity.newApplication(APP_NAME_PREFIX + "-" + suffix,
        APP_NAME_PREFIX + "-" + suffix, organization.getId());
  }

  @Test
  @Tag("regression")
  public void testPolicyTab_largeReportLoadsManyViolationRows() throws IOException {
    tempEntity.newPolicy(organization, 5, LogicalOperator.AND,
        new Condition("MatchState", "is", "exact"));

    String scanId = SCAN_ID_PREFIX + "-127-" + TemporaryEntity.uuid();
    evaluate(app, scanId, LARGE_REPORT_DIR);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, scanId));
    playwrightLogin();

    ApplicationReportPolicyTabPage policyTab = new ApplicationReportPolicyTabPage();
    new ApplicationReportPolicyTabPageAssertions(policyTab).shouldBeVisible();

    // Web-first: auto-retries until the Nth row mounts, instead of snapshotting `.count()`
    // before lazy-loaded rows have finished rendering.
    assertThat(policyTab.violationRows().nth(LARGE_REPORT_MIN_VIOLATION_ROWS - 1)).isVisible();
  }

  /**
   * Asserting "violation rows == 0" is unstable: tests in the same JVM session may leave
   * root-org policies that inherit into the new child org. The manual row's intent is
   * "report renders, no errors", which the no-error-alert check captures.
   */
  @Test
  @Tag("regression")
  public void testNoPoliciesConfigured_reportRendersWithoutError() throws IOException {
    String scanId = SCAN_ID_PREFIX + "-128-" + TemporaryEntity.uuid();
    evaluate(app, scanId, LARGE_REPORT_DIR);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, scanId));
    playwrightLogin();

    ApplicationReportPolicyTabPage policyTab = new ApplicationReportPolicyTabPage();
    new ApplicationReportPolicyTabPageAssertions(policyTab).shouldBeVisible();
    assertThat(policyTab.appReportMain()).isVisible();
    assertThat(policyTab.appReportMain().locator(".nx-alert--error")).hasCount(0);
  }

  private void evaluate(Application app, String scanId, String reportDir) throws IOException {
    URL zippedReport = ReportHelper.zipReport(reportDir, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work, Stage.ID_BUILD)
        .evaluatePolicy();
  }
}
