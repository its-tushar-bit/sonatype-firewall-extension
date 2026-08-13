/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder;
import com.sonatype.clm.testing.playwright.utils.PolicyEvaluationSeeder.SeededEvaluation;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * RSC threat-level → colour-category bucketing on the violation row's NxThreatIndicator. Buckets
 * (per RSC's {@code categoryByPolicyThreatLevel}): 0=none, 1=low, 2-3=moderate, 4-7=severe,
 * 8-10=critical. Each test seeds {@code MatchState=exact} at a representative level.
 */
public class PolicyEvaluationThreatLevelDisplayPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CONDITION_TYPE_ID = "MatchState";

  private static final String CONDITION_OPERATOR = "is";

  private static final String CONDITION_VALUE = "exact";

  private static final String ORG_NAME_PREFIX = "PolicyEvalThreatOrg";

  private static final String APP_NAME_PREFIX = "PolicyEvalThreatApp";

  private static final String APP_ID_PREFIX = "policy-eval-threat";

  private static final String SCAN_ID_PREFIX = "pet-scan";

  private static final String POLICY_NAME_PREFIX = "policy-eval-threat";

  private static final String CONSTRAINT_SUFFIX = "constraint";

  private static final String CANNED_REPORT_DIR = SmallReportFixture.CANNED_REPORT_DIR;

  private static final String COMPONENT_JETTY = SmallReportFixture.COMPONENT_JETTY;

  private PolicyEvaluationSeeder seeder;

  private ApplicationReportPageAssertions reportAssertions;

  @Before
  public void initSeederAndAssertions() {
    seeder = new PolicyEvaluationSeeder(
        tempEntity, tempDir, testCLMServer.getCLMServer().getConfiguration(),
        baseUrlFromTest, CANNED_REPORT_DIR);
    reportAssertions = new ApplicationReportPageAssertions(new ApplicationReportPage());
  }

  @Test
  @Category(RegressionTest.class)
  public void testThreatIndicator_critical_at10() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(10);
    reportAssertions.shouldShowViolationRowWithThreatCategory(
        COMPONENT_JETTY, 10, seeded.policyName(), "critical");
  }

  @Test
  @Category(RegressionTest.class)
  public void testThreatIndicator_critical_at8() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(8);
    reportAssertions.shouldShowViolationRowWithThreatCategory(
        COMPONENT_JETTY, 8, seeded.policyName(), "critical");
  }

  @Test
  @Category(RegressionTest.class)
  public void testThreatIndicator_severe_at4() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(4);
    reportAssertions.shouldShowViolationRowWithThreatCategory(
        COMPONENT_JETTY, 4, seeded.policyName(), "severe");
  }

  @Test
  @Category(RegressionTest.class)
  public void testThreatIndicator_moderate_at3() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(3);
    reportAssertions.shouldShowViolationRowWithThreatCategory(
        COMPONENT_JETTY, 3, seeded.policyName(), "moderate");
  }

  @Test
  @Category(RegressionTest.class)
  public void testThreatIndicator_low_at1() throws IOException {
    SeededEvaluation seeded = seedAndOpenReport(1);
    reportAssertions.shouldShowViolationRowWithThreatCategory(
        COMPONENT_JETTY, 1, seeded.policyName(), "low");
  }

  private SeededEvaluation seedAndOpenReport(int threatLevel) throws IOException {
    SeededEvaluation seeded = seeder.seedSingleConditionAndEvaluate(
        ORG_NAME_PREFIX, APP_NAME_PREFIX, APP_ID_PREFIX, SCAN_ID_PREFIX, POLICY_NAME_PREFIX,
        CONSTRAINT_SUFFIX, CONDITION_TYPE_ID, CONDITION_OPERATOR, CONDITION_VALUE, threatLevel);
    playwrightRefreshOrOpen(ApplicationReportPage.url(seeded.app(), seeded.scanId()));
    playwrightLogin();
    reportAssertions.shouldShowReportHeaderContaining(seeded.app().getName());
    return seeded;
  }
}
