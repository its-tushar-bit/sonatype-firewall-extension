/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantine;
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

public class FirewallPageTest
    extends AbstractFunctionalTest
{
  private final FirewallPage page = new FirewallPage();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallPage.url());
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
  public void testFirewallPage_AutoUnquarantineFeatureIsNotSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(hidden);
    page.firewallQuarantineStatus().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallQuarantine().shouldBe(hidden);
    page.firewallAutoReleaseQuarantine().shouldBe(hidden);
    page.firewallQuarantineTable().shouldBe(hidden);
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallPage_AutoUnquarantineFeatureSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

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
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantine firewallAutoUnquarantine = page.firewallAutoReleaseQuarantine();
    firewallAutoUnquarantine.shouldBe(visible);
    firewallAutoUnquarantine.shouldBe(visible);
    firewallAutoUnquarantine.cardContent().shouldBe(Condition.text("0"));
    firewallAutoUnquarantine.autoUnquarantineLink().shouldBe(visible);
  }

  @Test
  public void testFirewallPage_OpenCloseModal() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

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

    page.firewallConfigurationModal().cancelButton().click();
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallPage_AutoUnquarantineLink() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    // click button
    page.firewallAutoReleaseQuarantine().autoUnquarantineLink().click();

    // verify firewall page loads
    waitUntilUrl(FirewallAutoUnquarantinePage.url());
  }
}
