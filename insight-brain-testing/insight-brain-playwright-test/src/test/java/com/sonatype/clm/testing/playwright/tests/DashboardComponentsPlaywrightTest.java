/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

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

  @Test
  @Category(RegressionTest.class)
  public void testComponentRisk_detailViewRendersWithoutDashboardContainer() {
    seedViolationWithComponent();
    playwrightRefreshOrOpen(DashboardPage.urlToComponents());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardComponentsComponent table = new DashboardComponentsComponent();
    new DashboardComponentsComponentAssertions(table).shouldHaveCount(1);

    table.clickComponent(0);
    playwrightWaitUntilUrlContains("/component/");

    new DashboardComponentsComponentAssertions(table).shouldShowComponentRiskDetail(DATA.componentArtifactId());
  }

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
      String cveId,
      String scanId,
      String dashboardViolationsUrlFragment,
      String violationUrlPattern,
      int navigationTimeoutMs,
      int dirtyFilterMin,
      int dirtyFilterMax,
      String hashSuffixHigh,
      String hashSuffixMid,
      String hashSuffixLow,
      String paginationScanId,
      int paginationViolationCount,
      int paginationWaitForResultsMs,
      String sortScanIdPrefix,
      String sortHashFormat,
      String sortAppAlphaName,
      String sortAppAlphaId,
      String sortAppBetaName,
      String sortAppBetaId,
      String sortHighPolicyName,
      int sortHighThreatLevel,
      String sortMidPolicyName,
      int sortMidThreatLevel,
      String sortLowPolicyName,
      int sortLowThreatLevel,
      String sortComponentArtifactA,
      String sortComponentArtifactB,
      String sortComponentArtifactC)
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

  private void seedViolationWithComponent() {
    seedViolation();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion());
    tempEntity.newApplicationComponent(
        application.getId(), StageTypes.BUILD.getId(), DATA.componentHash(), componentIdentifier);
  }
}
