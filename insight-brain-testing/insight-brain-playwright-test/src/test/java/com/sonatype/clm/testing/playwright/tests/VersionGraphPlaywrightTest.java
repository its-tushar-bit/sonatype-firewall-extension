/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Playwright test for the Version Graph.
 * <p>
 * Each test follows a Given/When/Then shape:
 * <ul>
 * <li>{@link #seedAppPolicyAndOpenDashboardAsAdmin()} seeds the per-test {@link Organization} +
 * {@link Application} + security {@link Policy}, evaluates the canned report, and lands on
 * the dashboard logged-in as admin.</li>
 * <li>The test body stubs the per-component HDS responses, navigates to the application report,
 * opens a specific row by index, and asserts the Component Details Overview tab via
 * {@link ComponentDetailsPage}.</li>
 * </ul>
 *
 * <p>
 * Selectors live in {@link ApplicationReportPage} / {@link ComponentDetailsPage}.
 */
public class VersionGraphPlaywrightTest
    extends AbstractIqUiTest
{

  private static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private static final String ORGANIZATION_NAME = "ApplicationReportTest";

  private static final String APPLICATION_ID = "8bbaa746602142d9adf2de00a9ca4d4a";

  private static final String APPLICATION_NAME = "ApplicationReportTest";

  private static final String APPLICATION_INTERNAL_NAME = "ApplicationReportTest";

  private static final String CANNED_REPORT_CLASSPATH_DIR = "/canned-reports/version-graph-test-report";

  private static final PolicySpec POLICY = new PolicySpec(
      "SecurityPolicy", 10, "SecurityVulnerabilitySeverity", ">", "7");

  private static final java.util.Map<String, ComponentScenario> COMPONENT_SCENARIOS = java.util.Map.of(
      "debugComponent", new ComponentScenario(
          0, true, "3.0.0", "3.2.7",
          new HdsStub("rest/ci/componentDetails", "/componentDetails/debugComponentDetails-3.2.7.json"),
          List.of(
              new HdsStub("rest/ci/componentDetails", "/componentDetails/debugComponentDetails-3.0.0.json"),
              new HdsStub("rest/ci/componentDetails/list", "/componentDetails/debugComponentVersionsList.json"))),
      "postgresqlComponent", new ComponentScenario(
          1, false, null, null, null,
          List.of(
              new HdsStub("rest/ci/componentDetails", "/componentDetails/postgresqlComponentDetails-42.2.2.json"),
              new HdsStub("rest/ci/componentDetails/list",
                  "/componentDetails/postgresqlComponentVersionsList.json"))));

  private static ComponentScenario componentScenario(String key) {
    ComponentScenario scenario = COMPONENT_SCENARIOS.get(key);
    if (scenario == null) {
      throw new IllegalArgumentException(
          "Unknown component scenario '" + key + "' — known keys: " + COMPONENT_SCENARIOS.keySet());
    }
    return scenario;
  }

  private Application app;

  // --------------- @Before / @After / setup ---------------

  /**
   * Reset the inner-source transitive-waiver feature flag to its default ({@code true}) on both
   * sides of every test so a flag flip does not leak into other tests sharing the same fork.
   * See PLAYWRIGHT_TEST_AUTHORING_GUIDE.md §3b ("Combined {@code @Before @After}") and §7b.
   */
  @BeforeEach
  @AfterEach
  public void resetInnerSourceTransitiveWaiverFlag() {
    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.setEnabled(true);
  }

  @BeforeEach
  public void seedAppPolicyAndOpenDashboardAsAdmin() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.setEnabled(false);

    Organization org = tempEntity.newOrganization(ORGANIZATION_NAME);
    app = tempEntity.newApplicationWithSpecificId(
        APPLICATION_ID, APPLICATION_NAME, APPLICATION_INTERNAL_NAME, org.getId());

    seedPolicy(app.getId(), POLICY);
    evaluateCannedReport(app, SCAN_ID, CANNED_REPORT_CLASSPATH_DIR);

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  // --------------- @Test methods ---------------
  //
  // {@code debugComponent_compareVersionsTable} ports the legacy Selenide
  // {@code VersionGraphTest#testVersionGraph_debugComponent_CompareVersionButton}, exercising
  // the full Version Explorer → Recommended Versions → Compare Versions flow.

  @Test
  @Tag("sanity")
  public void testVersionGraph_debugComponent_versionExplorerTileVisible() {
    runComponentScenario("debugComponent");
  }

  @Test
  @Tag("sanity")
  public void testVersionGraph_postgresqlComponent_noVersionExplorerTileWhenHdsHasNoGraph() {
    runComponentScenario("postgresqlComponent");
  }

  @Test
  @Tag("sanity")
  public void testVersionGraph_debugComponent_compareVersionsTable() {
    ComponentScenario scenario = openScenarioOverview("debugComponent");

    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);
    detailsAssertions.shouldShowOverviewForVersionGraph(scenario.expectVersionExplorerTile());

    // CRITICAL: wait for the Recommended Versions list to render BEFORE re-stubbing
    // {@code rest/ci/componentDetails}. The list is rendered inside RiskRemediation.jsx's
    // {@code NxLoadWrapper}, so its appearance is the observable signal that the initial
    // {@code loadVersionExplorerData()} fetch has completed and {@code currentVersionDetails}
    // is cached in redux. If we swap the stub before this fetch returns, the initial fetch
    // resolves with the recommended-version response and the "current" column ends up showing
    // the recommended version too (root cause of CLM-xxxxx flake — see legacy
    // {@code VersionGraphTest#testVersionGraph_debugComponent_CompareVersionButton}).
    detailsAssertions.shouldShowRecommendedVersionsList();

    // Re-stub componentDetails with the recommended version response so the Compare click
    // produces a populated "recommended" column (mirrors the legacy mock swap before click).
    HdsStub recommended = scenario.recommendedVersionHdsStub();
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource(recommended.resourcePath()))
        .atUri(recommended.uri());

    detailsPage.compareRecommendationAndAssertVersions(scenario.currentVersion(), scenario.recommendedVersion());
  }

  // --------------- shared scenario flow ---------------

  /**
   * Stubs the per-component HDS responses, opens the configured row from the application report,
   * and asserts the Overview tab loaded. Optionally also asserts the Version Explorer tile when
   * the scenario expects one (controlled by {@link ComponentScenario#expectVersionExplorerTile()}).
   */
  private void runComponentScenario(String scenarioKey) {
    ComponentScenario scenario = openScenarioOverview(scenarioKey);
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    new ComponentDetailsPageAssertions(detailsPage)
        .shouldShowOverviewForVersionGraph(scenario.expectVersionExplorerTile());
  }

  /**
   * Stubs per-scenario HDS responses, opens the application report and the configured row.
   * Returns the resolved {@link ComponentScenario} so callers can drive further interactions.
   */
  private ComponentScenario openScenarioOverview(String scenarioKey) {
    ComponentScenario scenario = componentScenario(scenarioKey);
    stubComponentHdsEndpoints(scenario.hdsStubs());

    ApplicationReportPage reportPage = new ApplicationReportPage();
    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.openComponentFromReportRow(scenario.rowIndex());
    return scenario;
  }

  // --------------- backend/HDS helpers ---------------

  private void seedPolicy(String ownerId, PolicySpec spec) {
    Policy policy = new Policy(null, spec.name());
    policy.setThreatLevel(spec.threatLevel());
    policy.setOwnerId(ownerId);

    Constraint constraint = new Constraint(null, spec.name() + " constraint", LogicalOperator.AND);
    constraint.setConditions(Collections.singletonList(
        new Condition(spec.conditionTypeId(), spec.operator(), spec.value())));
    policy.setConstraints(Collections.singletonList(constraint));

    tempEntity.newPolicy(policy);
  }

  private void evaluateCannedReport(Application application, String scanId, String classpathDir) throws IOException {
    URL zippedReport = ReportHelper.zipReport(classpathDir, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(application, scanId, zippedReport, baseUrlFromTest, work).evaluatePolicy();
  }

  private void stubComponentHdsEndpoints(List<HdsStub> stubs) {
    for (HdsStub stub : stubs) {
      testCLMServer.getHdsServer()
          .respondWith(getClass().getResource(stub.resourcePath()))
          .atUri(stub.uri());
    }
    // All version-graph scenarios share the same empty dependency-tree stub.
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  // --------------- Test data records ---------------

  /** Spec for the security policy seeded in {@code @Before}. */
  private record PolicySpec(String name, int threatLevel, String conditionTypeId, String operator, String value)
  {
  }

  /**
   * Per-component scenario — which row to click, which HDS endpoints to stub, and (for
   * scenarios that exercise Compare Versions) the recommended-version swap and expected
   * version cells. {@code currentVersion}, {@code recommendedVersion}, and
   * {@code recommendedVersionHdsStub} are nullable for scenarios that only assert
   * tile presence/absence.
   */
  private record ComponentScenario(
      int rowIndex,
      boolean expectVersionExplorerTile,
      String currentVersion,
      String recommendedVersion,
      HdsStub recommendedVersionHdsStub,
      List<HdsStub> hdsStubs)
  {
  }

  /** Single HDS endpoint stub: respond with the JSON resource at {@code resourcePath} on {@code uri}. */
  private record HdsStub(String uri, String resourcePath)
  {
  }
}
