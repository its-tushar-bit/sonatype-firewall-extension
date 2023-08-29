/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseContainerSupport;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import io.dropwizard.cli.Cli;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
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
      DatabaseMigrator.setForceEnableMigration(true);

      // TODO MTIQ - soon InsightConfig will be a parameter to create the DatabaseContainer
      DatabaseContainer databaseContainer = createDatabaseContainer();

      DatabaseProvisionUtils databaseProvisionUtils = databaseContainer.getDatabaseProvisionUtils();
      databaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig);

      tryCheckLastCheckinTimeNotRecent(getAttemptsToWaitForLastCheckinToNotBeRecent());

      databaseProvisionUtils.migrateDatabasesIfNeeded(insightConfig);
    }
    finally {
      DatabaseMigrator.setForceEnableMigration(false);
    }
  }

  protected boolean quartzSchedulerStateTableExists() {
    return DatabaseUtil.quartzSchedulerStateTableExists(OperationalDataStoreProvider.getDataSource());
  }

  void tryCheckLastCheckinTimeNotRecent(int attemptsToWait) {
    if (!quartzSchedulerStateTableExists()) {
      return;
    }
    int attemptedToWait = 0;
    while (true) {
      Long lastCheckinTime = DatabaseUtil.getLastCheckinTime(OperationalDataStoreProvider.getDataSource());
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
  public DatabaseContainer createDatabaseContainer() {
    return new DatabaseContainer();
  }
}
