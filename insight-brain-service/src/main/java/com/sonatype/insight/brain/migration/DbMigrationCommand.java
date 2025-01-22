/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseContainerSupport;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.service.InsightConfig;

import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class DbMigrationCommand
    extends ConfiguredCommand<InsightConfig>
    implements DatabaseContainerSupport
{
  // Visible for testing
  static final long RECENT_CHECKIN_INTERVAL_MILLIS = QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS * 2;

  // Visible for testing
  static final int ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT = 1;

  public DbMigrationCommand() {
    super("migrate-db", "Migrates the database to the latest schema version.");
  }

  @Override
  public void onError(Cli cli, Namespace namespace, Throwable t) {
    // throw up to let our main() method do the desired error logging/handling
    throw new IllegalStateException("Error trying to migrate the database: " + t.getMessage(), t);
  }

  @Override
  protected void run(Bootstrap<InsightConfig> bootstrap, Namespace namespace, InsightConfig insightConfig) {
    try {
      DatabaseMigrations.setForceEnableMigration(true);

      DatabaseContainer databaseContainer = createDatabaseContainer(insightConfig);

      DatabaseProvisioner databaseProvisioner = databaseContainer.getDatabaseProvisioner();
      databaseProvisioner.initializeDatabaseWithoutMigration();

      tryCheckLastCheckinTimeNotRecent(databaseProvisioner.getOperationalDataStore(),
          getAttemptsToWaitForLastCheckinToNotBeRecent());

      databaseProvisioner.migrateDatabase();
    }
    finally {
      DatabaseMigrations.setForceEnableMigration(false);
    }
  }

  protected boolean quartzSchedulerStateTableExists(final OperationalDataStore operationalDataStore) {
    return DatabaseUtil.quartzSchedulerStateTableExists(operationalDataStore.getDataSource(),
        operationalDataStore.getDatabaseSchema());
  }

  void tryCheckLastCheckinTimeNotRecent(final OperationalDataStore operationalDataStore, int attemptsToWait) {
    if (!quartzSchedulerStateTableExists(operationalDataStore)) {
      return;
    }
    int attemptedToWait = 0;
    while (true) {
      Long lastCheckinTime = DatabaseUtil.getLastCheckinTime(operationalDataStore.getDataSource(),
          operationalDataStore.getDatabaseSchema());
      if (lastCheckinTime == null) {
        return;
      }
      long nowMinusRecentCheckinInterval = getCurrentTimeMillis() - RECENT_CHECKIN_INTERVAL_MILLIS;
      if (lastCheckinTime < nowMinusRecentCheckinInterval) {
        return;
      }
      if (attemptedToWait >= attemptsToWait) {
        break;
      }
      trySleep(lastCheckinTime - nowMinusRecentCheckinInterval + 1);
      attemptedToWait++;
    }
    throw new IllegalStateException(String.format(
        "Cannot migrate the IQ Server database if it is in use by one or more IQ Server instances. " +
            "Aborting since an IQ Server instance actively used the database within the past %s seconds.",
        RECENT_CHECKIN_INTERVAL_MILLIS / 1000));
  }

  // Visible for testing
  int getAttemptsToWaitForLastCheckinToNotBeRecent() {
    return ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT;
  }

  // Visible for testing
  long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  // Visible for testing
  void trySleep(long time) {
    try {
      Thread.sleep(time);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
    return new DefaultDatabaseContainer(insightConfig);
  }
}
