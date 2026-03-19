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

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
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
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class FirewallPageTest
    extends AbstractFunctionalTest
{
  private final FirewallPage page = new FirewallPage();

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

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
    page.firewallStatus()
        .statusPartiallyProtected()
        .shouldBe(Condition.text("0 of 1 repositories protected"))
        .shouldBe(visible);
    page.firewallStatus().componentsMonitored().shouldBe(Condition.text("4 Components Monitored")).shouldBe(visible);

    eyesWatcher.eyesCheck("Firewall Status - Partially Protected");
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
  public void testFirewallQuarantineTable_TableBodyCount() {
    setupData();
    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallQuarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallQuarantineTable().tableBodyRows().shouldHave(texts("g : a : v", "g : a : v"));

    eyesWatcher.eyesCheck("Quarantine Grid visible with data");
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
}
