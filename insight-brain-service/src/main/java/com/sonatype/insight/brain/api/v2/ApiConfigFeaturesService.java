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
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.features.FeaturesResource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;

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

  public static final String NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR = "NXIQ_ENABLE_UNAUTHENTICATED_PAGES";

  public static final String NXIQ_ENABLE_SSO_ONLY_ENV_VAR = "NXIQ_ENABLE_SSO_ONLY";

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final TenantUtil tenantUtil;

  private static final List<UnsupportedFeature> NO_LONGER_SUPPORTED_FLAGS = Arrays.asList(
      new UnsupportedFeature("transitiveSolverDisable", FEATURE_TRANSITIVE_SOLVER)
  );

  /**
   * This list contains a list of features that must not be enabled for Self Hosted IQ
   */
  static final List<String> NOT_SUPPORTED_SELF_HOSTED_SYSTEM_PROPERTIES = Arrays.asList(
      SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED
  );

  /**
   * This enumeration contains features that can be enabled/disabled by the {@link ApiConfigFeaturesResource}.
   * <br/><br/> Each enum value has these properties:
   * <ul>
   *   <li>
   *     {@code name} - this is returned to the frontend via {@link FeaturesResource#getFeatures()} after being
   *     transformed according to {@link Feature#getId()}.
   *   </li>
   *   <li>
   *     {@code propertyName} - this is the name stored in the {@link SystemConfigurationProperty} table.
   *   </li>
   *   <li>
   *     {@code propertyValue} - this represents the value that will be stored in the
   *     {@link SystemConfigurationProperty} table. Note that the value has no impact on whether or not the feature is
   *     enabled/disabled (only the presence/absence of the row itself). However the value can be set to help
   *     understanding. This defaults to the opposite of {@code enabledWhenAbsent}.
   *   </li>
   *   <li>
   *     {@code enabledWhenAbsent} - if this is {@code true}, then the feature will be enabled even if its
   *     {@code propertyName} is absent from the {@link SystemConfigurationProperty} table.
   *   </li>
   * </ul>
   * Note that if you want the feature name passed to {@link ApiConfigFeaturesResource} to be different to the
   * {@code name}, result of {@link Feature#getId()}, and {@code propertyName}, then you need to add a mapping to the
   * {@link ApiConfigFeaturesService#getPropertyNameForFeature} method.
   * <br/><br/>
   * Typically, a feature would start with {@code enabledWhenAbsent} set to {@code false}, making it experimental.
   * When it's production-ready {@code enabledWhenAbsent} can be changed to {@code true} alongside an incremental script
   * to delete the feature from the {@link SystemConfigurationProperty} table.
   */
  public enum SystemConfigurationPropertyFeature
      implements Feature
  {
    DASHBOARD_CAN_BE_ENABLED(SystemConfigurationProperty.DASHBOARD_DISABLED, true, true),
    REPORTS_LIST_CAN_BE_ENABLED(SystemConfigurationProperty.REPORTS_LIST_DISABLED, true, true),
    VULNERABILITY_SOURCE(
        SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED, true, true),
    BUILT_FROM_SOURCE(SystemConfigurationProperty.BUILT_FROM_SOURCE, false),
    CROWD_INTEGRATION(SystemConfigurationProperty.CROWD_INTEGRATION, true),
    WEBHOOK_CONFIGURATION(SystemConfigurationProperty.WEBHOOK_CONFIGURATION, true),
    PRODUCT_LICENSE_CONFIGURATION(SystemConfigurationProperty.PRODUCT_LICENSE_CONFIGURATION, true),
    LDAP_CONFIGURATION(SystemConfigurationProperty.LDAP_CONFIGURATION, true),
    EMAIL_CONFIGURATION(SystemConfigurationProperty.EMAIL_CONFIGURATION, true),
    PROXY_CONFIGURATION(SystemConfigurationProperty.PROXY_CONFIGURATION, true),
    SYSTEM_NOTICE_CONFIGURATION(SystemConfigurationProperty.SYSTEM_NOTICE_CONFIGURATION, true),
    SUCCESS_METRICS_CONFIGURATION(SystemConfigurationProperty.SUCCESS_METRICS_CONFIGURATION, true),
    AUTOMATIC_APPLICATION_CONFIGURATION(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CONFIGURATION, true),
    AUTOMATIC_SCM_CONFIGURATION(SystemConfigurationProperty.AUTOMATIC_SCM_CONFIGURATION, true),
    ADVANCED_SEARCH_CONFIGURATION(SystemConfigurationProperty.ADVANCED_SEARCH_CONFIGURATION, true),
    TRANSITIVE_SOLVER(SystemConfigurationProperty.TRANSITIVE_SOLVER_ENABLED, true),
    CODE_INSIGHTS(SystemConfigurationProperty.CODE_INSIGHTS, true),
    COMPONENT_SEARCH_API_WITH_INNERSOURCE(SystemConfigurationProperty.COMPONENT_SEARCH_API_WITH_INNERSOURCE, true),
    DEFAULT_BRANCH_MONITORING(SystemConfigurationProperty.DEFAULT_BRANCH_MONITORING, true),
    DEPENDENCY_DATA_IN_API(SystemConfigurationProperty.DEPENDENCY_DATA_IN_API, true),
    INNER_SOURCE_TRANSITIVE_WAIVER(SystemConfigurationProperty.INNER_SOURCE_TRANSITIVE_WAIVER, true),
    INNER_SOURCE_REPOSITORY_INTEGRATION(SystemConfigurationProperty.INNER_SOURCE_REPOSITORY_INTEGRATION, true),
    PR_COMMENTING(SystemConfigurationProperty.PR_COMMENTING, true),
    PR_LINE_COMMENTING(SystemConfigurationProperty.PR_LINE_COMMENTING, true),
    ENABLE_UNAUTHENTICATED_PAGES(SystemConfigurationProperty.ENABLE_UNAUTHENTICATED_PAGES, true)
    {
      @Override
      public boolean isEnabled() {
        String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR);
        return valueInEnvVar == null ? super.isEnabled() : Boolean.parseBoolean(valueInEnvVar);
      }
    },
    ENABLE_SSO_ONLY(SystemConfigurationProperty.ENABLE_SSO_ONLY, false)
    {
      @Override
      public boolean isEnabled() {
        String valueInEnvVar = System.getenv().get(NXIQ_ENABLE_SSO_ONLY_ENV_VAR);
        return valueInEnvVar == null ? super.isEnabled() : Boolean.parseBoolean(valueInEnvVar);
      }
    },

    API_PAGE(SystemConfigurationProperty.API_PAGE, false),

    SCAN_POM_FILES_IN_META_INF_DIRECTORY(SystemConfigurationProperty.SCAN_POM_FILES_IN_META_INF_DIRECTORY, false),

    SCAN_NPM_DEV_AND_OPT_DEPENDENCIES(SystemConfigurationProperty.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES, false),

    /**
     * @deprecated Use {@link SourceControl#getSourceControlEvaluationsEnabled() instead}
     */
    @Deprecated
    INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS(
        SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS, true),

    /**
     * Internal feature flag to enable Firewall Onboarding.
     * It is enabled by default in IQ >= 167. Disabled by default in IQ < 167.
     */
    INTERNAL_FIREWALL_ONBOARDING_ENABLED(SystemConfigurationProperty.INTERNAL_FIREWALL_ONBOARDING_ENABLED,
        true /* propertyValue */, true /* enabledWhenAbsent */),

    /**
     * If configured a logout request will be sent to Auth0 via a browser redirect when the application is logged out
     */
    LOGOUT_AUTH0_ON_LOGOUT(SystemConfigurationProperty.LOGOUT_AUTH0_ON_LOGOUT, false, false),

    /**
     * If configured the UI will show the Sonatype managed IDP Auth0 user management pages
     */
    SSO_IDP_MANAGED_BY_SONATYPE(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE, false, false),
    INTEGRATED_ENTERPRISE_REPORTING(
        SystemConfigurationProperty.INTEGRATED_ENTERPRISE_REPORTING, false),

    SCM_UX_IMPROVEMENTS(SystemConfigurationProperty.SCM_UX_IMPROVEMENTS, true, false),

    POLICY_MANAGEMENT_AT_REPOSITORY_MANAGER_LEVEL_ENABLED(
        SystemConfigurationProperty.POLICY_MANAGEMENT_AT_REPOSITORY_MANAGER_LEVEL_ENABLED, false, false),

    ORG_APP_MANAGEMENT_WEBHOOK_EVENT(SystemConfigurationProperty.ORG_APP_MANAGEMENT_WEBHOOK_EVENT, false),

    /**
     * Self-Hosted: SCM is not feature flagged for self-hosted, so it must always return true for self-hosted.
     * SaaS: If SAAS_LIFECYCLE_SCM_ENABLED is configured it will enable the SCM resources for a SaaS tenant, disabled
     * by default.
     */
    SAAS_LIFECYCLE_SCM_ENABLED(SystemConfigurationProperty.SAAS_LIFECYCLE_SCM_ENABLED, false, false)
    {
      @Override
      public boolean isEnabled() {
        if (tenantUtil.isSingleTenant()) {
          return true;
        }
        return super.isEnabled();
      }
    };

    protected final TenantUtil tenantUtil;

    private final String propertyName;

    private final boolean propertyValue;

    private final boolean enabledWhenAbsent;

    SystemConfigurationPropertyFeature(final String propertyName, final boolean enabledWhenAbsent) {
      this(propertyName, !enabledWhenAbsent, enabledWhenAbsent);
    }

    SystemConfigurationPropertyFeature(
        final String propertyName,
        final boolean propertyValue,
        final boolean enabledWhenAbsent)
    {
      this(propertyName, propertyValue, enabledWhenAbsent, new TenantUtil());
    }

    SystemConfigurationPropertyFeature(
        final String propertyName,
        final boolean propertyValue,
        final boolean enabledWhenAbsent,
        TenantUtil tenantUtil)
    {
      this.propertyName = propertyName;
      this.propertyValue = propertyValue;
      this.enabledWhenAbsent = enabledWhenAbsent;
      this.tenantUtil = tenantUtil;
    }

    public String getPropertyName() {
      return propertyName;
    }

    public boolean getPropertyValue() {
      return propertyValue;
    }

    public boolean isEnabledWhenAbsent() {
      return enabledWhenAbsent;
    }

    public boolean isEnabled() {
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();
      SystemConfigurationProperty systemConfigurationProperty = systemConfigurationPropertyDAO.getByName(propertyName);
      return isEnabled(systemConfigurationProperty, enabledWhenAbsent);
    }

    public static boolean isEnabled(
        SystemConfigurationProperty systemConfigurationProperty,
        boolean enabledWhenAbsent)
    {
      return systemConfigurationProperty == null ? enabledWhenAbsent : !enabledWhenAbsent;
    }

    public void setEnabled(boolean enabled) {
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();

      if (isEnabled() == enabled) {
        return;
      }
      if (enabled) {
        ApiConfigFeaturesService.enableFeature(systemConfigurationPropertyDAO, tenantUtil, this);
      }
      else {
        ApiConfigFeaturesService.disableFeature(systemConfigurationPropertyDAO, tenantUtil, this);
      }
    }
  }

  @Inject
  public ApiConfigFeaturesService(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      TenantUtil tenantUtil)
  {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.tenantUtil = tenantUtil;
  }

  /**
   * isFeatureEnabled this method is useful as it allows the use of features with injection rather than using the
   * static {@link SystemConfigurationPropertyFeature#isEnabled()} method.
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

  public boolean isSaasScmAndDefaultBranchMonitoringEnabled() {
    return isSaasLifecycleScmEnabled() &&
        isFeatureEnabled(SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void enableFeature(String feature) {
    enableFeatureNoAuthz(feature);
  }

  public void enableFeatureNoAuthz(String feature) {
    throwErrorIfFeatureNoLongerSupported(feature);

    String propertyName = getPropertyNameForFeature(feature);
    throwErrorWhenSingleTenantAndPropertyNotSupported(tenantUtil, propertyName);

    enableFeature(systemConfigurationPropertyDAO, tenantUtil, getSystemConfigurationPropertyFeature(feature));
    log.debug("Enabled feature '{}'", feature);
  }

  private static void enableFeature(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      TenantUtil tenantUtil,
      SystemConfigurationPropertyFeature systemConfigurationPropertyFeature)
  {
    String featureName = systemConfigurationPropertyFeature.getPropertyName();
    boolean featureValue = systemConfigurationPropertyFeature.getPropertyValue();
    boolean enabledWhenAbsent = systemConfigurationPropertyFeature.isEnabledWhenAbsent();

    throwErrorWhenSingleTenantAndPropertyNotSupported(tenantUtil, featureName);

    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (SystemConfigurationPropertyFeature.isEnabled(systemConfiguration, enabledWhenAbsent)) {
      throw new FeatureAlreadyEnabledException("Feature is already enabled.");
    }
    if (enabledWhenAbsent) {
      AuditData.get().setData(featureName, "null");
      systemConfigurationPropertyDAO.delete(systemConfiguration);
    }
    else {
      AuditData.get().setData(featureName, String.valueOf(featureValue));
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, String.valueOf(featureValue)));
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {
    disableFeatureNoAuthz(feature);
  }

  public void disableFeatureNoAuthz(String feature) {
    throwErrorIfFeatureNoLongerSupported(feature);

    disableFeature(systemConfigurationPropertyDAO, tenantUtil, getSystemConfigurationPropertyFeature(feature));
    log.debug("Disabled feature '{}'", feature);
  }

  private static void disableFeature(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      TenantUtil tenantUtil,
      SystemConfigurationPropertyFeature systemConfigurationPropertyFeature)
  {
    String featureName = systemConfigurationPropertyFeature.getPropertyName();
    boolean featureValue = systemConfigurationPropertyFeature.getPropertyValue();
    boolean enabledWhenAbsent = systemConfigurationPropertyFeature.isEnabledWhenAbsent();

    throwErrorWhenSingleTenantAndPropertyNotSupported(tenantUtil, featureName);

    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);

    if (!SystemConfigurationPropertyFeature.isEnabled(systemConfiguration, enabledWhenAbsent)) {
      throw new FeatureAlreadyDisabledException("Feature is already disabled.");
    }
    if (!enabledWhenAbsent) {
      AuditData.get().setData(featureName, "null");
      systemConfigurationPropertyDAO.delete(systemConfiguration);
    }
    else {
      AuditData.get().setData(featureName, String.valueOf(featureValue));
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, String.valueOf(featureValue)));
    }
  }

  private void throwErrorIfFeatureNoLongerSupported(final String feature) {
    NO_LONGER_SUPPORTED_FLAGS
        .stream()
        .filter(entry -> entry.getFeatureName().equals(feature))
        .findFirst()
        .ifPresent(unsupportedFeature -> {
          if (unsupportedFeature.hasReplacementFeatureName()) {
            throw new javax.ws.rs.BadRequestException("'" + feature + "' is no longer supported. Instead you can " +
                "disable and enable the feature using '" + unsupportedFeature.getReplacementFeatureName() + "'");
          }
          else
          {
            throw new javax.ws.rs.BadRequestException("'" + feature + "' is no longer supported.");
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
