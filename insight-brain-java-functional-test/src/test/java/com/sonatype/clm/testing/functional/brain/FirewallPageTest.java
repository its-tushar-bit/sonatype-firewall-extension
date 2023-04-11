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
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxTableHeader;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantine;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineTable;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.junit.After;
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
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class FirewallPageTest
    extends AbstractFunctionalTest
{
  private final FirewallPage page = new FirewallPage();

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @BeforeClass
  public static void beforeClass() {
    setupData();
  }

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);

    hardreset();
  }

  public static void setupData() {
    Policy policy = staticTempEntity.newPolicy();
    RepositoryManager repositoryManager = staticTempEntity.newRepositoryManager("1");
    Repository repository = staticTempEntity.newRepository(repositoryManager, "maven-central", true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    RepositoryComponent repositoryComponent1 = staticTempEntity.newRepositoryComponent(repository.getId(),
        "g:a:1", date1, date1, true);
    staticTempEntity.newRepositoryPolicyViolation(repositoryComponent1, policy.getId());

    RepositoryComponent repositoryComponent2 = staticTempEntity.newRepositoryComponent(repository.getId(),
        "g:a:2", date2, date2, true);
    staticTempEntity.newRepositoryPolicyViolation(repositoryComponent2, policy.getId());

    RepositoryComponent repositoryComponent3 = staticTempEntity.newRepositoryComponent(repository.getId(),
        "g:a:3", date1, null, false);
    staticTempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent3.getPathname(), false,
        FailActionType.ID, policy.getId(), "policyName", repositoryComponent3.getComponentIdentifier());

    RepositoryComponent repositoryComponent4 = staticTempEntity.newRepositoryComponent(repository.getId(),
        "g:a:4", date2, null, false);
    staticTempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent4.getPathname(), false,
        FailActionType.ID, policy.getId(), "policyName", repositoryComponent4.getComponentIdentifier());
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240)).pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  private void waitUntilSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallComponentDetailsPage.getAllLoadingSpinners().get(0)));
    firewallComponentDetailsPage.getAllLoadingSpinners().shouldHave(size(0));
  }

  @Test
  public void testFirewallPage_AutoUnquarantinePageLoads() {
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallQuarantineStatus().shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallQuarantine().shouldBe(visible);
    page.firewallAutoReleaseQuarantine().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallPage_AutoReleaseQuarantine_showsCount() {
    refreshOrOpen(FirewallPage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantine firewallAutoUnquarantine = page.firewallAutoReleaseQuarantine();
    firewallAutoUnquarantine.shouldBe(visible);
    firewallAutoUnquarantine.shouldBe(visible);
    firewallAutoUnquarantine.cardContent().shouldBe(Condition.text("2"));
    firewallAutoUnquarantine.autoUnquarantineLink().shouldBe(visible);
  }

  @Test
  public void testFirewallPage_OpenCloseModal() {
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);

    page.firewallAutoUnquarantineStatus().configureLink().click();
    page.firewallConfigurationModal().shouldBe(visible);
    page.firewallConfigurationModal().loadError().shouldBe(hidden);
    page.firewallConfigurationModal().saveButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineCheckBoxIntegrityRating().shouldNotBe(checked);

    eyesWatcher.eyesCheck("Firewall configuration modal is present.");

    page.firewallConfigurationModal().cancelButton().click();
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallPage_AutoUnquarantineLink() {
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    // click button
    page.firewallAutoReleaseQuarantine().autoUnquarantineLink().click();

    // verify firewall page loads
    waitUntilUrl(FirewallAutoUnquarantinePage.url());
  }

  @Test
  public void testFirewallQuarantineTable_TableBodyCount() {
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallQuarantineTable().tableBodyRows().shouldHaveSize(2);
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : a : v", "g : a : v"));

    eyesWatcher.eyesCheck("Quarantine Grid visible with data");
  }

  @Test
  public void testFirewallQuarantineTable_Sorting() {
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    NxTableHeader quarantineTimeHeader = page.firewallQuarantineTable().quarantineTimeHeader();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date ascending"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date descending"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
  }

  @Test
  public void testFirewallQuarantineTable_ComponentNameSearch() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "b1", "v"), true);
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "testFirewallQuarantineTable_ComponentNameSearch");
    tempEntity.newRepositoryPolicyViolation(repositoryComponent.getRepositoryId(), 5, repositoryComponent.getPathname(),
        false, FailActionType.ID, policy.getId(), policy.getName(), repositoryComponent.getComponentIdentifier());

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    FirewallQuarantineTable firewallQuarantineTable = page.firewallQuarantineTable();
    // We initially have 3 rows
    firewallQuarantineTable.tableBodyRows().shouldHaveSize(3);
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : a : v", "g : a : v", "g : b1 : v"));
    // One character in the component name search should not trigger the search
    firewallQuarantineTable.componentNameInput().sendKeys("b");
    firewallQuarantineTable.tableBodyRows().shouldHaveSize(3);
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : a : v", "g : a : v", "g : b1 : v"));
    // Two characters in the component name search should trigger the search
    firewallQuarantineTable.componentNameInput().sendKeys("1");
    firewallQuarantineTable.tableBodyRows().shouldHaveSize(1);
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : b1 : v"));
    // Zero characters in the component name search should trigger the search
    firewallQuarantineTable.componentNameInput().sendKeys("\b\b");
    firewallQuarantineTable.tableBodyRows().shouldHaveSize(3);
    firewallQuarantineTable.tableBodyRows().shouldHave(texts("g : a : v", "g : a : v", "g : b1 : v"));
  }

  @Test
  public void testFirewallQuarantineTable_RepoViewLink() {
    refreshOrOpen(FirewallPage.url());
    page.firewallQuarantineTable().tableBodyRows().get(0).find("#iq-firewall-quarantine-table--repo-view-link").click();
    Selenide.switchTo().window(1);
    RepositoryReportContainerPage.title().shouldHave(text("maven-central Repository Results"));
  }

  @Test
  public void testRedirectToComponentDetailsPage() {
    refreshOrOpen(FirewallPage.url());

    page.firewallQuarantineTable().tableBodyRows().get(0).find("#iq-firewall-quarantine-table--component-details-page")
        .click();

    waitUntilSpinnersGone();
    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
  }
}
