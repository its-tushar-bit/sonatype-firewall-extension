/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class FirewallServiceTest
    extends AbstractComponentTest
{
  @Inject
  FirewallService firewallService;

  @Inject
  private InsightConfig config;

  @Inject
  private TestProductLicense testProductLicense;

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
  }

  @Test
  public void testGetFirewallStatus_FeatureFlag_True() {
    //set experimental feature
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    // expect feature flag to be true
    assertThat(firewallService.getFirewallStatus().experimentalFeatures)
        .containsEntry(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true);
  }

  @Test
  public void testGetFirewallStatus_FeatureFlag_False() {
    //set experimental feature
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    //then: expect feature flag to be false
    assertThat(firewallService.getFirewallStatus().experimentalFeatures)
        .containsEntry(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false);
  }

  @Test
  public void testGetFirewallStatus_NoFirewallAutoUnquarantineFeature() {
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    //when: setting firewall auto unquarantine
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        firewallService.getFirewallStatus());
  }

  @Test
  public void testGetFirewallStatus_NoReleaseIntegrityFeature() {
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: setting firewall auto unquarantine
    //then: expect invalid license exception

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        firewallService.getFirewallStatus());
  }
}
