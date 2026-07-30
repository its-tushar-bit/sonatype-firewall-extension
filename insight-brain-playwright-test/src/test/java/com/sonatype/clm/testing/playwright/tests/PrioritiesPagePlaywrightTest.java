/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPage;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.microsoft.playwright.Route;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression tests for the Developer Priorities page. */
public class PrioritiesPagePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "AppReportTestOrg";

  private static final String APP_NAME_PREFIX = "AppReportTestApp";

  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private Application app;

  private String appName;

  private TestReportEvaluator evaluator;

  @Before
  public void prepareApp() throws IOException {
    seedDb();
  }

  @After
  public void resetContextState() {
    // BrowserContext is reused across tests in the same fork — clear clipboard grants and any
    // page.route intercepts to avoid leaking them into sibling tests.
    context.clearPermissions();
    page.unrouteAll();
  }

  private void openPrioritiesPage() {
    playwrightRefreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID));
    playwrightLogin();
    new PrioritiesPageAssertions(new PrioritiesPage()).shouldBeVisible();
  }

  /**
   * Re-opens the priorities page on the same session, scoped to a single component via the
   * {@code componentNameFilter} route param so the seeded row is visible regardless of
   * sort/pagination. Use after the initial {@link #openPrioritiesPage()} + re-evaluation.
   */
  private PrioritiesPageAssertions reopenPrioritiesPageFilteredBy(String componentNameFilter) {
    playwrightRefreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID, componentNameFilter));
    PrioritiesPageAssertions assertions = new PrioritiesPageAssertions(new PrioritiesPage());
    assertions.shouldBeVisible();
    return assertions;
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_expiredWaiverShowsQuestionCircleIcon() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();

    Date pastExpiry = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    tempEntity.newWaiver(
        violation.getHash(),
        violation.getPolicyId(),
        app.getId(),
        "Expired waiver regression check",
        pastExpiry);

    evaluator.reevaluatePolicy();

    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = reopenPrioritiesPageFilteredBy(artifactId);

    assertions.shouldShowExpiredWaiverIconOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_soonToExpireWaiverShowsWarningTriangleIcon() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation representative = firstSeedableViolation();
    // Soon-to-expire icon requires isAllViolationsWaived=true. Waive every violation on the
    // component hash (deduped by policyId — waiver's unique key is hash+policyId+ownerId).
    List<PolicyViolation> componentViolations = uniqueViolationsForHash(representative.getHash());

    Date nearFutureExpiry = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
    for (PolicyViolation v : componentViolations) {
      tempEntity.newWaiver(
          v.getHash(),
          v.getPolicyId(),
          app.getId(),
          "Soon-to-expire waiver regression check",
          nearFutureExpiry);
    }

    evaluator.reevaluatePolicy();

    String artifactId = representative.getComponentIdentifier().getCoordinates().get("artifactId");
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = reopenPrioritiesPageFilteredBy(artifactId);

    assertions.shouldShowSoonToExpireWaiverIconOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_rendersWithHeaderAndTitleForNoBranch() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PrioritiesPageAssertions assertions = new PrioritiesPageAssertions(new PrioritiesPage());
    assertions.shouldShowPageHeader();
    assertions.shouldShowBreadcrumbLink("Priorities");
    assertions.shouldHaveHeaderTitleText(appName + " - Priorities");
    assertions.shouldShowTableColumnHeaders();
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_headerShowsMetadataAndViewDropdown() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = new PrioritiesPageAssertions(prioritiesPage);

    assertions.shouldShowPageHeader();
    prioritiesPage.openViewDropdown();
    assertions.shouldShowViewDropdownLinks();
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_commitCopyButtonShowsCheckIconAfterClick() throws IOException {
    // Scope clipboard permissions to this test: BrowserContext is reused across tests in the
    // same fork, so revoke in finally to avoid leaking the grant to siblings even if this test
    // fails. @After.clearPermissions() is a belt-and-braces backstop.
    context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    try {
      evaluator.evaluatePolicy();
      openPrioritiesPage();

      PrioritiesPage prioritiesPage = new PrioritiesPage();
      assertThat(prioritiesPage.commitCopyButton()).isVisible();
      prioritiesPage.commitCopyButton().click();
      // Hover after click because NxTooltip only mounts its title portal on hover — click alone
      // triggers the copy action but does not surface the "Copied" tooltip text.
      prioritiesPage.commitCopyButton().hover();
      assertThat(prioritiesPage.commitCopyTooltipCopiedText()).isVisible();
    }
    finally {
      context.clearPermissions();
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_filterInputAndFailWarnToggleUpdateUrlParams() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();
    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");

    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = new PrioritiesPageAssertions(prioritiesPage);

    prioritiesPage.componentFilterInput().fill(artifactId);
    assertions.shouldHaveComponentNameFilterUrlParam(artifactId);
    assertThat(prioritiesPage.rows().first()).isVisible();

    prioritiesPage.failWarnToggleLabel().click();
    assertions.shouldHaveFilterOnPolicyActionsUrlParamOn();
    assertThat(prioritiesPage.failWarnToggleInput())
        .isChecked();
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_fullyWaivedRowShowsWaivedLabel() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation representative = firstSeedableViolation();
    List<PolicyViolation> componentViolations = uniqueViolationsForHash(representative.getHash());

    Date farFutureExpiry = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));
    for (PolicyViolation v : componentViolations) {
      tempEntity.newWaiver(v.getHash(), v.getPolicyId(), app.getId(),
          "Fully-waived row label regression check", farFutureExpiry);
    }

    evaluator.reevaluatePolicy();

    String artifactId = representative.getComponentIdentifier().getCoordinates().get("artifactId");
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = reopenPrioritiesPageFilteredBy(artifactId);
    assertions.shouldShowFullyWaivedLabelOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_recommendationCellShowsWaiveViolationsForUnknownReachability() throws IOException {
    evaluator.evaluatePolicy();
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.stubNoUpgradeRecommendations();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();
    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");
    PrioritiesPageAssertions assertions = reopenPrioritiesPageFilteredBy(artifactId);
    assertions.shouldShowWaiveViolationsRecommendationOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_dependencyIndicatorsRenderInComponentCell() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    new PrioritiesPageAssertions(new PrioritiesPage()).shouldShowAtLeastOneDependencyIndicatorInTable();
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_licenseLockScreenShownWhenDeveloperDashboardDisabled() throws IOException {
    evaluator.evaluatePolicy();
    page.route(Pattern.compile(".*/rest/product/features([?#][^/]*)?$"),
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("[]")));

    playwrightRefreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID));
    playwrightLogin();
    // Explicit reload is load-bearing (not a double-nav): the SPA cached productFeatures at first
    // load, so we reload to force `fetchProductFeaturesIfNeeded` to re-fire through the intercept.
    // Same pattern used in OrganizationRegressionPlaywrightTest#testLegacyViolationsLicenseGate*.
    page.reload();

    new PrioritiesPageAssertions(new PrioritiesPage()).shouldShowLicenseLockScreen();
  }

  private PolicyViolation firstSeedableViolation() {
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    List<PolicyViolation> violations = dao.getByOwnerId(app.getId());
    return violations.stream()
        .filter(v -> v.getHash() != null && v.getPolicyId() != null
            && v.getComponentIdentifier() != null
            && v.getComponentIdentifier().getCoordinates() != null
            && v.getComponentIdentifier().getCoordinates().get("artifactId") != null)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No suitable PolicyViolation found after canned-report evaluation for app=" + app.getId()));
  }

  /**
   * All violations on {@code hash} deduped by {@code policyId} (waiver's unique key is
   * {@code (hash, policyId, ownerId)}). Fails closed if the canned report drifts.
   */
  private List<PolicyViolation> uniqueViolationsForHash(String hash) {
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    Map<String, PolicyViolation> uniqueByPolicyId = dao.getByOwnerId(app.getId())
        .stream()
        .filter(v -> hash.equals(v.getHash()) && v.getPolicyId() != null)
        .collect(Collectors.toMap(PolicyViolation::getPolicyId, v -> v, (a, b) -> a));
    List<PolicyViolation> componentViolations = List.copyOf(uniqueByPolicyId.values());
    requireNonEmptyPrecondition(componentViolations,
        "violations on the seeded component (precondition)");
    return componentViolations;
  }

  /**
   * Precondition check on a Java collection (not a Playwright web element). Kept as a helper so
   * the file's only {@code assertThat} import remains Playwright's — callers use one assertion
   * style, and the AssertJ FQN is confined here.
   */
  private static void requireNonEmptyPrecondition(List<?> items, String description) {
    if (items == null || items.isEmpty()) {
      throw new IllegalStateException(description + " must not be empty");
    }
  }

  /**
   * Seeds org + policies + app + configured evaluator; does NOT call {@code evaluatePolicy()} —
   * each test runs the initial evaluation itself to avoid an {@code AutoPolicyWaiver}-induced
   * 404 race we hit when it was called from {@code @Before}. Do not lift the call back here
   * without re-testing that race.
   */
  private void seedDb() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);

    String suffix = TemporaryEntity.uuid();
    String orgName = ORG_NAME_PREFIX + "-" + suffix;
    appName = APP_NAME_PREFIX + "-" + suffix;

    Organization org = tempEntity.newOrganization(orgName);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    // Reference policies have no actions — add fail so waiver icons render.
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    for (Policy policy : policyDAO.getByOwnerId(org.getId())) {
      policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
      policyDAO.update(policy);
    }
    app = tempEntity.newApplication(appName, appName, org.getId());

    URL zippedReport = ReportHelper.zipReport(REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest,
        work, Stage.ID_BUILD);
  }
}
