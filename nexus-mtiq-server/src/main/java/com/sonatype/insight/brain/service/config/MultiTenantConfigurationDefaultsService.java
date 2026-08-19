/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.config;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES;

/**
 * Sets meaningful configuration defaults for MTIQ. Ideally this would be done using a MTIQ specific database migration
 * script. This class serves as a temporary measure until we've decided on a long-term database migration solution for
 * MTIQ.
 */
@Named
@Singleton
public class MultiTenantConfigurationDefaultsService
    implements TenantManaged, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantConfigurationDefaultsService.class);

  private static final Map<String, String> CONFIG_DEFAULTS = ImmutableMap.of(
      AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, "120");

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public MultiTenantConfigurationDefaultsService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Override
  public void register() {
    setMtiqGlobalConfigurationDefaults();
  }

  private void setMtiqGlobalConfigurationDefaults() {
    log.info("Setting MTIQ global configuration defaults: {}", CONFIG_DEFAULTS);

    for (Map.Entry<String, String> entry : CONFIG_DEFAULTS.entrySet()) {
      systemConfigurationPropertyDAO.set(entry.getKey(), entry.getValue());
    }
  }
}
