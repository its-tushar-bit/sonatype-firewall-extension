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
import java.util.Objects;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiversComponent;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.SeededEvaluation;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

public class ApplicationReportPlaywrightTest
    extends AbstractIqUiTest
{
  private static final AppReportData DATA =
      TestDataManager.load("application-report", AppReportData.class);

  private Application app;

  private String appName;

  private TestReportEvaluator evaluator;

  @BeforeEach
  public void seedReportAndOpen() throws IOException {
    seedDb();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    playwrightLogin();
    new ApplicationReportPageAssertions(new ApplicationReportPage())
        .shouldShowReportHeaderContaining(appName);
  }

  /**
   * Clear any {@code page.route(...)} intercepts registered by individual tests so they don't
   * bleed into siblings running in the same BrowserContext fork ({@code AbstractIqUiTest.afterTest()}
   * doesn't call this).
   */
  @AfterEach
  public void unrouteAll() {
    page.unrouteAll();
  }

  @Test
  @Tag("sanity")
  public void testSummaryIndicators() {
    ApplicationReportPage report = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssert = new ApplicationReportPageAssertions(report);

    reportAssert.shouldShowReportHeaderContaining("Build Report");

    assertThat(report.threatIndicatorsCritical()).containsText(DATA.expectedThreatCritical());
    assertThat(report.threatIndicatorsSevere()).containsText(DATA.expectedThreatSevere());
    assertThat(report.threatIndicatorsModerate()).containsText(DATA.expectedThreatModerate());
    assertThat(report.threatIndicatorsCaption()).containsText(DATA.expectedViolationsCaption());
    assertThat(report.threatIndicatorsSubCaption()).containsText(DATA.expectedViolationsSubCaption());

    assertThat(report.coverageCaption()).containsText(DATA.expectedCoverageCaption());
    assertThat(report.coverageSubCaption()).containsText(DATA.expectedCoverageSubCaption());
  }

  @Test
  @Tag("sanity")
  public void testAggregateByComponentToggle() {
    ApplicationReportPage report = new ApplicationReportPage();
    new ApplicationReportPageAssertions(report).shouldBeVisible();

    assertThat(report.violationRows()).hasCount(DATA.expectedViolationRowCount());

    report.componentFilter().fill(DATA.componentFilterTerm());
    assertThat(report.violationRows()).hasCount(DATA.expectedFilteredViolationRowCount());

    report.aggregateByComponentToggle().click();
    assertThat(report.violationRows()).hasCount(DATA.expectedFilteredTotalRowCount());

    report.componentFilter().fill("");
    assertThat(report.violationRows()).hasCount(DATA.expectedTotalRowCount());
  }

  @Test
  @Tag("sanity")
  public void testReevaluate() throws IOException {
    Policy licenseBanned = lookup(PolicyDAO.class).getByName("License-Banned").get(0);
    tempEntity.newWaiver(licenseBanned.getId(), app.getId());
    stubReevaluationEndpoint();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));

    ApplicationReportPage report = new ApplicationReportPage();
    new ApplicationReportPageAssertions(report).shouldBeVisible();

    report.componentFilter().fill("mycila");
    assertThat(report.violationRows()).hasCount(1);
    assertThat(report.violationRows().first().locator(".iq-waiver-indicator")).isHidden();

    report.triggerFullReevaluationAndWait();

    assertThat(report.componentFilter()).hasValue("mycila");
    assertThat(report.violationRows()).hasCount(1);
    assertThat(report.violationRows().first().locator(".iq-waiver-indicator")).isVisible();
  }

  @Test
  @Tag("sanity")
  public void testBackNavigation() {
    ApplicationReportPage report = new ApplicationReportPage();
    new ApplicationReportPageAssertions(report).shouldBeVisible();

    report.backButton().click();
    playwrightWaitUntilUrlContains("/reports/violations");
    assertThat(new ReportListPage().container()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_rendersTabsAndPolicyViolationTable() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    reportAssertions.shouldBeVisible();
    reportAssertions.shouldShowNavigationControls();
    reportAssertions.shouldShowOptionsDropdownLinks();
    page.keyboard().press("Escape");

    Locator violationRows = reportPage.violationRows();
    assertThat(violationRows).not().hasCount(0);

    Locator firstRow = violationRows.first();
    assertThat(reportPage.violationRowThreatNumber(firstRow)).isVisible();
    assertThat(reportPage.violationRowThreatNumber(firstRow)).hasText(Pattern.compile("\\d+"));
    assertThat(reportPage.violationRowPolicyName(firstRow)).isVisible();
    assertThat(reportPage.violationRowComponentName(firstRow)).isVisible();

    reportAssertions.shouldShowViolationsSortedByThreatDescending();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_otherTabsRenderContent() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    reportAssertions.shouldBeVisible();

    reportPage.navigateToDependencyTree();
    playwrightWaitUntilUrlContains(DATA.dependencyTreeUrlFragment());
    assertThat(reportPage.dependencyTreeContainer()).isVisible();

    page.goBack();
    reportAssertions.shouldBeVisible();

    reportPage.navigateToVulnerabilities();
    playwrightWaitUntilUrlContains(DATA.vulnerabilitiesUrlFragment());
    assertThat(reportPage.vulnerabilitiesContainer()).isVisible();

    page.goBack();
    reportAssertions.shouldBeVisible();

    reportPage.navigateToRawData();
    playwrightWaitUntilUrlContains(DATA.rawDataUrlFragment());
    assertThat(reportPage.rawDataContainer()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_viewExistingWaiversForViolation() throws IOException {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    reportAssertions.shouldBeVisible();
    reportAssertions.shouldShowViolationRows();

    seedWaiverForFirstViolationAndReevaluate();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    reportAssertions.shouldBeVisible();
    reportAssertions.shouldShowViolationRows();
    reportAssertions.shouldShowWaivedViolationsIndicator();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_vulnerabilityCustomizeNavigation() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    reportAssertions.shouldBeVisible();

    reportPage.navigateToVulnerabilities();
    playwrightWaitUntilUrlContains(DATA.vulnerabilitiesUrlFragment());
    assertThat(reportPage.vulnerabilitiesContainer()).isVisible();

    assertThat(reportPage.vulnerabilityRows().first()).isVisible();

    Locator refIdLink = reportPage.vulnerabilityRefIdLink();
    assertThat(refIdLink).isVisible();
    refIdLink.click();
    page.waitForURL(url -> url.contains("/vulnerabilities/") && !url.contains("/applicationReport/"));
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_unscannableComponentsAlertAndModal() throws IOException {
    seedReportWithUnscannableComponent();

    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.unscannableScanId()));
    reportAssertions.shouldBeVisible();

    reportPage.waitForLoadingSpinnerHidden();
    reportAssertions.shouldShowUnscannableAlert(DATA.unscannableAlertText());

    reportPage.unscannableViewButton().click();
    reportAssertions.shouldShowUnscannedComponentsModal(DATA.unscannableModalHeaderText());

    reportPage.unscannedComponentsModalCloseButton().click();
    assertThat(reportPage.unscannedComponentsModal()).not().isVisible();
  }

  private void seedReportWithUnscannableComponent() throws IOException {
    URL zippedReport = ReportHelper.zipReport(DATA.unscannableReportDir(), tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, DATA.unscannableScanId(), zippedReport, baseUrlFromTest, work, Stage.ID_BUILD)
        .evaluatePolicy();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_compatibilityWarnings() {
    ApplicationReportPage reportPage = new ApplicationReportPage();

    page.navigate("about:blank");

    page.route("**/browseReport/**", route -> {
      String url = route.request().url();
      if (url.contains("/policythreats.json")) {
        route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("{\"version\": 2, \"aaData\": []}"));
      }
      else if (url.contains("/dependencies.json")) {
        route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("{\"dependencyTree\": null}"));
      }
      else {
        route.resume();
      }
    });

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);
    reportAssertions.shouldBeVisible();

    reportPage.waitForLoadingSpinnerHidden();

    assertThat(reportPage.policyTypeFilterWarning()).isVisible();
    assertThat(reportPage.policyTypeFilterWarning()).containsText(DATA.policyTypeFilterWarningText());

    assertThat(reportPage.oldReportWarning()).isVisible();
    assertThat(reportPage.oldReportWarning()).containsText(DATA.oldReportWarningText());
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_reevaluationErrors() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    reportAssertions.shouldBeVisible();

    page.route("**/rest/report/**/reevaluatePolicy**", route -> {
      route.fulfill(new Route.FulfillOptions()
          .setStatus(403)
          .setContentType("application/json")
          .setBody("{\"message\": \"Insufficient permissions\"}"));
    });

    assertThat(reportPage.reevaluateButton()).isVisible();
    reportPage.reevaluateButton().click();

    assertThat(reportPage.reevaluationOptionsModal()).isVisible();
    reportPage.fullReevaluateButton().click();

    reportAssertions.shouldShowReevaluationErrorWithoutModal(DATA.insufficientPermissionsError());
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_backButtonContextDependentLabel() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);

    reportAssertions.shouldBeVisible();
    reportAssertions.shouldShowBackButtonWithText(DATA.backButtonDefaultText());

    String prioritiesUrl = "/assets/index.html#/applicationReport/" + app.getPublicId() +
        "/" + DATA.scanId() + "/policy?origin=prioritiesPage";
    playwrightRefreshOrOpen(prioritiesUrl);
    reportAssertions.shouldBeVisible();
    assertThat(reportPage.backButton()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_orgLevelWaiverAppliesToAllAppsInOrg() throws IOException {
    String suffix = TemporaryEntity.uuid();
    String app2Name = DATA.applicationNamePrefix() + "-2-" + suffix;
    String orgId = app.getOrganizationId();
    Application app2 = tempEntity.newApplication(app2Name, app2Name, orgId);

    URL zippedReport2 = ReportHelper.zipReport(DATA.reportDir(), tempDir);
    InsightWork work2 = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    String scanId2 = TemporaryEntity.uuid().replace("-", "").substring(0, 32);
    TestReportEvaluator evaluator2 = new TestReportEvaluator(app2, scanId2, zippedReport2,
        baseUrlFromTest, work2, Stage.ID_BUILD);
    evaluator2.evaluatePolicy();

    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    List<PolicyViolation> violations = dao.getByOwnerId(app.getId());
    PolicyViolation target = violations.stream()
        .filter(v -> v.getHash() != null && v.getPolicyId() != null)
        .findFirst()
        .orElseThrow();
    dao.loadConstraintFacts(Collections.singletonList(target));

    tempEntity.newWaiver(new PolicyWaiver()
        .setHash(target.getHash())
        .setPolicyId(target.getPolicyId())
        .setOwnerId(orgId)
        .setConstraintFacts(target.getConstraintFacts())
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("Org-level waiver regression check"));

    evaluator.reevaluatePolicy();
    evaluator2.reevaluatePolicy();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    ApplicationReportPage reportA = new ApplicationReportPage();
    new ApplicationReportPageAssertions(reportA).shouldBeVisible();
    assertThat(reportA.firstWaivedViolationsIndicator()).isVisible();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app2, scanId2));
    ApplicationReportPage reportB = new ApplicationReportPage();
    new ApplicationReportPageAssertions(reportB).shouldBeVisible();
    assertThat(reportB.firstWaivedViolationsIndicator()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testApplicationReport_deleteWaiverRestoresViolation() throws IOException {
    seedWaiverForFirstViolationAndReevaluate();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssertions = new ApplicationReportPageAssertions(reportPage);
    reportAssertions.shouldBeVisible();
    reportAssertions.shouldShowWaivedViolationsIndicator();

    playwrightRefreshOrOpen(DashboardPage.urlToWaivers());
    new DashboardPage().waitUntilSpinnersGone();
    DashboardWaiversComponent waiversTable = new DashboardWaiversComponent();
    waiversTable.waivers().first().waitFor();
    waiversTable.waiver(0).click();
    playwrightWaitUntilUrlContains("/waiver/");
    WaiverDetailsPage detailsPage = new WaiverDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    detailsPage.deleteWaiverAndConfirm();

    evaluator.reevaluatePolicy();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    ApplicationReportPage reportAfterDelete = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAfterDeleteAssertions =
        new ApplicationReportPageAssertions(reportAfterDelete);
    reportAfterDeleteAssertions.shouldBeVisible();
    reportAfterDeleteAssertions.shouldShowViolationRows();
    assertThat(reportAfterDelete.waivedViolationsIndicator()).hasCount(0);
  }

  @Test
  @Tag("regression")
  public void testComponentDetails_fromApplicationReport_withViolationPopover() throws IOException {
    PolicyEvaluationSeeder seeder = new PolicyEvaluationSeeder(
        tempEntity, tempDir, testCLMServer.getCLMServer().getConfiguration(),
        baseUrlFromTest, SmallReportFixture.CANNED_REPORT_DIR);
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        "CMCDOrg", "CMCDApp", "cm-cd", "cm-cd-s", "CMCDPol", "c",
        "MatchState", "is", "exact", 7);

    stubComponentHdsEndpoints();

    navigateAndWaitForUrl(
        ApplicationReportPage.url(seeded.app(), seeded.scanId()), "/applicationReport/");
    ApplicationReportPage reportPage = new ApplicationReportPage();
    assertThat(reportPage.appReportMain()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    reportPage.openFirstComponentFromReport();
    playwrightWaitUntilUrlContains("/componentDetails/");

    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    assertThat(detailsPage.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(detailsPage.headerTitle()).isVisible();
    assertThat(detailsPage.overviewComponentInformationTile()).isVisible();

    detailsPage.clickComponentDetailsTab("Policy Violations");
    playwrightWaitUntilUrlContains("/violations");

    assertThat(detailsPage.policyViolationsTable()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    detailsPage.policyViolationRows().first().waitFor();
    detailsPage.policyViolationRows().first().click();

    assertThat(detailsPage.popoverViolationPage()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    detailsPage.popoverCloseButton().click();
    detailsPage.policyViolationDetailsPopover()
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

    detailsPage.navigateBackToApplicationReport();
    playwrightWaitUntilUrlContains("/policy");
    assertThat(reportPage.appReportMain()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  /**
   * In embedded/popover mode the ViolationPage renders with {@code isFromPolicyViolations=true},
   * which drops the MenuBarBackButton and applies the {@code iq-violation-details-popover-section}
   * layout class. Verifies:
   * <ul>
   * <li>No page-level back button inside the popover's {@code #violation-page} container</li>
   * <li>Popover section class is applied</li>
   * <li>Applicable Waivers and Similar Waivers tabs render — Vulnerability Details is
   * security-policy-only and this test seeds a License-Banned violation, so that tab is
   * deliberately not asserted (see body comment on the tab assertions)</li>
   * </ul>
   */
  @Test
  @Tag("regression")
  public void testComponentDetails_violationPopoverHasNoBackButtonAndAllThreeTabsPresent() throws IOException {
    PolicyEvaluationSeeder seeder = new PolicyEvaluationSeeder(
        tempEntity, tempDir, testCLMServer.getCLMServer().getConfiguration(),
        baseUrlFromTest, SmallReportFixture.CANNED_REPORT_DIR);
    // No manual UUID suffix required — PolicyEvaluationSeeder#seedSingleConditionAndEvaluate
    // already appends TemporaryEntity.uuid() to each prefix internally, so the seeded names are
    // parallel-fork-safe (SONATYPE-001) without further mangling. Adding a second UUID pushed
    // the policy name past the 60-char InvalidPolicyException limit.
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        "PopoverOrg", "PopoverApp", "popover-pub", "popover-scan",
        "PopoverPol", "c", "MatchState", "is", "exact", 7);

    stubComponentHdsEndpoints();

    navigateAndWaitForUrl(
        ApplicationReportPage.url(seeded.app(), seeded.scanId()), "/applicationReport/");
    ApplicationReportPage reportPage = new ApplicationReportPage();
    assertThat(reportPage.appReportMain()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    reportPage.openFirstComponentFromReport();
    playwrightWaitUntilUrlContains("/componentDetails/");

    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    detailsPage.clickComponentDetailsTab("Policy Violations");
    playwrightWaitUntilUrlContains("/violations");

    detailsPage.policyViolationRows().first().waitFor();
    detailsPage.policyViolationRows().first().click();

    // Positive scope first (popover mounted + its section class applied) so the count-0 below
    // cannot trivially pass by locating nothing at all — if the popover were misconfigured or
    // renamed, the two isVisible assertions above would fail before we get here.
    assertThat(detailsPage.popoverViolationPage()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(detailsPage.popoverSection()).isVisible();
    assertThat(detailsPage.popoverPageLevelBackButton()).hasCount(0);
    // "Applicable Waivers" appends a dynamic count suffix ("Applicable Waivers (N)") — regex
    // matches the stable prefix. "Similar Waivers" doesn't get a suffix so an exact string works.
    // Do NOT normalise both to exact strings; the regex is deliberate.
    // Vulnerability Details is security-policy-only (see ViolationPage.jsx
    // `shouldShowVulnerabilityTab`) and the seeded policy here is a License-Banned violation,
    // so we don't assert it.
    assertThat(detailsPage.popoverTab(Pattern.compile("Applicable Waivers"))).isVisible();
    assertThat(detailsPage.popoverTab("Similar Waivers")).isVisible();
  }

  private void stubComponentHdsEndpoints() {
    URL componentDetailsResource = Objects.requireNonNull(
        getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"),
        "test resource not found: /componentDetails/javancssComponentDetails-29.50.json");
    testCLMServer.getHdsServer()
        .respondWith(componentDetailsResource)
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  private void stubReevaluationEndpoint() throws IOException {
    URL zippedReport = ReportHelper.zipReport(DATA.reportDir(), tempDir);
    testCLMServer.getHdsServer()
        .respondWith(zippedReport)
        .atUri("rest/application/analysis/" + HdsMockServer.RestServlet.SCAN_ID);
  }

  private void seedDb() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);

    String suffix = TemporaryEntity.uuid();
    String orgName = DATA.organizationNamePrefix() + "-" + suffix;
    appName = DATA.applicationNamePrefix() + "-" + suffix;
    String username = DATA.userPrefix() + "-" + suffix;
    String email = username + "@" + DATA.userEmailDomain();

    Organization org = tempEntity.newOrganization(orgName);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    tempEntity.newUser(username, DATA.userFirstName(), DATA.userLastName(), email);
    app = tempEntity.newApplication(appName, appName, org.getId(), username);

    URL zippedReport = ReportHelper.zipReport(DATA.reportDir(), tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, DATA.scanId(), zippedReport, baseUrlFromTest,
        work, Stage.ID_BUILD);
    evaluator.evaluatePolicy();
  }

  private PolicyViolation seedWaiverForFirstViolationAndReevaluate() throws IOException {
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    List<PolicyViolation> violations = dao.getByOwnerId(app.getId());
    PolicyViolation target = violations.stream()
        .filter(v -> v.getHash() != null && v.getPolicyId() != null)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No suitable PolicyViolation found after canned-report evaluation for app="
                + app.getId()));
    dao.loadConstraintFacts(Collections.singletonList(target));

    tempEntity.newWaiver(new PolicyWaiver()
        .setHash(target.getHash())
        .setPolicyId(target.getPolicyId())
        .setOwnerId(app.getId())
        .setConstraintFacts(target.getConstraintFacts())
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("Seeded for testApplicationReport_viewExistingWaiversForViolation"));

    evaluator.reevaluatePolicy();
    return target;
  }

  private record AppReportData(
      String organizationNamePrefix,
      String applicationNamePrefix,
      String userPrefix,
      String userFirstName,
      String userLastName,
      String userEmailDomain,
      String scanId,
      String reportDir,
      String unscannableScanId,
      String unscannableReportDir,
      String expectedThreatCritical,
      String expectedThreatSevere,
      String expectedThreatModerate,
      String expectedViolationsCaption,
      String expectedViolationsSubCaption,
      String expectedCoverageCaption,
      String expectedCoverageSubCaption,
      int expectedViolationRowCount,
      int expectedTotalRowCount,
      String componentFilterTerm,
      int expectedFilteredViolationRowCount,
      int expectedFilteredTotalRowCount,
      String backButtonDefaultText,
      String unscannableAlertText,
      String unscannableModalHeaderText,
      String policyTypeFilterWarningText,
      String oldReportWarningText,
      String insufficientPermissionsError,
      String dependencyTreeUrlFragment,
      String vulnerabilitiesUrlFragment,
      String rawDataUrlFragment)
  {
  }
}
