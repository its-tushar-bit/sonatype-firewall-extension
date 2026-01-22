/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.backup;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import io.dropwizard.servlets.tasks.Task;

/**
 * Creates a hot backup of the ODS database.
 *
 * To trigger a backup using curl:
 * curl -X POST http://localhost:8071/tasks/backupDb
 *
 * @since 1.15.0
 */
@Named
public class DbBackupTask
    extends Task
{
  public static final String PATH = "backupDb";

  public static final String RESPONSE_MESSAGE_PREFIX = "Created db backup: ";

  private final InsightConfig config;

  private final Configuration configuration;

  private final OperationalDataStore operationalDataStore;

  @Inject
  public DbBackupTask(
      final InsightConfig config,
      final Configuration configuration,
      final OperationalDataStore operationalDataStore)
  {
    super(PATH);
    this.config = config;
    this.configuration = configuration;
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    if (!config.isDatabaseEmbedded()) {
      throw new BadRequestException("The DB backup task is supported only for h2 databases.");
    }

    File dbBackupDir = getDbBackupDir();
    H2DatabaseBackup h2DatabaseBackup = new H2DatabaseBackup();
    h2DatabaseBackup.backup(operationalDataStore.getDatabaseConfig(),
        operationalDataStore.getDataSource(), dbBackupDir);

    output.write(RESPONSE_MESSAGE_PREFIX + dbBackupDir.getAbsolutePath());
  }

  private File getDbBackupDir() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
    String newBackupDir = "backup-" + dateFormat.format(new Date());
    return new File(configuration.getDbBackupDir(), newBackupDir);
  }
}
