/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class FirewallReleaseIntegrityLicenseListenerTest
    extends AbstractComponentH2Test
{
  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private FirewallReleaseIntegrityLicenseListener listener;

  @Inject
  private AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO;

  @Inject
  private PolicyMonitoringDAO policyMonitoringDAO;

  @Test
  public void testProductLicenseChanged_EnablesPolicyMonitoring() {
    listener.productLicenseChanged();

    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)
        .getValue())
            .isEqualTo(String.valueOf(true));
    List<PolicyMonitoring> policyMonitorings = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitorings).isNotNull().hasSize(1);
    assertThat(policyMonitorings.get(0).getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNotNull();
  }

  @Test
  public void testProductLicenseChanged_AlreadyInstalled() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED,
        String.valueOf(true));

    listener.productLicenseChanged();

    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)
        .getValue())
            .isEqualTo(String.valueOf(true));
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isEmpty();
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNull();
  }

  @Test
  public void testProductLicenseChanged_DisabledConfigurationProperty() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED,
        String.valueOf(false));

    listener.productLicenseChanged();

    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)
        .getValue())
            .isEqualTo(String.valueOf(true));
    List<PolicyMonitoring> policyMonitorings = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitorings).isNotNull().hasSize(1);
    assertThat(policyMonitorings.get(0).getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNotNull();
  }

  @Test
  public void testProductLicenseChanged_WithoutFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    listener.productLicenseChanged();

    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)).isNull();
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isEmpty();
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNull();
  }

  @Test
  public void testProductLicenseChanged_WithoutReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    listener.productLicenseChanged();

    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)).isNull();
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isEmpty();
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNull();
  }

  @Test
  public void testProductLicenseChanged_PolicyMonitoringAlreadyEnabled() {
    String policyMonitoringId =
        tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId()).getId();

    listener.productLicenseChanged();

    List<PolicyMonitoring> policyMonitorings = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitorings).isNotNull().hasSize(1);
    assertThat(policyMonitorings.get(0).getId()).isEqualTo(policyMonitoringId);
    assertThat(policyMonitorings.get(0).getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNotNull();
  }

  @Test
  public void testProductLicenseChanged_AutoUnquarantineAlreadyEnabled() {
    autoUnquarantinePolicyConditionTypeDAO
        .insert(new AutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID));

    listener.productLicenseChanged();

    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNotNull();
  }
}
