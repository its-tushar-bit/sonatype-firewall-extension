/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.features.FeaturesResource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.Authorize;
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

  static final String FEATURE_TRANSITIVE_SOLVER = "transitiveSolverDisable";

  static final String FEATURE_CODE_INSIGHTS = "codeInsights";

  static final String FEATURE_COMPONENT_SEARCH_API_WITH_INNERSOURCE = "componentSearchApiWithInnerSource";

  static final String FEATURE_DEFAULT_BRANCH_MONITORING = "defaultBranchMonitoring";

  static final String FEATURE_DEPENDENCY_DATA_IN_API = "dependencyDataInApi";

  static final String FEATURE_INNER_SOURCE_TRANSITIVE_WAIVER = "innerSourceTransitiveWaiver";

  static final String FEATURE_INNER_SOURCE_REPOSITORY_INTEGRATION = "innerSourceRepositoryIntegration";

  static final String FEATURE_PR_COMMENTING = "prCommenting";

  static final String FEATURE_PR_LINE_COMMENTING = "prLineCommenting";

  static final String FEATURE_ENABLE_UNAUTHENTICATED_PAGES = "enableUnauthenticatedPages";

  static final String FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS = "internalSourceControlPolicyEvaluations";

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  public static final String NXIQ_ENABLE_UNAUTHENTICATED_PAGES_ENV_VAR = "NXIQ_ENABLE_UNAUTHENTICATED_PAGES";

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
    TRANSITIVE_SOLVER(SystemConfigurationProperty.TRANSITIVE_SOLVER_DISABLED, false),
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
    API_PAGE(SystemConfigurationProperty.API_PAGE, false),

    /**
     * @deprecated Use {@link SourceControl#getSourceControlEvaluationsEnabled() instead}
     */
    @Deprecated
    INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS(
        SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS, true);

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
      this.propertyName = propertyName;
      this.propertyValue = propertyValue;
      this.enabledWhenAbsent = enabledWhenAbsent;
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
      return ApiConfigFeaturesService.isEnabled(systemConfigurationProperty, enabledWhenAbsent);
    }

    public void setEnabled(boolean enabled) {
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();
      if (isEnabled() == enabled) {
        return;
      }
      if (enabled) {
        ApiConfigFeaturesService.enableFeature(systemConfigurationPropertyDAO, this);
      }
      else {
        ApiConfigFeaturesService.disableFeature(systemConfigurationPropertyDAO, this);
      }
    }
  }

  @Inject
  public ApiConfigFeaturesService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void enableFeature(String feature) {
    enableFeatureNoAuthz(feature);
  }

  public void enableFeatureNoAuthz(String feature) {
    enableFeature(systemConfigurationPropertyDAO, getSystemConfigurationPropertyFeature(feature));
    log.debug("Enabled feature '{}'", feature);
  }

  private static void enableFeature(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      SystemConfigurationPropertyFeature systemConfigurationPropertyFeature)
  {
    enableFeature(systemConfigurationPropertyDAO, systemConfigurationPropertyFeature.getPropertyName(),
        systemConfigurationPropertyFeature.getPropertyValue(),
        systemConfigurationPropertyFeature.isEnabledWhenAbsent());
  }

  private static void enableFeature(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      String featureName,
      boolean featureValue,
      boolean enabledWhenAbsent)
  {
    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (isEnabled(systemConfiguration, enabledWhenAbsent)) {
      throw new FeatureAlreadyEnabledException("Feature is already enabled.");
    }
    if (enabledWhenAbsent) {
      systemConfigurationPropertyDAO.delete(systemConfiguration);
    }
    else {
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, String.valueOf(featureValue)));
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {
    disableFeatureNoAuthz(feature);
  }

  public void disableFeatureNoAuthz(String feature) {
    disableFeature(systemConfigurationPropertyDAO, getSystemConfigurationPropertyFeature(feature));
    log.debug("Disabled feature '{}'", feature);
  }

  private static void disableFeature(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      SystemConfigurationPropertyFeature systemConfigurationPropertyFeature)
  {
    disableFeature(systemConfigurationPropertyDAO, systemConfigurationPropertyFeature.getPropertyName(),
        systemConfigurationPropertyFeature.getPropertyValue(),
        systemConfigurationPropertyFeature.isEnabledWhenAbsent());
  }

  private static void disableFeature(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      String featureName,
      boolean featureValue,
      boolean enabledWhenAbsent)
  {
    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (!isEnabled(systemConfiguration, enabledWhenAbsent)) {
      throw new FeatureAlreadyDisabledException("Feature is already disabled.");
    }
    if (!enabledWhenAbsent) {
      systemConfigurationPropertyDAO.delete(systemConfiguration);
    }
    else {
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, String.valueOf(featureValue)));
    }
  }

  private static boolean isEnabled(SystemConfigurationProperty systemConfigurationProperty, boolean enabledWhenAbsent) {
    if (systemConfigurationProperty == null) {
      return enabledWhenAbsent;
    }
    return !enabledWhenAbsent;
  }

  // Visible for testing
  SystemConfigurationPropertyFeature getSystemConfigurationPropertyFeature(String feature) {
    String propertyName = getPropertyNameForFeature(feature);
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
        return SystemConfigurationProperty.TRANSITIVE_SOLVER_DISABLED;
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
      case FEATURE_INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS:
        return SystemConfigurationProperty.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS;
      default:
        return feature;
    }
  }
}
