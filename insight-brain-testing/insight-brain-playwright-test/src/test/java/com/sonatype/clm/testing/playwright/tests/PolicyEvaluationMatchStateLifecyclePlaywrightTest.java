/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardViolationsComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiversComponent;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.SeededEvaluation;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

/**
 * Lifecycle tests around {@code MatchState}-driven policy violations: waiver suppression
 * (active/expired), Dashboard surfacing, re-eval deltas, Violation Details, and inheritance.
 */
public class PolicyEvaluationMatchStateLifecyclePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CONDITION_TYPE_ID = "MatchState";

  private static final String OPERATOR_IS = "is";

  private static final String OPERATOR_IS_NOT = "is not";

  private static final String VALUE_EXACT = "exact";

  private static final String VALUE_UNKNOWN = "unknown";

  private static final String VALUE_SIMILAR = "similar";

  private static final int DEFAULT_THREAT_LEVEL = 7;

  private static final String ORG_NAME_PREFIX = "PolicyEvalLifecycleOrg";

  private static final String APP_NAME_PREFIX = "PolicyEvalLifecycleApp";

  // application.public_id is varchar(60); short prefix keeps room for "-childN-" + uuid(32).
  private static final String APP_ID_PREFIX = "pol-lc";

  // scan_id is varchar(50); 8 + tag + uuid(32) fits.
  private static final String SCAN_ID_PREFIX = "pelc-scn";

  private static final String POLICY_NAME_PREFIX = "policy-eval-lc";

  private static final String CONSTRAINT_SUFFIX = "constraint";

  private static final String CANNED_REPORT_DIR = SmallReportFixture.CANNED_REPORT_DIR;

  private static final String COMPONENT_JETTY = SmallReportFixture.COMPONENT_JETTY;

  private static final String COMPONENT_GERONIMO = SmallReportFixture.COMPONENT_GERONIMO;

  private static final String WAIVER_COMMENT = "Lifecycle test waiver";

  private static final int EXACT_COMPONENT_COUNT = SmallReportFixture.EXACT_COMPONENT_COUNT;

  private static final int UNKNOWN_COMPONENT_COUNT = SmallReportFixture.UNKNOWN_COMPONENT_COUNT;

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

  /**
   * Active waiver: the report's default view filters out waived violations entirely
   * (filterMode='default' in applicationReportService.js), so suppression is observed by the
   * row's absence. Sibling exact-match components stay visible to confirm row-specific scope.
   */
  @Test
  @Tag("regression")
  public void testActiveWaiver_suppressesViolation() throws IOException {
    SeededEvaluation seeded = seedExact();

    PolicyViolation target = firstViolationForJetty(seeded);
    seedWaiver(seeded, target, /* expiry */ null);
    seeded.evaluator().reevaluatePolicy();
    openReport(seeded);

    reportAssertions.shouldShowNoViolationForComponentWithPolicy(COMPONENT_JETTY, seeded.policyName());
    reportAssertions.shouldShowViolationRow(COMPONENT_GERONIMO, DEFAULT_THREAT_LEVEL, seeded.policyName());
  }

  /** Past-dated expiry: the violation row renders without the waived indicator. */
  @Test
  @Tag("regression")
  public void testExpiredWaiver_doesNotSuppressViolation() throws IOException {
    SeededEvaluation seeded = seedExact();

    PolicyViolation target = firstViolationForJetty(seeded);
    Date yesterday = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
    seedWaiver(seeded, target, yesterday);
    seeded.evaluator().reevaluatePolicy();
    openReport(seeded);

    reportAssertions.shouldNotShowWaivedIndicatorForComponentWithPolicy(
        COMPONENT_JETTY, seeded.policyName());
  }

  /** Waiver created at the app level surfaces in the Dashboard Waivers tab. */
  @Test
  @Tag("regression")
  public void testCreatedWaiver_appearsOnDashboardWaiversTab() throws IOException {
    SeededEvaluation seeded = seedExact();
    PolicyViolation target = firstViolationForJetty(seeded);
    seedWaiver(seeded, target, null);

    // urlToWaivers() lands directly on the Waivers tab; clicking waiversTab() collides with
    // the inner Existing/Requested sub-tabs in strict mode.
    playwrightRefreshOrOpen(DashboardPage.urlToWaivers());
    playwrightLogin();
    Locator testRow = new DashboardWaiversComponent().waiverRowForApp(seeded.app().getName());
    assertThat(testRow).isVisible();
    assertThat(testRow).containsText(seeded.policyName());
    assertThat(testRow).containsText(seeded.app().getName());
    // Component column is em-dash for waivers without a parseable PURL (see seedWaiver Javadoc).
  }

  /** Tightening the constraint from "is unknown" (1 match) to "is exact" (12) produces 12 violations after re-eval. */
  @Test
  @Tag("regression")
  public void testTightenedPolicy_producesMoreViolationsAfterReeval() throws IOException {
    SeededEvaluation seeded = seedSingleCondition(OPERATOR_IS, VALUE_UNKNOWN);

    openReport(seeded);
    reportAssertions.shouldShowViolationCountForPolicy(UNKNOWN_COMPONENT_COUNT, seeded.policyName());

    updatePolicyCondition(seeded, OPERATOR_IS, VALUE_EXACT);
    seeded.evaluator().reevaluatePolicy();

    openReport(seeded);
    reportAssertions.shouldShowViolationCountForPolicy(EXACT_COMPONENT_COUNT, seeded.policyName());
  }

  /** Relaxing a policy (constraint matches fewer components) drops the violation count to zero. */
  @Test
  @Tag("regression")
  public void testRelaxedPolicy_removesViolationsAfterReeval() throws IOException {
    SeededEvaluation seeded = seedExact();

    openReport(seeded);
    reportAssertions.shouldShowViolationCountForPolicy(EXACT_COMPONENT_COUNT, seeded.policyName());

    updatePolicyCondition(seeded, OPERATOR_IS, VALUE_SIMILAR);
    seeded.evaluator().reevaluatePolicy();

    openReport(seeded);
    reportAssertions.shouldShowNoPolicyViolations(seeded.policyName());
  }

  /** Violations surface on the Dashboard Violations tab with policy + threat-level attribution. */
  @Test
  @Tag("regression")
  public void testViolationsAppearOnDashboardViolationsTab() throws IOException {
    SeededEvaluation seeded = seedSingleCondition(OPERATOR_IS_NOT, VALUE_UNKNOWN);

    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    playwrightLogin();
    Locator testRow = new DashboardViolationsComponent().violationRowForApp(seeded.app().getName());
    assertThat(testRow).isVisible();
    assertThat(testRow).containsText(seeded.policyName());
    assertThat(testRow).containsText(String.valueOf(DEFAULT_THREAT_LEVEL));
  }

  /** Violation Details renders component, policy, and constraint for a non-security violation. */
  @Test
  @Tag("regression")
  public void testViolationDetails_rendersComponentPolicyAndConstraint() throws IOException {
    SeededEvaluation seeded = seedExact();
    PolicyViolation target = firstViolationForJetty(seeded);

    playwrightRefreshOrOpen(ViolationDetailsPage.url(target.getId()));
    playwrightLogin();
    ViolationDetailsPage details = new ViolationDetailsPage();
    assertThat(details.container()).isVisible();
    assertThat(details.componentName()).containsText(COMPONENT_JETTY);
    assertThat(details.policyName()).containsText(seeded.policyName());
    assertThat(details.constraintSection()).containsText("Match state was 'Exact'");
  }

  /** Non-security violations hide the Vulnerability Details tab; the Waivers tabs remain. */
  @Test
  @Tag("regression")
  public void testViolationDetails_nonSecurityViolation_hidesVulnerabilityTab() throws IOException {
    SeededEvaluation seeded = seedExact();
    PolicyViolation target = firstViolationForJetty(seeded);

    playwrightRefreshOrOpen(ViolationDetailsPage.url(target.getId()));
    playwrightLogin();
    ViolationDetailsPage details = new ViolationDetailsPage();
    assertThat(details.container()).isVisible();
    assertThat(details.applicableWaiversTab()).isVisible();
    assertThat(details.similarWaiversTab()).isVisible();
    assertThat(details.securityTab()).hasCount(0);
  }

  /** Parent-org policy evaluates against a child application's scan; violations attribute to the parent policy. */
  @Test
  @Tag("regression")
  public void testInheritedPolicy_evaluatesAgainstChildApplication() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization parentOrg = tempEntity.newOrganization(ORG_NAME_PREFIX + "-parent-" + suffix);
    Application childApp = tempEntity.newApplication(
        APP_NAME_PREFIX + "-child-" + suffix, APP_ID_PREFIX + "-child-" + suffix, parentOrg.getId());
    // scan_id column is varchar(50) — keep prefix short (SCAN_ID_PREFIX 8 + tag + 32 uuid).
    String scanId = SCAN_ID_PREFIX + "-i-" + suffix;

    String parentPolicyName = POLICY_NAME_PREFIX + "-parent-" + suffix;
    seeder.seedSingleConditionPolicyForOwner(parentOrg.getId(), parentPolicyName, CONSTRAINT_SUFFIX,
        CONDITION_TYPE_ID, OPERATOR_IS, VALUE_EXACT, DEFAULT_THREAT_LEVEL);

    seeder.newEvaluator(childApp, scanId).evaluatePolicy();
    openReport(childApp, scanId);

    reportAssertions.shouldShowViolationCountForPolicy(EXACT_COMPONENT_COUNT, parentPolicyName);
    reportAssertions.shouldShowViolationRow(COMPONENT_JETTY, DEFAULT_THREAT_LEVEL, parentPolicyName);
  }

  /**
   * A policy at the parent org and a separate policy at the child application both evaluate
   * against the same scan. Each contributes its own violation rows attributed to the right
   * policy, validating that local policies don't shadow inherited ones (and vice versa).
   */
  @Test
  @Tag("regression")
  public void testParentAndChildPolicies_bothEvaluateOnChildApplication() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization parentOrg = tempEntity.newOrganization(ORG_NAME_PREFIX + "-parent2-" + suffix);
    Application childApp = tempEntity.newApplication(
        APP_NAME_PREFIX + "-child2-" + suffix, APP_ID_PREFIX + "-child2-" + suffix, parentOrg.getId());
    String scanId = SCAN_ID_PREFIX + "-b-" + suffix;

    String parentPolicyName = POLICY_NAME_PREFIX + "-parent2-" + suffix;
    String childPolicyName = POLICY_NAME_PREFIX + "-child2-" + suffix;
    seeder.seedSingleConditionPolicyForOwner(parentOrg.getId(), parentPolicyName, CONSTRAINT_SUFFIX,
        CONDITION_TYPE_ID, OPERATOR_IS, VALUE_EXACT, DEFAULT_THREAT_LEVEL);
    seeder.seedSingleConditionPolicyForOwner(childApp.getId(), childPolicyName, CONSTRAINT_SUFFIX,
        CONDITION_TYPE_ID, OPERATOR_IS, VALUE_UNKNOWN, DEFAULT_THREAT_LEVEL);

    seeder.newEvaluator(childApp, scanId).evaluatePolicy();
    openReport(childApp, scanId);

    reportAssertions.shouldShowViolationCountForPolicy(EXACT_COMPONENT_COUNT, parentPolicyName);
    reportAssertions.shouldShowViolationCountForPolicy(UNKNOWN_COMPONENT_COUNT, childPolicyName);
    reportAssertions.shouldShowViolationRow(COMPONENT_JETTY, DEFAULT_THREAT_LEVEL, parentPolicyName);
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(COMPONENT_JETTY, childPolicyName);
  }

  private SeededEvaluation seedExact() throws IOException {
    return seedSingleCondition(OPERATOR_IS, VALUE_EXACT);
  }

  private SeededEvaluation seedSingleCondition(String operator, String value) throws IOException {
    return seeder.seedSingleConditionAndEvaluate(
        ORG_NAME_PREFIX, APP_NAME_PREFIX, APP_ID_PREFIX, SCAN_ID_PREFIX, POLICY_NAME_PREFIX,
        CONSTRAINT_SUFFIX, CONDITION_TYPE_ID, operator, value, DEFAULT_THREAT_LEVEL);
  }

  /**
   * Idempotency latch for tests that call {@link #openReport} more than once within a single
   * {@code @Test} (e.g. the re-eval tests: open → mutate policy → re-evaluate → open again).
   *
   * <p>
   * Why this is needed: {@link #playwrightLogin()} delegates to {@code LoginPage.loginAs}, which
   * asserts the login modal is visible. After the first login the browser session (cookies +
   * Redux auth slice) persists across {@link #playwrightRefreshOrOpen} calls in the same
   * {@code @Test}, so a second {@code playwrightLogin()} would time out waiting for a modal
   * that will never appear. Calling it unconditionally is therefore unsafe, not free.
   *
   * <p>
   * Why this is safe: JUnit 4 instantiates the test class fresh for every {@code @Test} method,
   * so this field resets to {@code false} between tests; {@code AbstractIqUiTest}'s lifecycle
   * rule also installs a new {@code Page} per test, so the latch can never carry session state
   * across {@code @Test} boundaries.
   */
  private boolean loggedIn;

  private void openReport(SeededEvaluation seeded) {
    openReport(seeded.app(), seeded.scanId());
  }

  private void openReport(Application app, String scanId) {
    playwrightRefreshOrOpen(ApplicationReportPage.url(app, scanId));
    if (!loggedIn) {
      playwrightLogin();
      loggedIn = true;
    }
    reportAssertions.shouldShowReportHeaderContaining(app.getName());
  }

  // Condition.setOperator() doesn't exist; replace the whole Condition object on the constraint.
  private void updatePolicyCondition(SeededEvaluation seeded, String operator, String value) {
    Constraint constraint = seeded.policy().getConstraints().get(0);
    constraint.setConditions(Collections.singletonList(
        new Condition(CONDITION_TYPE_ID, operator, value)));
    lookup(PolicyDAO.class).update(seeded.policy());
  }

  private PolicyViolation firstViolationForJetty(SeededEvaluation seeded) {
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    List<PolicyViolation> violations = dao.getByOwnerId(seeded.app().getId());
    PolicyViolation target = violations.stream()
        .filter(v -> v.getPolicyId() != null
            && v.getPolicyId().equals(seeded.policy().getId())
            && v.getHash() != null
            && v.getComponentIdentifier() != null
            && v.getComponentIdentifier().getCoordinates() != null
            && jettyArtifact(v.getComponentIdentifier().getCoordinates()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No PolicyViolation found for component '" + COMPONENT_JETTY + "' on app="
                + seeded.app().getId() + " policy=" + seeded.policy().getId()));
    dao.loadConstraintFacts(Collections.singletonList(target));
    return target;
  }

  private static boolean jettyArtifact(Map<String, String> coordinates) {
    return COMPONENT_JETTY.equals(coordinates.get("artifactId"));
  }

  // associatedPackageUrl deliberately unset: a Drools-fact-derived PURL trips the dashboard
  // waivers loader's "Invalid package url" path and wipes the whole table; em-dash is fine.
  private void seedWaiver(SeededEvaluation seeded, PolicyViolation violation, Date expiryTime) {
    PolicyWaiver waiver = new PolicyWaiver()
        .setHash(violation.getHash())
        .setPolicyId(violation.getPolicyId())
        .setOwnerId(seeded.app().getId())
        .setConstraintFacts(violation.getConstraintFacts())
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment(WAIVER_COMMENT);
    if (expiryTime != null) {
      waiver.setExpiryTime(expiryTime);
    }
    tempEntity.newWaiver(waiver);
  }
}
