/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.FIPSConfig;
import com.sonatype.insight.brain.service.SystemConfigurationPropertyCacheInvalidationJob;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.*;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiConfigFeaturesServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiConfigFeaturesService service;

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void testGetPropertyNameForFeature() {
    assertThat(service.getPropertyNameForFeature(FEATURE_DASHBOARD)).isEqualTo(DASHBOARD_DISABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_REPORTS_LIST)).isEqualTo(REPORTS_LIST_DISABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION)).isEqualTo(
        SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_TRANSITIVE_SOLVER)).isEqualTo(TRANSITIVE_SOLVER_ENABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_CODE_INSIGHTS)).isEqualTo(CODE_INSIGHTS);
    assertThat(service.getPropertyNameForFeature(FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE)).isEqualTo(
        COMPONENT_SEARCH_API_WITH_INNERSOURCE);
    assertThat(service.getPropertyNameForFeature(FEATURE_DEFAULT_BRANCH_MONITORING)).isEqualTo(
        DEFAULT_BRANCH_MONITORING);
    assertThat(service.getPropertyNameForFeature(FEATURE_DEPENDENCY_DATA_IN_API)).isEqualTo(DEPENDENCY_DATA_IN_API);
    assertThat(service.getPropertyNameForFeature(FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER)).isEqualTo(
        INNER_SOURCE_TRANSITIVE_WAIVER);
    assertThat(service.getPropertyNameForFeature(FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION)).isEqualTo(
        INNER_SOURCE_REPOSITORY_INTEGRATION);
    assertThat(service.getPropertyNameForFeature(FEATURE_PR_COMMENTING)).isEqualTo(PR_COMMENTING);
    assertThat(service.getPropertyNameForFeature(FEATURE_PR_LINE_COMMENTING)).isEqualTo(PR_LINE_COMMENTING);
    assertThat(service.getPropertyNameForFeature(FEATURE_ENABLE_UNAUTHENTICATED_PAGES)).isEqualTo(
        ENABLE_UNAUTHENTICATED_PAGES);
    assertThat(service.getPropertyNameForFeature(FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS)).isEqualTo(
        INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
    assertThat(service.getPropertyNameForFeature(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)).isEqualTo(
        SAAS_LIFECYCLE_SCM_ENABLED);
    assertThat(service.getPropertyNameForFeature(FEATURE_USER_ACTIVITY_TRACKING)).isEqualTo(
        USER_ACTIVITY_TRACKING);
    assertThat(service.getPropertyNameForFeature(FEATURE_EXIT_ON_FATAL_ERROR)).isEqualTo(EXIT_ON_FATAL_ERROR);
    assertThat(service.getPropertyNameForFeature("default-value")).isEqualTo("default-value");
  }

  @Test
  public void testGetFeatureForPropertyName() {
    assertThat(service.getFeatureForPropertyName(DASHBOARD_DISABLED)).isEqualTo(FEATURE_DASHBOARD);
    assertThat(service.getFeatureForPropertyName(REPORTS_LIST_DISABLED)).isEqualTo(FEATURE_REPORTS_LIST);
    assertThat(service.getFeatureForPropertyName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED)).isEqualTo(
        FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION);
    assertThat(service.getFeatureForPropertyName(TRANSITIVE_SOLVER_ENABLED)).isEqualTo(FEATURE_TRANSITIVE_SOLVER);
    assertThat(service.getFeatureForPropertyName(CODE_INSIGHTS)).isEqualTo(FEATURE_CODE_INSIGHTS);
    assertThat(service.getFeatureForPropertyName(COMPONENT_SEARCH_API_WITH_INNERSOURCE)).isEqualTo(
        FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE);
    assertThat(service.getFeatureForPropertyName(DEFAULT_BRANCH_MONITORING)).isEqualTo(
        FEATURE_DEFAULT_BRANCH_MONITORING);
    assertThat(service.getFeatureForPropertyName(DEPENDENCY_DATA_IN_API)).isEqualTo(FEATURE_DEPENDENCY_DATA_IN_API);
    assertThat(service.getFeatureForPropertyName(INNER_SOURCE_TRANSITIVE_WAIVER)).isEqualTo(
        FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER);
    assertThat(service.getFeatureForPropertyName(INNER_SOURCE_REPOSITORY_INTEGRATION)).isEqualTo(
        FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION);
    assertThat(service.getFeatureForPropertyName(PR_COMMENTING)).isEqualTo(FEATURE_PR_COMMENTING);
    assertThat(service.getFeatureForPropertyName(PR_LINE_COMMENTING)).isEqualTo(FEATURE_PR_LINE_COMMENTING);
    assertThat(service.getFeatureForPropertyName(ENABLE_UNAUTHENTICATED_PAGES)).isEqualTo(
        FEATURE_ENABLE_UNAUTHENTICATED_PAGES);
    assertThat(service.getFeatureForPropertyName(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS)).isEqualTo(
        FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
    assertThat(service.getFeatureForPropertyName(SAAS_LIFECYCLE_SCM_ENABLED)).isEqualTo(
        FEATURE_SAAS_LIFECYCLE_SCM_ENABLED);
    assertThat(service.getFeatureForPropertyName(USER_ACTIVITY_TRACKING)).isEqualTo(
        FEATURE_USER_ACTIVITY_TRACKING);
    assertThat(service.getFeatureForPropertyName(EXIT_ON_FATAL_ERROR)).isEqualTo(
        FEATURE_EXIT_ON_FATAL_ERROR);
    assertThat(service.getFeatureForPropertyName("default-value")).isEqualTo("default-value");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature() {
    for (SystemConfigurationPropertyFeature feature : SystemConfigurationPropertyFeature.values()) {
      if (NOT_SUPPORTED_SELF_HOSTED_SYSTEM_PROPERTIES.contains(feature.getPropertyName())) {
        assertThatThrownBy(() -> service.getSystemConfigurationPropertyFeature(feature.name()))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("'" + feature.getPropertyName() + "' is not supported for self hosted.");
      }
      else {
        assertThat(service.getSystemConfigurationPropertyFeature(feature.name())).isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.name().toUpperCase(Locale.ROOT)))
            .isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.name().toLowerCase(Locale.ROOT)))
            .isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.getId())).isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.getId().toUpperCase(Locale.ROOT)))
            .isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.getId().toLowerCase(Locale.ROOT)))
            .isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.getPropertyName())).isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.getPropertyName().toUpperCase(Locale.ROOT)))
            .isEqualTo(feature);
        assertThat(service.getSystemConfigurationPropertyFeature(feature.getPropertyName().toLowerCase(Locale.ROOT)))
            .isEqualTo(feature);
      }
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
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_CODE_INSIGHTS))
        .isEqualTo(SystemConfigurationPropertyFeature.CODE_INSIGHTS);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE))
        .isEqualTo(SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_DEFAULT_BRANCH_MONITORING))
        .isEqualTo(SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_DEPENDENCY_DATA_IN_API))
        .isEqualTo(SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER))
        .isEqualTo(SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION))
        .isEqualTo(SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_PR_COMMENTING))
        .isEqualTo(SystemConfigurationPropertyFeature.PR_COMMENTING);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_PR_LINE_COMMENTING))
        .isEqualTo(SystemConfigurationPropertyFeature.PR_LINE_COMMENTING);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_ENABLE_UNAUTHENTICATED_PAGES))
        .isEqualTo(SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS))
        .isEqualTo(SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
    assertThat(service.getSystemConfigurationPropertyFeature(FEATURE_USER_ACTIVITY_TRACKING))
        .isEqualTo(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING);

    assertThatThrownBy(() -> service.getSystemConfigurationPropertyFeature(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("'SAAS_LIFECYCLE_SCM_ENABLED' is not supported for self hosted.");

    assertThatThrownBy(() -> service.getSystemConfigurationPropertyFeature("bogus-feature")).isInstanceOf(
        BadRequestException.class).hasMessage("Feature not supported: bogus-feature");
  }

  @Test
  public void testEnableTransitive_solver_feature() {
    assertThat(systemConfigurationPropertyDAO.getByName(TRANSITIVE_SOLVER_ENABLED)).isNull();
  }

  @Test
  public void testEnableTransitive_solver_feature_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(FEATURE_TRANSITIVE_SOLVER)).isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableTransitive_solver_feature() {
    service.disableFeature(FEATURE_TRANSITIVE_SOLVER);
    assertThat(systemConfigurationPropertyDAO.getByName(TRANSITIVE_SOLVER_ENABLED).getValue())
        .isEqualTo("false");
  }

  @Test
  public void testDisableTransitive_solver_feature_AlreadyDisabled() {
    service.disableFeature(FEATURE_TRANSITIVE_SOLVER);
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

  @Test
  public void testEnableFeature_CodeInsights() {
    tempEntity.newSystemConfigurationProperty(CODE_INSIGHTS, "false");
    service.enableFeature(CODE_INSIGHTS);
    assertThat(systemConfigurationPropertyDAO.getByName(CODE_INSIGHTS)).isNull();
  }

  @Test
  public void testEnableFeature_CodeInsights_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(CODE_INSIGHTS, "false");
    service.enableFeature(CODE_INSIGHTS);
    assertThatThrownBy(() -> service.enableFeature(CODE_INSIGHTS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_CodeInsights() {
    service.disableFeature(CODE_INSIGHTS);
    assertThat(systemConfigurationPropertyDAO.getByName(CODE_INSIGHTS).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_CodeInsights_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(CODE_INSIGHTS, "false");
    assertThatThrownBy(() -> service.disableFeature(CODE_INSIGHTS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_ComponentSearchApiWithInnerSource() {
    tempEntity.newSystemConfigurationProperty(COMPONENT_SEARCH_API_WITH_INNERSOURCE, "false");
    service.enableFeature(COMPONENT_SEARCH_API_WITH_INNERSOURCE);
    assertThat(systemConfigurationPropertyDAO.getByName(COMPONENT_SEARCH_API_WITH_INNERSOURCE)).isNull();
  }

  @Test
  public void testEnableFeature_ComponentSearchApiWithInnerSource_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(COMPONENT_SEARCH_API_WITH_INNERSOURCE, "false");
    service.enableFeature(COMPONENT_SEARCH_API_WITH_INNERSOURCE);
    assertThatThrownBy(() -> service.enableFeature(COMPONENT_SEARCH_API_WITH_INNERSOURCE))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ComponentSearchApiWithInnerSource() {
    service.disableFeature(COMPONENT_SEARCH_API_WITH_INNERSOURCE);
    assertThat(systemConfigurationPropertyDAO.getByName(COMPONENT_SEARCH_API_WITH_INNERSOURCE).getValue()).isEqualTo(
        "false");
  }

  @Test
  public void testDisableFeature_ComponentSearchApiWithInnerSource_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(COMPONENT_SEARCH_API_WITH_INNERSOURCE, "false");
    assertThatThrownBy(() -> service.disableFeature(COMPONENT_SEARCH_API_WITH_INNERSOURCE))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_DefaultBranchMonitoring() {
    tempEntity.newSystemConfigurationProperty(DEFAULT_BRANCH_MONITORING, "false");
    service.enableFeature(DEFAULT_BRANCH_MONITORING);
    assertThat(systemConfigurationPropertyDAO.getByName(DEFAULT_BRANCH_MONITORING)).isNull();
  }

  @Test
  public void testEnableFeature_DefaultBranchMonitoring_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(DEFAULT_BRANCH_MONITORING, "false");
    service.enableFeature(DEFAULT_BRANCH_MONITORING);
    assertThatThrownBy(() -> service.enableFeature(DEFAULT_BRANCH_MONITORING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_DefaultBranchMonitoring() {
    service.disableFeature(DEFAULT_BRANCH_MONITORING);
    assertThat(systemConfigurationPropertyDAO.getByName(DEFAULT_BRANCH_MONITORING).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_DefaultBranchMonitoring_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(DEFAULT_BRANCH_MONITORING, "false");
    assertThatThrownBy(() -> service.disableFeature(DEFAULT_BRANCH_MONITORING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_DependencyDataInApi() {
    tempEntity.newSystemConfigurationProperty(DEPENDENCY_DATA_IN_API, "false");
    service.enableFeature(DEPENDENCY_DATA_IN_API);
    assertThat(systemConfigurationPropertyDAO.getByName(DEPENDENCY_DATA_IN_API)).isNull();
  }

  @Test
  public void testEnableFeature_DependencyDataInApi_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(DEPENDENCY_DATA_IN_API, "false");
    service.enableFeature(DEPENDENCY_DATA_IN_API);
    assertThatThrownBy(() -> service.enableFeature(DEPENDENCY_DATA_IN_API))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_DependencyDataInApi() {
    service.disableFeature(DEPENDENCY_DATA_IN_API);
    assertThat(systemConfigurationPropertyDAO.getByName(DEPENDENCY_DATA_IN_API).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_DependencyDataInApi_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(DEPENDENCY_DATA_IN_API, "false");
    assertThatThrownBy(() -> service.disableFeature(DEPENDENCY_DATA_IN_API))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_InnerSourceTransitiveWaiver() {
    tempEntity.newSystemConfigurationProperty(INNER_SOURCE_TRANSITIVE_WAIVER, "false");
    service.enableFeature(INNER_SOURCE_TRANSITIVE_WAIVER);
    assertThat(systemConfigurationPropertyDAO.getByName(INNER_SOURCE_TRANSITIVE_WAIVER)).isNull();
  }

  @Test
  public void testEnableFeature_InnerSourceTransitiveWaiver_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(INNER_SOURCE_TRANSITIVE_WAIVER, "false");
    service.enableFeature(INNER_SOURCE_TRANSITIVE_WAIVER);
    assertThatThrownBy(() -> service.enableFeature(INNER_SOURCE_TRANSITIVE_WAIVER))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_InnerSourceTransitiveWaiver() {
    service.disableFeature(INNER_SOURCE_TRANSITIVE_WAIVER);
    assertThat(systemConfigurationPropertyDAO.getByName(INNER_SOURCE_TRANSITIVE_WAIVER).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_InnerSourceTransitiveWaiver_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(INNER_SOURCE_TRANSITIVE_WAIVER, "false");
    assertThatThrownBy(() -> service.disableFeature(INNER_SOURCE_TRANSITIVE_WAIVER))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_InnerSourceRepositoryIntegration() {
    tempEntity.newSystemConfigurationProperty(INNER_SOURCE_REPOSITORY_INTEGRATION, "false");
    service.enableFeature(INNER_SOURCE_REPOSITORY_INTEGRATION);
    assertThat(systemConfigurationPropertyDAO.getByName(INNER_SOURCE_REPOSITORY_INTEGRATION)).isNull();
  }

  @Test
  public void testEnableFeature_InnerSourceRepositoryIntegration_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(INNER_SOURCE_REPOSITORY_INTEGRATION, "false");
    service.enableFeature(INNER_SOURCE_REPOSITORY_INTEGRATION);
    assertThatThrownBy(() -> service.enableFeature(INNER_SOURCE_REPOSITORY_INTEGRATION))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_InnerSourceRepositoryIntegration() {
    service.disableFeature(INNER_SOURCE_REPOSITORY_INTEGRATION);
    assertThat(systemConfigurationPropertyDAO.getByName(INNER_SOURCE_REPOSITORY_INTEGRATION).getValue()).isEqualTo(
        "false");
  }

  @Test
  public void testDisableFeature_InnerSourceRepositoryIntegration_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(INNER_SOURCE_REPOSITORY_INTEGRATION, "false");
    assertThatThrownBy(() -> service.disableFeature(INNER_SOURCE_REPOSITORY_INTEGRATION))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_PrCommenting() {
    tempEntity.newSystemConfigurationProperty(PR_COMMENTING, "false");
    service.enableFeature(PR_COMMENTING);
    assertThat(systemConfigurationPropertyDAO.getByName(PR_COMMENTING)).isNull();
  }

  @Test
  public void testEnableFeature_PrCommenting_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(PR_COMMENTING, "false");
    service.enableFeature(PR_COMMENTING);
    assertThatThrownBy(() -> service.enableFeature(PR_COMMENTING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_PrCommenting() {
    service.disableFeature(PR_COMMENTING);
    assertThat(systemConfigurationPropertyDAO.getByName(PR_COMMENTING).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_PrCommenting_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(PR_COMMENTING, "false");
    assertThatThrownBy(() -> service.disableFeature(PR_COMMENTING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_PrLineCommenting() {
    tempEntity.newSystemConfigurationProperty(PR_LINE_COMMENTING, "false");
    service.enableFeature(PR_LINE_COMMENTING);
    assertThat(systemConfigurationPropertyDAO.getByName(PR_LINE_COMMENTING)).isNull();
  }

  @Test
  public void testEnableFeature_PrLineCommenting_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(PR_LINE_COMMENTING, "false");
    service.enableFeature(PR_LINE_COMMENTING);
    assertThatThrownBy(() -> service.enableFeature(PR_LINE_COMMENTING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_PrLineCommenting() {
    service.disableFeature(PR_LINE_COMMENTING);
    assertThat(systemConfigurationPropertyDAO.getByName(PR_LINE_COMMENTING).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_PrLineCommenting_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(PR_LINE_COMMENTING, "false");
    assertThatThrownBy(() -> service.disableFeature(PR_LINE_COMMENTING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_PrLineCommentingBitbucketOnNoChange() {
    service.enableFeature(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE);
    assertThat(systemConfigurationPropertyDAO.getByName(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE).getValue())
        .isEqualTo("true");
  }

  @Test
  public void testEnableFeature_PrLineCommentingBitbucketOnNoChange_AlreadyEnabled() {
    service.enableFeature(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE);
    assertThatThrownBy(() -> service.enableFeature(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_PrLineCommentingBitbucketOnNoChange() {
    tempEntity.newSystemConfigurationProperty(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE, "true");
    service.disableFeature(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE);
    assertThat(systemConfigurationPropertyDAO.getByName(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE)).isNull();
  }

  @Test
  public void testDisabledByDefaultFeature_PrLineCommentingBitbucketOnNoChange() {
    assertThat(systemConfigurationPropertyDAO.getByName(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE)).isNull();
  }

  @Test
  public void testDisableFeature_PrLineCommentingBitbucketOnNoChange_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_EnableUnauthenticatedPages() {
    tempEntity.newSystemConfigurationProperty(ENABLE_UNAUTHENTICATED_PAGES, "false");
    service.enableFeature(ENABLE_UNAUTHENTICATED_PAGES);
    assertThat(systemConfigurationPropertyDAO.getByName(ENABLE_UNAUTHENTICATED_PAGES)).isNull();
  }

  @Test
  public void testFeature_EnableUnauthenticatedPages_EnvironmentalVariableOverridesWithFalse() {
    environmentVariables.set(SystemConfigurationPropertyFeature.NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR, "false");
    // null in db indicating it is enabled since enabled by default
    assertThat(systemConfigurationPropertyDAO.getByName(ENABLE_UNAUTHENTICATED_PAGES)).isNull();
    // env variable overrides and returns false
    assertThat(SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled()).isFalse();
  }

  @Test
  public void testFeature_EnableUnauthenticatedPages_EnvironmentalVariableOverridesWithTrue() {
    environmentVariables.set(SystemConfigurationPropertyFeature.NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR, "true");
    // false in db:
    systemConfigurationPropertyDAO.set(ENABLE_UNAUTHENTICATED_PAGES, "false");
    // env variable overrides and returns true
    assertThat(SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled()).isTrue();
  }

  @Test
  public void testEnableFeature_EnableUnauthenticatedPages_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(ENABLE_UNAUTHENTICATED_PAGES, "false");
    service.enableFeature(ENABLE_UNAUTHENTICATED_PAGES);
    assertThatThrownBy(() -> service.enableFeature(ENABLE_UNAUTHENTICATED_PAGES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_EnableUnauthenticatedPages() {
    service.disableFeature(ENABLE_UNAUTHENTICATED_PAGES);
    assertThat(systemConfigurationPropertyDAO.getByName(ENABLE_UNAUTHENTICATED_PAGES).getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_EnableUnauthenticatedPages_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(ENABLE_UNAUTHENTICATED_PAGES, "false");
    assertThatThrownBy(() -> service.disableFeature(ENABLE_UNAUTHENTICATED_PAGES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_InternalSourceControlPolicyEvaluations() {
    tempEntity.newSystemConfigurationProperty(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS, "false");
    service.enableFeature(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
    assertThat(systemConfigurationPropertyDAO.getByName(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS)).isNull();
  }

  @Test
  public void testEnableFeature_InternalSourceControlPolicyEvaluations_AlreadyEnabled() {
    tempEntity.newSystemConfigurationProperty(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS, "false");
    service.enableFeature(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
    assertThatThrownBy(() -> service.enableFeature(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_InternalSourceControlPolicyEvaluations() {
    service.disableFeature(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
    assertThat(
        systemConfigurationPropertyDAO.getByName(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS).getValue()).isEqualTo(
            "false");
  }

  @Test
  public void testDisableFeature_InternalSourceControlPolicyEvaluations_AlreadyDisabled() {
    tempEntity.newSystemConfigurationProperty(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS, "false");
    assertThatThrownBy(() -> service.disableFeature(INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_ApiPage() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("api-page"),
        service.getSystemConfigurationPropertyFeature("API-PAGE"),
        service.getSystemConfigurationPropertyFeature("Api-Page"));

    // Assert all map to API_PAGE
    assertThat(actual).allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.API_PAGE));
    assertThatThrownBy(() -> service.getSystemConfigurationPropertyFeature("apiPage")).isInstanceOf(
        BadRequestException.class).hasMessage("Feature not supported: apiPage");
  }

  @Test
  public void testEnableFeature_ApiPage() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.API_PAGE.getPropertyName(), "false");
    service.enableFeature(SystemConfigurationProperty.API_PAGE);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.API_PAGE)).isNull();
  }

  @Test
  public void testEnableFeature_ApiPage_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.API_PAGE)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ApiPage() {
    service.disableFeature(SystemConfigurationProperty.API_PAGE);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.API_PAGE).getValue()).isEqualTo(
        "false");
  }

  @Test
  public void testDisableFeature_ApiPage_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.API_PAGE);
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.API_PAGE)).isInstanceOf(
        BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_ScanPomFilesInMetaInfDirectory() {
    service.enableFeature(SCAN_POM_FILES_IN_META_INF_DIRECTORY);
    assertThat(systemConfigurationPropertyDAO.getByName(SCAN_POM_FILES_IN_META_INF_DIRECTORY).getValue()).isEqualTo(
        "true");
  }

  @Test
  public void testEnableFeature_ScanPomFilesInMetaInfDirectory_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY);
    assertThatThrownBy(
        () -> service.enableFeature(SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY)).isInstanceOf(
            BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ScanPomFilesInMetaInfDirectory() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.SCAN_POM_FILES_IN_META_INF_DIRECTORY.getPropertyName(), "true");
    service.disableFeature(SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY);
    assertThat(systemConfigurationPropertyDAO.getByName(
        SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY)).isNull();
  }

  @Test
  public void testDisableFeature_ScanPomFilesInMetaInfDirectory_AlreadyDisabled() {
    assertThatThrownBy(
        () -> service.disableFeature(SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY)).isInstanceOf(
            BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_ScanNpmDevAndOptDependencies() {
    service.enableFeature(SCAN_NPM_DEV_AND_OPT_DEPENDENCIES);
    assertThat(systemConfigurationPropertyDAO.getByName(SCAN_NPM_DEV_AND_OPT_DEPENDENCIES).getValue()).isEqualTo(
        "true");
  }

  @Test
  public void testEnableFeature_ScanNpmDevAndOptDependencies_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ScanNpmDevAndOptDependencies() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES.getPropertyName(), "true");
    service.disableFeature(SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES);
    assertThat(systemConfigurationPropertyDAO.getByName(
        SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES)).isNull();
  }

  @Test
  public void testDisableFeature_ScanNpmDevAndOptDependencies_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_SaasLifecycleScmEnabled_singleTenant() {
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.isEnabled()).isTrue();
  }

  @Test
  public void testEnableFeature_SaasLifecycleScmEnabled_enableForSingleTenant() {
    assertThatThrownBy(() -> service.enableFeature(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("'SAAS_LIFECYCLE_SCM_ENABLED' is not supported for self hosted.");
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.isEnabled()).isTrue();
  }

  @Test
  public void testEnableFeature_SaasLifecycleScmEnabled_disableForSingleTenant() {
    assertThatThrownBy(() -> service.disableFeature(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("'SAAS_LIFECYCLE_SCM_ENABLED' is not supported for self hosted.");
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.isEnabled()).isTrue();
  }

  @Test
  public void testEnableFeature_SaasLifecycleScmEnabled_setEnabledForSingleTenant() {
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.isEnabled()).isTrue();
  }

  @Test
  public void testFeatureHelperMethod_isSaasLifecycleScmEnabled() {
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.setEnabled(true);
    assertThat(service.isSaasLifecycleScmEnabled()).isTrue();
  }

  @Test
  public void testFeatureHelperMethod_isScmAndDefaultBranchMonitoringEnabled() {
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.setEnabled(true);
    SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.setEnabled(true);
    assertThat(service.isSaasLifecycleScmEnabled()).isTrue();
  }

  @Test
  public void testFeatureHelperMethod_isScmAndDefaultBranchMonitoringEnabled_branchMonitoringFalse() {
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED.setEnabled(true);
    SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.setEnabled(false);
    assertThat(service.isSaasLifecycleScmEnabled()).isTrue();
  }

  @Test
  public void testEnableFeature_SbomManager_singleTenant() {
    assertThat(SystemConfigurationPropertyFeature.SBOM_MANAGER.isEnabled()).isFalse();
  }

  @Test
  public void testEnableFeature_SbomManager_setEnabledForSingleTenant() {
    SystemConfigurationPropertyFeature.SBOM_MANAGER.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.SBOM_MANAGER.isEnabled()).isTrue();
  }

  @Test
  public void testEnableFeature_SbomManager_setDisabledForSingleTenant() {
    SystemConfigurationPropertyFeature.SBOM_MANAGER.setEnabled(false);
    assertThat(SystemConfigurationPropertyFeature.SBOM_MANAGER.isEnabled()).isFalse();
  }

  @Test
  public void testEnableFeature_alpForSbomManager_setEnabledForSingleTenant() {
    SystemConfigurationPropertyFeature.ALP_FOR_SBOM_MANAGER.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.ALP_FOR_SBOM_MANAGER.isEnabled()).isTrue();
  }

  @Test
  public void testDisabledByDefaultFeature_AlpForSbomManager() {
    assertThat(systemConfigurationPropertyDAO.getByName(ALP_FOR_SBOM_MANAGER))
        .isNull();
  }

  @Test
  public void testDisableFeature_AlpForSbomManager_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(ALP_FOR_SBOM_MANAGER))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_PrioritizedFindingsReport() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT
        .getPropertyName(), "false");
    service.enableFeature(PRIORITIZED_FINDINGS_REPORT);
    assertThat(systemConfigurationPropertyDAO.getByName(PRIORITIZED_FINDINGS_REPORT)).isNull();
  }

  @Test
  public void testEnableFeature_PrioritizedFindingsReport_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.PRIORITIZED_FINDINGS_REPORT))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_PrioritizedFindingsReport() {
    service.disableFeature(SystemConfigurationProperty.PRIORITIZED_FINDINGS_REPORT);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.PRIORITIZED_FINDINGS_REPORT)
        .getValue()).isEqualTo("false");
  }

  @Test
  public void testDisabledByDefaultFeature_PrioritizedFindingsReport() {
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.PRIORITIZED_FINDINGS_REPORT))
        .isNull();
  }

  @Test
  public void testDisableFeature_PrioritizedFindingsReport_AlreadyDisabled() {
    service.disableFeature(PRIORITIZED_FINDINGS_REPORT);
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.PRIORITIZED_FINDINGS_REPORT))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_SkipSbomImportValidation() {
    service.enableFeature(SKIP_SBOM_IMPORT_VALIDATION);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableFeature_SkipSbomImportValidation_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_SkipSbomImportValidation() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION
        .getPropertyName(), "true");
    service.disableFeature(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION))
        .isNull();
  }

  @Test
  public void testDisabledByDefaultFeature_SkipSbomImportValidation() {
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION))
        .isNull();
  }

  @Test
  public void testDisableFeature_SkipSbomImportValidation_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testEnableFeature_DeveloperBulkRecommendations() {
    service.enableFeature(DEVELOPER_BULK_RECOMMENDATIONS);
    assertThat(systemConfigurationPropertyDAO.getByName(DEVELOPER_BULK_RECOMMENDATIONS)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableFeature_DeveloperBulkRecommendations_AlreadyEnabled() {
    service.enableFeature(DEVELOPER_BULK_RECOMMENDATIONS);
    assertThatThrownBy(() -> service.enableFeature(DEVELOPER_BULK_RECOMMENDATIONS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testEnabledByDefaultFeature_DeveloperSummaryTable() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE
        .getPropertyName(), "true");
    assertThat(systemConfigurationPropertyDAO.getByName(DEVELOPER_SUMMARY_TABLE)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableFeature_DeveloperSummaryTable_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(DEVELOPER_SUMMARY_TABLE))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisabledFeature_DeveloperSummaryTable() {
    service.disableFeature(DEVELOPER_SUMMARY_TABLE);
    assertThat(systemConfigurationPropertyDAO.getByName(DEVELOPER_SUMMARY_TABLE)
        .getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_DeveloperSummaryTable_AlreadyDisabled() {
    service.disableFeature(DEVELOPER_SUMMARY_TABLE);
    assertThatThrownBy(() -> service.disableFeature(DEVELOPER_SUMMARY_TABLE))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testDisableFeature_DeveloperBulkRecommendations() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS
        .getPropertyName(), "true");
    service.disableFeature(DEVELOPER_BULK_RECOMMENDATIONS);
    assertThat(systemConfigurationPropertyDAO.getByName(DEVELOPER_BULK_RECOMMENDATIONS))
        .isNull();
  }

  @Test
  public void testDisabledByDefaultFeature_DeveloperBulkRecommendations() {
    assertThat(systemConfigurationPropertyDAO.getByName(DEVELOPER_BULK_RECOMMENDATIONS))
        .isNull();
  }

  @Test
  public void testDisableFeature_DeveloperBulkRecommendations_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(DEVELOPER_BULK_RECOMMENDATIONS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_CleanUpContinuousMonitoringReport() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("clean-up-sbom-continuous-monitoring-report"),
        service.getSystemConfigurationPropertyFeature("Clean-Up-Sbom-Continuous-Monitoring-Report"),
        service.getSystemConfigurationPropertyFeature("CLEAN-UP-SBOM-CONTINUOUS-MONITORING-REPORT"),
        service.getSystemConfigurationPropertyFeature("cleanUpSbomContinuousMonitoringReport"));

    // Assert all map to CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT
    assertThat(actual).allMatch(
        feature -> feature.equals(SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT));
  }

  @Test
  public void testDisableFeature_CleanUpContinuousMonitoringReport() {
    service.disableFeature(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT);

    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT)
            .getValue())
                .isEqualTo("false");
  }

  @Test
  public void testEnableFeature_CleanUpContinuousMonitoringReport_AlreadyEnabled() {
    assertThatThrownBy(
        () -> service.enableFeature(
            SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_CleanUpContinuousMonitoringReport_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT);
    assertThatThrownBy(
        () -> service.disableFeature(
            SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_SbomBinaryScanning() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("sbomBinaryScanning"),
        service.getSystemConfigurationPropertyFeature("sbom-binary-scanning"),
        service.getSystemConfigurationPropertyFeature("Sbom-Binary-Scanning"),
        service.getSystemConfigurationPropertyFeature("SBOM-BINARY-SCANNING"));

    // Assert all map to SBOM_BINARY_SCANNING
    assertThat(actual).allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING));
  }

  @Test
  public void testDisableFeature_SbomBinaryScanning() {
    service.disableFeature(SystemConfigurationProperty.SBOM_BINARY_SCANNING);
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SBOM_BINARY_SCANNING)
            .getValue())
                .isEqualTo("false");
  }

  @Test
  public void testEnableFeature_SbomBinaryScanning_AlreadyEnabled() {
    assertThatThrownBy(
        () -> service.enableFeature(
            SystemConfigurationProperty.SBOM_BINARY_SCANNING)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_SbomBinaryScanning_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.SBOM_BINARY_SCANNING);
    assertThatThrownBy(
        () -> service.disableFeature(
            SystemConfigurationProperty.SBOM_BINARY_SCANNING)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testDisableFeature_developerSuggestNonBreakingVersion() {
    service.disableFeature(SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION);
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION)
            .getValue())
                .isEqualTo("false");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION))
        .isFalse();
  }

  @Test
  public void testEnableFeature_developerSuggestNonBreakingVersion_AlreadyEnabled() {
    assertThatThrownBy(
        () -> service.enableFeature(
            SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testIsEnabled_developerSuggestNonBreakingVersion_EnabledByDefault() {
    // Enabled by default
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION))
            .isNull();
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION))
        .isTrue();
  }

  @Test
  public void testIsEnabled_developerSuggestNonBreakingVersion() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.DEVELOPER_SUGGEST_NON_BREAKING_VERSION)
            .getValue())
                .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION))
        .isTrue();
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_SbomContinuousMonitoringUi() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("sbomContinuousMonitoringUi"),
        service.getSystemConfigurationPropertyFeature("sbom-continuous-monitoring-ui"),
        service.getSystemConfigurationPropertyFeature("Sbom-Continuous-Monitoring-Ui"),
        service.getSystemConfigurationPropertyFeature("SBOM-CONTINUOUS-MONITORING-UI"));

    // Assert all map to SBOM_CONTINUOUS_MONITORING_UI
    assertThat(actual)
        .allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI));
  }

  @Test
  public void testDisableFeature_SbomContinuousMonitoringUi() {
    service.disableFeature(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI);

    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI)
            .getValue())
                .isEqualTo("false");
  }

  @Test
  public void testEnableFeature_SbomContinuousMonitoringUi_AlreadyEnabled() {
    assertThatThrownBy(
        () -> service.enableFeature(
            SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_SbomContinuousMonitoringUi_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI);

    assertThatThrownBy(
        () -> service.disableFeature(
            SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_SbomPolicies() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("sbomPolicies"),
        service.getSystemConfigurationPropertyFeature("sbom-policies"),
        service.getSystemConfigurationPropertyFeature("Sbom-Policies"),
        service.getSystemConfigurationPropertyFeature("SBOM-POLICIES"));

    // Assert all map to SBOM_POLICIES
    assertThat(actual).allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.SBOM_POLICIES));
  }

  @Test
  public void testEnableFeature_SbomPolicies_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.SBOM_POLICIES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_SbomPolicies() {
    service.disableFeature(SystemConfigurationProperty.SBOM_POLICIES);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SBOM_POLICIES)).isNull();
  }

  @Test
  public void testDisableFeature_SbomPolicies_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.SBOM_POLICIES);

    assertThatThrownBy(
        () -> service.disableFeature(
            SystemConfigurationProperty.SBOM_POLICIES)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_AutoWaivers() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("autoWaivers"),
        service.getSystemConfigurationPropertyFeature("autowaivers"),
        service.getSystemConfigurationPropertyFeature("AutoWaivers"),
        service.getSystemConfigurationPropertyFeature("AUTOWAIVERS"));

    // Assert all map to AUTO_WAIVERS
    assertThat(actual).allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.AUTO_WAIVERS));
  }

  @Test
  public void testEnabledByDefaultFeature_AutoWaivers() {
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.AUTO_WAIVERS))
            .isNull();
  }

  @Test
  public void testDisabledFeature_AutoWaivers() {
    service.disableFeature(AUTO_WAIVERS);
    assertThat(
        systemConfigurationPropertyDAO.getByName(AUTO_WAIVERS)
            .getValue())
                .isEqualTo("false");
  }

  @Test
  public void testEnableFeature_AutoWaivers_AlreadyEnabled() {
    assertThatThrownBy(
        () -> service.enableFeature(
            AUTO_WAIVERS)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_AutoWaivers_AlreadyDisabled() {
    service.disableFeature(AUTO_WAIVERS);
    assertThatThrownBy(
        () -> service.disableFeature(
            AUTO_WAIVERS)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_AutoWaivers_EnabledByDefault() {
    // Enabled by default
    assertThat(systemConfigurationPropertyDAO.getByName(AUTO_WAIVERS)).isNull();
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.AUTO_WAIVERS)).isTrue();
  }

  @Test
  public void testIsEnabled_AutoWaivers() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.AUTO_WAIVERS, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(
        systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.AUTO_WAIVERS).getValue())
            .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.AUTO_WAIVERS)).isTrue();
  }

  @Test
  public void testGetAllSystemConfigurationPropertyFeatureWithValue_singleTenant() {
    Map<String, Boolean> expectedFeatureConfigMap = getExpectedFeatureConfigMap();

    Map<String, Boolean> featureConfigMap = service.getAllSystemConfigurationPropertyFeatureWithValue(List.of());

    assertThat(featureConfigMap)
        .hasSize(expectedFeatureConfigMap.size())
        .containsExactlyInAnyOrderEntriesOf(expectedFeatureConfigMap)
        .doesNotContainKeys("transitiveSolver", "SAAS_LIFECYCLE_SCM_ENABLED");
  }

  @Test
  public void testGetAllSystemConfigurationPropertyFeatureWithValue_multiTenant() {
    TenantUtil tenantUtilMock = Mockito.mock(TenantUtil.class);
    Mockito.when(tenantUtilMock.isSingleTenant()).thenReturn(false);
    ApiConfigFeaturesService service = new ApiConfigFeaturesService(tenantUtilMock,
        Mockito.mock(TaskScheduler.class),
        Mockito.mock(SystemConfigurationPropertyCacheInvalidationJob.class));

    Map<String, Boolean> expectedFeatureConfigMap = getExpectedFeatureConfigMap();
    expectedFeatureConfigMap.put("saasLifecycleScmEnabled", true);

    Map<String, Boolean> featureConfigMap = service.getAllSystemConfigurationPropertyFeatureWithValue(List.of());

    assertThat(featureConfigMap)
        .hasSize(expectedFeatureConfigMap.size())
        .containsExactlyInAnyOrderEntriesOf(expectedFeatureConfigMap)
        .doesNotContainKeys("transitiveSolver");
  }

  @Test
  public void testGetAllSystemConfigurationPropertyFeatureWithValue_customFilter() {
    Map<String, Boolean> expectedFeatureConfigMap = getExpectedFeatureConfigMap();
    List<SystemConfigurationPropertyFeature> customFilter = List.of(
        SystemConfigurationPropertyFeature.SUCCESS_METRICS_CONFIGURATION,
        SystemConfigurationPropertyFeature.PRODUCT_LICENSE_CONFIGURATION,
        SystemConfigurationPropertyFeature.SYSTEM_NOTICE_CONFIGURATION,
        SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES,
        SystemConfigurationPropertyFeature.PROXY_CONFIGURATION,
        SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API,
        SystemConfigurationPropertyFeature.CODE_INSIGHTS,
        SystemConfigurationPropertyFeature.LDAP_CONFIGURATION,
        SystemConfigurationPropertyFeature.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES,
        SystemConfigurationPropertyFeature.SCAN_POM_FILES_IN_META_INF_DIRECTORY);
    customFilter.forEach(
        filtered -> expectedFeatureConfigMap.remove(service.getFeatureForPropertyName(filtered.getPropertyName())));

    Map<String, Boolean> featureConfigMap = service.getAllSystemConfigurationPropertyFeatureWithValue(customFilter);

    assertThat(featureConfigMap)
        .hasSize(expectedFeatureConfigMap.size())
        .containsExactlyInAnyOrderEntriesOf(expectedFeatureConfigMap)
        .doesNotContainKeys("transitiveSolver", "SAAS_LIFECYCLE_SCM_ENABLED");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_ContainerImagesEvalEnabled() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("containerImagesEvalEnabled"),
        service.getSystemConfigurationPropertyFeature("container-images-eval-enabled"),
        service.getSystemConfigurationPropertyFeature("Container-Images-Eval-Enabled"),
        service.getSystemConfigurationPropertyFeature("CONTAINER-IMAGES-EVAL-ENABLED"));

    // Assert all map to CONTAINER_IMAGES_EVAL_ENABLED
    assertThat(actual)
        .allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED));
  }

  @Test
  public void testEnableFeature_ContainerImagesEvalEnabled_AlreadyEnabled() {
    assertThatThrownBy(
        () -> service.enableFeature(
            SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ContainerImagesEvalEnabled_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED);

    assertThatThrownBy(
        () -> service.disableFeature(
            SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED)).isInstanceOf(
                BadRequestException.class).hasMessage("Feature is already disabled.");
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_ZScaler() {
    assertThat(service.getSystemConfigurationPropertyFeature(SystemConfigurationProperty.ZSCALER))
        .isEqualTo(SystemConfigurationPropertyFeature.ZSCALER);
  }

  @Test
  public void testEnableFeature_ZScaler_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.ZSCALER))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ZScaler_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.ZSCALER);
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.ZSCALER))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_ZScaler() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.ZSCALER, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.ZSCALER).getValue())
        .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.ZSCALER)).isTrue();
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_ThirdPartyKevLookup() {
    assertThat(service.getSystemConfigurationPropertyFeature(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP))
        .isEqualTo(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP);
  }

  @Test
  public void testEnableFeature_ThirdPartyKevLookup_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_ThirdPartyKevLookup_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP);
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_ThirdPartyKevLookup() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP).getValue())
        .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP)).isTrue();
  }

  private Map<String, Boolean> getExpectedFeatureConfigMap() {
    Map<String, Boolean> expectedFeatureConfigMap = new LinkedHashMap<>();

    expectedFeatureConfigMap.put("ADVANCED_SEARCH_CONFIGURATION", true);
    expectedFeatureConfigMap.put("ADVANCED_SEARCH_ENABLED", false);
    expectedFeatureConfigMap.put("GLOBAL_SEARCH", false);
    expectedFeatureConfigMap.put("CATALOG_FEDERATION", false);
    expectedFeatureConfigMap.put("alpForSbomManager", false);
    expectedFeatureConfigMap.put("API_PAGE", true);
    expectedFeatureConfigMap.put("AUTOMATIC_APPLICATION_CONFIGURATION", true);
    expectedFeatureConfigMap.put("AUTOMATIC_SCM_CONFIGURATION", true);
    expectedFeatureConfigMap.put("autoWaivers", true);
    expectedFeatureConfigMap.put("BUILT_FROM_SOURCE", false);
    expectedFeatureConfigMap.put("PREVIEW_NEXUS_ONE_UI", false);
    expectedFeatureConfigMap.put("PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED", false);
    expectedFeatureConfigMap.put("PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED", false);
    expectedFeatureConfigMap.put("PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW", false);
    expectedFeatureConfigMap.put("PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK", false);
    expectedFeatureConfigMap.put("cleanUpSbomContinuousMonitoringReport", true);
    expectedFeatureConfigMap.put("codeInsights", true);
    expectedFeatureConfigMap.put("componentChangeDetectionApi", false);
    expectedFeatureConfigMap.put("componentSearchApiWithInnerSource", true);
    expectedFeatureConfigMap.put("CROWD_INTEGRATION", true);
    expectedFeatureConfigMap.put("dashboard", true);
    expectedFeatureConfigMap.put("defaultBranchMonitoring", true);
    expectedFeatureConfigMap.put("dependencyDataInApi", true);
    expectedFeatureConfigMap.put("developerBulkRecommendations", false);
    expectedFeatureConfigMap.put("developerSuggestNonBreakingVersion", true);
    expectedFeatureConfigMap.put("developerSummaryTable", true);
    expectedFeatureConfigMap.put("developmentDashboardMetricCollection", true);
    expectedFeatureConfigMap.put("EMAIL_CONFIGURATION", true);
    expectedFeatureConfigMap.put("enableSsoOnly", false);
    expectedFeatureConfigMap.put("enableUnauthenticatedPages", true);
    expectedFeatureConfigMap.put("expireWaiverWhenRemediationAvailable", false);
    expectedFeatureConfigMap.put("firewallEnterpriseReporting", true);
    expectedFeatureConfigMap.put("hostedRepositoryEvaluation", false);
    expectedFeatureConfigMap.put("innerSourceRepositoryIntegration", true);
    expectedFeatureConfigMap.put("innerSourceTransitiveWaiver", true);
    expectedFeatureConfigMap.put("internalFirewallOnboardingEnabled", false);
    expectedFeatureConfigMap.put("internalSourceControlPolicyEvaluations", true);
    expectedFeatureConfigMap.put("LDAP_CONFIGURATION", true);
    expectedFeatureConfigMap.put("logoutAuth0OnLogout", false);
    expectedFeatureConfigMap.put("nonBreakingVersionSuggestionTelemetry", true);
    expectedFeatureConfigMap.put("OAUTH2_ENABLED", false);
    expectedFeatureConfigMap.put("prioritizedFindingsReport", true);
    expectedFeatureConfigMap.put("PRODUCT_LICENSE_CONFIGURATION", true);
    expectedFeatureConfigMap.put("PROXY_CONFIGURATION", true);
    expectedFeatureConfigMap.put("prCommenting", true);
    expectedFeatureConfigMap.put("prLineCommenting", true);
    expectedFeatureConfigMap.put("prLineCommentingBitbucketOnNoChange", false);
    expectedFeatureConfigMap.put("reportsList", true);
    expectedFeatureConfigMap.put("sbomBinaryScanning", true);
    expectedFeatureConfigMap.put("sbomContinuousMonitoringUi", true);
    expectedFeatureConfigMap.put("sbomManager", false);
    expectedFeatureConfigMap.put("sbomPolicies", true);
    expectedFeatureConfigMap.put("scanNpmDevAndOptDependencies", false);
    expectedFeatureConfigMap.put("scanPomFilesInMetaInfDirectory", false);
    expectedFeatureConfigMap.put("scmUxImprovements", false);
    expectedFeatureConfigMap.put("skipSbomImportValidation", false);
    expectedFeatureConfigMap.put("SSO_IDP_MANAGED_BY_SONATYPE", false);
    expectedFeatureConfigMap.put("SUCCESS_METRICS_CONFIGURATION", true);
    expectedFeatureConfigMap.put("SYSTEM_NOTICE_CONFIGURATION", true);
    expectedFeatureConfigMap.put("vulnerabilitySource", false);
    expectedFeatureConfigMap.put("WEBHOOK_CONFIGURATION", true);
    expectedFeatureConfigMap.put("containerImagesEvalEnabled", true);
    expectedFeatureConfigMap.put("zScaler", true);
    expectedFeatureConfigMap.put("thirdPartyKevLookup", true);
    expectedFeatureConfigMap.put("SAML_ENABLED", true);
    expectedFeatureConfigMap.put("userManagementPages", true);
    expectedFeatureConfigMap.put("epssDataEnabled", false);
    expectedFeatureConfigMap.put("enableFedRAMPAudit", false);
    expectedFeatureConfigMap.put("saasLifecycleScmPrsEnabled", true);
    expectedFeatureConfigMap.put("userActivityTracking", false);
    expectedFeatureConfigMap.put("waiverRequestWorkflowEnabled", true);
    expectedFeatureConfigMap.put("exitOnFatalError", true);
    expectedFeatureConfigMap.put("maliciousUrlsPartnerAccess", false);
    expectedFeatureConfigMap.put("guideUiEnabled", false);
    expectedFeatureConfigMap.put("consumptionReportingEnabled", false);
    expectedFeatureConfigMap.put("iqProxyEnabled", false);
    expectedFeatureConfigMap.put("iqFirewallEnterpriseEnabled", false);
    expectedFeatureConfigMap.put("iqFirewallEnterpriseRedirectUiEnabled", false);
    expectedFeatureConfigMap.put("scmRelayIntegration", false);
    expectedFeatureConfigMap.put("sloViolationFeedEnabled", false);

    return expectedFeatureConfigMap;
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_UserManagementPages() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("userManagementPages"),
        service.getSystemConfigurationPropertyFeature("user-management-pages"),
        service.getSystemConfigurationPropertyFeature("User-Management-Pages"),
        service.getSystemConfigurationPropertyFeature("USER-MANAGEMENT-PAGES"));

    // Assert all map to USER_MANAGEMENT_PAGES
    assertThat(actual).allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES));
  }

  @Test
  public void testEnableFeature_UserManagementPages_AlreadyEnabled() {
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.USER_MANAGEMENT_PAGES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_UserManagementPages() {
    service.disableFeature(SystemConfigurationProperty.USER_MANAGEMENT_PAGES);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_MANAGEMENT_PAGES)
        .getValue()).isEqualTo("false");
  }

  @Test
  public void testDisableFeature_UserManagementPages_AlreadyDisabled() {
    service.disableFeature(SystemConfigurationProperty.USER_MANAGEMENT_PAGES);
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.USER_MANAGEMENT_PAGES))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_UserManagementPages_EnabledByDefault() {
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_MANAGEMENT_PAGES))
        .isNull();
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)).isTrue();
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_EpssDataEnabled() {
    assertThat(service.getSystemConfigurationPropertyFeature(SystemConfigurationProperty.EPSS_DATA))
        .isEqualTo(SystemConfigurationPropertyFeature.EPSS_DATA);
  }

  @Test
  public void testEnableFeature_EpssDataEnabled_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.EPSS_DATA);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.EPSS_DATA))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_EpssDataEnabled_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.EPSS_DATA))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_EpssDataEnabled() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.EPSS_DATA, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.EPSS_DATA).getValue())
        .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.EPSS_DATA)).isTrue();
  }

  @Test
  public void testFeatureDefaultValue_UserManagementPages_WithFips() {
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    assertThat(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES.isEnabled()).isTrue();
  }

  @Test
  public void testFeatureDefaultValue_SamlEnabled_WithFips() {
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    assertThat(SystemConfigurationPropertyFeature.SAML_ENABLED.isEnabled()).isTrue();
  }

  @Test
  public void testFeatureDefaultValue_LogoutAuth0OnLogout_WithFips() {
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    assertThat(SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.isEnabled()).isFalse();
  }

  @Test
  public void testFeatureDefaultValue_Oauth2Enabled_WithFips() {
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    assertThat(SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()).isFalse();
  }

  @Test
  public void testFeatureDefaultValue_EnableSsoOnly_WithFips() {
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    assertThat(SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.isEnabled()).isFalse();
  }

  @Test
  public void testFeatureDefaultValue_UserManagementPages() {
    assertThat(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES.isEnabled()).isTrue();
  }

  @Test
  public void testFeatureDefaultValue_SamlEnabled() {
    assertThat(SystemConfigurationPropertyFeature.SAML_ENABLED.isEnabled()).isTrue();
  }

  @Test
  public void testFeatureDefaultValue_LogoutAuth0OnLogout() {
    assertThat(SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.isEnabled()).isFalse();
  }

  @Test
  public void testFeatureDefaultValue_Oauth2Enabled() {
    assertThat(SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()).isFalse();
  }

  @Test
  public void testFeatureDefaultValue_EnableSsoOnly() {
    assertThat(SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.isEnabled()).isFalse();
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_UserActivityTracking() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("userActivityTracking"),
        service.getSystemConfigurationPropertyFeature("user-activity-tracking"),
        service.getSystemConfigurationPropertyFeature("User-Activity-Tracking"),
        service.getSystemConfigurationPropertyFeature("USER-ACTIVITY-TRACKING"));

    // Assert all map to USER_ACTIVITY_TRACKING
    assertThat(actual).allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING));
  }

  @Test
  public void testEnableFeature_UserActivityTracking() {
    service.enableFeature(SystemConfigurationProperty.USER_ACTIVITY_TRACKING);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_ACTIVITY_TRACKING)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableFeature_UserActivityTracking_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.USER_ACTIVITY_TRACKING);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.USER_ACTIVITY_TRACKING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_UserActivityTracking() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.getPropertyName(), "true");
    service.disableFeature(SystemConfigurationProperty.USER_ACTIVITY_TRACKING);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_ACTIVITY_TRACKING))
        .isNull();
  }

  @Test
  public void testDisabledByDefaultFeature_UserActivityTracking() {
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_ACTIVITY_TRACKING))
        .isNull();
  }

  @Test
  public void testDisableFeature_UserActivityTracking_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.USER_ACTIVITY_TRACKING))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_UserActivityTracking_DisabledByDefault() {
    // Disabled by default
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_ACTIVITY_TRACKING)).isNull();
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)).isFalse();
  }

  @Test
  public void testIsEnabled_UserActivityTracking() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.USER_ACTIVITY_TRACKING, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.USER_ACTIVITY_TRACKING).getValue())
        .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)).isTrue();
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_MaliciousUrlsPartnerAccess() {
    assertThat(service.getSystemConfigurationPropertyFeature(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS))
        .isEqualTo(SystemConfigurationPropertyFeature.MALICIOUS_URLS_PARTNER_ACCESS);
  }

  @Test
  public void testEnableFeature_MaliciousUrlsPartnerAccess_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_MaliciousUrlsPartnerAccess_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_MaliciousUrlsPartnerAccess() {
    SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(MALICIOUS_URLS_PARTNER_ACCESS, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS)
        .getValue()).isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.MALICIOUS_URLS_PARTNER_ACCESS)).isTrue();
  }

  @Test
  public void testGetSystemConfigurationPropertyFeature_HostedRepositoryEvaluation() {
    // All input variants for the feature
    List<SystemConfigurationPropertyFeature> actual = List.of(
        service.getSystemConfigurationPropertyFeature("hostedRepositoryEvaluation"),
        service.getSystemConfigurationPropertyFeature("hosted-repository-evaluation"),
        service.getSystemConfigurationPropertyFeature("Hosted-Repository-Evaluation"),
        service.getSystemConfigurationPropertyFeature("HOSTED-REPOSITORY-EVALUATION"));

    // Assert all map to HOSTED_REPOSITORY_EVALUATION
    assertThat(actual)
        .allMatch(feature -> feature.equals(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION));
  }

  @Test
  public void testEnableFeature_HostedRepositoryEvaluation() {
    service.enableFeature(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION)
        .getValue()).isEqualTo("true");
  }

  @Test
  public void testEnableFeature_HostedRepositoryEvaluation_AlreadyEnabled() {
    service.enableFeature(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION);
    assertThatThrownBy(() -> service.enableFeature(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already enabled.");
  }

  @Test
  public void testDisableFeature_HostedRepositoryEvaluation() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.getPropertyName(), "true");
    service.disableFeature(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION))
        .isNull();
  }

  @Test
  public void testDisableFeature_HostedRepositoryEvaluation_AlreadyDisabled() {
    assertThatThrownBy(() -> service.disableFeature(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature is already disabled.");
  }

  @Test
  public void testIsEnabled_HostedRepositoryEvaluation_DisabledByDefault() {
    // Disabled by default
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION))
        .isNull();
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)).isFalse();
  }

  @Test
  public void testIsEnabled_HostedRepositoryEvaluation() {
    final SystemConfigurationProperty systemConfigurationProperty =
        new SystemConfigurationProperty(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION, "true");
    systemConfigurationPropertyDAO.insert(systemConfigurationProperty);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION)
        .getValue())
            .isEqualTo("true");
    assertThat(service.isFeatureEnabled(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)).isTrue();
  }
}
