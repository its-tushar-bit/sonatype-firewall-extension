/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;

public class FirewallPageTest
    extends AbstractFunctionalTest
{
  private final FirewallPage page = new FirewallPage();

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    //Clear the experimental feature flag after running the test
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    hardreset();
  }

  @Test
  public void testFirewallAutoUnquarantineFeatureIsNotSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(hidden);
    page.firewallQuarantineStatus().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallQuarantine().shouldBe(hidden);
    page.firewallAutoUnquarantine().shouldBe(hidden);
    page.firewallQuarantineTable().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantineFeatureSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallQuarantineStatus().shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallQuarantine().shouldBe(visible);
    page.firewallAutoUnquarantine().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
  }
}
