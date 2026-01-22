/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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

  static final String FEATURE_PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE = "prLineCommentingBitbucketOnNoChange";

  static final String FEATURE_ENABLE_UNAUTHENTICATED_PAGES = "enableUnauthenticatedPages";

  static final String FEATURE_ENABLE_SSO_ONLY = "enableSsoOnly";

  static final String FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS = "internalSourceControlPolicyEvaluations";

  public static final String FEATURE_SAAS_LIFECYCLE_SCM_ENABLED = "saasLifecycleScmEnabled";

  public static final String FEATURE_SAAS_LIFECYCLE_SCM_PRS_ENABLED = "saasLifecycleScmPrsEnabled";

  public static final String FEATURE_SCM_UX_IMPROVEMENTS = "scmUxImprovements";

  static final String FEATURE_USER_ACTIVITY_TRACKING = "userActivityTracking";

  static final String FEATURE_EXIT_ON_FATAL_ERROR = "exitOnFatalError";

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

  public boolean isSaasLifecycleScmPrsEnabled() {
    return isFeatureEnabled(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED);
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

  private static boolean isSingleTenantAndPropertyNotSupported(TenantUtil tenantUtil, String propertyName) {
    return tenantUtil.isSingleTenant() && NOT_SUPPORTED_SELF_HOSTED_SYSTEM_PROPERTIES.contains(propertyName);
  }

  private static void throwErrorWhenSingleTenantAndPropertyNotSupported(TenantUtil tenantUtil, String propertyName) {
    if (isSingleTenantAndPropertyNotSupported(tenantUtil, propertyName)) {
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
    return switch (feature) {
      case FEATURE_DASHBOARD -> SystemConfigurationProperty.DASHBOARD_DISABLED;
      case FEATURE_REPORTS_LIST -> SystemConfigurationProperty.REPORTS_LIST_DISABLED;
      case FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION ->
          SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED;
      case FEATURE_TRANSITIVE_SOLVER -> SystemConfigurationProperty.TRANSITIVE_SOLVER_ENABLED;
      case FEATURE_CODE_INSIGHTS -> SystemConfigurationProperty.CODE_INSIGHTS;
      case FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE ->
          SystemConfigurationProperty.COMPONENT_SEARCH_API_WITH_INNERSOURCE;
      case FEATURE_DEFAULT_BRANCH_MONITORING -> SystemConfigurationProperty.DEFAULT_BRANCH_MONITORING;
      case FEATURE_DEPENDENCY_DATA_IN_API -> SystemConfigurationProperty.DEPENDENCY_DATA_IN_API;
      case FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER -> SystemConfigurationProperty.INNER_SOURCE_TRANSITIVE_WAIVER;
      case FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION ->
          SystemConfigurationProperty.INNER_SOURCE_REPOSITORY_INTEGRATION;
      case FEATURE_PR_COMMENTING -> SystemConfigurationProperty.PR_COMMENTING;
      case FEATURE_PR_LINE_COMMENTING -> SystemConfigurationProperty.PR_LINE_COMMENTING;
      case FEATURE_PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE ->
          SystemConfigurationProperty.PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE;
      case FEATURE_ENABLE_UNAUTHENTICATED_PAGES -> SystemConfigurationProperty.ENABLE_UNAUTHENTICATED_PAGES;
      case FEATURE_ENABLE_SSO_ONLY -> SystemConfigurationProperty.ENABLE_SSO_ONLY;
      case FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS ->
          SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS;
      case FEATURE_SCM_UX_IMPROVEMENTS -> SystemConfigurationProperty.SCM_UX_IMPROVEMENTS;
      case FEATURE_SAAS_LIFECYCLE_SCM_ENABLED -> SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED;
      case FEATURE_SAAS_LIFECYCLE_SCM_PRS_ENABLED -> SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_PRS_ENABLED;
      case FEATURE_USER_ACTIVITY_TRACKING -> SystemConfigurationProperty.USER_ACTIVITY_TRACKING;
      case FEATURE_EXIT_ON_FATAL_ERROR -> SystemConfigurationProperty.EXIT_ON_FATAL_ERROR;
      default -> feature;
    };
  }

  /**
   * Get all system configuration property features with their current values.
   * Filtering out unsupported and multitenant specific features.
   *
   * @param customFilter Optional list of features to filter out
   * @return Map of feature name to boolean value
   */
  public Map<String, Boolean> getAllSystemConfigurationPropertyFeatureWithValue(
      List<SystemConfigurationPropertyFeature> customFilter)
  {
    List<SystemConfigurationPropertyFeature> allFeatures =
        new ArrayList<>(List.of(SystemConfigurationPropertyFeature.values()));

    //filter out custom requested features
    Optional.ofNullable(customFilter).ifPresent(allFeatures::removeAll);

    //filter out unsupported features
    NO_LONGER_SUPPORTED_FLAGS.forEach(unsupportedFeature -> allFeatures.removeIf(
        property -> unsupportedFeature.getReplacementFeatureName()
            .equals(getFeatureForPropertyName(property.getPropertyName()))));

    return allFeatures.stream()
        .filter(property -> !isSingleTenantAndPropertyNotSupported(tenantUtil, property.getPropertyName()))
        .sorted(Comparator.comparing(SystemConfigurationPropertyFeature::name))
        .collect(Collectors.toMap(property -> getFeatureForPropertyName(property.getPropertyName()),
            SystemConfigurationPropertyFeature::isEnabled,
            (e1, e2) -> e1, LinkedHashMap::new));
  }

  // Visible for testing
  String getFeatureForPropertyName(String propertyName) {
    return switch (propertyName) {
      case SystemConfigurationProperty.DASHBOARD_DISABLED -> FEATURE_DASHBOARD;
      case SystemConfigurationProperty.REPORTS_LIST_DISABLED -> FEATURE_REPORTS_LIST;
      case SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED ->
          FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION;
      case SystemConfigurationProperty.TRANSITIVE_SOLVER_ENABLED -> FEATURE_TRANSITIVE_SOLVER;
      case SystemConfigurationProperty.CODE_INSIGHTS -> FEATURE_CODE_INSIGHTS;
      case SystemConfigurationProperty.COMPONENT_SEARCH_API_WITH_INNERSOURCE ->
          FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE;
      case SystemConfigurationProperty.DEFAULT_BRANCH_MONITORING -> FEATURE_DEFAULT_BRANCH_MONITORING;
      case SystemConfigurationProperty.DEPENDENCY_DATA_IN_API -> FEATURE_DEPENDENCY_DATA_IN_API;
      case SystemConfigurationProperty.INNER_SOURCE_TRANSITIVE_WAIVER -> FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER;
      case SystemConfigurationProperty.INNER_SOURCE_REPOSITORY_INTEGRATION ->
          FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION;
      case SystemConfigurationProperty.PR_COMMENTING -> FEATURE_PR_COMMENTING;
      case SystemConfigurationProperty.PR_LINE_COMMENTING -> FEATURE_PR_LINE_COMMENTING;
      case SystemConfigurationProperty.PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE ->
          FEATURE_PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE;
      case SystemConfigurationProperty.ENABLE_UNAUTHENTICATED_PAGES -> FEATURE_ENABLE_UNAUTHENTICATED_PAGES;
      case SystemConfigurationProperty.ENABLE_SSO_ONLY -> FEATURE_ENABLE_SSO_ONLY;
      case SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS ->
          FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS;
      case SystemConfigurationProperty.SCM_UX_IMPROVEMENTS -> FEATURE_SCM_UX_IMPROVEMENTS;
      case SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED -> FEATURE_SAAS_LIFECYCLE_SCM_ENABLED;
      case SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_PRS_ENABLED -> FEATURE_SAAS_LIFECYCLE_SCM_PRS_ENABLED;
      case SystemConfigurationProperty.USER_ACTIVITY_TRACKING -> FEATURE_USER_ACTIVITY_TRACKING;
      case SystemConfigurationProperty.EXIT_ON_FATAL_ERROR -> FEATURE_EXIT_ON_FATAL_ERROR;
      default -> propertyName;
    };
  }
}
