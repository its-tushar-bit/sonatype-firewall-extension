/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.SeededEvaluation;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Cross-screen navigation around an evaluated MatchState policy: Reports list, status bar, report rows. */
public class PolicyEvaluationReportNavigationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CONDITION_TYPE_ID = "MatchState";

  private static final String OPERATOR_IS = "is";

  private static final String VALUE_EXACT = "exact";

  private static final String ORG_NAME_PREFIX = "PolicyEvalReportNavOrg";

  private static final String APP_NAME_PREFIX = "PolicyEvalReportNavApp";

  private static final String APP_ID_PREFIX = "policy-eval-rn";

  private static final String SCAN_ID_PREFIX = "pern-scan";

  private static final String POLICY_NAME_PREFIX = "policy-eval-rn";

  private static final String CONSTRAINT_SUFFIX = "constraint";

  private static final String CANNED_REPORT_DIR = SmallReportFixture.CANNED_REPORT_DIR;

  private static final String COMPONENT_JETTY = SmallReportFixture.COMPONENT_JETTY;

  private static final String COMPONENT_GERONIMO = SmallReportFixture.COMPONENT_GERONIMO;

  private static final int EXACT_COMPONENT_COUNT = SmallReportFixture.EXACT_COMPONENT_COUNT;

  private static final int SEVERE_THREAT_LEVEL = 7;

  private PolicyEvaluationSeeder seeder;

  private ApplicationReportPage reportPage;

  private ApplicationReportPageAssertions reportAssertions;

  @BeforeEach
  public void initSeederAndAssertions() {
    seeder = new PolicyEvaluationSeeder(
        tempEntity, tempDir, testCLMServer.getCLMServer().getConfiguration(),
        baseUrlFromTest, CANNED_REPORT_DIR);
    reportPage = new ApplicationReportPage();
    reportAssertions = new ApplicationReportPageAssertions(reportPage);
  }

  /** Reports list shows the evaluated app; clicking its build-stage link lands on the Application Report. */
  @Test
  @Tag("regression")
  public void testEvaluatedApplication_appearsInReportsListAndLinksBackToReport() throws IOException {
    SeededEvaluation seeded = seedAndEvaluate();

    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();

    ReportListPage reportList = new ReportListPage();
    assertThat(reportList.container()).isVisible();
    Locator appRow = reportList.rows()
        .filter(new Locator.FilterOptions().setHasText(seeded.app().getName()))
        .first();
    assertThat(appRow).isVisible();

    Locator buildLink = reportList.buildReportLinkOf(appRow);
    assertThat(buildLink).isVisible();
    buildLink.click();
    reportAssertions.shouldShowReportHeaderContaining(seeded.app().getName());
  }

  /**
   * Status-bar severe counter == 12 for "MatchState is exact" at threat 7 against small-report, and
   * the report-side rows render jetty + geronimo under that same policy. Row-presence is folded
   * in here (rather than a separate test) because the per-condition row coverage already lives in
   * {@code PolicyEvaluationMatchStatePlaywrightTest.testIsExact_flagsAllExactComponents}; this
   * class's distinct value is the cross-screen navigation around the evaluated app.
   */
  @Test
  @Tag("regression")
  public void testStatusBar_showsViolationCountsBucketedByCategory() throws IOException {
    SeededEvaluation seeded = seedAndEvaluate();

    playwrightRefreshOrOpen(ApplicationReportPage.url(seeded.app(), seeded.scanId()));
    playwrightLogin();
    reportAssertions.shouldShowReportHeaderContaining(seeded.app().getName());

    assertThat(reportPage.threatIndicatorsSevere()).containsText(String.valueOf(EXACT_COMPONENT_COUNT));
    assertThat(reportPage.threatIndicatorsSubCaption())
        .containsText("Affecting " + EXACT_COMPONENT_COUNT + " component");
    reportAssertions.shouldShowViolationCountForPolicy(EXACT_COMPONENT_COUNT, seeded.policyName());
    reportAssertions.shouldShowViolationRow(COMPONENT_JETTY, SEVERE_THREAT_LEVEL, seeded.policyName());
    reportAssertions.shouldShowViolationRow(COMPONENT_GERONIMO, SEVERE_THREAT_LEVEL, seeded.policyName());
  }

  private SeededEvaluation seedAndEvaluate() throws IOException {
    return seeder.seedSingleConditionAndEvaluate(
        ORG_NAME_PREFIX, APP_NAME_PREFIX, APP_ID_PREFIX, SCAN_ID_PREFIX, POLICY_NAME_PREFIX,
        CONSTRAINT_SUFFIX, CONDITION_TYPE_ID, OPERATOR_IS, VALUE_EXACT, SEVERE_THREAT_LEVEL);
  }
}
