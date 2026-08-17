/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.SeededEvaluation;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Carve-out from {@link ApplicationReportPlaywrightTest} for the policy-change-to-report
 * reflection scenario. Verifies that a policy threat-level change (5 → 9) is reflected in
 * a subsequent evaluation's application report.
 * <p>
 * Cannot be merged into {@link ApplicationReportPlaywrightTest} because that class's
 * {@code @Before} seeds a canned report fixture and navigates directly to the report page —
 * incompatible with this test's Dashboard-first {@code @Before} and live
 * {@link PolicyEvaluationSeeder} setup.
 */
public class ApplicationReportPolicyChangePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_PREFIX = "PCROrg";

  private static final String APP_PREFIX = "PCRApp";

  private static final String APP_ID_PREFIX = "pcr-app";

  private static final String SCAN_ID_PREFIX = "pcr-s";

  private static final String POLICY_PREFIX = "PCRPol";

  private static final String CONSTRAINT_SUFFIX = "c";

  private static final String CONDITION_TYPE_ID = "MatchState";

  private static final String OPERATOR_IS = "is";

  private static final String VALUE_EXACT = "exact";

  private static final int INITIAL_THREAT_LEVEL = 5;

  private static final int UPDATED_THREAT_LEVEL = 9;

  private static final String UPDATED_THREAT_LABEL = "Critical";

  private static final String COMPONENT_NAME = SmallReportFixture.COMPONENT_JETTY;

  private PolicyEvaluationSeeder seeder;

  @BeforeEach
  public void setupSeederAndLogin() {
    seeder = new PolicyEvaluationSeeder(
        tempEntity, tempDir, testCLMServer.getCLMServer().getConfiguration(),
        baseUrlFromTest, SmallReportFixture.CANNED_REPORT_DIR);
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("regression")
  public void testPolicyThreatLevelChange_reflectedInSubsequentEvaluationReport() throws IOException {
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        ORG_PREFIX, APP_PREFIX, APP_ID_PREFIX, SCAN_ID_PREFIX + "1", POLICY_PREFIX, CONSTRAINT_SUFFIX,
        CONDITION_TYPE_ID, OPERATOR_IS, VALUE_EXACT, INITIAL_THREAT_LEVEL);

    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    navigateAndWaitForUrl(
        ApplicationReportPage.url(seeded.app(), seeded.scanId()), "/applicationReport/");
    assertThat(reportPage.appReportMain()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    reportAssertions.shouldShowViolationRow(COMPONENT_NAME, INITIAL_THREAT_LEVEL, seeded.policyName());

    PolicyEditorPage editorPage = new PolicyEditorPage();
    navigateAndWaitForUrl(
        PolicyEditorPage.url(seeded.app(), seeded.policy()), PolicyEditorPage.EDIT_URL_FRAGMENT);
    assertThat(editorPage.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    editorPage.selectThreatLevel(UPDATED_THREAT_LEVEL, UPDATED_THREAT_LABEL);
    editorPage.clickSubmit();
    new PolicyEditorPageAssertions(editorPage).shouldShowSaveSuccessMask();

    String scanId2 = SCAN_ID_PREFIX + "2-" + TemporaryEntity.uuid();
    seeder.newEvaluator(seeded.app(), scanId2).evaluatePolicy();

    navigateAndWaitForUrl(
        ApplicationReportPage.url(seeded.app(), scanId2), "/applicationReport/");
    assertThat(reportPage.appReportMain()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    reportAssertions.shouldShowViolationRow(COMPONENT_NAME, UPDATED_THREAT_LEVEL, seeded.policyName());
  }
}
