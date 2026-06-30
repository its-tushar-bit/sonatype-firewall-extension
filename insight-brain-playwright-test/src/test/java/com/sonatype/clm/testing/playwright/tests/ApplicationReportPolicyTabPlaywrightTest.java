/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPolicyTabPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPolicyTabPageAssertions;
import com.sonatype.clm.testing.playwright.utils.CannedReports;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Per-tab carve-out from {@link ApplicationReportPlaywrightTest}. Covers the Policy tab
 * regression scenarios: legacy-violation indicator, filter popover options (Waived + Legacy),
 * and aggregate-by-component row rendering.
 */
public class ApplicationReportPolicyTabPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "AppReportPolicyTabOrg";

  private static final String APP_NAME_PREFIX = "AppReportPolicyTabApp";

  private static final String SCAN_ID_PREFIX = "policyTab";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private static final String REFERENCE_POLICIES_RESOURCE = "/reference-policies-v3.json";

  private static final String LEGACY_TEST_POLICY_NAME = "Legacy-Indicator-Test-Policy";

  private static final int LEGACY_TEST_POLICY_THREAT_LEVEL = 7;

  /**
   * Component hash must match a component in the report's BOM — orphan violations on synthetic
   * identifiers do not render in the Application Report.
   */
  private static final String LEGACY_COMPONENT_HASH = CannedReports.LARGE_REPORT_TILES_CORE_HASH;

  private Application app;

  private String scanId;

  @Before
  public void seedAndOpen() throws IOException {
    String suffix = TemporaryEntity.uuid();
    scanId = SCAN_ID_PREFIX + "-" + suffix;

    InputStream referencePolicyStream = getClass().getResourceAsStream(REFERENCE_POLICIES_RESOURCE);
    if (referencePolicyStream == null) {
      throw new IllegalStateException("Missing classpath resource: " + REFERENCE_POLICIES_RESOURCE);
    }
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyStream, PolicyExportResult.class);

    Organization org = tempEntity.newOrganization(ORG_NAME_PREFIX + "-" + suffix);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    app = tempEntity.newApplication(APP_NAME_PREFIX + "-" + suffix,
        APP_NAME_PREFIX + "-" + suffix, org.getId());

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator.seedEvaluation(app, scanId, REPORT_DIR, tempDir, baseUrlFromTest, work, Stage.ID_BUILD);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, scanId));
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFilterPopover_showsWaivedAndLegacyOptions() {
    ApplicationReportPolicyTabPage policyTab = new ApplicationReportPolicyTabPage();
    ApplicationReportPolicyTabPageAssertions assertions =
        new ApplicationReportPolicyTabPageAssertions(policyTab);

    assertions.shouldBeVisible();
    policyTab.filterToggleButton().click();
    assertions.shouldShowFilterPopoverOpen();
    // Violation State section is collapsed by default.
    policyTab.expandViolationStateSection();
    assertions.shouldHaveViolationStateSectionExpanded();
    assertions.shouldShowFilterPopoverContainsOptions();
  }

  /**
   * Note: legacy violations seeded post-scan do not propagate into the frontend's
   * {@code allEntries} list — that requires a real re-evaluation that flags the entry as
   * legacy during scan-time grandfathering. The verifiable UI claim here is therefore the
   * Legacy filter option visibility, not a row-level legacy-tag.
   */
  @Test
  @Category(RegressionTest.class)
  public void testLegacyFilterOption_visibleWhenLegacyViolationExists() {
    Policy legacyPolicy = tempEntity.newPolicy(
        app.getOrganizationId(), LEGACY_TEST_POLICY_NAME, LEGACY_TEST_POLICY_THREAT_LEVEL);
    List<PolicyEvaluation> evaluations =
        lookup(PolicyEvaluationDAO.class).getByApplicationId(app.getId(), 1, 10);
    PolicyEvaluation evaluation = evaluations.stream()
        .filter(e -> scanId.equals(e.getScanId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Expected PolicyEvaluation for scanId=" + scanId + " on app=" + app.getId()));
    ComponentIdentifier tilesCoreId =
        ComponentIdentifier.createMavenCoordinates("org.apache.tiles", "tiles-core", "2.2.2");
    tempEntity.newLegacyPolicyViolation(evaluation, legacyPolicy, tilesCoreId,
        LEGACY_COMPONENT_HASH);

    // The page loaded in @Before before the legacy violation existed — refresh so the
    // frontend re-fetches the evaluation and the Legacy filter option becomes available.
    playwrightRefreshOrOpen(ApplicationReportPage.url(app, scanId));

    ApplicationReportPolicyTabPage policyTab = new ApplicationReportPolicyTabPage();
    ApplicationReportPolicyTabPageAssertions tabAssertions =
        new ApplicationReportPolicyTabPageAssertions(policyTab);
    tabAssertions.shouldBeVisible();
    policyTab.filterToggleButton().click();
    policyTab.expandViolationStateSection();
    tabAssertions.shouldHaveViolationStateSectionExpanded();
    assertThat(policyTab.legacyFilterOption()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAggregateByComponent_aggregatedRowExposesThreatNumber() {
    ApplicationReportPage report = new ApplicationReportPage();
    ApplicationReportPolicyTabPage policyTab = new ApplicationReportPolicyTabPage();
    ApplicationReportPolicyTabPageAssertions assertions =
        new ApplicationReportPolicyTabPageAssertions(policyTab);

    assertions.shouldBeVisible();
    report.aggregateByComponentToggle().click();

    // Playwright's web-first `isVisible()` auto-retries until the locator stabilises, so the
    // assertions below absorb the async aggregation re-render on their own — no explicit wait
    // on the toggle's checked state is needed (and the toggle's `aria-checked` doesn't track
    // the controlled React state in a way Playwright's `isChecked()` reads reliably).
    Locator firstAggregatedRow = report.violationRows().first();
    assertThat(firstAggregatedRow).isVisible();
    assertThat(policyTab.violationRowThreatNumber(firstAggregatedRow)).isVisible();
  }
}
