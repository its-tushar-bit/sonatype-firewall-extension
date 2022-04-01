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
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
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

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  public enum SystemConfigurationPropertyFeature
      implements Feature
  {
    DASHBOARD_CAN_BE_ENABLED(SystemConfigurationProperty.DASHBOARD_DISABLED, true),
    REPORTS_LIST_CAN_BE_ENABLED(SystemConfigurationProperty.REPORTS_LIST_DISABLED, true),
    VULNERABILITY_SOURCE(
        SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED, true);

    private final String propertyName;

    private final boolean enabledWhenAbsent;

    SystemConfigurationPropertyFeature(final String propertyName, final boolean enabledWhenAbsent) {
      this.propertyName = propertyName;
      this.enabledWhenAbsent = enabledWhenAbsent;
    }

    public String getPropertyName() {
      return propertyName;
    }

    public boolean isEnabledWhenAbsent() {
      return enabledWhenAbsent;
    }

    public boolean isEnabled(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
      SystemConfigurationProperty systemConfigurationProperty = systemConfigurationPropertyDAO.getByName(propertyName);
      return ApiConfigFeaturesService.isEnabled(systemConfigurationProperty, enabledWhenAbsent);
    }
  }

  @Inject
  public ApiConfigFeaturesService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void enableFeature(String feature) {
    enableFeature(getSystemConfigurationPropertyFeature(feature));
    log.debug("Enabled feature '{}'", feature);
  }

  private void enableFeature(SystemConfigurationPropertyFeature systemConfigurationPropertyFeature) {
    enableFeature(systemConfigurationPropertyFeature.getPropertyName(),
        systemConfigurationPropertyFeature.isEnabledWhenAbsent());
  }

  private void enableFeature(String featureName, boolean enabledWhenAbsent) {
    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (isEnabled(systemConfiguration, enabledWhenAbsent)) {
      throw new BadRequestException("Feature is already enabled.");
    }
    if (enabledWhenAbsent) {
      systemConfigurationPropertyDAO.delete(systemConfiguration);
    }
    else {
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, "true"));
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {
    disableFeature(getSystemConfigurationPropertyFeature(feature));
    log.debug("Disabled feature '{}'", feature);
  }

  private void disableFeature(SystemConfigurationPropertyFeature systemConfigurationPropertyFeature) {
    disableFeature(systemConfigurationPropertyFeature.getPropertyName(),
        systemConfigurationPropertyFeature.isEnabledWhenAbsent());
  }

  private void disableFeature(String featureName, boolean enabledWhenAbsent) {
    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (!isEnabled(systemConfiguration, enabledWhenAbsent)) {
      throw new BadRequestException("Feature is already disabled.");
    }
    if (!enabledWhenAbsent) {
      systemConfigurationPropertyDAO.delete(systemConfiguration);
    }
    else {
      systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, "true"));
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
        .filter(s -> s.getPropertyName().equalsIgnoreCase(propertyName))
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
      default:
        return feature;
    }
  }
}
