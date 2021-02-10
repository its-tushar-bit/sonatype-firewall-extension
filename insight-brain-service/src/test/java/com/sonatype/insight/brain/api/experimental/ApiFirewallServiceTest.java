/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallServiceTest
    extends AbstractComponentTest
{
  @Inject
  ApiFirewallService apiFirewallService;

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
  public void testGetFirewallConfiguration_Enabled() {
    //setup: enable feature flag and insert policy monitoring
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    //when: retrieving firewall config
    FirewallConfigurationDTO firewallConfigurationDTO = apiFirewallService.getFirewallConfiguration();

    //then: expect auto unquarintine to be enabled
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isTrue();
  }

  @Test
  public void testGetFirewallConfiguration_Disabled() {
    //setup: enable feature flag
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    //when: retrieving firewall config
    FirewallConfigurationDTO firewallConfigurationDTO = apiFirewallService.getFirewallConfiguration();

    //then: expect auto unquarintine to be disabled
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetFirewallConfiguration_NoFirewallFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    //when: getting firewall config
    //then: expect invalid license exception
    apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO());
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetFirewallConfiguration_NoReleaseIntegrityFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: getting firewall config
    //then: expect invalid license exception
    apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO());
  }

  @Test(expected = BadRequestException.class)
  public void testGetFirewallConfiguration_FeatureFlag_False() {
    //when: getting firewall status
    //then: expect bad request exception
    apiFirewallService.getFirewallConfiguration();
  }

  @Test
  public void testSetFirewallConfiguration_DoesNotExistToTrue() {
    //setup: enable feature flag and create dto
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;

    //when: setting firewall auto unquarantine with dto
    firewallConfigurationDTO = apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO);

    //then: expect auto unquarantine to be enabled
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isTrue();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
  }

  @Test
  public void testSetFirewallConfiguration_ExistsToFalse() {
    //setup: enable feature flag and create dto, setup existing policy monitoring
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = false;
    tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    //when: setting firewall auto unquarantine with dto
    firewallConfigurationDTO = apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO);

    //then: expect auto unquarantine to be disabled
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();
  }

  @Test
  public void testSetFirewallConfiguration_DoesNotExistToFalse_NoEffect() {
    //setup: enable feature flag and create dto
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = false;

    //when: setting firewall auto unquarantine with dto
    firewallConfigurationDTO = apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO);

    //then: expect auto unquarantine to be disabled
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();
  }

  @Test
  public void testSetFirewallConfiguration_ExistsToTrue_NoEffect() {
    //setup: enable feature flag and create dto, setup existing policy monitoring
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;
    PolicyMonitoring existingPolicyMonitoring =
        tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    //when: setting firewall auto unquarantine with dto
    firewallConfigurationDTO = apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO);

    //then: expect auto unquarantine to be enabled, and Id to not have changed
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isTrue();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(policyMonitoring.getId()).isEqualTo(existingPolicyMonitoring.getId());
  }

  @Test(expected = InvalidLicenseException.class)
  public void testSetFirewallConfiguration_NoFirewallFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    //when: setting firewall auto unquarantine
    //then: expect invalid license exception
    apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO());
  }

  @Test(expected = InvalidLicenseException.class)
  public void testSetFirewallConfiguration_NoReleaseIntegrityFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: setting firewall auto unquarantine
    //then: expect invalid license exception
    apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO());
  }

  @Test(expected = BadRequestException.class)
  public void testSetFirewallConfiguration_FeatureFlag_False() {
    //when: setting firewall status
    //then: expect bad request exception
    apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO());
  }
}
