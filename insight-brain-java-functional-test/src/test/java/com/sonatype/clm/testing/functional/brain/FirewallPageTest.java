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
import com.sonatype.clm.testing.functional.elements.NxTableHeader;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallMetricsContent;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineTable;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
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
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
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
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD);
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
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240)).pollingEvery(Duration.ofSeconds(2))
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
  public void testFirewallPage_StatusPartiallyProtected() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallStatus().statusFullyProtected().shouldNotBe(visible);
    page.firewallStatus().statusPartiallyProtected()
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
    page.firewallStatus().statusFullyProtected()
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
    FirewallMetrics firewallMetrics5
        = new FirewallMetrics(toLocalDate(testDate), SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 20);
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

    FirewallMetricsContent supplyChainAttacksBlocked
        = page.firewallMetricsContent("#firewall-metrics-content-supply-chain-attacks-blocked");
    supplyChainAttacksBlocked.shouldBe(visible);
    supplyChainAttacksBlocked.value().shouldHave(text("1(all time)"));
    supplyChainAttacksBlocked.link().click();
    ScrollUtil.awaitEndOfScrolling(supplyChainAttacksBlocked.link());
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(1));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("maliciousCode"));

    FirewallMetricsContent namespaceAttacksBlocked
        = page.firewallMetricsContent("#firewall-metrics-content-namespace-attacks-blocked");
    namespaceAttacksBlocked.shouldBe(visible);
    namespaceAttacksBlocked.value().shouldHave(text("2(all time)"));
    namespaceAttacksBlocked.link().click();
    ScrollUtil.awaitEndOfScrolling(namespaceAttacksBlocked.link());
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("namespaceConflict", "namespaceConflict"));

    FirewallMetricsContent componentsQuarantined
        = page.firewallMetricsContent("#firewall-metrics-content-components-quarantined");
    componentsQuarantined.shouldBe(visible);
    componentsQuarantined.value().shouldHave(text("4Last 12 months"));
    componentsQuarantined.link().click();
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(4));

    FirewallMetricsContent componentsAutoReleased
        = page.firewallMetricsContent("#firewall-metrics-content-components-auto-released");
    componentsAutoReleased.shouldBe(visible);
    componentsAutoReleased.value().shouldHave(text("10Last 12 months"));
    componentsAutoReleased.link().click();
    autoUnquarantinePage.title().shouldHave(text("Auto Release from Quarantine"));
    autoUnquarantinePage.backToFirewallButton().click();

    FirewallMetricsContent componentsAutoSelected
        = page.firewallMetricsContent("#firewall-metrics-content-safe-components-auto-selected");
    componentsAutoSelected.shouldBe(visible);
    componentsAutoSelected.value().shouldHave(text("20Last 12 months"));
    componentsAutoSelected.link().click();
    Selenide.switchTo().window(1);

    Selenide.switchTo().window(0);

    FirewallMetricsContent componentsWaived
        = page.firewallMetricsContent("#firewall-metrics-content-components-waived");
    componentsWaived.shouldBe(visible);
    componentsWaived.value().shouldHave(text("30Last 12 months"));
    componentsWaived.link().click();
    // Confirm you're on the dashboard's waivers tab by confirming that its selected
    assertThat(DashboardPage.waiversTab().getElement().attr("aria-selected")).isEqualTo("true");
  }

  @Test
  public void testFirewallQuarantineTable() {
    /*
    This test data covers all below scenarios.
    1. sort by quarantine time (default desc)
    2. sort by threat level for same quarantine times
    3. sort by component name for same threat level & quarantine times
    4. sort by quarantine time asc
    5. excluding waived violations, 'warn' action policies and unquarantined components
    6. selecting the valid highest threat level and policy name combination for a quarantined component
    7. policy id and component name filters
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

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Time descending"));

    firewallPage.firewallQuarantineTable().tableBodyRows().shouldHave(size(5));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(0)
        .shouldHave(texts("10", "policy2", "2024-01-02 14:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(1)
        .shouldHave(texts("7", "policy3", "2024-01-02 14:00:00", "g : a : v", "repo2"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(2)
        .shouldHave(texts("10", "policy1", "2024-01-02 12:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(3)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g1 : a1 : v1", "repo2"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(4)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g : a : v", "repo1"));

    eyesWatcher.eyesCheck("Firewall Quarantine table visible with data");

    quarantineTimeHeader.click();
    waitUntilFirewallPageSpinnersGone();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Time ascending"));

    firewallPage.firewallQuarantineTable().tableBodyRows().shouldHave(size(5));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(0)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g1 : a1 : v1", "repo2"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(1)
        .shouldHave(texts("7", "policy3", "2024-01-01 12:10:05", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(2)
        .shouldHave(texts("10", "policy1", "2024-01-02 12:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(3)
        .shouldHave(texts("10", "policy2", "2024-01-02 14:00:00", "g : a : v", "repo1"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(4)
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

    Long time = repositoryComponentDAO.getAllQuarantinedComponent().stream()
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
        repositoryComponent.getComponentIdentifier()
    );

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

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Time descending"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Time ascending"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Time descending"));
    quarantineTimeHeader.click();
  }

  @Test
  public void testFirewallQuarantineTable_ComponentNameSearch() {
    setupData();
    Long time = repositoryComponentDAO.getAllQuarantinedComponent().stream()
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
    firewallQuarantineTable.tableBodyRows().shouldHave(texts( "g : b1 : v", "g : a : v", "g : a : v"));
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

    page.firewallQuarantineTable().tableBodyRows().get(0).find("#iq-firewall-quarantine-table--component-details-page")
        .click();

    waitUntilComponentDetailsPageSpinnersGone();
    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
  }

  private Policy createTestPolicyWithCondition(
      String name,
      boolean withSecurityVulnerabilityCategoryMaliciousCodeCondition,
      boolean withProprietaryNameConflictCondition )
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

  @Test
  public void testFirewallPage_QuarantineStatusCount() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    tempEntity.newProxyRepository(repositoryManager, "proxyRepo1", "maven", true, true);
    tempEntity.newProxyRepository(repositoryManager, "proxyRepo2", "npm",true, false);
    tempEntity.newHostedRepository(repositoryManager, "hostedRepo2", "maven", false);
    tempEntity.newHostedRepository(repositoryManager, "hostedRepo1", "npm", true);

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallStatus().statusPartiallyProtected()
        .shouldBe(Condition.text("1 of 2 repositories protected"))
        .shouldBe(visible);

    eyesWatcher.eyesCheck("Firewall Status - Quarantine Status Count");
  }
}
