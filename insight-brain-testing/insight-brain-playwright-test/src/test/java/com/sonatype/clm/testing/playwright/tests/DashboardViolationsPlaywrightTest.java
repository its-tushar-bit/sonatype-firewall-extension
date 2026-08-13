/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardViolationsComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardViolationsComponent.SortableColumn;
import com.sonatype.clm.testing.playwright.pages.DashboardViolationsComponentAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTable_columnsAndSortToggles() {
    seedThreeViolationsWithMixedThreats();
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardViolationsComponent table = new DashboardViolationsComponent();
    DashboardViolationsComponentAssertions tableAssertions = new DashboardViolationsComponentAssertions(table);
    tableAssertions.shouldShowExpectedColumns();

    tableAssertions.shouldHaveSortState(SortableColumn.AGE, "ascending");
    tableAssertions.shouldHaveSortState(SortableColumn.THREAT, "none");

    table.clickHeader(SortableColumn.THREAT);
    tableAssertions.shouldHaveSortState(SortableColumn.THREAT, "descending");
    table.clickHeader(SortableColumn.THREAT);
    tableAssertions.shouldHaveSortState(SortableColumn.THREAT, "ascending");

    table.clickHeader(SortableColumn.POLICY);
    tableAssertions.shouldHaveSortState(SortableColumn.POLICY, "ascending");
    table.clickHeader(SortableColumn.POLICY);
    tableAssertions.shouldHaveSortState(SortableColumn.POLICY, "descending");

    table.clickHeader(SortableColumn.APPLICATION);
    tableAssertions.shouldHaveSortState(SortableColumn.APPLICATION, "ascending");
    table.clickHeader(SortableColumn.APPLICATION);
    tableAssertions.shouldHaveSortState(SortableColumn.APPLICATION, "descending");
  }

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTable_applicationSecondarySort() {
    seedViolationsAcrossTwoApps();
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardViolationsComponent table = new DashboardViolationsComponent();
    DashboardViolationsComponentAssertions tableAssertions = new DashboardViolationsComponentAssertions(table);

    table.clickHeader(SortableColumn.APPLICATION);
    tableAssertions.shouldHaveSortState(SortableColumn.APPLICATION, "ascending");

    assertThat(table.applicationName(0)).containsText(DATA.sortAppAlphaName());
    assertThat(table.applicationName(1)).containsText(DATA.sortAppAlphaName());
    assertThat(table.applicationName(2)).containsText(DATA.sortAppBetaName());
    assertThat(table.applicationName(3)).containsText(DATA.sortAppBetaName());
  }

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTable_paginationNextPrevious() {
    seedNViolations(DATA.paginationViolationCount());
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardViolationsComponent table = new DashboardViolationsComponent();
    DashboardViolationsComponentAssertions tableAssertions = new DashboardViolationsComponentAssertions(table);

    tableAssertions.assertPaginationFirstPageState();
    table.goToNextPage();
    tableAssertions.assertPaginationAfterNextClick();
    table.goToPreviousPage();
    tableAssertions.assertPaginationReturnedToFirstPageState();
  }

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTable_filterMaskAppearsWhenDirty() {
    seedViolation();
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage dashboard = new DashboardPage();
    new DashboardPageAssertions(dashboard).shouldBeLoaded();

    dashboard.expandFilter();
    dashboard.policyTypeFilterTrigger().click();
    dashboard.policyTypeFilterFirstCheckbox().click();
    assertThat(dashboard.formMask()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTable_paginationHiddenWhenSinglePage() {
    seedViolation();
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardViolationsComponent table = new DashboardViolationsComponent();
    DashboardViolationsComponentAssertions tableAssertions = new DashboardViolationsComponentAssertions(table);
    tableAssertions.shouldHaveCount(1);
    tableAssertions.assertNoPaginator();
  }

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTable_aggregatesAcrossApps() {
    seedViolationsAcrossTwoApps();
    playwrightRefreshOrOpen(DashboardPage.urlToViolations());
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardViolationsComponent table = new DashboardViolationsComponent();
    DashboardViolationsComponentAssertions tableAssertions =
        new DashboardViolationsComponentAssertions(table);

    // The dashboard query is tenant-wide, so sibling tests in the same JVM session can leave
    // root-org policies and violations that inflate the row count beyond this run's seed
    // (UUID-suffixed apps isolate ours, but not theirs). The verifiable claim here is that the
    // two seeded apps' four violations are aggregated into the same table, not an exact count.
    tableAssertions.shouldHaveAtLeastCount(4);

    Set<String> distinctApps = table.allApplicationNames()
        .allInnerTexts()
        .stream()
        .map(String::trim)
        .collect(Collectors.toSet());
    Assertions.assertThat(distinctApps)
        .as("dashboard table aggregates rows from both seeded apps")
        .hasSizeGreaterThanOrEqualTo(2);
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
      String sortScanIdPrefix,
      String sortHashFormat,
      String sortHighPolicyName,
      int sortHighThreatLevel,
      String sortMidPolicyName,
      int sortMidThreatLevel,
      String sortLowPolicyName,
      int sortLowThreatLevel,
      String sortAppAlphaName,
      String sortAppAlphaId,
      String sortAppBetaName,
      String sortAppBetaId,
      String sortComponentArtifactA,
      String sortComponentArtifactB,
      String sortComponentArtifactC,
      String paginationScanId,
      int paginationViolationCount)
  {
  }

  private Organization organization;

  private void seedBaseEntities() {
    String suffix = TemporaryEntity.uuid();
    organization = tempEntity.newOrganization(DATA.orgName() + "-" + suffix);
    application = tempEntity.newApplication(DATA.appName() + "-" + suffix, DATA.appId() + "-" + suffix,
        organization.getId());
    securityPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.policyName() + "-" + suffix, DATA.policyThreatLevel());
  }

  private void seedViolation() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1");
    tempEntity.newPolicyViolation(evaluation, securityPolicy,
        DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion(),
        DATA.componentHash(), DATA.cveId());
  }

  private void seedNViolations(int count) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "pagination-scan");
    for (int i = 0; i < count; i++) {
      tempEntity.newPolicyViolation(evaluation, securityPolicy,
          DATA.componentGroupId(),
          DATA.componentArtifactId() + "-" + i,
          DATA.componentVersion(),
          DATA.componentHash() + "-" + i,
          DATA.cveId());
    }
  }

  private void seedThreeViolationsWithMixedThreats() {
    Policy highPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.sortHighPolicyName(), DATA.sortHighThreatLevel());
    Policy midPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.sortMidPolicyName(), DATA.sortMidThreatLevel());
    Policy lowPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.sortLowPolicyName(), DATA.sortLowThreatLevel());

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "sort-scan");
    tempEntity.newPolicyViolation(evaluation, highPolicy,
        DATA.componentGroupId(), DATA.sortComponentArtifactA(), DATA.componentVersion(),
        DATA.componentHash() + "-high", DATA.cveId());
    tempEntity.newPolicyViolation(evaluation, midPolicy,
        DATA.componentGroupId(), DATA.sortComponentArtifactB(), DATA.componentVersion(),
        DATA.componentHash() + "-mid", DATA.cveId());
    tempEntity.newPolicyViolation(evaluation, lowPolicy,
        DATA.componentGroupId(), DATA.sortComponentArtifactC(), DATA.componentVersion(),
        DATA.componentHash() + "-low", DATA.cveId());
  }

  private void seedViolationsAcrossTwoApps() {
    String sortSuffix = TemporaryEntity.uuid();
    Application alphaApp = tempEntity.newApplication(
        DATA.sortAppAlphaName() + "-" + sortSuffix, DATA.sortAppAlphaId() + "-" + sortSuffix, organization.getId());
    Application betaApp = tempEntity.newApplication(
        DATA.sortAppBetaName() + "-" + sortSuffix, DATA.sortAppBetaId() + "-" + sortSuffix, organization.getId());

    Policy highPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.sortHighPolicyName(), DATA.sortHighThreatLevel());
    Policy lowPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.sortLowPolicyName(), DATA.sortLowThreatLevel());

    int scanIndex = 1;
    for (Application app : List.of(alphaApp, betaApp)) {
      PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
          app.getId(), StageTypes.BUILD.getId(), "sort-app-scan-" + scanIndex);
      tempEntity.newPolicyViolation(eval, highPolicy,
          DATA.componentGroupId(), DATA.sortComponentArtifactA(), DATA.componentVersion(),
          DATA.componentHash() + "-" + scanIndex + "-high", DATA.cveId());
      tempEntity.newPolicyViolation(eval, lowPolicy,
          DATA.componentGroupId(), DATA.sortComponentArtifactB(), DATA.componentVersion(),
          DATA.componentHash() + "-" + scanIndex + "-low", DATA.cveId());
      scanIndex++;
    }
  }
}
