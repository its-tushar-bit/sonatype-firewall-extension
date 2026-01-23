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
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxTableHeader;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineMtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineYtd;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.WebDriverRunner;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallAutoUnquarantinePageTest
    extends AbstractFunctionalTest
{
  private final FirewallAutoUnquarantinePage page = new FirewallAutoUnquarantinePage();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    setupData();
    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);
  }

  private void setupData() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("1");
    Repository repository = tempEntity.newRepository(repositoryManager, "central", true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    tempEntity.newRepositoryComponent(repository.getId(), "g:a:1", date1, date1, true);
    tempEntity.newRepositoryComponent(repository.getId(), "g:a:2", date2, date2, true);
    tempEntity.newRepositoryComponent(repository.getId(), "g:a:3", new Date(), new Date(), false);
    tempEntity.newRepositoryComponent(repository.getId(), "g:a:4", new Date(), null, false);
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoUnquarantinePageLoads() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    eyesWatcher.eyesCheck("Auto Unquarantine Page - Auto Unquarantine Page Loads");

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
    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantineMtd firewallAutoUnquarantineMtd = page.firewallAutoReleaseQuarantineMtd();
    firewallAutoUnquarantineMtd.shouldBe(visible);
    firewallAutoUnquarantineMtd.shouldBe(visible);
    firewallAutoUnquarantineMtd.cardContent().shouldBe(text("2"));
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoReleaseQuarantineYtd_showsCount() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantineYtd firewallAutoUnquarantineYtd = page.firewallAutoReleaseQuarantineYtd();
    firewallAutoUnquarantineYtd.shouldBe(visible);
    firewallAutoUnquarantineYtd.shouldBe(visible);
    firewallAutoUnquarantineYtd.cardContent().shouldBe(text("2"));
  }

  @Test
  public void testFirewallAutoUnquarantinePage_OpenCloseModal() {
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
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    // click button
    page.backToFirewallButton().click();

    // verify firewall page loads
    waitUntilUrl(FirewallPage.url());
  }

  @Test
  public void testFirewallAutoUnquarantinePage_LoadErrorTest() {
    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

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
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallUnquarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallUnquarantineTable().tableBodyRows().shouldHave(texts("g : a : v", "g : a : v"));
  }

  @Test
  public void testFirewallAutoUnquarantineTable_Sorting() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    NxTableHeader quarantineTimeHeader = page.firewallUnquarantineTable().quarantineTimeHeader();
    NxTableHeader releaseQuarantineTimeHeader = page.firewallUnquarantineTable().releaseQuarantineTimeHeader();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date ascending"));
    releaseQuarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
    quarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date descending"));
    releaseQuarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
    releaseQuarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Date Cleared ascending"));
    releaseQuarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Date Cleared descending"));
    releaseQuarantineTimeHeader.click();

    quarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Quarantine Date unsorted"));
    releaseQuarantineTimeHeader.sortBtn().shouldHave(
        attribute("aria-label", "Date Cleared unsorted"));
  }

  @Test
  public void testFirewallAutoUnquarantineTable_RowClickNavigatesToComponentDetails() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY);
    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);
    page.firewallUnquarantineTable().shouldBe(visible);
    page.firewallUnquarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallUnquarantineTable().tableBodyRows().first().click();
    page.shouldBe(hidden);

    String currentUrl = WebDriverRunner.url();
    assertThat(currentUrl).contains("/firewall/repository/");
    assertThat(currentUrl).contains("/component/");
    assertThat(currentUrl).contains("hash");

    FirewallComponentDetailsPage componentDetailsPage = new FirewallComponentDetailsPage();
    componentDetailsPage.shouldBe(visible);
  }

  @Test
  public void backButtonToAutoUnquarantine_whenUserCameFromAutoUnquarantinePage() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY);
    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);
    page.firewallUnquarantineTable().shouldBe(visible);
    page.firewallUnquarantineTable().tableBodyRows().shouldHave(size(2));
    page.firewallUnquarantineTable().tableBodyRows().first().click();
    page.shouldBe(hidden);
    FirewallComponentDetailsPage componentDetailsPage = new FirewallComponentDetailsPage();
    componentDetailsPage.shouldBe(visible);
    MainHeader.backButton().shouldHave(text("Back to Auto Release from Quarantine"));
    MainHeader.backButton().click();
    page.shouldBe(visible);
    page.firewallUnquarantineTable().shouldBe(visible);
    page.firewallUnquarantineTable().tableBodyRows().shouldHave(size(2));
  }
}
