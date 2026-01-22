/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates proprietary.json to the db
 *
 * @since 1.22
 */
@Named
public class ProprietaryConfigMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryConfigMigrator.class);

  static final String PROPRIETARY_CONFIG_FILENAME = "proprietary.json";

  static final String MIGRATION_ID = "proprietary-config";

  private final InsightWork work;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  private final MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  public ProprietaryConfigMigrator(
      InsightWork work,
      ProprietaryConfigDAO proprietaryConfigDAO,
      MigrationTrackerDAO migrationTrackerDAO)
  {
    this.work = work;
    this.proprietaryConfigDAO = proprietaryConfigDAO;
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  void migrate() {
    long start = System.currentTimeMillis();
    log.debug("Migrating proprietary config data...");

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.info("Proprietary config already migrated.");
      return;
    }

    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = getObsoleteProprietaryConfig();

    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(obsoleteConfig.getPackages());
    proprietaryConfig.setRegexes(obsoleteConfig.getRegexes());
    proprietaryConfig.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    try (TransactionContext tx = proprietaryConfigDAO.createTransactionContext()) {
      tx.begin();
      if (!obsoleteConfig.getRegexes().isEmpty() || !obsoleteConfig.getPackages().isEmpty()) {
        proprietaryConfigDAO.insert(tx, proprietaryConfig);
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated proprietary config data in {} ms.", System.currentTimeMillis() - start);
  }

  private com.sonatype.clm.dto.model.ProprietaryConfig getObsoleteProprietaryConfig() {
    try {
      final JsonNode config = readObsoleteProprietaryConfig();
      return (config != null) ? JsonUtils.asPojo(config, com.sonatype.clm.dto.model.ProprietaryConfig.class)
          : new com.sonatype.clm.dto.model.ProprietaryConfig();
    }
    catch (IOException e) {
      log.error("Failed to load proprietary component configuration", e);
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode readObsoleteProprietaryConfig() throws IOException {
    File obsoleteProprietaryConfigFile = new File(work.getDataDir(), PROPRIETARY_CONFIG_FILENAME);
    if (obsoleteProprietaryConfigFile.exists()) {
      JsonNode data = JsonUtils.read(obsoleteProprietaryConfigFile).get(0);
      if (data != null && data.has("data")) {
        data = data.get("data");
      }
      return data;
    }
    return null;
  }
}
