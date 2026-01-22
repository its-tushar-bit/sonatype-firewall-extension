/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.138
 */
@Named
public class BaseUrlConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(BaseUrlConfigurationMigrator.class);

  // Visible for testing
  static final String OBSOLETE_CONFIG_MESSAGE = "Base URL is now configured using the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  // Visible for testing
  static final String MIGRATION_ID = "base-url-config";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ApiConfigurationService configurationService;

  private final InsightConfig insightConfig;

  @Inject
  public BaseUrlConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ApiConfigurationService configurationService,
      InsightConfig insightConfig)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.configurationService = configurationService;
    this.insightConfig = insightConfig;
  }

  void migrate() {
    String baseUrl = insightConfig.getBaseUrl();
    Boolean forceBaseUrl = insightConfig.isForceBaseUrl();
    boolean hasCustomSetting = baseUrl != null || forceBaseUrl != null;
    if (hasCustomSetting) {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Base URL configuration already migrated.");
      return;
    }

    log.debug("Migrating base URL configuration to the database...");

    Map<String, Object> properties = new HashMap<>();
    try (TransactionContext tx = migrationTrackerDAO.createTransactionContext()) {
      tx.begin();
      if (hasCustomSetting) {
        properties.put(SystemConfigurationProperty.BASE_URL, baseUrl);
        properties.put(SystemConfigurationProperty.FORCE_BASE_URL, forceBaseUrl);
        try {
          configurationService.setConfigurationInDatabaseNoAuthz(tx, properties);
        }
        catch (BadRequestException e) {
          log.warn("The current base URL configuration is invalid and cannot be migrated.", e);
          properties.clear();
        }
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }
    if (!properties.isEmpty()) {
      configurationService.applyConfigurationToClients(properties.keySet());
    }
    log.info("Migrated base URL configuration to the database.");
  }
}
