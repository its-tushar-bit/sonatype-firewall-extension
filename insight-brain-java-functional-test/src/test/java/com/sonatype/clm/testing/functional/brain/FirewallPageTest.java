/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversResults;
import com.sonatype.clm.testing.functional.elements.NxTableHeader;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.ContainerQuarantineTile;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.ContainerWaiverTile;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallMetricsContent;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineTable;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.RoiFirewallMetrics;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.CRITICAL;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_AUTO_RELEASED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_QUARANTINED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallPageTest
    extends AbstractFunctionalTest
{
  private final FirewallPage page = new FirewallPage();

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

  private final FirewallAutoUnquarantinePage autoUnquarantinePage = new FirewallAutoUnquarantinePage();

  private FirewallMetricsDAO firewallMetricsDAO;

  private PolicyDAO policyDAO;

  private RepositoryComponentDAO repositoryComponentDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    firewallMetricsDAO = lookup(FirewallMetricsDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    repositoryComponentDAO = lookup(RepositoryComponentDAO.class);

    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
  }

  private void setupData() {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central", true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(),
        "g:a:1", date1, date1, true);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent1, policy.getId());

    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(),
        "g:a:2", date2, date2, true);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent2, policy.getId());

    RepositoryComponent repositoryComponent3 = tempEntity.newRepositoryComponent(repository.getId(),
        "g:a:3", date1, null, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent3.getPathname(), false,
        FailActionType.ID, policy.getId(), "policyName", repositoryComponent3.getComponentIdentifier());

    RepositoryComponent repositoryComponent4 = tempEntity.newRepositoryComponent(repository.getId(),
        "g:a:4", date2, null, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent4.getPathname(), false,
        FailActionType.ID, policy.getId(), "policyName", repositoryComponent4.getComponentIdentifier());
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  private void waitUntilFirewallPageSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(page.getAllLoadingSpinners().get(0)));
    firewallComponentDetailsPage.getAllLoadingSpinners().shouldHave(size(0));
  }

  private void waitUntilComponentDetailsPageSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallComponentDetailsPage.getAllLoadingSpinners().get(0)));
  }

  @Test
  public void testFirewallPage_ShowComponentsTabPanelWhenContainerImagesEvalEnabled() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallPageTabs().tab("components").shouldBe(visible);
    page.firewallPageTabs().tab("containers").shouldBe(visible);
    page.firewallPageTabs().tabPanel("components").shouldBe(visible);
    page.firewallPageTabs().tabPanel("containers").shouldNotBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallMetrics().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
  }

  @Test
  public void testFirewallPage_NotShowComponentsTabPanelWhenContainerImagesEvalDisabled() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallPageTabs().tab("components").shouldNotBe(visible);
    page.firewallPageTabs().tab("containers").shouldNotBe(visible);
    page.firewallPageTabs().tabPanel("components").shouldNotBe(visible);
    page.firewallPageTabs().tabPanel("containers").shouldNotBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallMetrics().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
  }

  @Test
  public void testFirewallPage_StatusPartiallyProtected() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallStatus().statusFullyProtected().shouldNotBe(visible);
    page.firewallStatus()
        .statusPartiallyProtected()
        .shouldBe(Condition.text("0 of 1 repositories protected"))
        .shouldBe(visible);
    page.firewallStatus().componentsMonitored().shouldBe(Condition.text("4 Components Monitored")).shouldBe(visible);

    eyesWatcher.eyesCheck("Firewall Status - Partially Protected");
  }

  @Test
  public void testFirewallPage_StatusFullyProtected() {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central", true, true);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(),
        "g:a:1", date1, date1, true);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent1, policy.getId());

    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(),
        "g:a:2", date2, date2, true);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent2, policy.getId());
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallStatus().statusPartiallyProtected().shouldNotBe(visible);
    page.firewallStatus()
        .statusFullyProtected()
        .shouldBe(Condition.text("1 of 1 repositories protected"))
        .shouldBe(visible);
    page.firewallStatus().componentsMonitored().shouldBe(Condition.text("2 Components Monitored")).shouldBe(visible);

    eyesWatcher.eyesCheck("Firewall Status - Fully Protected");
  }

  @Test
  public void testFirewallPage_AutoUnquarantinePageLoads() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallMetrics() {
    Policy maliciousCodePolicy =
        createTestPolicyWithCondition("maliciousCode", true, false);
    Policy namespaceConflictPolicy = createTestPolicyWithCondition("namespaceConflict", false, true);
    Policy notMaliciousCodeOrNamespaceConflictPolicy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "notMaliciousCodeOrNamespaceConflict");
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "maven-central", true, false);
    Date testDate = new Date();

    RepositoryComponent repositoryComponentMaliciousCode =
        tempEntity.newRepositoryComponent(
            repository.getId(),
            MatchState.EXACT,
            "a:a:1",
            "a:a:1",
            ComponentIdentifier.createMavenCoordinates("a", "a", "1"),
            testDate,
            testDate);
    tempEntity.newRepositoryPolicyViolation(
        repository.getId(),
        10,
        repositoryComponentMaliciousCode.getPathname(),
        false,
        FailActionType.ID,
        maliciousCodePolicy.getId(),
        maliciousCodePolicy.getName(),
        repositoryComponentMaliciousCode.getComponentIdentifier());

    RepositoryComponent repositoryComponentNamespaceConflict1 =
        tempEntity.newRepositoryComponent(
            repository.getId(),
            MatchState.EXACT,
            "b:a:1",
            "b:a:1",
            ComponentIdentifier.createMavenCoordinates("b", "a", "1"),
            testDate,
            testDate);
    tempEntity.newRepositoryPolicyViolation(
        repository.getId(),
        10,
        repositoryComponentNamespaceConflict1.getPathname(),
        false,
        FailActionType.ID,
        namespaceConflictPolicy.getId(),
        namespaceConflictPolicy.getName(),
        repositoryComponentNamespaceConflict1.getComponentIdentifier());

    RepositoryComponent repositoryComponentNamespaceConflict2 =
        tempEntity.newRepositoryComponent(
            repository.getId(),
            MatchState.EXACT,
            "c:a:1",
            "c:a:1",
            ComponentIdentifier.createMavenCoordinates("c", "a", "1"),
            testDate,
            testDate);
    tempEntity.newRepositoryPolicyViolation(
        repository.getId(),
        10,
        repositoryComponentNamespaceConflict2.getPathname(),
        false,
        FailActionType.ID,
        namespaceConflictPolicy.getId(),
        namespaceConflictPolicy.getName(),
        repositoryComponentNamespaceConflict2.getComponentIdentifier());

    RepositoryComponent repositoryNotMaliciousCodeOrNamespaceConflict =
        tempEntity.newRepositoryComponent(
            repository.getId(),
            MatchState.EXACT,
            "d:a:1",
            "d:a:1",
            ComponentIdentifier.createMavenCoordinates("d", "a", "1"),
            testDate,
            testDate);
    tempEntity.newRepositoryPolicyViolation(
        repository.getId(),
        5,
        repositoryNotMaliciousCodeOrNamespaceConflict.getPathname(),
        false,
        FailActionType.ID,
        notMaliciousCodeOrNamespaceConflictPolicy.getId(),
        notMaliciousCodeOrNamespaceConflictPolicy.getName(),
        repositoryNotMaliciousCodeOrNamespaceConflict.getComponentIdentifier());

    FirewallMetrics firewallMetrics1 = new FirewallMetrics(toLocalDate(testDate), SUPPLY_CHAIN_ATTACKS_BLOCKED, 1);
    FirewallMetrics firewallMetrics2 = new FirewallMetrics(toLocalDate(testDate), NAMESPACE_ATTACKS_BLOCKED, 2);
    FirewallMetrics firewallMetrics3 = new FirewallMetrics(toLocalDate(testDate), COMPONENTS_QUARANTINED, 4);
    FirewallMetrics firewallMetrics4 = new FirewallMetrics(toLocalDate(testDate), COMPONENTS_AUTO_RELEASED, 10);
    FirewallMetrics firewallMetrics5 =
        new FirewallMetrics(toLocalDate(testDate), SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 20);
    FirewallMetrics firewallMetrics6 = new FirewallMetrics(toLocalDate(testDate), WAIVED_COMPONENTS, 30);

    firewallMetricsDAO.insert(firewallMetrics1);
    firewallMetricsDAO.insert(firewallMetrics2);
    firewallMetricsDAO.insert(firewallMetrics3);
    firewallMetricsDAO.insert(firewallMetrics4);
    firewallMetricsDAO.insert(firewallMetrics5);
    firewallMetricsDAO.insert(firewallMetrics6);

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallMetrics().shouldBe(visible);

    FirewallMetricsContent supplyChainAttacksBlocked =
        page.firewallMetricsContent("#firewall-metrics-content-supply-chain-attacks-blocked");
    supplyChainAttacksBlocked.shouldBe(visible);
    supplyChainAttacksBlocked.value().shouldHave(text("1(all time)"));
    supplyChainAttacksBlocked.link().click();
    ScrollUtil.awaitEndOfScrolling(supplyChainAttacksBlocked.link());
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(1));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("maliciousCode"));

    FirewallMetricsContent namespaceAttacksBlocked =
        page.firewallMetricsContent("#firewall-metrics-content-namespace-attacks-blocked");
    namespaceAttacksBlocked.shouldBe(visible);
    namespaceAttacksBlocked.value().shouldHave(text("2(all time)"));
    namespaceAttacksBlocked.link().click();
    ScrollUtil.awaitEndOfScrolling(namespaceAttacksBlocked.link());
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("namespaceConflict", "namespaceConflict"));

    FirewallMetricsContent componentsQuarantined =
        page.firewallMetricsContent("#firewall-metrics-content-components-quarantined");
    componentsQuarantined.shouldBe(visible);
    componentsQuarantined.value().shouldHave(text("4Last 12 months"));
    componentsQuarantined.link().click();
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(4));

    FirewallMetricsContent componentsAutoReleased =
        page.firewallMetricsContent("#firewall-metrics-content-components-auto-released");
    componentsAutoReleased.shouldBe(visible);
    componentsAutoReleased.value().shouldHave(text("10Last 12 months"));
    componentsAutoReleased.link().click();
    autoUnquarantinePage.title().shouldHave(text("Auto Release from Quarantine"));
    autoUnquarantinePage.backToFirewallButton().click();

    FirewallMetricsContent componentsAutoSelected =
        page.firewallMetricsContent("#firewall-metrics-content-safe-components-auto-selected");
    componentsAutoSelected.shouldBe(visible);
    componentsAutoSelected.value().shouldHave(text("20Last 12 months"));
    componentsAutoSelected.link().click();
    Selenide.switchTo().window(1);
  }

  @Test
  public void testFirewallQuarantineTable() {
    /*
     * This test data covers all below scenarios.
     * 1. sort by quarantine time (default desc)
     * 2. sort by threat level for same quarantine times
     * 3. sort by component name for same threat level & quarantine times
     * 4. sort by quarantine time asc
     * 5. excluding waived violations, 'warn' action policies and unquarantined components
     * 6. selecting the valid highest threat level and policy name combination for a quarantined component
     * 7. policy id and component name filters
     */
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 10, 5).toInstant(ZoneOffset.UTC));
    Date jan1st2024hour14 = Date.from(LocalDateTime.of(2024, 1, 1, 14, 0).toInstant(ZoneOffset.UTC));
    Date jan2nd2024hour12 = Date.from(LocalDateTime.of(2024, 1, 2, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan2nd2024hour14 = Date.from(LocalDateTime.of(2024, 1, 2, 14, 0).toInstant(ZoneOffset.UTC));

    Policy policy1 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy2 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy2", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy3 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy3", 7, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy4 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy4", 10, Action.ID_WARN,
        Stage.ID_PROXY, null);
    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    RepositoryManager rm2 = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(rm1, "repo1", true, true);
    Repository repo2 = tempEntity.newRepository(rm2, "repo2", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname1", jan2nd2024hour14, null);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo2.getId(), "pathname2", jan2nd2024hour14, null);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname3", jan2nd2024hour12, null);
    final RepositoryComponent c4 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname4", jan1st2024hour12, null);
    final RepositoryComponent c5 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "pathname5", "hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), jan1st2024hour12, jan1st2024hour12);
    tempEntity.newRepositoryComponent(repo2.getId(), "pathname6", jan1st2024hour14, jan1st2024hour12);
    PolicyViolationTestHelper.createPolicyViolationFail(policy1, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy2, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy3, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy3, c2, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationWarn(policy4, c2, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy1, c3, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy3, c4, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationWaived(policy2, c5, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy3, c5, tempEntity);

    refreshOrOpen(FirewallPage.url());
    waitUntilFirewallPageSpinnersGone();

    FirewallPage firewallPage = new FirewallPage();
    NxTableHeader quarantineTimeHeader = firewallPage.firewallQuarantineTable().quarantineTimeHeader();

    quarantineTimeHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Quarantine Time descending"));

    firewallPage.firewallQuarantineTable().tableBodyRows().shouldHave(size(5));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy2", "2024-01-02 14:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("7", "policy3", "2024-01-02 14:00:00", "g : a : v", "repo2"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(2)
        .shouldHave(texts("10", "policy1", "2024-01-02 12:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(3)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g1 : a1 : v1", "repo2"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(4)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g : a : v", "repo1"));

    eyesWatcher.eyesCheck("Firewall Quarantine table visible with data");

    quarantineTimeHeader.click();
    waitUntilFirewallPageSpinnersGone();

    quarantineTimeHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Quarantine Time ascending"));

    firewallPage.firewallQuarantineTable().tableBodyRows().shouldHave(size(5));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g1 : a1 : v1", "repo2"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(2)
        .shouldHave(texts("10", "policy1", "2024-01-02 12:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(3)
        .shouldHave(texts("10", "policy2", "2024-01-02 14:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(4)
        .shouldHave(texts("7", "policy3", "2024-01-02 14:00:00", "g : a : v", "repo2"));
  }

  @Test
  public void testFirewallQuarantineTable_TableBodyCount() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : a : v", "g : a : v"));

    eyesWatcher.eyesCheck("Quarantine Grid visible with data");
  }

  @Test
  public void testFirewallQuarantineTable_Filtering() {
    setupData();

    Long time = repositoryComponentDAO.getAllQuarantinedComponent()
        .stream()
        .map(RepositoryComponent::getQuarantineTime)
        .map(Date::getTime)
        .max(Comparator.naturalOrder())
        .orElse(null);

    assertThat(time).isNotNull();

    Date date = new Date(time + 1);

    Repository repository = tempEntity.newRepository();
    String pathname = TemporaryEntity.uuid();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        pathname, pathname.substring(0, Math.min(pathname.length(), 20)),
        ComponentIdentifier.createMavenCoordinates("g", "b1", "v"), date, date);
    Policy anotherPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "testPolicyOne");

    tempEntity.newRepositoryPolicyViolation(
        repositoryComponent.getRepositoryId(), 5, repositoryComponent.getPathname(),
        false, FailActionType.ID, anotherPolicy.getId(), anotherPolicy.getName(),
        repositoryComponent.getComponentIdentifier());

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    waitUntilFirewallPageSpinnersGone();
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(3));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : b1 : v", "g : a : v", "g : a : v"));
    page.firewallQuarantineTable().policyNameSelect().shouldBe(visible).click();
    ElementsCollection filterCheckboxes = page.firewallQuarantineTable().policyNameCheckboxes();
    filterCheckboxes.get(0).click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : a : v", "g : a : v"));
    filterCheckboxes.get(0).click();
    filterCheckboxes.get(1).click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(1));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : b1 : v"));
    page.firewallQuarantineTable().policyFilterReset().click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(3));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : b1 : v", "g : a : v", "g : a : v"));
  }

  @Test
  public void testFirewallQuarantineTable_Sorting() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    NxTableHeader quarantineTimeHeader = page.firewallQuarantineTable().quarantineTimeHeader();

    quarantineTimeHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Quarantine Time descending"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Quarantine Time ascending"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Quarantine Time descending"));
    quarantineTimeHeader.click();
  }

  @Test
  public void testFirewallQuarantineTable_SortByPolicyName() {
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan1st2024hour14 = Date.from(LocalDateTime.of(2024, 1, 1, 14, 0).toInstant(ZoneOffset.UTC));
    Policy policy1 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy2 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy2", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    RepositoryManager rm = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(rm, "repo", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo.getId(), "pathname1", jan1st2024hour14, null);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo.getId(), "pathname2", jan1st2024hour12, null);
    PolicyViolationTestHelper.createPolicyViolationFail(policy1, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy2, c2, tempEntity);

    refreshOrOpen(FirewallPage.url());
    waitUntilFirewallPageSpinnersGone();
    FirewallPage firewallPage = new FirewallPage();
    NxTableHeader policyNameHeader = page.firewallQuarantineTable().policyNameHeader();

    firewallPage.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));

    policyNameHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Policy Name unsorted"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy1", "2024-01-01 14:00:00", "g : a : v", "repo"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy2", "2024-01-01 12:00:00", "g : a : v", "repo"));

    policyNameHeader.click();

    policyNameHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Policy Name descending"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy2", "2024-01-01 12:00:00", "g : a : v", "repo"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy1", "2024-01-01 14:00:00", "g : a : v", "repo"));

    policyNameHeader.click();

    policyNameHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Policy Name ascending"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy1", "2024-01-01 14:00:00", "g : a : v", "repo"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy2", "2024-01-01 12:00:00", "g : a : v", "repo"));
  }

  @Test
  public void testFirewallQuarantineTable_SortByComponent() {
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan1st2024hour14 = Date.from(LocalDateTime.of(2024, 1, 1, 14, 0).toInstant(ZoneOffset.UTC));
    Policy policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    RepositoryManager rm = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(rm, "repo", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname1", "hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), jan1st2024hour14, jan1st2024hour14);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname2", "hash",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), jan1st2024hour12, jan1st2024hour12);
    PolicyViolationTestHelper.createPolicyViolationFail(policy, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy, c2, tempEntity);

    refreshOrOpen(FirewallPage.url());
    waitUntilFirewallPageSpinnersGone();
    FirewallPage firewallPage = new FirewallPage();
    NxTableHeader componentHeader = page.firewallQuarantineTable().componentHeader();

    firewallPage.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));

    componentHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Component unsorted"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy", "2024-01-01 14:00:00", "g1 : a1 : v1", "repo"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy", "2024-01-01 12:00:00", "g2 : a2 : v2", "repo"));

    componentHeader.click();

    componentHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Component descending"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy", "2024-01-01 12:00:00", "g2 : a2 : v2", "repo"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy", "2024-01-01 14:00:00", "g1 : a1 : v1", "repo"));

    componentHeader.click();

    componentHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Component ascending"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy", "2024-01-01 14:00:00", "g1 : a1 : v1", "repo"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy", "2024-01-01 12:00:00", "g2 : a2 : v2", "repo"));
  }

  @Test
  public void testFirewallQuarantineTable_SortByRepository() {
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan1st2024hour14 = Date.from(LocalDateTime.of(2024, 1, 1, 14, 0).toInstant(ZoneOffset.UTC));
    Policy policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    RepositoryManager rm = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(rm, "repo1", true, true);
    Repository repo2 = tempEntity.newRepository(rm, "repo2", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname1", jan1st2024hour14, null);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo2.getId(), "pathname2", jan1st2024hour12, null);
    PolicyViolationTestHelper.createPolicyViolationFail(policy, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy, c2, tempEntity);

    refreshOrOpen(FirewallPage.url());
    waitUntilFirewallPageSpinnersGone();
    FirewallPage firewallPage = new FirewallPage();
    NxTableHeader repositoryHeader = page.firewallQuarantineTable().repositoryHeader();

    repositoryHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Repository unsorted"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy", "2024-01-01 14:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy", "2024-01-01 12:00:00", "g : a : v", "repo2"));

    repositoryHeader.click();

    repositoryHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Repository descending"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy", "2024-01-01 12:00:00", "g : a : v", "repo2"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy", "2024-01-01 14:00:00", "g : a : v", "repo1"));

    repositoryHeader.click();

    repositoryHeader.sortBtn()
        .shouldHave(
            attribute("aria-label", "Repository ascending"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy", "2024-01-01 14:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(1)
        .shouldHave(texts("10", "policy", "2024-01-01 12:00:00", "g : a : v", "repo2"));
  }

  @Test
  public void testFirewallQuarantineTable_ComponentNameSearch() {
    setupData();
    Long time = repositoryComponentDAO.getAllQuarantinedComponent()
        .stream()
        .map(RepositoryComponent::getQuarantineTime)
        .map(Date::getTime)
        .max(Comparator.naturalOrder())
        .orElse(null);
    assertThat(time).isNotNull();
    Date date = new Date(time + 1);
    Repository repository = tempEntity.newRepository();
    String pathname = TemporaryEntity.uuid();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        pathname, pathname.substring(0, Math.min(pathname.length(), 20)),
        ComponentIdentifier.createMavenCoordinates("g", "b1", "v"), date, date);
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "testFirewallQuarantineTable_ComponentNameSearch");
    tempEntity.newRepositoryPolicyViolation(repositoryComponent.getRepositoryId(), 5, repositoryComponent.getPathname(),
        false, FailActionType.ID, policy.getId(), policy.getName(), repositoryComponent.getComponentIdentifier());

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    FirewallQuarantineTable firewallQuarantineTable = page.firewallQuarantineTable();
    // We initially have 3 rows
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : b1 : v", "g : a : v", "g : a : v"));
    // One character in the component name search should not trigger the search
    firewallQuarantineTable.componentNameInput().sendKeys("b");
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : b1 : v", "g : a : v", "g : a : v"));
    // Two characters in the component name search should trigger the search
    firewallQuarantineTable.componentNameInput().sendKeys("1");
    firewallQuarantineTable.tableBodyRows().shouldHave(size(1));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : b1 : v"));
    // Zero characters in the component name search should trigger the search
    firewallQuarantineTable.componentNameInput().sendKeys("\b\b");
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : b1 : v", "g : a : v", "g : a : v"));
  }

  @Test
  public void testFirewallQuarantineTable_RepositoryPublicIdSearch() {
    Date date = new Date();
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "testFirewallQuarantineTable_RepositoryPublicIdSearch");
    Repository repo1 = tempEntity.newRepository("repoPublicId1");
    Repository repo2 = tempEntity.newRepository("repoPublicId2");
    Repository repo3 = tempEntity.newRepository("repoPublicId3");
    RepositoryComponent comp1 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "repoPublicIdSearch1", "repoPublicIdSearch1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), date, date);
    RepositoryComponent comp2 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "repoPublicIdSearch2", "repoPublicIdSearch2",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), date, date);
    RepositoryComponent comp3 =
        tempEntity.newRepositoryComponent(repo3.getId(), MatchState.EXACT, "repoPublicIdSearch3", "repoPublicIdSearch3",
            ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), date, date);
    tempEntity.newRepositoryPolicyViolation(comp1.getRepositoryId(), 5, comp1.getPathname(), false, FailActionType.ID,
        policy.getId(), policy.getName(), comp1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(comp2.getRepositoryId(), 5, comp2.getPathname(), false, FailActionType.ID,
        policy.getId(), policy.getName(), comp2.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(comp3.getRepositoryId(), 5, comp3.getPathname(), false, FailActionType.ID,
        policy.getId(), policy.getName(), comp3.getComponentIdentifier());

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    FirewallQuarantineTable firewallQuarantineTable = page.firewallQuarantineTable();
    // We initially have 3 rows
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("repoPublicId3", "repoPublicId2", "repoPublicId1"));
    // One character in the repository public id search should not trigger the search
    firewallQuarantineTable.repositoryPublicIdInput().sendKeys("i");
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("repoPublicId3", "repoPublicId2", "repoPublicId1"));
    // Two characters in the repository public id search should trigger the search
    firewallQuarantineTable.repositoryPublicIdInput().sendKeys("d1");
    firewallQuarantineTable.tableBodyRows().shouldHave(size(1));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("repoPublicId1"));
    // Zero characters in the repository public id search should trigger the search
    firewallQuarantineTable.repositoryPublicIdInput().sendKeys("\b\b");
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("repoPublicId3", "repoPublicId2", "repoPublicId1"));
  }

  @Test
  public void testFirewallQuarantineTable_quarantineTimeSearch() {
    Date date1 = new Date(1727784000000L);
    Date date2 = new Date(1727870400000L);
    Date date3 = new Date(1727870400000L);
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "testFirewallQuarantineTable_quarantineTimeSearch");
    Repository repo1 = tempEntity.newRepository("repoPublicId1");
    Repository repo2 = tempEntity.newRepository("repoPublicId2");
    Repository repo3 = tempEntity.newRepository("repoPublicId3");
    RepositoryComponent comp1 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "repoPublicIdSearch1", "repoPublicIdSearch1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), date1, date1);
    RepositoryComponent comp2 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "repoPublicIdSearch2", "repoPublicIdSearch2",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), date2, date2);
    RepositoryComponent comp3 =
        tempEntity.newRepositoryComponent(repo3.getId(), MatchState.EXACT, "repoPublicIdSearch3", "repoPublicIdSearch3",
            ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), date3, date3);
    tempEntity.newRepositoryPolicyViolation(comp1.getRepositoryId(), 5, comp1.getPathname(), false, FailActionType.ID,
        policy.getId(), policy.getName(), comp1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(comp2.getRepositoryId(), 5, comp2.getPathname(), false, FailActionType.ID,
        policy.getId(), policy.getName(), comp2.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(comp3.getRepositoryId(), 5, comp3.getPathname(), false, FailActionType.ID,
        policy.getId(), policy.getName(), comp3.getComponentIdentifier());

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    FirewallQuarantineTable firewallQuarantineTable = page.firewallQuarantineTable();
    // We initially have 3 rows
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("repoPublicId3", "repoPublicId2", "repoPublicId1"));
    firewallQuarantineTable.quarantineTimeInput().shouldBe(visible).click();
    // 1 day ago search must have no results
    ElementsCollection quarantineTimeOptions = firewallQuarantineTable.quarantineTimeOptions();
    quarantineTimeOptions.get(0).click();
    waitUntilFirewallPageSpinnersGone();
    firewallQuarantineTable.tableBodyRows().shouldHave(size(1));
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("No data found"));
    // Selecting the option ALL must bring all the elements back
    firewallQuarantineTable.quarantineTimeInput().click();
    quarantineTimeOptions = firewallQuarantineTable.quarantineTimeOptions();
    quarantineTimeOptions.get(5).click();
    waitUntilFirewallPageSpinnersGone();
    firewallQuarantineTable.tableBodyRows().shouldHave(size(3));
  }

  @Test
  public void testFirewallQuarantineTable_RepoViewLink() {
    setupData();
    refreshOrOpen(FirewallPage.url());
    page.firewallQuarantineTable().tableBodyRows().get(0).find("#iq-firewall-quarantine-table--repo-view-link").click();
    RepositoryReportContainerPage.title().shouldHave(text("maven-central Repository Results"));
  }

  @Test
  public void testRedirectToComponentDetailsPage() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.firewallQuarantineTable()
        .tableBodyRows()
        .get(0)
        .find("#iq-firewall-quarantine-table--component-details-page")
        .click();

    waitUntilComponentDetailsPageSpinnersGone();
    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
  }

  @Test
  public void testFirewallPage_QuarantineStatusCount() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    tempEntity.newProxyRepository(repositoryManager, "proxyRepo1", "maven", true, true);
    tempEntity.newProxyRepository(repositoryManager, "proxyRepo2", "npm", true, false);
    tempEntity.newHostedRepository(repositoryManager, "hostedRepo2", "maven", false);
    tempEntity.newHostedRepository(repositoryManager, "hostedRepo1", "npm", true);

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallStatus()
        .statusPartiallyProtected()
        .shouldBe(Condition.text("1 of 2 repositories protected"))
        .shouldBe(visible);

    eyesWatcher.eyesCheck("Firewall Status - Quarantine Status Count");
  }

  @Test
  public void testFirewallPage_shouldShowProperTableWhenMetricIsClicked() {
    refreshOrOpen(FirewallPage.url());

    String quarantinedComponentsMetricId = "#firewall-metrics-content-components-quarantined";
    FirewallMetricsContent componentsQuarantinedMetric = page.firewallMetricsContent(quarantinedComponentsMetricId);
    componentsQuarantinedMetric.link().click();
    page.firewallQuarantineTable().shouldBe(visible);

    String namespaceAttacksBlockedMetricId = "#firewall-metrics-content-namespace-attacks-blocked";
    FirewallMetricsContent namespaceAttacksBlockedMetric = page.firewallMetricsContent(namespaceAttacksBlockedMetricId);
    namespaceAttacksBlockedMetric.link().click();
    page.firewallQuarantineTable().shouldBe(visible);

    String supplyChainAttacksBlockedMetricId = "#firewall-metrics-content-supply-chain-attacks-blocked";
    FirewallMetricsContent supplyChainAttacksBlockedMetric =
        page.firewallMetricsContent(supplyChainAttacksBlockedMetricId);
    supplyChainAttacksBlockedMetric.link().click();
    page.firewallQuarantineTable().shouldBe(visible);

    String componentsWaivedMetricId = "#firewall-metrics-content-components-waived";
    FirewallMetricsContent componentsWaivedMetric = page.firewallMetricsContent(componentsWaivedMetricId);
    componentsWaivedMetric.link().click();
    page.firewallWaiversTable().shouldBe(visible);
  }

  @Test
  public void testRoiFirewallMetrics_rendersCorrectly() {
    String malwareAttackPreventedSelector = "malware-attacks-prevented";
    String namespaceAttacksPreventedSelector = "namespace-attacks-prevented";
    String safeComponentsAutoSelectedSelector = "safe-components-auto-selected";

    refreshOrOpen(FirewallPage.roiTabUrl());
    page.roiFirewallMetricsTab().click();
    RoiFirewallMetrics roiFirewallMetrics = page.roiFirewallMetrics();
    roiFirewallMetrics.title().shouldHave(text("Return on Investment (ROI)"));
    roiFirewallMetrics.total().shouldHave(text("Total USD Saved$600,000"));

    roiFirewallMetrics.contentHeader(malwareAttackPreventedSelector)
        .shouldHave(text("Malware attacks prevented"));
    roiFirewallMetrics.contentHeader(namespaceAttacksPreventedSelector)
        .shouldHave(text("Namespace attacks prevented"));
    roiFirewallMetrics.contentHeader(safeComponentsAutoSelectedSelector)
        .shouldHave(text("Safe Components Auto-selected"));

    roiFirewallMetrics.contentHeaderTooltipIcon(malwareAttackPreventedSelector).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text(
            "Determined based on the number of Malware attacks prevented and the ROI value configured per attack."));
    roiFirewallMetrics.contentHeaderTooltipIcon(namespaceAttacksPreventedSelector).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text(
            "Determined based on the number of namespace attacks protected and the ROI value configured per attack."));
    roiFirewallMetrics.contentHeaderTooltipIcon(safeComponentsAutoSelectedSelector).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Determined based on the number of safe components auto-selected " +
            "and the ROI value configured per attack."));

    roiFirewallMetrics.contentValue(malwareAttackPreventedSelector).shouldHave(text("$100,000"));
    roiFirewallMetrics.contentValue(namespaceAttacksPreventedSelector).shouldHave(text("$200,000"));
    roiFirewallMetrics.contentValue(safeComponentsAutoSelectedSelector).shouldHave(text("$300,000"));
  }

  @Test
  public void testRoiFirewallMetrics_rendersCorrectDescriptionForSystemAdmin() {
    String roiFirewallMetricsDescriptionConfigurePermission = "The metrics below highlights the Return on " +
        "Investment (ROI) of your organization’s partnership with Sonatype. Configure the values for each " +
        "category based on your industry to provide accurate results. Configure ROI values";

    refreshOrOpen(FirewallPage.roiTabUrl());
    page.roiFirewallMetricsTab().click();
    RoiFirewallMetrics roiFirewallMetrics = page.roiFirewallMetrics();
    roiFirewallMetrics.description().shouldHave(text(roiFirewallMetricsDescriptionConfigurePermission));
  }

  @Test
  public void testRoiFirewallMetrics_tabDoesNotRenderIfUrlDoesNotHaveQueryParam() {
    refreshOrOpen(FirewallPage.url());
    page.roiFirewallMetricsTab().shouldNot(exist);
    page.roiFirewallMetrics().shouldNot(exist);
  }

  @Test
  public void testWaiversTable_rendersCorrectly() {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central", true, false);

    // Component identifier for the waiver
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.2.3", "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    // Dates for the waiver
    Date now = new Date();
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date threeDaysFromNow = DateUtils.addDays(now, 3);

    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash1")
        .setPolicyId(policy.getId())
        .setOwnerId(repository.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment repository")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(FirewallPage.urlToFirewallWaivers());
    WaiversResults waiversResults = DashboardPage.waiversView().results();
    WaiverTile waiver1 = waiversResults.firstWaiver();

    page.firewallWaiversTable().shouldBe(visible);
    waiversResults.waivers().shouldHave(size(1));
    waiver1.threatIndicator().shouldBe(SEVERE);
    waiver1.threatNumber().shouldHave(text("5"));
    waiver1.createTime().shouldHave(text(DateFormatUtils.format(policyWaiver1.getCreateTime(), "yyyy-MM-dd")));
    waiver1.expiryTime().shouldHave(text(DateFormatUtils.format(policyWaiver1.getExpiryTime(), "yyyy-MM-dd")));
    waiver1.policy().shouldHave(text(policy.getName()));
    waiver1.scope().shouldHave(text(repository.getName()));
    waiver1.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiver1.upgradeAvailable().shouldHave(text("—"));
  }

  @Test
  public void testContainerWaiverTable_rendersCorrectly() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    refreshOrOpen(FirewallPage.urlToFirewallContainerWaivers());

    Organization org = tempEntity.newOrganization();
    PolicyWaiver containerImageWaiver = createFirewallContainerPolicyWaiver(org, "app1", 10, 1);
    PolicyWaiver containerImageWaiver1 = createFirewallContainerPolicyWaiver(org, "app2", 6, 3);

    refresh();
    page.firewallContainerWaiversTabContent().shouldBe(visible);
    page.firewallContainerWaiversTabContent().waiverTableTitle().shouldHave(text("Containers Waived"));
    page.firewallContainerWaiversTabContent().refreshButton().shouldBe(visible);
    page.firewallContainerWaiversTabContent().waiverTable().shouldBe(visible);
    page.firewallContainerWaiversTabContent().waivers().shouldHave(size(2));

    ContainerWaiverTile waiver1 = page.firewallContainerWaiversTabContent().waiver(0);
    waiver1.threatIndicator().shouldBe(CRITICAL);
    waiver1.threatNumber().shouldHave(text("10"));
    waiver1.createTime().shouldHave(text(DateFormatUtils.format(containerImageWaiver.getCreateTime(), "yyyy-MM-dd")));
    waiver1.expiryTime().shouldHave(text(DateFormatUtils.format(containerImageWaiver.getExpiryTime(), "yyyy-MM-dd")));
    waiver1.policy().shouldHave(text("Multiple-Policy-Types(1)"));
    waiver1.scope().shouldHave(text("app1"));
    waiver1.component().shouldHave(text("Multiple Components(1)"));

    ContainerWaiverTile waiver2 = page.firewallContainerWaiversTabContent().waiver(1);
    waiver2.threatIndicator().shouldBe(SEVERE);
    waiver2.threatNumber().shouldHave(text("6"));
    waiver2.createTime().shouldHave(text(DateFormatUtils.format(containerImageWaiver1.getCreateTime(), "yyyy-MM-dd")));
    waiver2.expiryTime().shouldHave(text(DateFormatUtils.format(containerImageWaiver1.getExpiryTime(), "yyyy-MM-dd")));
    waiver2.policy().shouldHave(text("Multiple-Policy-Types(1)"));
    waiver2.scope().shouldHave(text("app2"));
    waiver2.component().shouldHave(text("Multiple Components(3)"));
  }

  @Test
  public void testContainerWaiverTable_rendersCorrectPagination() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    refreshOrOpen(FirewallPage.urlToFirewallContainerWaivers());

    Organization org = tempEntity.newOrganization();
    for (int i = 0; i < 12; i++) {
      createFirewallContainerPolicyWaiver(org, "app" + i, 10, 1);
    }

    refresh();
    page.firewallContainerWaiversTabContent().shouldBe(visible);
    page.firewallContainerWaiversTabContent().waiverTable().shouldBe(visible);
    page.firewallContainerWaiversTabContent().waivers().shouldHave(size(10));

    // on page 1
    page.firewallContainerWaiversTabContent().nextPageButton().shouldBe(visible);
    page.firewallContainerWaiversTabContent().paginationButtons().shouldHave(size(2));

    // goto next page
    page.firewallContainerWaiversTabContent().nextPageButton().click();
    waitUntilFirewallPageSpinnersGone();

    page.firewallContainerWaiversTabContent().previousPageButton().shouldBe(visible);
    page.firewallContainerWaiversTabContent().waivers().shouldHave(size(2));

    // goto previous page
    page.firewallContainerWaiversTabContent().previousPageButton().click();
    waitUntilFirewallPageSpinnersGone();

    page.firewallContainerWaiversTabContent().waivers().shouldHave(size(10));

    // goto page 2
    page.firewallContainerWaiversTabContent().paginationButtons().get(1).click();
    waitUntilFirewallPageSpinnersGone();

    page.firewallContainerWaiversTabContent().waivers().shouldHave(size(2));

    // goto page 1
    page.firewallContainerWaiversTabContent().paginationButtons().get(0).click();
    waitUntilFirewallPageSpinnersGone();

    page.firewallContainerWaiversTabContent().waivers().shouldHave(size(10));
  }

  @Test
  public void testContainerQuarantineTable_rendersCorrectly() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    refreshOrOpen(FirewallPage.urlToFirewallContainerQuarantine());

    Organization org =
        tempEntity.newOrgWithRepoManagerAndProxyRepo("org-with-repo", "docker-proxy", "docker", true, true);
    Application app = tempEntity.newApplication("test-app", "test-app", org.getId());
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, 9,
        PolicyThreatCategory.OTHER, "test-group-id", "test-artifact-id", "v1",
        "test-hash", FailActionType.ID);

    refresh();
    page.firewallContainerQuarantineTabContent()
        .quarantineTableTitle()
        .shouldHave(text("Containers Actively in Quarantine"));
    page.firewallContainerQuarantineTabContent().refreshButton().shouldBe(visible);
    page.firewallContainerQuarantineTabContent().quarantineTable().shouldBe(visible);
    page.firewallContainerQuarantineTabContent().quarantinedContainers().shouldHave(size(1));

    ContainerQuarantineTile quarantinedContainer = page.firewallContainerQuarantineTabContent().quarantinedContainer(0);
    quarantinedContainer.threatIndicator().shouldBe(CRITICAL);
    quarantinedContainer.threatNumber().shouldHave(text("9"));
    quarantinedContainer.policy().shouldHave(text("Multiple-Policy-Types(1)"));
    quarantinedContainer.quarantineTime()
        .shouldHave(text(DateFormatUtils.format(policyViolation.getOpenTime(), "yyyy-MM-dd")));
    quarantinedContainer.container().shouldHave(text("test-app"));
    quarantinedContainer.containerReportPageLink().shouldBe(visible);
    quarantinedContainer.repository().shouldHave(text("docker-proxy"));
    quarantinedContainer.repositoryResultsPageLink().shouldBe(visible);
  }

  @Test
  public void testContainerQuarantineTable_rendersPaginationCorrectly() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    refreshOrOpen(FirewallPage.urlToFirewallContainerQuarantine());

    Organization org =
        tempEntity.newOrgWithRepoManagerAndProxyRepo("org-with-repo", "docker-proxy", "docker", true, true);
    for (int i = 0; i <= 14; i++) {
      Application app = tempEntity.newApplication("test-app-" + i, "test-app-" + i, org.getId());
      Policy policy = tempEntity.newPolicy(app);
      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scanId-" + i);
      tempEntity.newPolicyViolation(policyEvaluation, policy, 9,
          PolicyThreatCategory.OTHER, "test-group-id-" + i, "test-artifact-id-" + i, "v" + i,
          "test-hash-" + i, FailActionType.ID);
    }

    refresh();
    page.firewallContainerQuarantineTabContent()
        .quarantineTableTitle()
        .shouldHave(text("Containers Actively in Quarantine"));
    page.firewallContainerQuarantineTabContent().quarantineTable().shouldBe(visible);
    page.firewallContainerQuarantineTabContent().quarantinedContainers().shouldHave(size(12));

    // goto next page
    page.firewallContainerQuarantineTabContent().nextPageButton().shouldBe(visible).click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallContainerQuarantineTabContent().quarantinedContainers().shouldHave(size(3));

    // goto previous page
    page.firewallContainerQuarantineTabContent().previousPageButton().shouldBe(visible).click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallContainerQuarantineTabContent().quarantinedContainers().shouldHave(size(12));

    // goto page 2
    page.firewallContainerQuarantineTabContent().paginationButtons().shouldHave(size(2));
    page.firewallContainerQuarantineTabContent().paginationButtons().get(1).click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallContainerQuarantineTabContent().quarantinedContainers().shouldHave(size(3));

    // goto page 1
    page.firewallContainerQuarantineTabContent().paginationButtons().get(0).click();
    waitUntilFirewallPageSpinnersGone();
    page.firewallContainerQuarantineTabContent().quarantinedContainers().shouldHave(size(12));
  }

  @Test
  public void testFirewallPage_AdminUserSeesFullContent() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.limitedFirewallAccessAlert().shouldNotBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallMetrics().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
  }

  @Test
  public void testFirewallPage_RepositoryUserSeesLimitedAccessAlert() {
    // Create a non-admin user with repository access
    User repositoryUser = tempEntity.newUser("repo.user", "Repository", "User", "repo@user.com");
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("repo-manager-1");
    Repository repository = tempEntity.newRepository(repositoryManager, "test-repo", true, false);

    // Grant repository-level permissions to the user
    tempEntity.newMembershipMapping(repository.getId(), Role.DEVELOPER_ROLE_ID, repositoryUser.getUsername(),
        MemberType.USER);

    setupData();
    refreshOrOpen(FirewallPage.url());
    logout();
    login(repositoryUser.getUsername(), repositoryUser.getPassword());
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.limitedFirewallAccessAlert().shouldBe(visible);
    page.limitedFirewallAccessAlert()
        .shouldHave(text("You have limited access to Repository Firewall based on your current permissions."));
    page.limitedFirewallAccessAlert()
        .shouldHave(text("Some data or settings may not be visible. Contact your administrator to request full " +
            "access to Repository Firewall."));

    // Verify that the full firewall content is NOT displayed
    page.firewallStatus().shouldNotBe(visible);
    page.firewallMetrics().shouldNotBe(visible);
    page.firewallQuarantineTable().shouldNotBe(visible);

    logout();
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallPage_RepositoryManagerUserSeesLimitedAccessAlert() {
    // Create a non-admin user with repository manager access
    User repositoryManagerUser = tempEntity.newUser("repo.manager.user", "Repo Manager", "User",
        "repomanager@user.com");
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("repo-manager-2");

    // Grant repository manager-level permissions to the user
    tempEntity.newMembershipMapping(repositoryManager.getId(), Role.DEVELOPER_ROLE_ID,
        repositoryManagerUser.getUsername(), MemberType.USER);

    setupData();
    refreshOrOpen(FirewallPage.url());
    logout();
    login(repositoryManagerUser.getUsername(), repositoryManagerUser.getPassword());
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.limitedFirewallAccessAlert().shouldBe(visible);
    page.limitedFirewallAccessAlert()
        .shouldHave(text("You have limited access to Repository Firewall based on your current permissions."));
    page.limitedFirewallAccessAlert()
        .shouldHave(text("Some data or settings may not be visible. Contact your administrator to request full " +
            "access to Repository Firewall."));

    // Verify that the full firewall content is NOT displayed
    page.firewallStatus().shouldNotBe(visible);
    page.firewallMetrics().shouldNotBe(visible);
    page.firewallQuarantineTable().shouldNotBe(visible);

    logout();
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallPage_NonRepositoryUserCannotAccessPage() {
    // Create a user with no permissions at all
    User nonRepositoryUser = tempEntity.newUser("non.repo.user", "Non Repository", "User", "nonrepo@user.com");

    // Don't grant any permissions - user has no access to anything

    refreshOrOpen(FirewallPage.url());
    logout();
    login(nonRepositoryUser.getUsername(), nonRepositoryUser.getPassword());
    refreshOrOpen(FirewallPage.url());

    // User should see the limited access alert
    page.limitedFirewallAccessAlert().shouldBe(visible);

    logout();
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallPage_SystemAdminWithoutRepositoryAccessSeesLimitedAccessAlert() {
    // Create a system admin user who is not a policy admin and has no repository-specific access
    User systemAdminUser = tempEntity.newUser("system.admin.user", "System Admin", "User", "sysadmin@user.com");

    // Grant system admin role at global level (not repository or repository manager specific)
    tempEntity.newMembershipMapping("global", Role.SYSTEM_ADMIN_ROLE_ID, systemAdminUser.getUsername(),
        MemberType.USER);

    setupData();
    refreshOrOpen(FirewallPage.url());
    logout();
    login(systemAdminUser.getUsername(), systemAdminUser.getPassword());
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.limitedFirewallAccessAlert().shouldBe(visible);
    page.limitedFirewallAccessAlert()
        .shouldHave(text("You have limited access to Repository Firewall based on your current permissions."));
    page.limitedFirewallAccessAlert()
        .shouldHave(text("Some data or settings may not be visible. Contact your administrator to request full " +
            "access to Repository Firewall."));

    // Verify that the full firewall content is NOT displayed
    page.firewallStatus().shouldNotBe(visible);
    page.firewallMetrics().shouldNotBe(visible);
    page.firewallQuarantineTable().shouldNotBe(visible);

    logout();
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallWaiversTable_includesRepositoryManagerWaivers() {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("rm1");
    // Create a repository under the repository manager (may be needed for setup)
    tempEntity.newRepository(repositoryManager, "maven-central", true, false);

    // Component identifier for the waiver
    String purl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.2.3", "", "jar")).getPackageUrl();

    // Dates for the waiver
    Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
    Date threeDaysFromNow = DateUtils.addDays(new Date(), 3);

    // Create waiver scoped to repository manager
    PolicyWaiver rmWaiver = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash1")
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager.getId()) // Repository Manager ID
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("repository manager waiver")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(FirewallPage.urlToFirewallWaivers());
    WaiversResults waiversResults = DashboardPage.waiversView().results();

    // Verify: Repository manager waiver is displayed
    waiversResults.waivers().shouldHave(size(1));

    WaiverTile waiver = waiversResults.firstWaiver();
    page.firewallWaiversTable().shouldBe(visible);
    waiver.threatIndicator().shouldBe(SEVERE);
    waiver.threatNumber().shouldHave(text("5"));
    waiver.createTime().shouldHave(text(DateFormatUtils.format(rmWaiver.getCreateTime(), "yyyy-MM-dd")));
    waiver.expiryTime().shouldHave(text(DateFormatUtils.format(rmWaiver.getExpiryTime(), "yyyy-MM-dd")));
    waiver.policy().shouldHave(text(policy.getName()));
    waiver.scope().shouldHave(text("rm1"));
    waiver.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiver.upgradeAvailable().shouldHave(text("—"));
  }

  @Test
  public void testFirewallWaiversTable_multipleRepositoryManagerWaivers() {
    Policy policy = tempEntity.newPolicy();

    // Create multiple repository managers
    RepositoryManager rm1 = tempEntity.newRepositoryManager("rm1");
    RepositoryManager rm2 = tempEntity.newRepositoryManager("rm2");
    RepositoryManager rm3 = tempEntity.newRepositoryManager("rm3");

    // Create repositories under each manager
    tempEntity.newRepository(rm1, "maven-central-1", true, false);
    tempEntity.newRepository(rm2, "npm-registry-2", true, false);
    tempEntity.newRepository(rm3, "pypi-registry-3", true, false);

    // Component identifiers for the waivers
    String purl1 = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.0.0", "", "jar")).getPackageUrl();
    String purl2 = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("Group2", "Artifact2", "2.0.0", "", "jar")).getPackageUrl();
    String purl3 = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("Group3", "Artifact3", "3.0.0", "", "jar")).getPackageUrl();

    // Dates for the waivers
    Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
    Date threeDaysFromNow = DateUtils.addDays(new Date(), 3);

    // Create waivers scoped to different repository managers
    tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash1")
        .setPolicyId(policy.getId())
        .setOwnerId(rm1.getId())
        .setAssociatedPackageUrl(purl1)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("rm1 waiver")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash2")
        .setPolicyId(policy.getId())
        .setOwnerId(rm2.getId())
        .setAssociatedPackageUrl(purl2)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("rm2 waiver")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash3")
        .setPolicyId(policy.getId())
        .setOwnerId(rm3.getId())
        .setAssociatedPackageUrl(purl3)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("rm3 waiver")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(FirewallPage.urlToFirewallWaivers());
    WaiversResults waiversResults = DashboardPage.waiversView().results();

    // Verify: All three repository manager waivers are displayed
    waiversResults.waivers().shouldHave(size(3));

    // Verify first waiver
    WaiverTile tile1 = waiversResults.waiver(0);
    tile1.scope().shouldHave(text("rm1"));
    tile1.component().shouldHave(text("Group1 : Artifact1 : 1.0.0"));

    // Verify second waiver
    WaiverTile tile2 = waiversResults.waiver(1);
    tile2.scope().shouldHave(text("rm2"));
    tile2.component().shouldHave(text("Group2 : Artifact2 : 2.0.0"));

    // Verify third waiver
    WaiverTile tile3 = waiversResults.waiver(2);
    tile3.scope().shouldHave(text("rm3"));
    tile3.component().shouldHave(text("Group3 : Artifact3 : 3.0.0"));
  }

  @Test
  public void testFirewallWaiversTable_repositoryManagerWaiverWithMultipleRepositories() {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("multi-repo-rm");

    // Create multiple repositories under the same repository manager
    tempEntity.newRepository(repositoryManager, "maven-central", true, false);
    tempEntity.newRepository(repositoryManager, "maven-releases", true, false);
    tempEntity.newRepository(repositoryManager, "maven-snapshots", true, false);

    // Component identifier for the waiver
    String purl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.example", "multi-repo-component", "1.5.0", "", "jar"))
        .getPackageUrl();

    // Dates for the waiver
    Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
    Date threeDaysFromNow = DateUtils.addDays(new Date(), 3);

    // Create waiver scoped to repository manager (not individual repositories)
    PolicyWaiver rmWaiver = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash-multi-repo")
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager.getId()) // Scoped to repository manager, not individual repos
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("waiver for repository manager with multiple repositories")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(FirewallPage.urlToFirewallWaivers());
    WaiversResults waiversResults = DashboardPage.waiversView().results();

    // Verify: Repository manager waiver is displayed
    waiversResults.waivers().shouldHave(size(1));

    WaiverTile waiver = waiversResults.firstWaiver();
    page.firewallWaiversTable().shouldBe(visible);
    waiver.threatIndicator().shouldBe(SEVERE);
    waiver.threatNumber().shouldHave(text("5"));
    waiver.createTime().shouldHave(text(DateFormatUtils.format(rmWaiver.getCreateTime(), "yyyy-MM-dd")));
    waiver.expiryTime().shouldHave(text(DateFormatUtils.format(rmWaiver.getExpiryTime(), "yyyy-MM-dd")));
    waiver.policy().shouldHave(text(policy.getName()));
    // Verify scope shows repository manager name, not individual repository names
    waiver.scope().shouldHave(text("multi-repo-rm"));
    waiver.component().shouldHave(text("com.example : multi-repo-component : 1.5.0"));
    waiver.upgradeAvailable().shouldHave(text("—"));
  }

  @Test
  public void testFirewallWaiversTable_repositoryManagerAndRepositoryWaivers() {
    Policy policy = tempEntity.newPolicy();

    // Create repository manager with two repositories
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("test-rm");
    tempEntity.newRepository(repositoryManager, "test-repo-1", true, false);
    Repository repository2 = tempEntity.newRepository(repositoryManager, "test-repo-2", true, false);

    // Component for repository manager waiver
    String rmPurl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.rm", "rm-component", "2.0.0", "", "jar")).getPackageUrl();

    // Component for individual repository waiver
    String repoPurl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.repo", "repo-component", "3.0.0", "", "jar")).getPackageUrl();

    // Dates for the waivers
    Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
    Date threeDaysFromNow = DateUtils.addDays(new Date(), 3);

    // Create waiver scoped to repository manager
    tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash-rm")
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager.getId())
        .setAssociatedPackageUrl(rmPurl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("repository manager waiver")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    // Create waiver scoped to individual repository
    tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash-repo")
        .setPolicyId(policy.getId())
        .setOwnerId(repository2.getId())
        .setAssociatedPackageUrl(repoPurl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("repository waiver")
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(FirewallPage.urlToFirewallWaivers());
    WaiversResults waiversResults = DashboardPage.waiversView().results();

    // Verify: Both repository manager and repository waivers are displayed
    waiversResults.waivers().shouldHave(size(2));

    // Verify repository manager waiver
    WaiverTile tile1 = waiversResults.waiver(0);
    tile1.scope().shouldHave(text("test-rm"));
    tile1.component().shouldHave(text("com.rm : rm-component : 2.0.0"));

    // Verify individual repository waiver
    WaiverTile tile2 = waiversResults.waiver(1);
    tile2.scope().shouldHave(text("test-repo-2"));
    tile2.component().shouldHave(text("com.repo : repo-component : 3.0.0"));
  }

  private Policy createTestPolicyWithCondition(
      String name,
      boolean withSecurityVulnerabilityCategoryMaliciousCodeCondition,
      boolean withProprietaryNameConflictCondition)
  {
    Policy policy = new Policy(name, name);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint("test-constraint", "Test Constraint", LogicalOperator.OR);
    constraint.addCondition(new com.sonatype.insight.brain.model.policy.Condition(
        SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    if (withSecurityVulnerabilityCategoryMaliciousCodeCondition) {
      constraint.addCondition(new com.sonatype.insight.brain.model.policy.Condition(
          SecurityVulnerabilityCategoryConditionType.ID, "is", "malicious_code"));
    }
    if (withProprietaryNameConflictCondition) {
      constraint.addCondition(new com.sonatype.insight.brain.model.policy.Condition(
          ProprietaryNameConflictConditionType.ID, "is present"));
    }
    policy.addConstraint(constraint);
    policyDAO.insert(policy);
    return policy;
  }

  private PolicyWaiver createFirewallContainerPolicyWaiver(
      Organization org,
      String appName,
      int threatNumber,
      int containerImageComponentCount)
  {
    Application app = tempEntity.newApplication(appName, appName, org.getId());
    Policy policy = tempEntity.newPolicy(app, threatNumber);
    Date now = new Date();
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date threeDaysFromNow = DateUtils.addDays(now, 3);

    for (int i = 0; i < containerImageComponentCount; i++) {
      tempEntity.newWaiver(new PolicyWaiver()
          .setHash(appName + i)
          .setPolicyId(policy.getId())
          .setOwnerId(app.getId())
          .setCreateTime(twoDaysAgo)
          .setExpiryTime(threeDaysFromNow)
          .setForContainerImage(false)
          .setForContainerImageComponent(true));
    }

    return tempEntity.newWaiver(new PolicyWaiver()
        .setHash(null)
        .setPolicyId(policy.getId())
        .setOwnerId(app.getId())
        .setCreateTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setForContainerImage(true)
        .setForContainerImageComponent(false));
  }
}
