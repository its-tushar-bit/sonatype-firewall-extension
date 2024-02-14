/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApiConfigFeaturesService
{
  private static final Logger log = LoggerFactory.getLogger(ApiConfigFeaturesService.class);

  static final String FEATURE_DASHBOARD = "dashboard";

  static final String FEATURE_REPORTS_LIST = "reportsList";

  static final String FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION = "vulnerabilitySource";

  static final String FEATURE_TRANSITIVE_SOLVER = "transitiveSolver";

  static final String FEATURE_CODE_INSIGHTS = "codeInsights";

  static final String FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE = "componentSearchApiWithInnerSource";

  static final String FEATURE_DEFAULT_BRANCH_MONITORING = "defaultBranchMonitoring";

  static final String FEATURE_DEPENDENCY_DATA_IN_API = "dependencyDataInApi";

  static final String FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER = "innerSourceTransitiveWaiver";

  static final String FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION = "innerSourceRepositoryIntegration";

  static final String FEATURE_PR_COMMENTING = "prCommenting";

  static final String FEATURE_PR_LINE_COMMENTING = "prLineCommenting";

  static final String FEATURE_ENABLE_UNAUTHENTICATED_PAGES = "enableUnauthenticatedPages";

  static final String FEATURE_ENABLE_SSO_ONLY = "enableSsoOnly";

  static final String FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS = "internalSourceControlPolicyEvaluations";

  public static final String FEATURE_SAAS_LIFECYCLE_SCM_ENABLED = "saasLifecycleScmEnabled";

  public static final String FEATURE_SCM_UX_IMPROVEMENTS = "scmUxImprovements";

  private static final List<UnsupportedFeature> NO_LONGER_SUPPORTED_FLAGS = Arrays.asList(
      new UnsupportedFeature("transitiveSolverDisable", FEATURE_TRANSITIVE_SOLVER)
  );

  /**
   * This list contains a list of features that must not be enabled for Self Hosted IQ
   */
  static final List<String> NOT_SUPPORTED_SELF_HOSTED_SYSTEM_PROPERTIES = Arrays.asList(
      SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED
  );

  private final TenantUtil tenantUtil;

  @Inject
  public ApiConfigFeaturesService(final TenantUtil tenantUtil) {
    this.tenantUtil = tenantUtil;
  }

  /**
   * isFeatureEnabled this method is useful as it allows the use of features with injection rather than using the static
   * {@link SystemConfigurationPropertyFeature#isEnabled()} method.
   *
   * @return boolean true if the feature is enabled, else false
   */
  public boolean isFeatureEnabled(SystemConfigurationPropertyFeature feature) {
    return feature.isEnabled();
  }

  /**
   * ApiConfigFeaturesService is feature enabled helper methods
   */
  public boolean isSaasLifecycleScmEnabled() {
    return isFeatureEnabled(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED);
  }

  public boolean isDefaultBranchMonitoringEnabled() {
    return isFeatureEnabled(SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void enableFeature(String feature) {
    enableFeatureNoAuthz(feature);
  }

  public void enableFeatureNoAuthz(String feature) {
    SystemConfigurationPropertyFeature systemConfigurationPropertyFeature =
        getSystemConfigurationPropertyFeature(feature);

    if (systemConfigurationPropertyFeature.isEnabled()) {
      throw new FeatureAlreadyEnabledException("Feature is already enabled.");
    }

    systemConfigurationPropertyFeature.setEnabled(true);
    AuditData.get().setSystemConfigurationPropertyFeature(systemConfigurationPropertyFeature);
    log.debug("Enabled feature '{}'", feature);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {
    disableFeatureNoAuthz(feature);
  }

  public void disableFeatureNoAuthz(String feature) {
    SystemConfigurationPropertyFeature systemConfigurationPropertyFeature =
        getSystemConfigurationPropertyFeature(feature);

    if (!systemConfigurationPropertyFeature.isEnabled()) {
      throw new FeatureAlreadyDisabledException("Feature is already disabled.");
    }

    systemConfigurationPropertyFeature.setEnabled(false);
    AuditData.get().setSystemConfigurationPropertyFeature(systemConfigurationPropertyFeature);
    log.debug("Disabled feature '{}'", feature);
  }

  private static void throwErrorIfFeatureNoLongerSupported(final String feature) {
    NO_LONGER_SUPPORTED_FLAGS
        .stream()
        .filter(entry -> entry.getFeatureName().equals(feature))
        .findFirst()
        .ifPresent(unsupportedFeature -> {
          if (unsupportedFeature.hasReplacementFeatureName()) {
            throw new BadRequestException("'" + feature + "' is no longer supported. Instead you can " +
                "disable and enable the feature using '" + unsupportedFeature.getReplacementFeatureName() + "'");
          }
          else {
            throw new BadRequestException("'" + feature + "' is no longer supported.");
          }
        });
  }

  private static void throwErrorWhenSingleTenantAndPropertyNotSupported(TenantUtil tenantUtil, String propertyName) {
    if (tenantUtil.isSingleTenant() && NOT_SUPPORTED_SELF_HOSTED_SYSTEM_PROPERTIES.contains(propertyName)) {
      throw new BadRequestException("'" + propertyName + "' is not supported for self hosted.");
    }
  }

  // Visible for testing
  SystemConfigurationPropertyFeature getSystemConfigurationPropertyFeature(String feature) {
    throwErrorIfFeatureNoLongerSupported(feature);

    String propertyName = getPropertyNameForFeature(feature);
    throwErrorWhenSingleTenantAndPropertyNotSupported(tenantUtil, propertyName);

    return Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(s -> s.getPropertyName().equalsIgnoreCase(propertyName) ||
            s.name().equalsIgnoreCase(feature) ||
            s.getId().equalsIgnoreCase(feature))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("Feature not supported: " + feature));
  }

  // Visible for testing
  String getPropertyNameForFeature(String feature) {
    switch (feature) {
      case FEATURE_DASHBOARD:
        return SystemConfigurationProperty.DASHBOARD_DISABLED;
      case FEATURE_REPORTS_LIST:
        return SystemConfigurationProperty.REPORTS_LIST_DISABLED;
      case FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION:
        return SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED;
      case FEATURE_TRANSITIVE_SOLVER:
        return SystemConfigurationProperty.TRANSITIVE_SOLVER_ENABLED;
      case FEATURE_CODE_INSIGHTS:
        return SystemConfigurationProperty.CODE_INSIGHTS;
      case FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE:
        return SystemConfigurationProperty.COMPONENT_SEARCH_API_WITH_INNERSOURCE;
      case FEATURE_DEFAULT_BRANCH_MONITORING:
        return SystemConfigurationProperty.DEFAULT_BRANCH_MONITORING;
      case FEATURE_DEPENDENCY_DATA_IN_API:
        return SystemConfigurationProperty.DEPENDENCY_DATA_IN_API;
      case FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER:
        return SystemConfigurationProperty.INNER_SOURCE_TRANSITIVE_WAIVER;
      case FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION:
        return SystemConfigurationProperty.INNER_SOURCE_REPOSITORY_INTEGRATION;
      case FEATURE_PR_COMMENTING:
        return SystemConfigurationProperty.PR_COMMENTING;
      case FEATURE_PR_LINE_COMMENTING:
        return SystemConfigurationProperty.PR_LINE_COMMENTING;
      case FEATURE_ENABLE_UNAUTHENTICATED_PAGES:
        return SystemConfigurationProperty.ENABLE_UNAUTHENTICATED_PAGES;
      case FEATURE_ENABLE_SSO_ONLY:
        return SystemConfigurationProperty.ENABLE_SSO_ONLY;
      case FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS:
        return SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS;
      case FEATURE_SCM_UX_IMPROVEMENTS:
        return SystemConfigurationProperty.SCM_UX_IMPROVEMENTS;
      case FEATURE_SAAS_LIFECYCLE_SCM_ENABLED:
        return SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED;
      default:
        return feature;
    }
  }
}
