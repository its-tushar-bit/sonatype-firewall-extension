/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApiConfigFeaturesService
{
  private static final Logger log = LoggerFactory.getLogger(ApiConfigFeaturesService.class);

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  static final String FEATURE_DASHBOARD = "dashboard";

  static final String FEATURE_REPORTS_LIST = "reportsList";

  @Inject
  public ApiConfigFeaturesService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void enableFeature(String feature) {
    String featureName = getPropertyNameForFeature(feature);

    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (systemConfiguration == null) {
      throw new BadRequestException("Feature is already enabled.");
    }

    systemConfigurationPropertyDAO.delete(systemConfiguration);
    log.debug("Enabled feature '{}'", feature);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {
    String featureName = getPropertyNameForFeature(feature);

    SystemConfigurationProperty systemConfiguration = systemConfigurationPropertyDAO.getByName(featureName);
    if (systemConfiguration != null) {
      throw new BadRequestException("Feature is already disabled.");
    }

    systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(featureName, "true"));
    log.debug("Disabled feature '{}'", feature);
  }

  String getPropertyNameForFeature(String feature) {
    switch (feature) {
      case FEATURE_DASHBOARD:
        return SystemConfigurationProperty.DASHBOARD_DISABLED;
      case FEATURE_REPORTS_LIST:
        return SystemConfigurationProperty.REPORTS_LIST_DISABLED;
      default:
        throw new BadRequestException("Feature not supported: " + feature);
    }
  }
}
