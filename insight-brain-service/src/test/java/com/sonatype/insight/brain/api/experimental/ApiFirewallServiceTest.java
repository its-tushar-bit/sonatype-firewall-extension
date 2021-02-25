/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
  public void testGetFirewallReleaseQuarantineSummary() {
    //setup: enable feature flag
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    Repository repository = tempEntity.newRepository();
    // create repository components in various state of quarantine
    final Date mtdDate = new Date();
    final Date ytdDate = DateUtils.setMonths(new Date(), 0);
    final Date previousYearDate = DateUtils.addYears(new Date(), -1);
    tempEntity.newRepositoryComponent(repository.getId(), "/audit", null, null);
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined", mtdDate, null);
    tempEntity.newRepositoryComponent(repository.getId(), "/autoUnquarantinedMtd", mtdDate, mtdDate, true);
    tempEntity.newRepositoryComponent(repository.getId(), "/manualUnquarantinedMtd", mtdDate, mtdDate, false);
    tempEntity.newRepositoryComponent(repository.getId(), "/autoUnquarantinedYtd", ytdDate, ytdDate, true);
    tempEntity.newRepositoryComponent(repository.getId(), "/manualUnquarantinedYtd", ytdDate, ytdDate, false);
    tempEntity
        .newRepositoryComponent(repository.getId(), "/autoUnquarantinedPrevious", previousYearDate, previousYearDate,
            true);
    tempEntity
        .newRepositoryComponent(repository.getId(), "/manualUnquarantinedPrevious", previousYearDate, previousYearDate,
            false);

    //when: retrieving release quarantine summary
    ApiFirewallReleaseQuarantineSummaryDTO releaseQuarantineSummary = apiFirewallService.getReleaseQuarantineSummary();

    //then: expect to get a valid pojo back
    // if MTD and YTD dates are the same day it's January, and MTD counts will equal YTD counts
    if (DateUtils.isSameDay(mtdDate, ytdDate)) {
      assertThat(releaseQuarantineSummary.autoReleaseQuarantineCountMTD).isEqualTo(2);
    }
    else {
      assertThat(releaseQuarantineSummary.autoReleaseQuarantineCountMTD).isOne();
    }
    assertThat(releaseQuarantineSummary.autoReleaseQuarantineCountYTD).isEqualTo(2);
  }

  @Test
  public void testGetFirewallReleaseQuarantineSummary_NoFirewallFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    //when: getting release quarantine summary
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallReleaseQuarantineSummary_NoReleaseIntegrityFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: getting release quarantine summary
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallReleaseQuarantineSummary_FeatureFlag_False() {
    //when: getting release quarantine summary
    //then: expect bad request exception
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallConfiguration_Enabled() {
    //setup: enable feature flag and insert policy monitoring
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    //when: retrieving firewall config
    FirewallConfigurationDTO firewallConfigurationDTO = apiFirewallService.getFirewallConfiguration();

    //then: expect auto unquarantine to be enabled
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

  @Test
  public void testGetFirewallConfiguration_NoFirewallFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    //when: getting firewall config
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getFirewallConfiguration());
  }

  @Test
  public void testGetFirewallConfiguration_NoReleaseIntegrityFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: getting firewall config
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getFirewallConfiguration());
  }

  @Test
  public void testGetFirewallConfiguration_FeatureFlag_False() {
    //when: getting firewall status
    //then: expect bad request exception
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        apiFirewallService.getFirewallConfiguration());
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

  @Test
  public void testSetFirewallConfiguration_NoFirewallFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    //when: setting firewall auto unquarantine
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy( () ->
        apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO()));
  }

  @Test
  public void testSetFirewallConfiguration_NoReleaseIntegrityFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: setting firewall auto unquarantine
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy( () ->
        apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO()));
  }

  @Test
  public void testSetFirewallConfiguration_FeatureFlag_False() {
    //when: setting firewall status
    //then: expect bad request exception
    assertThatExceptionOfType(BadRequestException.class).isThrownBy( () ->
        apiFirewallService.setFirewallConfiguration(new FirewallConfigurationDTO()));
  }

  @Test
  public void testGetQuarantineSummary() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    tempEntity.newRepositoryComponent(repo, "hash");
    tempEntity.newRepositoryComponent(repo.getId(), "path", new Date(), null);
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo2", true, false);

    ApiFirewallQuarantineSummaryDTO summary = apiFirewallService.getQuarantineSummary();

    assertThat(summary).isNotNull();
    assertThat(summary.repositoryCount).isEqualTo(2);
    assertThat(summary.quarantineEnabled).isTrue();
    assertThat(summary.quarantineEnabledRepositoryCount).isEqualTo(1);
    assertThat(summary.totalComponentCount).isEqualTo(2);
    assertThat(summary.quarantinedComponentCount).isEqualTo(1);
  }

  @Test
  public void testGetQuarantineSummary_NoExperimentalFeature() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiFirewallService.getQuarantineSummary();
    }).withMessage("Firewall experimental feature is not enabled.");
  }

  @Test
  public void testGetQuarantineSummary_NoReleaseIntegrityFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      apiFirewallService.getQuarantineSummary();
    });
  }

  @Test
  public void testGetQuarantineSummary_NoFirewallFeature() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      apiFirewallService.getQuarantineSummary();
    });
  }
}
