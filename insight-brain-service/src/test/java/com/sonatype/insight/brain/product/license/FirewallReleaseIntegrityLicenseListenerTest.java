/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.component.IntegrityRating;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static com.sonatype.insight.brain.product.license.FirewallReleaseIntegrityLicenseListener.POLICY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallReleaseIntegrityLicenseListenerTest
    extends AbstractComponentTest
{
  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private FirewallReleaseIntegrityLicenseListener listener;

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO =
      new AutoUnquarantinePolicyConditionTypeDAO();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @After
  public void cleanup() {
    systemConfigurationPropertyDAO.delete(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED));
    List<Policy> policies = policyDAO.getByName(POLICY_NAME);
    if (!policies.isEmpty()) {
      policyDAO.delete(policies.get(0));
    }
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
    autoUnquarantinePolicyConditionTypeDAO
        .delete(new AutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID));
  }

  @Test
  public void testProductLicenseChanged_InstallsReleaseIntegrityPolicyAndEnablesPolicyMonitoring() {
    listener.productLicenseChanged();

    assertThat(policyDAO.getByName(POLICY_NAME)).hasSize(1);
    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED).getValue())
        .isEqualTo(String.valueOf(true));
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNotNull();
  }

  @Test
  public void testProductLicenseChanged_AlreadyInstalled() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED,
        String.valueOf(true));

    listener.productLicenseChanged();

    assertThat(policyDAO.getByName(POLICY_NAME)).isEmpty();
    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED).getValue())
        .isEqualTo(String.valueOf(true));
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNull();
  }

  @Test
  public void testProductLicenseChanged_DisabledConfigurationProperty() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED,
        String.valueOf(false));

    listener.productLicenseChanged();

    assertThat(policyDAO.getByName(POLICY_NAME)).hasSize(1);
    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED).getValue())
        .isEqualTo(String.valueOf(true));
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNotNull();
  }

  @Test
  public void testProductLicenseChanged_PolicyNameExists() {
    Constraint constraint = new Constraint("c1", "Test Constraint", LogicalOperator.OR);
    constraint.addCondition(new Condition(IntegrityRatingConditionType.ID, "is", IntegrityRating.PENDING.getId()));
    tempEntity.newPolicy(POLICY_NAME, constraint);
    tempEntity.newPolicy(POLICY_NAME + "-1", constraint);

    listener.productLicenseChanged();

    List<Policy> policies = policyDAO.getByName(POLICY_NAME + "-2");
    assertThat(policies).hasSize(1);
    policyDAO.delete(policies.get(0));
    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED).getValue())
        .isEqualTo(String.valueOf(true));
  }

  @Test
  public void testProductLicenseChanged_WithoutFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    listener.productLicenseChanged();

    assertThat(policyDAO.getByName(POLICY_NAME)).isEmpty();
    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)).isNull();
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNull();
  }

  @Test
  public void testProductLicenseChanged_WithoutReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    listener.productLicenseChanged();

    assertThat(policyDAO.getByName(POLICY_NAME)).isEmpty();
    assertThat(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED)).isNull();
    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();
    assertThat(autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)).isNull();
  }

  @Test
  public void testProductLicenseChanged_PolicyMonitoringAlreadyEnabled() {
    String policyMonitoringId =
        tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId()).getId();

    listener.productLicenseChanged();

    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getId()).isEqualTo(policyMonitoringId);
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
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
