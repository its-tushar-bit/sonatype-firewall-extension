/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SuccessMetricsService
{
  private static final Logger log = LoggerFactory.getLogger(SuccessMetricsService.class);

  public static final String PROPERTY_ENABLED = "SUCCESS_METRICS_ENABLED";

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public SuccessMetricsService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SuccessMetricsConfigurationDTO get() {
    SystemConfigurationProperty property = systemConfigurationPropertyDAO.getByNameNotNull(PROPERTY_ENABLED);
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    configuration.enabled = Boolean.parseBoolean(property.getValue());
    return configuration;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SuccessMetricsConfigurationDTO update(SuccessMetricsConfigurationDTO configuration) {
    log.debug("{} success metrics", configuration.enabled ? "Enabling" : "Disabling");
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(PROPERTY_ENABLED, Boolean.toString(configuration.enabled)));
    AuditData.get().setData("successMetricsFeature", configuration.enabled ? "enabled" : "disabled");
    return configuration;
  }
}
