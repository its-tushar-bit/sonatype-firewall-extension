/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineMtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineYtd;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;

public class FirewallAutoUnquarantinePageTest
    extends AbstractFunctionalTest
{
  private final FirewallAutoUnquarantinePage page = new FirewallAutoUnquarantinePage();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @BeforeClass
  public static void beforeClass() throws Exception {
    setupData();
  }

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    //Clear the experimental feature flag after running the test
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);

    hardreset();
  }

  public static void setupData() {
    RepositoryManager repositoryManager = staticTempEntity.newRepositoryManager("1");
    Repository repository = staticTempEntity.newRepository(repositoryManager, "central", true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    staticTempEntity.newRepositoryComponent(repository.getId(), "g:a:1", date1, date1, true);
    staticTempEntity.newRepositoryComponent(repository.getId(), "g:a:2", date2, date2, true);
    staticTempEntity.newRepositoryComponent(repository.getId(), "g:a:3", new Date(), new Date(), false);
    staticTempEntity.newRepositoryComponent(repository.getId(), "g:a:4", new Date(), null, false);
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoUnquarantineFeatureIsNotSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(hidden);
    page.firewallUnquarantineTable().shouldBe(hidden);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.backToFirewallButton().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoUnquarantineFeatureSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    eyesWatcher.eyesCheck("Auto Unquarantine Page - Auto Unquarantine Feature is Set");

    page.shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(visible);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(visible);
    page.firewallUnquarantineTable().shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.backToFirewallButton().shouldBe(visible);
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoReleaseQuarantineMtd_showsCount() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantineMtd firewallAutoUnquarantineMtd = page.firewallAutoReleaseQuarantineMtd();
    firewallAutoUnquarantineMtd.shouldBe(visible);
    firewallAutoUnquarantineMtd.shouldBe(visible);
    firewallAutoUnquarantineMtd.cardContent().shouldBe(text("2"));
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoReleaseQuarantineYtd_showsCount() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantineYtd firewallAutoUnquarantineYtd = page.firewallAutoReleaseQuarantineYtd();
    firewallAutoUnquarantineYtd.shouldBe(visible);
    firewallAutoUnquarantineYtd.shouldBe(visible);
    firewallAutoUnquarantineYtd.cardContent().shouldBe(text("2"));
  }

  @Test
  public void testFirewallAutoUnquarantinePage_OpenCloseModal() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);

    page.firewallAutoUnquarantineStatus().configureLink().click();
    page.firewallConfigurationModal().shouldBe(visible);
    page.firewallConfigurationModal().loadError().shouldBe(hidden);
    page.firewallConfigurationModal().saveButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineCheckBoxIntegrityRating().shouldNotBe(checked);

    eyesWatcher.eyesCheck("Auto Unquarantine Page - Auto Release from Quarantine Configuration Modal");

    page.firewallConfigurationModal().cancelButton().click();
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantinePage_BackToFirewallButton() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    // click button
    page.backToFirewallButton().click();

    // verify firewall page loads
    waitUntilUrl(FirewallPage.url());
  }

  @Test
  public void testFirewallAutoUnquarantinePage_LoadErrorTest() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    eyesWatcher.eyesCheck("Auto Unquarantine Page - Load Error");

    //verify initial status with error
    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(hidden);
    page.loadError().shouldBe(visible);
    page.retryButton().shouldBe(visible);

    //resolve error
    testProductLicense.reset();

    //retry
    page.retryButton().click();

    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(visible);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(visible);
    page.loadError().shouldBe(hidden);
    page.retryButton().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantineTable_TableBodyCount() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallUnquarantineTable().tableBodyRows().shouldHaveSize(2);
    page.firewallUnquarantineTable().tableBodyRows().shouldHave(texts("g : a : v", "g : a : v"));

    eyesWatcher.eyesCheck("Auto Unquarantine Grid visible with data");
  }

  @Test
  public void testFirewallAutoUnquarantineTable_Sorting() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    SelenideElement quarantineTimeHeader = page.firewallUnquarantineTable().quarantineTimeHeader();
    SelenideElement releaseQuarantineTimeHeader = page.firewallUnquarantineTable().releaseQuarantineTimeHeader();

    quarantineTimeHeader.shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.shouldHave(
        attribute("aria-label", "Quarantine Date ascending"));
    releaseQuarantineTimeHeader.shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.shouldHave(
        attribute("aria-label", "Quarantine Date descending"));
    releaseQuarantineTimeHeader.shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
    releaseQuarantineTimeHeader.click();

    quarantineTimeHeader.shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.shouldHave(
        attribute("aria-label", "Date Cleared ascending"));
    releaseQuarantineTimeHeader.click();

    quarantineTimeHeader.shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.shouldHave(
        attribute("aria-label", "Date Cleared descending"));
    releaseQuarantineTimeHeader.click();

    quarantineTimeHeader.shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
  }
}
