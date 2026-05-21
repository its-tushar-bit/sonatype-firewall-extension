/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardComponentsComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardComponentsComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;
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
 * Playwright tests for the Dashboard <strong>Components</strong> tab.
 *
 * <p>
 * Split out of the legacy kitchen-sink {@code DashboardPlaywrightTest} so each dashboard tab has
 * its own dedicated test class — matching the pattern set by
 * {@code DashboardApplicationsPlaywrightTest}, {@code DashboardWaiversPlaywrightTest}, and
 * {@code DashboardWaiverRequestsPlaywrightTest}. {@code DashboardPlaywrightTest} itself now owns
 * only tab navigation (page chrome, tab switching, filter/export visibility).
 *
 * <p>
 * Test data lives in {@code src/test/resources/test-data/dashboard-violations.json} (shared with
 * {@link DashboardViolationsPlaywrightTest} — the components-tab assertions reuse the same seeded
 * org/app/policy/violation shape and only differ in which tab they assert against).
 */
public class DashboardComponentsPlaywrightTest
    extends AbstractIqUiTest
{

  private static final Data DATA = TestDataManager.load("dashboard-violations", Data.class);

  private Application application;

  private Policy securityPolicy;

  @Before
  public void openComponentsTabAsAdmin() {
    seedBaseEntities();

    playwrightRefreshOrOpen(DashboardPage.urlToComponents());
    playwrightLogin();
    new DashboardPage().waitUntilSpinnersGone();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentsTable_noDataMessage() {
    new DashboardComponentsComponentAssertions(new DashboardComponentsComponent())
        .shouldShowNoDataMessage(DATA.noDataMessageComponents());
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentsTable_loadsAndNavigatesOnRowClick() {
    seedViolation();
    playwrightRefreshOrOpen(DashboardPage.urlToComponents());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardComponentsComponent table = new DashboardComponentsComponent();
    DashboardComponentsComponentAssertions tableAssertions = new DashboardComponentsComponentAssertions(table);
    tableAssertions.shouldHaveCount(1);
    tableAssertions.shouldShowComponentRow(0, DATA.componentArtifactId());

    table.clickComponent(0);
    playwrightWaitUntilUrlContains("/component/");
  }

  /**
   * Typed view of {@code src/test/resources/test-data/dashboard-violations.json}. Shared with
   * {@link DashboardViolationsPlaywrightTest}; includes keys used by both tests.
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
