/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardViolationsComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardViolationsComponentAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright tests for the Dashboard <strong>Violations</strong> tab.
 *
 * <p>
 * Split out of the legacy kitchen-sink {@code DashboardPlaywrightTest} so each dashboard tab has
 * its own dedicated test class — matching the pattern set by
 * {@code DashboardApplicationsPlaywrightTest}, {@code DashboardWaiversPlaywrightTest}, and
 * {@code DashboardWaiverRequestsPlaywrightTest}. {@code DashboardPlaywrightTest} itself now owns
 * only tab navigation (page chrome, tab switching, filter/export visibility).
 *
 * <p>
 * Test data lives in {@code src/test/resources/test-data/dashboard-violations.json} and is
 * loaded once via {@link TestDataManager}. Backend setup is encapsulated in the private
 * {@link #seedBaseEntities()} / {@link #seedViolation()} helpers so DB writes are visible at
 * one call-site and don't leak across parallel forks (skill §3c).
 */
public class DashboardViolationsPlaywrightTest
    extends AbstractIqUiTest
{

  private static final Data DATA = TestDataManager.load("dashboard-violations", Data.class);

  private Application application;

  private Policy securityPolicy;

  @Before
  public void openViolationsTabAsAdmin() {
    seedBaseEntities();

    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    playwrightLogin();
    new DashboardPage().waitUntilSpinnersGone();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testViolationsTable_noDataMessage() {
    new DashboardViolationsComponentAssertions(new DashboardViolationsComponent())
        .shouldShowNoDataMessage(DATA.noDataMessage());
  }

  @Test
  @Category(SanityTest.class)
  public void testViolationsTable_loadsAndNavigatesOnRowClick() {
    seedViolation();
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardViolationsComponent table = new DashboardViolationsComponent();
    DashboardViolationsComponentAssertions tableAssertions = new DashboardViolationsComponentAssertions(table);
    tableAssertions.shouldHaveCount(1);
    tableAssertions.shouldShowViolationRow(0, DATA.componentArtifactId(), DATA.policyName(), DATA.appName());

    table.clickViolation(0);
    playwrightWaitUntilUrlContains("/violation/");
  }

  /**
   * Typed view of {@code src/test/resources/test-data/dashboard-violations.json}. Shared with
   * {@link DashboardComponentsPlaywrightTest}; includes keys used by both tests.
   */
  public record Data(
      String noDataMessage,
      String noDataMessageComponents,
      String orgName,
      String appName,
      String appId,
      String policyName,
      int policyThreatLevel,
      String componentGroupId,
      String componentArtifactId,
      String componentVersion,
      String componentHash,
      String cveId)
  {
  }

  private void seedBaseEntities() {
    Organization organization = tempEntity.newOrganization(DATA.orgName());
    application = tempEntity.newApplication(DATA.appName(), DATA.appId(), organization.getId());
    securityPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.policyName(), DATA.policyThreatLevel());
  }

  private void seedViolation() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1");
    tempEntity.newPolicyViolation(evaluation, securityPolicy,
        DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion(),
        DATA.componentHash(), DATA.cveId());
  }
}
