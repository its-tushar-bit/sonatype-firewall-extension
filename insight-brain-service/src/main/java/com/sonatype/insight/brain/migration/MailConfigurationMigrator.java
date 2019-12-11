/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MailConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since MIGRATE_MAIL_CONFIG
 */
@Named
public class MailConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(MailConfigurationMigrator.class);

  static final String MIGRATION_ID = "mail-config";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final MailConfigurationDAO mailConfigurationDAO;

  private final InsightConfig insightConfig;

  @Inject
  public MailConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      MailConfigurationDAO mailConfigurationDAO,
      InsightConfig insightConfig)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.insightConfig = insightConfig;
  }

  void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Mail configuration already migrated.");
      return;
    }

    log.debug("Migrating mail configuration to database...");
    try (TransactionContext tx = mailConfigurationDAO.createTransactionContext()) {
      tx.begin();
      MailConfig fileConfig = insightConfig.getMailConfig();
      if (fileConfig == null) {
        fileConfig = new MailConfig();
      }
      MailConfiguration dbConfig = new MailConfiguration();
      dbConfig.setHostname(fileConfig.getHostname());
      dbConfig.setPort(fileConfig.getPort());
      dbConfig.setUsername(fileConfig.getUsername());
      dbConfig.setPassword(fileConfig.getPassword());
      dbConfig.setSslEnabled(fileConfig.isSsl());
      dbConfig.setStartTlsEnabled(fileConfig.isTls());
      dbConfig.setSystemEmail(fileConfig.getSystemEmail());
      mailConfigurationDAO.insert(tx, dbConfig);
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated mail configuration to database.");
  }
}
