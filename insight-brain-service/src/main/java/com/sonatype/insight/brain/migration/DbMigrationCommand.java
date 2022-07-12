/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class DbMigrationCommand
    extends ConfiguredCommand<InsightConfig>
{
  // Visible for testing
  static final long RECENT_CHECKIN_INTERVAL_MILLIS = (long) (QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS * 3.5);

  // Visible for testing
  static final int ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT = 1;

  public DbMigrationCommand() {
    super("migrate-db", "Migrates the database to the latest schema version.");
  }

  @Override
  protected void run(Bootstrap<InsightConfig> bootstrap, Namespace namespace, InsightConfig insightConfig) {
    DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig);

    tryCheckLastCheckinTimeNotRecent(getAttemptsToWaitForLastCheckinToNotBeRecent());

    DatabaseProvisionUtils.migrateDatabases(insightConfig);
  }

  void tryCheckLastCheckinTimeNotRecent(int attemptsToWait) {
    if (!DatabaseUtil.quartzSchedulerStateTableExists(OperationalDataStoreProvider.getDataSource())) {
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
}
