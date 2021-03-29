/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineMtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineYtd;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;

public class FirewallAutoUnquarantinePageTest
    extends AbstractFunctionalTest
{
  private final FirewallAutoUnquarantinePage page = new FirewallAutoUnquarantinePage();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

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
    page.firewallPolicyConditionTypes().shouldBe(hidden);
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
    page.firewallPolicyConditionTypes().shouldBe(visible);
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
    firewallAutoUnquarantineMtd.cardContent().shouldBe(Condition.text("0"));
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
    firewallAutoUnquarantineYtd.cardContent().shouldBe(Condition.text("0"));
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

    page.firewallPolicyConditionTypes().moreLink().click();
    page.firewallConfigurationModal().shouldBe(visible);
    page.firewallConfigurationModal().loadError().shouldBe(hidden);
    page.firewallConfigurationModal().saveButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineCheckBoxIntegrityRating().shouldNotBe(checked);

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
    page.firewallPolicyConditionTypes().shouldBe(hidden);
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
    page.firewallPolicyConditionTypes().shouldBe(visible);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(visible);
    page.loadError().shouldBe(hidden);
    page.retryButton().shouldBe(hidden);
  }
}
