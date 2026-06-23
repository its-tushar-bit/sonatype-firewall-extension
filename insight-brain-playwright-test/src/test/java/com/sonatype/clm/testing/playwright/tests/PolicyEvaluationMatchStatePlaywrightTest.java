/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.AppAndScan;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.SeededEvaluation;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright tests for the {@code MatchState} policy condition. Seeds via
 * {@link PolicyEvaluationSeeder} against {@code small-report} (12 exact-match + 1 unknown-match
 * components) then asserts the violation rows.
 *
 * <p>
 * The fixture facts (component counts, component names, canned-report dir) loaded from
 * {@code policy-evaluation-match-state.json} must stay in sync with the Java source of truth
 * at {@code SmallReportFixture}. Inline-constant test classes ({@code
 * PolicyEvaluationMatchStateLifecyclePlaywrightTest}, {@code
 * PolicyEvaluationReportNavigationPlaywrightTest}, {@code
 * PolicyEvaluationThreatLevelDisplayPlaywrightTest}) already reference {@code SmallReportFixture}
 * directly.
 */
public class PolicyEvaluationMatchStatePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CONDITION_TYPE_ID = "MatchState";

  private static final Data DATA = TestDataManager.load(
      "policy-evaluation-match-state", Data.class);

  private record Data(
      String orgNamePrefix,
      String appNamePrefix,
      String appIdPrefix,
      String scanIdPrefix,
      String policyNamePrefix,
      String cannedReportClasspathDir,
      String constraintNameSuffix,
      String componentJetty,
      String componentGeronimo,
      int exactComponentsCount,
      int unknownComponentsCount,
      int isExactThreatLevel,
      String isExactOperator,
      String isExactValue,
      int isSimilarThreatLevel,
      String isSimilarOperator,
      String isSimilarValue,
      int isUnknownThreatLevel,
      String isUnknownOperator,
      String isUnknownValue,
      int isNotExactThreatLevel,
      String isNotExactOperator,
      String isNotExactValue,
      int isNotUnknownThreatLevel,
      String isNotUnknownOperator,
      String isNotUnknownValue,
      String componentFormatConditionTypeId,
      int formatIsMavenThreatLevel,
      String formatIsMavenOperator,
      String formatIsMavenValue,
      int mavenComponentsCount,
      String ageInDaysConditionTypeId,
      String ageInDaysOperatorOlderThan,
      String ageInDaysValueOneYear,
      int ageInDaysOlderThanOneYearThreatLevel,
      int ageInDaysOlderThanOneYearComponentsCount)
  {
  }

  private PolicyEvaluationSeeder seeder;

  private ApplicationReportPage reportPage;

  private ApplicationReportPageAssertions reportAssertions;

  @Before
  public void initSeederAndAssertions() {
    seeder = new PolicyEvaluationSeeder(
        tempEntity, tempDir, testCLMServer.getCLMServer().getConfiguration(),
        baseUrlFromTest, DATA.cannedReportClasspathDir());
    reportPage = new ApplicationReportPage();
    reportAssertions = new ApplicationReportPageAssertions(reportPage);
  }

  @Test
  @Category(RegressionTest.class)
  public void testIsExact_flagsAllExactComponents() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(
        DATA.isExactThreatLevel(), DATA.isExactOperator(), DATA.isExactValue());

    reportAssertions.shouldShowViolationCountForPolicy(DATA.exactComponentsCount(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.isExactThreatLevel(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentGeronimo(), DATA.isExactThreatLevel(), seeded.policyName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testIsSimilar_producesNoViolations() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(
        DATA.isSimilarThreatLevel(), DATA.isSimilarOperator(), DATA.isSimilarValue());

    reportAssertions.shouldShowNoPolicyViolations(seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentJetty(), seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentGeronimo(), seeded.policyName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testIsUnknown_flagsOnlyUnknownComponent() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(
        DATA.isUnknownThreatLevel(), DATA.isUnknownOperator(), DATA.isUnknownValue());

    reportAssertions.shouldShowViolationCountForPolicy(DATA.unknownComponentsCount(), seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentJetty(), seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentGeronimo(), seeded.policyName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testIsNotExact_flagsNonExactComponentOnly() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(
        DATA.isNotExactThreatLevel(), DATA.isNotExactOperator(), DATA.isNotExactValue());

    reportAssertions.shouldShowViolationCountForPolicy(DATA.unknownComponentsCount(), seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentJetty(), seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentGeronimo(), seeded.policyName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testIsNotUnknown_flagsAllNonUnknownComponents() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(
        DATA.isNotUnknownThreatLevel(), DATA.isNotUnknownOperator(), DATA.isNotUnknownValue());

    reportAssertions.shouldShowViolationCountForPolicy(DATA.exactComponentsCount(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.isNotUnknownThreatLevel(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentGeronimo(), DATA.isNotUnknownThreatLevel(), seeded.policyName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testTwoSeparatePolicies_independentViolations() throws IOException {
    AppAndScan provisioned = seeder.provisionAppAndScan(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix());
    String policyA = DATA.policyNamePrefix() + "-A-" + provisioned.suffix();
    String policyB = DATA.policyNamePrefix() + "-B-" + provisioned.suffix();
    seeder.seedSingleConditionPolicy(
        provisioned.app(), policyA, DATA.constraintNameSuffix(), CONDITION_TYPE_ID,
        DATA.isExactOperator(), DATA.isExactValue(), DATA.isExactThreatLevel());
    seeder.seedSingleConditionPolicy(
        provisioned.app(), policyB, DATA.constraintNameSuffix(), CONDITION_TYPE_ID,
        DATA.isUnknownOperator(), DATA.isUnknownValue(), DATA.isUnknownThreatLevel());
    seeder.newEvaluator(provisioned.app(), provisioned.scanId()).evaluatePolicy();
    openReport(provisioned);

    reportAssertions.shouldShowViolationCountForPolicy(DATA.exactComponentsCount(), policyA);
    reportAssertions.shouldShowViolationCountForPolicy(DATA.unknownComponentsCount(), policyB);
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.isExactThreatLevel(), policyA);
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(DATA.componentJetty(), policyB);
  }

  @Test
  @Category(RegressionTest.class)
  public void testAndConstraint_narrowsToBothMatching() throws IOException {
    // Coordinates format <fmt>:<groupId>:<artifactId>:<version>; empty parts wildcard.
    String mavenJettyAnyVersion = "maven::" + DATA.componentJetty() + ":";
    SeededEvaluation seeded = seeder.seedMultiConditionAndEvaluate(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix(),
        DATA.policyNamePrefix() + "-and", DATA.constraintNameSuffix(),
        List.of(
            new Condition(CONDITION_TYPE_ID, DATA.isExactOperator(), DATA.isExactValue()),
            new Condition(CoordinatesConditionType.ID, "match", mavenJettyAnyVersion)),
        LogicalOperator.AND, DATA.isExactThreatLevel());
    openReport(seeded);

    reportAssertions.shouldShowViolationCountForPolicy(1, seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.isExactThreatLevel(), seeded.policyName());
    reportAssertions.shouldShowNoViolationForComponentWithPolicy(
        DATA.componentGeronimo(), seeded.policyName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testMultiConstraint_independentViolationsUnderOnePolicy() throws IOException {
    AppAndScan provisioned = seeder.provisionAppAndScan(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix());
    String policyName = DATA.policyNamePrefix() + "-multi-" + provisioned.suffix();
    Policy policy = seeder.seedMultiConstraintPolicy(
        provisioned.app(), policyName, DATA.constraintNameSuffix(),
        List.of(
            new Condition(CONDITION_TYPE_ID, DATA.isExactOperator(), DATA.isExactValue()),
            new Condition(CONDITION_TYPE_ID, DATA.isUnknownOperator(), DATA.isUnknownValue())),
        DATA.isExactThreatLevel());
    seeder.newEvaluator(provisioned.app(), provisioned.scanId()).evaluatePolicy();
    openReport(provisioned);

    reportAssertions.shouldShowViolationCountForPolicy(
        DATA.exactComponentsCount() + DATA.unknownComponentsCount(), policy.getName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.isExactThreatLevel(), policy.getName());
  }

  /**
   * Translation of the License Threat Group row — license data is HDS-injected and absent from
   * the canned report, so we exercise the same operator-algebra surface via {@code ComponentFormat}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFormatIsMaven_flagsAllMavenComponents() throws IOException {
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix(),
        DATA.policyNamePrefix(), DATA.constraintNameSuffix(),
        DATA.componentFormatConditionTypeId(), DATA.formatIsMavenOperator(),
        DATA.formatIsMavenValue(), DATA.formatIsMavenThreatLevel());
    openReport(seeded);

    reportAssertions.shouldShowViolationCountForPolicy(
        DATA.mavenComponentsCount(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.formatIsMavenThreatLevel(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentGeronimo(), DATA.formatIsMavenThreatLevel(), seeded.policyName());
  }

  /**
   * 10 of small-report's 12 exact-match components have a non-null {@code createTime} that is
   * more than 365 days before any plausible test runtime; the newest such entry in {@code
   * small-report/bom.json} is {@code commons-io} at {@code createTime=2018-02-23}. The other 2
   * exact-match components ({@code createTime=null}: test-business, test-data) and the
   * unknown-match component are filtered by {@code AgeInDaysConditionType}'s null-short-circuit
   * and inherited UNKNOWN guard.
   *
   * <p>
   * Stability invariant: this count of 10 holds for as long as every non-null {@code createTime}
   * in {@code bom.json} remains older than the {@code "older than 365 days"} threshold. If a
   * future edit adds a recently-dated component (or backdates one into the last 365 days), the
   * {@code ageInDaysOlderThanOneYearComponentsCount} JSON entry must be adjusted to match.
   */
  @Test
  @Category(RegressionTest.class)
  public void testAgeInDays_olderThanOneYear_flagsDatedComponents() throws IOException {
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix(),
        DATA.policyNamePrefix(), DATA.constraintNameSuffix(),
        DATA.ageInDaysConditionTypeId(), DATA.ageInDaysOperatorOlderThan(),
        DATA.ageInDaysValueOneYear(), DATA.ageInDaysOlderThanOneYearThreatLevel());
    openReport(seeded);

    reportAssertions.shouldShowViolationCountForPolicy(
        DATA.ageInDaysOlderThanOneYearComponentsCount(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentJetty(), DATA.ageInDaysOlderThanOneYearThreatLevel(), seeded.policyName());
    reportAssertions.shouldShowViolationRow(
        DATA.componentGeronimo(), DATA.ageInDaysOlderThanOneYearThreatLevel(), seeded.policyName());
  }

  /** Clicking the Threat column header toggles {@code aria-sort} desc → asc; rows reorder. */
  @Test
  @Category(RegressionTest.class)
  public void testSortByThreatColumn_togglesDescendingToAscending() throws IOException {
    // Two policies at different threat levels so the column has multiple distinct values.
    AppAndScan provisioned = seeder.provisionAppAndScan(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix());
    seeder.seedSingleConditionPolicy(
        provisioned.app(), DATA.policyNamePrefix() + "-low-" + provisioned.suffix(), DATA.constraintNameSuffix(),
        CONDITION_TYPE_ID, DATA.isExactOperator(), DATA.isExactValue(), DATA.isExactThreatLevel());
    seeder.seedSingleConditionPolicy(
        provisioned.app(), DATA.policyNamePrefix() + "-high-" + provisioned.suffix(), DATA.constraintNameSuffix(),
        CONDITION_TYPE_ID, DATA.isNotExactOperator(), DATA.isNotExactValue(),
        DATA.isNotExactThreatLevel());
    seeder.newEvaluator(provisioned.app(), provisioned.scanId()).evaluatePolicy();
    openReport(provisioned);

    assertThat(reportPage.threatColumnHeader()).hasAttribute("aria-sort", "descending");
    awaitThreatColumnSortedBy(Comparator.reverseOrder(), "default load is sorted by threat descending");

    reportPage.threatColumnHeader().click();
    assertThat(reportPage.threatColumnHeader()).hasAttribute("aria-sort", "ascending");
    awaitThreatColumnSortedBy(Comparator.naturalOrder(), "after click, threat column is sorted ascending");
  }

  /**
   * Awaitility-wrapped retry of {@link #currentThreatColumnValues} so a slow re-sort animation
   * doesn't let the test capture stale row order.
   */
  private void awaitThreatColumnSortedBy(Comparator<Integer> comparator, String description) {
    Awaitility.await(description)
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(() -> Assertions.assertThat(currentThreatColumnValues())
            .as(description)
            .isSortedAccordingTo(comparator));
  }

  /** Policy-name filter narrows rendered rows to policies whose name contains the typed substring. */
  @Test
  @Category(RegressionTest.class)
  public void testFilterByPolicyName_narrowsToMatchingPolicyOnly() throws IOException {
    AppAndScan provisioned = seeder.provisionAppAndScan(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix());
    String suffix = provisioned.suffix();
    // policy.name is varchar(60); short tag keeps prefix(17) + tag + uuid(32) within budget.
    String exactPolicy = DATA.policyNamePrefix() + "-ef-" + suffix;
    String unknownPolicy = DATA.policyNamePrefix() + "-uf-" + suffix;
    seeder.seedSingleConditionPolicy(
        provisioned.app(), exactPolicy, DATA.constraintNameSuffix(), CONDITION_TYPE_ID,
        DATA.isExactOperator(), DATA.isExactValue(), DATA.isExactThreatLevel());
    seeder.seedSingleConditionPolicy(
        provisioned.app(), unknownPolicy, DATA.constraintNameSuffix(), CONDITION_TYPE_ID,
        DATA.isUnknownOperator(), DATA.isUnknownValue(), DATA.isUnknownThreatLevel());
    seeder.newEvaluator(provisioned.app(), provisioned.scanId()).evaluatePolicy();
    openReport(provisioned);

    reportAssertions.shouldShowViolationCountForPolicy(DATA.exactComponentsCount(), exactPolicy);
    reportAssertions.shouldShowViolationCountForPolicy(DATA.unknownComponentsCount(), unknownPolicy);

    reportPage.policyNameFilter().fill(unknownPolicy);
    assertThat(reportPage.violationRowsForPolicy(unknownPolicy))
        .hasCount(DATA.unknownComponentsCount());
    assertThat(reportPage.violationRowsForPolicy(exactPolicy)).hasCount(0);

    reportPage.policyNameFilter().fill("");
    reportAssertions.shouldShowViolationCountForPolicy(DATA.exactComponentsCount(), exactPolicy);
    reportAssertions.shouldShowViolationCountForPolicy(DATA.unknownComponentsCount(), unknownPolicy);
  }

  /** Single atomic {@code allInnerTexts()} round-trip — avoids {@code count()}+{@code nth(i)} TOCTOU. */
  private List<Integer> currentThreatColumnValues() {
    return reportPage.violationRowThreatNumbers()
        .allInnerTexts()
        .stream()
        .map(String::trim)
        .map(this::parseThreatNumber)
        .toList();
  }

  /** Surfaces non-numeric cell text as a readable AssertJ failure rather than NumberFormatException. */
  private Integer parseThreatNumber(String text) {
    Assertions.assertThat(text)
        .as("threat-number cell text should be numeric — got '%s'", text)
        .matches("\\d+");
    return Integer.parseInt(text);
  }

  private SeededEvaluation seedAndOpenReport(int threatLevel, String operator, String value) throws IOException {
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        DATA.orgNamePrefix(), DATA.appNamePrefix(), DATA.appIdPrefix(), DATA.scanIdPrefix(),
        DATA.policyNamePrefix(), DATA.constraintNameSuffix(),
        CONDITION_TYPE_ID, operator, value, threatLevel);
    openReport(seeded);
    return seeded;
  }

  private void openReport(SeededEvaluation seeded) {
    openReport(seeded.app(), seeded.scanId());
  }

  private void openReport(AppAndScan provisioned) {
    openReport(provisioned.app(), provisioned.scanId());
  }

  /**
   * Every {@code @Test} in this class navigates to the report exactly once, so no login-idempotency
   * latch is needed here. Contrast with {@code PolicyEvaluationMatchStateLifecyclePlaywrightTest},
   * whose re-eval tests call {@code openReport()} twice and gate the second login behind a
   * {@code loggedIn} latch — see that class's Javadoc for the modal-visibility precondition that
   * makes the latch load-bearing rather than an optimisation.
   */
  private void openReport(Application app, String scanId) {
    playwrightRefreshOrOpen(ApplicationReportPage.url(app, scanId));
    playwrightLogin();
    reportAssertions.shouldShowReportHeaderContaining(app.getName());
  }
}
