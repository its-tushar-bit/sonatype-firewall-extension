/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.FirewallComponentDetailsRegressionPage;
import com.sonatype.clm.testing.playwright.pages.FirewallDashboardRegressionAssertions;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.clm.testing.playwright.pages.FirewallRegressionPage;
import com.sonatype.clm.testing.playwright.pages.FirewallRepositoryResultsRegressionPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Playwright regression for the Firewall Dashboard. */
public class FirewallDashboardPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Data DATA = TestDataManager.load("firewall-dashboard", Data.class);

  private static final LocatorAssertions.HasCountOptions COUNT_OPTS =
      new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasCountOptions SLOW_COUNT_OPTS =
      new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.ContainsTextOptions SLOW_TEXT_OPTS =
      new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private String filterAlphaArtifactId;

  private String filterBetaArtifactId;

  private String filterAlphaPolicyName;

  private String navLinkArtifactId;

  private String sortSuffix;

  private String sortAaaPolicyActualName;

  private String sortZzzPolicyActualName;

  private String zeroThreatArtifactId;

  private String metricsNamespaceArtifactId;

  private String metricsMaliciousArtifactId;

  private String containerWaiverPolicyName;

  private String tabsQuarantineArtifactId;

  private String tabsContainerRepoId;

  private String containerQuarantineRepoId;

  @After
  public void resetContainerImagesEvalFlag() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
  }

  @Before
  public void openFirewallDashboardAsAdmin() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    setFeatures(
        LicensedFeature.FIREWALL,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    playwrightHardreset();
    playwrightRefreshOrOpen(FirewallPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_tabsFeatureGating_hiddenByDefaultVisibleWhenContainerEnabled() {
    seedTabsTestQuarantineComponent();
    seedTabsTestContainerQuarantineComponent();

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    FirewallDashboardRegressionAssertions assertions = new FirewallDashboardRegressionAssertions(firewallPage);

    assertThat(firewallPage.container()).isVisible();
    assertions.shouldHideOuterTabBar();
    firewallPage.tab(DATA.quarantineTabId()).click();
    assertThat(firewallPage.tabPanel(DATA.quarantineTabId())).isVisible(VISIBLE_OPTS);
    // Navigate directly to quarantine URL — tab click can race cached API data in debug mode.
    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());
    // Name filter prevents pagination from hiding this component under forkCount=4 parallel load.
    firewallPage.componentNameFilter().fill(tabsQuarantineArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(tabsQuarantineArtifactId))).hasCount(DATA.singleRowCount(),
            SLOW_COUNT_OPTS);

    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    // Full reload required so the SPA re-fetches productFeatures with CONTAINER_IMAGES_EVAL_ENABLED.
    playwrightRefresh();
    assertions.shouldShowOuterTabBar();
    firewallPage.tab(DATA.containersTabId()).click();
    assertThat(firewallPage.tab(DATA.containerQuarantineTabId())).isVisible(VISIBLE_OPTS);
    firewallPage.tab(DATA.containerQuarantineTabId()).click();
    assertions.shouldShowTabPanel(DATA.containerQuarantineTabId());
    playwrightRefreshOrOpen(FirewallRegressionPage.containerQuarantineTabUrl());
    assertThat(firewallPage.containerQuarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(tabsContainerRepoId))).hasCount(DATA.singleRowCount(),
            SLOW_COUNT_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_welcomeModal_showsFromLocalStorageFlagHidesOnClose() {
    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    FirewallDashboardRegressionAssertions assertions = new FirewallDashboardRegressionAssertions(firewallPage);

    // Set flag then reload — the welcome-modal localStorage check runs on #/firewall/dashboard.
    page.evaluate("localStorage.setItem('SHOW_FIREWALL_WELCOME_MODAL', 'true')");
    playwrightRefresh();
    assertions.shouldShowWelcomeModal();
    firewallPage.welcomeModalCloseButton().click();
    assertions.shouldHideWelcomeModal();

    // Subsequent visit must NOT re-show the modal (localStorage cleared by clicking Close).
    playwrightRefreshOrOpen(FirewallPage.url());
    assertThat(firewallPage.container()).isVisible();
    assertions.shouldHideWelcomeModal();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_statusIndicator_greenWhenAllProtectedAmberWhenNot() {
    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    FirewallDashboardRegressionAssertions assertions = new FirewallDashboardRegressionAssertions(firewallPage);

    seedFullyProtectedRepository();
    playwrightRefreshOrOpen(FirewallPage.url());
    assertThat(firewallPage.container()).isVisible();
    assertions.shouldShowGreenStatus();

    seedUnprotectedRepository();
    playwrightRefreshOrOpen(FirewallPage.url());
    assertThat(firewallPage.container()).isVisible();
    assertions.shouldShowAmberStatus();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_limitedAccessUser_alertShownDashboardHidden() {
    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    FirewallDashboardRegressionAssertions assertions = new FirewallDashboardRegressionAssertions(firewallPage);

    Organization childOrg = tempEntity.newOrganization();
    User limitedUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(childOrg.getId(), Role.DEVELOPER_ROLE_ID, limitedUser.getUsername());
    playwrightLogout();
    // NEXUS-53680: LimitedFirewallAccessAlert is no longer set by /firewall/releaseQuarantine/configuration
    // (loadConfiguration's 403 branch was removed in PR #16520). The banner is now driven only by the
    // waiver-list slices, so a limited user sees it on the Firewall Waivers page, not the dashboard.
    playwrightLoginAt(FirewallRegressionPage.waiversContainersApprovedUrl(), limitedUser.getUsername(),
        TemporaryEntity.USER_PASSWORD_CLEAR);
    playwrightRefresh();
    assertions.shouldShowLimitedAccessAlert();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_quarantineTableFilters_eachFilterReducesVisibleRows() {
    seedQuarantineFilterComponents();
    // Full reload — FirewallPage's loadPolicies() runs in a mount-only useEffect, and hash-only
    // navigations (dashboard → quarantine tab) don't remount the SPA so cached empty state sticks.
    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());
    playwrightRefresh();

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    // Scope via name filter — under forkCount=4 even recent components can be pushed past page 1.
    firewallPage.componentNameFilter().fill(filterAlphaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterAlphaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                SLOW_COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    // Beta is quarantined 200 days ago — sorted below recent entries; search by name to avoid pagination.
    firewallPage.componentNameFilter().fill(filterBetaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterBetaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();

    firewallPage.componentNameFilter().fill(filterAlphaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterAlphaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    // After clearing alpha name filter — search for beta by name to avoid pagination flakiness.
    firewallPage.componentNameFilter().fill(filterBetaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterBetaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();

    firewallPage.repositoryFilter().fill(DATA.alphaRepoPublicId());
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterAlphaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.repositoryFilterClearButton().click();
    // After clearing repo filter — search for beta by name to avoid pagination flakiness.
    firewallPage.componentNameFilter().fill(filterBetaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterBetaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();

    firewallPage.quarantineTimeFilterToggle().click();
    firewallPage.quarantineTimeFilterOption(DATA.timeFilterPast30Days()).click();
    firewallPage.componentNameFilter().fill(filterAlphaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterAlphaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    firewallPage.quarantineTimeFilterToggle().click();
    firewallPage.quarantineTimeFilterOption(DATA.timeFilterAll()).click();
    // "All" time filter includes beta (200-day-old component); name filter scopes the result.
    firewallPage.componentNameFilter().fill(filterBetaArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterBetaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();

    firewallPage.policyFilterToggle().click();
    firewallPage.policyFilterOption(filterAlphaPolicyName).click();
    assertThat(firewallPage.quarantineTableRows()
        .filter(
            new Locator.FilterOptions().setHasText(filterAlphaArtifactId))).hasCount(DATA.quarantineFilteredRows(),
                COUNT_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_quarantineTableNavLinks_navigateToComponentAndRepoPages() {
    seedNavLinkComponent();
    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    firewallPage.componentNameFilter().fill(navLinkArtifactId);
    Locator navRow = firewallPage.quarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(navLinkArtifactId));
    assertThat(navRow).hasCount(DATA.singleRowCount(), COUNT_OPTS);

    FirewallComponentDetailsRegressionPage detailsPage = new FirewallComponentDetailsRegressionPage();
    firewallPage.componentDetailsLinkInRow(navRow).click();
    assertThat(detailsPage.container()).isVisible();

    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());
    firewallPage.componentNameFilter().fill(navLinkArtifactId);
    navRow = firewallPage.quarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(navLinkArtifactId));
    assertThat(navRow).hasCount(DATA.singleRowCount(), COUNT_OPTS);

    FirewallRepositoryResultsRegressionPage repoResultsPage = new FirewallRepositoryResultsRegressionPage();
    firewallPage.quarantineTableRepoLinkInRow(navRow).click();
    assertThat(repoResultsPage.container()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_quarantineTableSorting_policyNameAndQuarantineTime() {
    seedQuarantineSortComponents();
    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    // Narrow the table to only our two seeded rows so parallel-test rows don't affect hasCount.
    firewallPage.componentNameFilter().fill(sortSuffix);
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilterTotalRows(), COUNT_OPTS);

    firewallPage.policyNameColumnHeader().click();
    assertThat(firewallPage.quarantineTablePolicyNameCell(0)).containsText(sortZzzPolicyActualName);
    Assertions.assertThat(firewallPage.quarantineTablePolicyNameCells().allInnerTexts())
        .isSortedAccordingTo(Comparator.reverseOrder());

    firewallPage.policyNameColumnHeader().click();
    assertThat(firewallPage.quarantineTablePolicyNameCell(0)).containsText(sortAaaPolicyActualName);
    Assertions.assertThat(firewallPage.quarantineTablePolicyNameCells().allInnerTexts())
        .isSortedAccordingTo(Comparator.naturalOrder());

    // Quarantine time descending (newest first): aaa quarantined now, zzz quarantined 7 days ago.
    firewallPage.quarantineTimeColumnHeader().click();
    assertThat(firewallPage.quarantineTablePolicyNameCell(0)).containsText(sortAaaPolicyActualName);
    Assertions.assertThat(firewallPage.quarantineTablePolicyNameCells().allInnerTexts())
        .isSortedAccordingTo(Comparator.naturalOrder());

    // Quarantine time ascending (oldest first): zzz before aaa.
    firewallPage.quarantineTimeColumnHeader().click();
    assertThat(firewallPage.quarantineTablePolicyNameCell(0)).containsText(sortZzzPolicyActualName);
    Assertions.assertThat(firewallPage.quarantineTablePolicyNameCells().allInnerTexts())
        .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_quarantineTable_refreshTimestampAndZeroThreatLevel() {
    seedZeroThreatLevelComponent();
    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    // The "No data found." text is not asserted here because other parallel test instances
    // may have seeded quarantine rows into the shared database, making that assertion fragile.
    assertThat(firewallPage.quarantineTimestamp()).containsText(DATA.timestampUpdatedText());

    firewallPage.quarantineRefreshButton().click();
    // Asserting the static "Updated" prefix rather than a changed timestamp: the relative-time
    // granularity ("just now") does not change within a single test run, so before/after
    // text capture would produce a flaky not().hasText() failure.
    assertThat(firewallPage.quarantineTimestamp()).containsText(DATA.timestampUpdatedText());

    firewallPage.componentNameFilter().fill(zeroThreatArtifactId);
    Locator ztRow = firewallPage.quarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(zeroThreatArtifactId));
    assertThat(ztRow).hasCount(DATA.singleRowCount(), COUNT_OPTS);
    assertThat(firewallPage.quarantineThreatLevelCellIn(ztRow)).containsText(DATA.zeroThreatLevelText());
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_roiTab_hiddenWhenRoiParamAbsent() {
    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    assertThat(firewallPage.tab(DATA.roiTabId())).isHidden();
    assertThat(firewallPage.tab(DATA.quarantineTabId())).isVisible(
        VISIBLE_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_firewallMetrics_filterShortcutLinksApplyPolicyFilter() {
    seedMetricsFilterComponents();
    playwrightRefreshOrOpen(FirewallPage.url());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    assertThat(firewallPage.firewallMetrics()).isVisible(VISIBLE_OPTS);

    // ProprietaryNameConflict condition type is unique to this test class, but forkCount=4 may run
    // two instances simultaneously — use not().hasCount(0) rather than exact hasCount(1).
    firewallPage.metricsDetailsButton(DATA.metricsNamespaceAttacksId()).click();
    assertThat(firewallPage.quarantineTableRows()).not().hasCount(0, SLOW_COUNT_OPTS);
    assertThat(firewallPage.quarantineTableThreatLevelCell(0))
        .containsText(String.valueOf(DATA.namespaceThreatLevel()), SLOW_TEXT_OPTS);

    // Same reasoning: MALICIOUS_CODE is seeded only by this test, but parallel forks may
    // double-seed it — assert at-least-one rather than exactly-one.
    firewallPage.metricsDetailsButton(DATA.metricsSupplyChainAttacksId()).click();
    assertThat(firewallPage.quarantineTableRows()).not().hasCount(0, SLOW_COUNT_OPTS);
    assertThat(firewallPage.quarantineTableThreatLevelCell(0))
        .containsText(String.valueOf(DATA.maliciousThreatLevel()), SLOW_TEXT_OPTS);

    // "Components quarantined" clears all policy-type filters — other parallel tests may have
    // quarantine rows, so use UUID name filters to scope assertions to our two seeded rows.
    firewallPage.metricsDetailsButton(DATA.metricsComponentsQuarantinedId()).click();
    firewallPage.componentNameFilter().fill(metricsNamespaceArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(metricsNamespaceArtifactId))).hasCount(
            DATA.quarantineFilteredRows(), SLOW_COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    firewallPage.componentNameFilter().fill(metricsMaliciousArtifactId);
    assertThat(firewallPage.quarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(metricsMaliciousArtifactId))).hasCount(
            DATA.quarantineFilteredRows(), SLOW_COUNT_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_containerQuarantineTable_refreshAndMultiPolicyDisplay() {
    seedContainerQuarantineComponents();
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    playwrightRefreshOrOpen(FirewallPage.url());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    firewallPage.openContainerSubTab(DATA.containerQuarantineTabId());

    Locator containerRow = firewallPage.containerQuarantineTableRows()
        .filter(new Locator.FilterOptions().setHasText(containerQuarantineRepoId));
    assertThat(containerRow).hasCount(DATA.singleRowCount(), COUNT_OPTS);
    assertThat(containerRow.locator(".iq-policy-cell .nx-truncate-ellipsis")).containsText(
        DATA.multiPolicyTypesText());
    assertThat(firewallPage.containerQuarantineTimestamp()).containsText(DATA.timestampUpdatedText());

    firewallPage.containerQuarantineRefreshButton().click();
    // Same reasoning as quarantineTimestamp: relative-time granularity prevents reliable
    // before/after capture; asserting the static prefix confirms the element re-renders.
    assertThat(firewallPage.containerQuarantineTimestamp()).containsText(DATA.timestampUpdatedText());
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallDashboard_containerWaiverTable_nullExpiryDisplaysNever() {
    seedContainerWaivers();
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    playwrightRefreshOrOpen(FirewallPage.url());
    playwrightNavigateTo(FirewallRegressionPage.waiversContainersApprovedUrl());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    Locator waiverRow = firewallPage.containerWaiverTableRows()
        .filter(new Locator.FilterOptions().setHasText(containerWaiverPolicyName));
    assertThat(waiverRow).hasCount(DATA.singleRowCount(), COUNT_OPTS);
    assertThat(firewallPage.containerWaiverExpiryCellIn(waiverRow)).containsText(DATA.waiverNeverExpiryText());
  }

  private void seedFullyProtectedRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newProxyRepository(repositoryManager, "maven-central-" + TemporaryEntity.uuid(), DATA.maven2Format(),
        true, true);
  }

  private void seedUnprotectedRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newProxyRepository(repositoryManager, "unprotected-" + TemporaryEntity.uuid(), DATA.maven2Format(),
        true, false);
  }

  private void seedTabsTestQuarantineComponent() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    tabsQuarantineArtifactId = "tabs-art" + suffix;
    String policyName = "tabs-policy" + suffix;
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyName);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager, "tabs-repo" + suffix, true, true);

    Date now = Date.from(Instant.now());
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("tabs-grp" + suffix, tabsQuarantineArtifactId,
            DATA.componentVersion());

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "tabs-path" + suffix, uuid.substring(0, 8), identifier, now, now);
    tempEntity.newRepositoryPolicyViolation(
        component.getRepositoryId(), DATA.violationThreatLevel(), component.getPathname(), false,
        FailActionType.ID, policy.getId(), policyName, component.getComponentIdentifier());
  }

  private void seedTabsTestContainerQuarantineComponent() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    tabsContainerRepoId = "tabs-docker" + suffix;

    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository dockerRepo = tempEntity.newProxyRepository(
        repoManager, tabsContainerRepoId, DATA.dockerFormat(), true, true);

    Organization org = tempEntity.newOrganization("tabs-cq-org" + suffix);
    org.setRelatedRepositoryId(dockerRepo.getId());
    lookup(OrganizationDAO.class).update(org);

    Application app = tempEntity.newApplication("tabs-cq-app" + suffix, org.getId());
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), DATA.proxyStage(), "tabs-cq-scan" + suffix);
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "tabs-cq-p" + suffix);
    tempEntity.newPolicyViolation(eval, policy, DATA.violationThreatLevel(), PolicyThreatCategory.OTHER,
        "tabs-cq-grp" + suffix, "tabs-cq-art" + suffix, DATA.componentVersion(), uuid.substring(0, 8),
        FailActionType.ID);
  }

  private void seedQuarantineFilterComponents() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repoAlpha = tempEntity.newRepository(repositoryManager, DATA.alphaRepoPublicId() + suffix, true, true);
    Repository repoBeta = tempEntity.newRepository(repositoryManager, DATA.betaRepoPublicId() + suffix, true, true);

    // Attach policies to the seeded repositories so they surface in /api/v2/policies for the admin —
    // root-org-owned policies don't reach ApiPolicyService.filterPolicies when the fork has no apps.
    filterAlphaPolicyName = DATA.alphaPolicyName() + suffix;
    Policy policyAlpha = tempEntity.newPolicy(repoAlpha.getId(), filterAlphaPolicyName, DATA.violationThreatLevel());
    Policy policyBeta =
        tempEntity.newPolicy(repoBeta.getId(), DATA.betaPolicyName() + suffix, DATA.violationThreatLevel());

    Date now = Date.from(Instant.now());
    Date oldDate = Date.from(Instant.now().minus(DATA.oldQuarantineDays(), ChronoUnit.DAYS));

    filterAlphaArtifactId = DATA.alphaComponentNameFilter() + suffix;
    filterBetaArtifactId = DATA.betaComponentNameFilter() + suffix;

    ComponentIdentifier identifierAlpha =
        ComponentIdentifier.createMavenCoordinates(DATA.alphaComponentGroup(), filterAlphaArtifactId,
            DATA.componentVersion());
    ComponentIdentifier identifierBeta =
        ComponentIdentifier.createMavenCoordinates(DATA.betaComponentGroup(), filterBetaArtifactId,
            DATA.componentVersion());

    ProxyRepositoryComponent c1 = tempEntity.newRepositoryComponent(
        repoAlpha.getId(), MatchState.EXACT, "path-alpha" + suffix, "ha" + uuid.substring(0, 8), identifierAlpha, now,
        now);
    tempEntity.newRepositoryPolicyViolation(
        c1.getRepositoryId(), DATA.violationThreatLevel(), c1.getPathname(), false,
        FailActionType.ID, policyAlpha.getId(), filterAlphaPolicyName, c1.getComponentIdentifier());

    ProxyRepositoryComponent c2 = tempEntity.newRepositoryComponent(
        repoBeta.getId(), MatchState.EXACT, "path-beta" + suffix, "hb" + uuid.substring(0, 8), identifierBeta, now,
        oldDate);
    tempEntity.newRepositoryPolicyViolation(
        c2.getRepositoryId(), DATA.violationThreatLevel(), c2.getPathname(), false,
        FailActionType.ID, policyBeta.getId(), DATA.betaPolicyName() + suffix, c2.getComponentIdentifier());
  }

  private void seedNavLinkComponent() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    navLinkArtifactId = "nav-art" + suffix;
    String policyName = DATA.navLinkPolicyName() + suffix;
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyName);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager, "nav-repo" + suffix, true, true);

    Date now = Date.from(Instant.now());
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("nav-grp" + suffix, navLinkArtifactId, DATA.componentVersion());

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "nav-path" + suffix, uuid.substring(0, 8), identifier, now, now);
    tempEntity.newRepositoryPolicyViolation(
        component.getRepositoryId(), DATA.violationThreatLevel(), component.getPathname(), false,
        FailActionType.ID, policy.getId(), policyName, component.getComponentIdentifier());
  }

  private void seedQuarantineSortComponents() {
    String uuid = TemporaryEntity.uuid();
    sortSuffix = uuid;
    String suffix = "-" + uuid;
    sortAaaPolicyActualName = DATA.sortAaaPolicyName() + suffix;
    sortZzzPolicyActualName = DATA.sortZzzPolicyName() + suffix;
    Policy policyAaa = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, sortAaaPolicyActualName);
    Policy policyZzz = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, sortZzzPolicyActualName);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager, "qs-repo" + suffix, true, true);

    Date now = Date.from(Instant.now());
    Date sevenDaysAgo = Date.from(Instant.now().minus(DATA.sortOlderDays(), ChronoUnit.DAYS));

    ComponentIdentifier identifierAaa =
        ComponentIdentifier.createMavenCoordinates("qs-aaa-grp" + suffix, "qs-aaa-art" + suffix,
            DATA.componentVersion());
    ComponentIdentifier identifierZzz =
        ComponentIdentifier.createMavenCoordinates("qs-zzz-grp" + suffix, "qs-zzz-art" + suffix,
            DATA.componentVersion());

    ProxyRepositoryComponent cAaa = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "qs-path-aaa" + suffix, "ha" + uuid.substring(0, 8), identifierAaa, now, now);
    tempEntity.newRepositoryPolicyViolation(
        cAaa.getRepositoryId(), DATA.violationThreatLevel(), cAaa.getPathname(), false,
        FailActionType.ID, policyAaa.getId(), sortAaaPolicyActualName, cAaa.getComponentIdentifier(), now);

    ProxyRepositoryComponent cZzz = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "qs-path-zzz" + suffix, "hz" + uuid.substring(0, 8), identifierZzz, now,
        sevenDaysAgo);
    tempEntity.newRepositoryPolicyViolation(
        cZzz.getRepositoryId(), DATA.violationThreatLevel(), cZzz.getPathname(), false,
        FailActionType.ID, policyZzz.getId(), sortZzzPolicyActualName, cZzz.getComponentIdentifier(), sevenDaysAgo);
  }

  private void seedZeroThreatLevelComponent() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    zeroThreatArtifactId = "zt-art" + suffix;
    String policyName = DATA.zeroThreatPolicyName() + suffix;
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyName);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager, "zt-repo" + suffix, true, true);

    Date now = Date.from(Instant.now());
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("zt-grp" + suffix, zeroThreatArtifactId, DATA.componentVersion());

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "zt-path" + suffix, uuid.substring(0, 8), identifier, now, now);
    tempEntity.newRepositoryPolicyViolation(
        component.getRepositoryId(), 0, component.getPathname(), false,
        FailActionType.ID, policy.getId(), policyName, component.getComponentIdentifier());
  }

  private void seedMetricsFilterComponents() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    metricsNamespaceArtifactId = "ml-ns-art" + suffix;
    metricsMaliciousArtifactId = "ml-mal-art" + suffix;
    String namespacePolicyName = DATA.namespaceConflictPolicyName() + suffix;
    String maliciousCodePolicyName = DATA.maliciousCodePolicyName() + suffix;

    Policy namespacePolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID,
        namespacePolicyName,
        new Condition(ProprietaryNameConflictConditionType.ID, ProprietaryNameConflictConditionType.OP_IS_PRESENT));

    Policy maliciousCodePolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID,
        maliciousCodePolicyName,
        new Condition(SecurityVulnerabilityCategoryConditionType.ID, "is",
            SecurityVulnerabilityCategory.MALICIOUS_CODE.getId()));

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager, "ml-repo" + suffix, true, true);

    Date now = Date.from(Instant.now());
    ComponentIdentifier identifierNs =
        ComponentIdentifier.createMavenCoordinates("ml-ns-grp" + suffix, metricsNamespaceArtifactId,
            DATA.componentVersion());
    ComponentIdentifier identifierMal =
        ComponentIdentifier.createMavenCoordinates("ml-mal-grp" + suffix, metricsMaliciousArtifactId,
            DATA.componentVersion());

    ProxyRepositoryComponent cNs = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "ml-ns-path" + suffix, "hn" + uuid.substring(0, 8), identifierNs, now, now);
    tempEntity.newRepositoryPolicyViolation(
        cNs.getRepositoryId(), DATA.namespaceThreatLevel(), cNs.getPathname(), false,
        FailActionType.ID, namespacePolicy.getId(), namespacePolicyName,
        cNs.getComponentIdentifier());

    ProxyRepositoryComponent cMal = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "ml-mal-path" + suffix, "hm" + uuid.substring(0, 8), identifierMal, now, now);
    tempEntity.newRepositoryPolicyViolation(
        cMal.getRepositoryId(), DATA.maliciousThreatLevel(), cMal.getPathname(), false,
        FailActionType.ID, maliciousCodePolicy.getId(), maliciousCodePolicyName,
        cMal.getComponentIdentifier());
  }

  private void seedContainerQuarantineComponents() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    containerQuarantineRepoId = "cq-docker" + suffix;
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository dockerRepo = tempEntity.newProxyRepository(
        repoManager, containerQuarantineRepoId, DATA.dockerFormat(), true, true);

    Organization org = tempEntity.newOrganization("cq-org" + suffix);
    org.setRelatedRepositoryId(dockerRepo.getId());
    lookup(OrganizationDAO.class).update(org);

    Application app = tempEntity.newApplication("cq-app" + suffix, org.getId());
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), DATA.proxyStage(), "cq-scan" + suffix);

    for (int i = 1; i <= 2; i++) {
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "cq-p" + i + suffix);
      tempEntity.newPolicyViolation(eval, policy, DATA.violationThreatLevel(), PolicyThreatCategory.OTHER,
          "cq-grp" + i + suffix, "cq-art" + i + suffix, DATA.componentVersion(), "cqh" + i + uuid.substring(0, 8),
          FailActionType.ID);
    }
  }

  private void seedContainerWaivers() {
    // NEXUS-53680 rewrote container-waiver scoping: /rest/policyWaivers/repository_container/... now
    // filters by owner_id = container-image application, not the virtual REPOSITORY_CONTAINER_ID.
    // Waivers must be stored under a container-image app (proxy repo → org.relatedRepositoryId → app)
    // with isForContainerImage=true.
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository proxyRepo =
        tempEntity.newProxyRepository(repoManager, "cw-repo-" + TemporaryEntity.uuid(), DATA.dockerFormat(), true,
            true);
    Organization containerImageOrg = tempEntity.newOrganization();
    containerImageOrg.setRelatedRepositoryId(proxyRepo.getId());
    lookup(OrganizationDAO.class).update(containerImageOrg);
    Application containerImage = tempEntity.newApplication(containerImageOrg.getId());

    containerWaiverPolicyName = "cw-p1-" + TemporaryEntity.uuid();
    Policy policy = tempEntity.newPolicy(containerImage.getOrganizationId(), containerWaiverPolicyName);
    PolicyWaiver waiver =
        new PolicyWaiver(null, policy.getId(), containerImage.getId(), DATA.waiverComment());
    waiver.setForContainerImage(true);
    tempEntity.newWaiver(waiver);
  }

  /** Typed view of {@code src/test/resources/test-data/firewall-dashboard.json}. */
  public record Data(
      String timestampUpdatedText,
      String zeroThreatLevelText,
      String multiPolicyTypesText,
      String waiverNeverExpiryText,
      String timeFilterPast30Days,
      String timeFilterAll,
      String metricsNamespaceAttacksId,
      String metricsSupplyChainAttacksId,
      String metricsComponentsQuarantinedId,
      String alphaRepoPublicId,
      String betaRepoPublicId,
      String alphaPolicyName,
      String betaPolicyName,
      String alphaComponentGroup,
      String alphaComponentNameFilter,
      String betaComponentGroup,
      String betaComponentNameFilter,
      String sortAaaPolicyName,
      String sortZzzPolicyName,
      String navLinkPolicyName,
      String zeroThreatPolicyName,
      String namespaceConflictPolicyName,
      String maliciousCodePolicyName,
      String componentVersion,
      String maven2Format,
      String dockerFormat,
      String proxyStage,
      String waiverComment,
      int violationThreatLevel,
      int namespaceThreatLevel,
      int maliciousThreatLevel,
      int sortOlderDays,
      int oldQuarantineDays,
      int quarantineFilterTotalRows,
      int quarantineFilteredRows,
      int singleRowCount,
      String quarantineTabId,
      String containersTabId,
      String containerQuarantineTabId,
      String roiTabId)
  {
  }
}
