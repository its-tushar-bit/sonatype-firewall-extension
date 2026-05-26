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
import com.sonatype.insight.brain.spring.InsightBrainCompatibilityCommand;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class DbMigrationCommand
    implements InsightBrainCompatibilityCommand, DatabaseContainerSupport
{
  public static final String NAME = "migrate-db";

  public static final String DESCRIPTION = "Migrates the database to the latest schema version.";

  // Visible for testing
  static final long RECENT_CHECKIN_INTERVAL_MILLIS = QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS * 2;

  // Visible for testing
  static final int ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT = 1;

  private final InsightConfig insightConfig;

  DbMigrationCommand() {
    this(new InsightConfig());
  }

  @Inject
  public DbMigrationCommand(InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public void run(String... args) {
    run(insightConfig);
  }

  void run(Object ignoredBootstrap, Object ignoredNamespace, InsightConfig runtimeConfig) {
    run(runtimeConfig);
  }

  public void onError(Object ignoredCli, Object ignoredNamespace, Throwable t) {
    throw new IllegalStateException("Error trying to migrate the database: " + t.getMessage(), t);
  }

  public void run(InsightConfig runtimeConfig) {
    try {
      DatabaseMigrations.setForceEnableMigration(true);

      DatabaseContainer databaseContainer = createDatabaseContainer(runtimeConfig);
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
        "Cannot migrate the IQ Server database if it is in use by one or more IQ Server instances. "
            + "Aborting since an IQ Server instance actively used the database within the past %s seconds.",
        RECENT_CHECKIN_INTERVAL_MILLIS / 1000));
  }

  int getAttemptsToWaitForLastCheckinToNotBeRecent() {
    return ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT;
  }

  long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  void trySleep(long time) {
    try {
      Thread.sleep(time);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public DatabaseContainer createDatabaseContainer(final InsightConfig runtimeConfig) {
    return new DefaultDatabaseContainer(runtimeConfig);
  }
}
