/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Locale;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_DASHBOARD;
import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_REPORTS_LIST;
import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION;
import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_TRANSITIVE_SOLVER;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.TRANSITIVE_SOLVER_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiConfigFeaturesServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiConfigFeaturesService service;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @After
  public void after() {
    for (SystemConfigurationPropertyFeature s : SystemConfigurationPropertyFeature.values()) {
      SystemConfigurationProperty systemConfigurationProperty =
          systemConfigurationPropertyDAO.getByName(s.getPropertyName());
      if (systemConfigurationProperty != null) {
        systemConfigurationPropertyDAO.delete(systemConfigurationProperty);
      }
    }
  }

  @Test
  public void testGetPropertyNameForFeature() {
    assertThat(service.getPropertyNameForFeature(FEATURE_DASHBOARD)).isEqualTo(DASHBOARD_DISABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_REPORTS_LIST)).isEqualTo(REPORTS_LIST_DISABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION)).isEqualTo(
        SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_TRANSITIVE_SOLVER)).isEqualTo(TRANSITIVE_SOLVER_DISABLED);
    assertThat(service.getPropertyNameForFeature("default-value")).isEqualTo("default-value");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature() {
    for (SystemConfigurationPropertyFeature feature : SystemConfigurationPropertyFeature.values()) {
      assertThat(service.getSystemConfigurationPropertyFeature(feature.name())).isEqualTo(feature);
      assertThat(service.getSystemConfigurationPropertyFeature(feature.name().toUpperCase(Locale.ROOT))).isEqualTo(
          feature);
      assertThat(service.getSystemConfigurationPropertyFeature(feature.name().toLowerCase(Locale.ROOT))).isEqualTo(
          feature);
      assertThat(service.getSystemConfigurationPropertyFeature(feature.getId())).isEqualTo(feature);
      assertThat(service.getSystemConfigurationPropertyFeature(feature.getId().toUpperCase(Locale.ROOT))).isEqualTo(
          feature);
      assertThat(service.getSystemConfigurationPropertyFeature(feature.getId().toLowerCase(Locale.ROOT))).isEqualTo(
          feature);
      assertThat(service.getSystemConfigurationPropertyFeature(feature.getPropertyName())).isEqualTo(feature);
      assertThat(
          service.getSystemConfigurationPropertyFeature(feature.getPropertyName().toUpperCase(Locale.ROOT))).isEqualTo(
          feature);
      assertThat(
          service.getSystemConfigurationPropertyFeature(feature.getPropertyName().toLowerCase(Locale.ROOT))).isEqualTo(
          feature);
    }
    assertThat(service.getSystemConfigurationPropertyFeature(DASHBOARD_DISABLED)).isEqualTo(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED);
    assertThat(service.getSystemConfigurationPropertyFeature(REPORTS_LIST_DISABLED)).isEqualTo(
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_TRANSITIVE_SOLVER))
        .isEqualTo(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER);
    assertThat(service.getSystemConfigurationPropertyFeature(
        SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED)).isEqualTo(
        SystemConfigurationPropertyFeature.VULNERABILITY_SOURCE);
    assertThatThrownBy(() -> service.getSystemConfigurationPropertyFeature("bogus-feature")).isInstanceOf(
        BadRequestException.class).hasMessage("Feature not supported: bogus-feature");
  }

  @Test
  public void testEnableTransitive_solver_feature() {
    service.enableFeature(FEATURE_TRANSITIVE_SOLVER);
    assertThat(systemConfigurationPropertyDAO.getByName(TRANSITIVE_SOLVER_DISABLED).getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableTransitive_solver_feature_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(TRANSITIVE_SOLVER_DISABLED, "true");
    assertThatThrownBy(() -> service.enableFeature(FEATURE_TRANSITIVE_SOLVER)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableTransitive_solver_feature() {
    tempEntity.newSystemConfigurationProperty(TRANSITIVE_SOLVER_DISABLED, "true");
    service.disableFeature(FEATURE_TRANSITIVE_SOLVER);
    assertThat(systemConfigurationPropertyDAO.getByName(TRANSITIVE_SOLVER_DISABLED)).isNull();
  }

  @Test
  public void testDisableTransitive_solver_feature_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(FEATURE_TRANSITIVE_SOLVER)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testDisableFeature_Dashboard() {
    service.disableFeature(FEATURE_DASHBOARD);
    assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED).getValue()).isEqualTo("true");
  }

  @Test
  public void testDisableFeature_Dashboard_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    assertThatThrownBy(() -> service.disableFeature(FEATURE_DASHBOARD)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_Dashboard() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    service.enableFeature(FEATURE_DASHBOARD);
    assertThat(systemConfigurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_Dashboard_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(FEATURE_DASHBOARD)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ReportsList() {
    service.disableFeature(FEATURE_REPORTS_LIST);
    assertThat(systemConfigurationPropertyDAO.getByName(REPORTS_LIST_DISABLED).getValue()).isEqualTo("true");
  }

  @Test
  public void testDisableFeature_ReportsList_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(REPORTS_LIST_DISABLED, "true");

    assertThatThrownBy(() -> service.disableFeature(FEATURE_REPORTS_LIST)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_ReportsList() {
    tempEntity.newSystemConfigurationProperty(REPORTS_LIST_DISABLED, "true");
    service.enableFeature(FEATURE_REPORTS_LIST);
    assertThat(systemConfigurationPropertyDAO.getByName(REPORTS_LIST_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_ReportsList_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(FEATURE_REPORTS_LIST)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_SecurityVulnerabilitySourcePolicyCondition() {
    systemConfigurationPropertyDAO.delete(systemConfigurationPropertyDAO
        .getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED));

    service.disableFeature(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION);

    assertThat(systemConfigurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testDisableFeature_SecurityVulnerabilitySourcePolicyCondition_AlreadyDisabled() {
    assertThatThrownBy(
        () -> service.disableFeature(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_SecurityVulnerabilitySourcePolicyCondition() {
    service.enableFeature(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION);

    assertThat(systemConfigurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED))
        .isNull();
  }

  @Test
  public void testEnableFeature_SecurityVulnerabilitySourcePolicyCondition_AlreadyEnabled() {
    systemConfigurationPropertyDAO.delete(systemConfigurationPropertyDAO
        .getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED));

    assertThatThrownBy(
        () -> service.enableFeature(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testEnableFeature_JavaRecompilation() {
    service.enableFeature(SystemConfigurationProperty.BUILT_FROM_SOURCE);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.BUILT_FROM_SOURCE)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableFeature_JavaRecompilation_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.BUILT_FROM_SOURCE);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.BUILT_FROM_SOURCE)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_JavaRecompilation() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getPropertyName(),
        "true");
    service.disableFeature(SystemConfigurationProperty.BUILT_FROM_SOURCE);
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.BUILT_FROM_SOURCE)).isNull();
  }

  @Test
  public void testDisableFeature_JavaRecompilation_AlreadyDisabled() {
    assertThatThrownBy(
        () -> service.disableFeature(SystemConfigurationProperty.BUILT_FROM_SOURCE)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_CrowdIntegration() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.CROWD_INTEGRATION.getPropertyName(),
        "false");
    service.enableFeature(SystemConfigurationProperty.CROWD_INTEGRATION);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.CROWD_INTEGRATION)).isNull();
  }

  @Test
  public void testEnableFeature_CrowdIntegration_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.CROWD_INTEGRATION.getPropertyName(),
        "false");
    service.enableFeature(SystemConfigurationProperty.CROWD_INTEGRATION);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.CROWD_INTEGRATION)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_CrowdIntegration() {
    service.disableFeature(SystemConfigurationProperty.CROWD_INTEGRATION);
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.CROWD_INTEGRATION).getValue()).isEqualTo(
        "false");
  }

  @Test
  public void testDisableFeature_CrowdIntegration_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.CROWD_INTEGRATION);
    assertThatThrownBy(
        () -> service.disableFeature(SystemConfigurationProperty.CROWD_INTEGRATION)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already disabled.");
  }
}
