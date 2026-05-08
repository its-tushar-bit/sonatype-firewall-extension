/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.Comparator;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAsyncDbMigration
    implements Comparable<AbstractAsyncDbMigration>
{
  private static final Comparator<AbstractAsyncDbMigration> COMPARATOR = Comparator
      .comparingInt(AbstractAsyncDbMigration::migrationPriority)
      .thenComparing(AbstractAsyncDbMigration::getMigrationName);

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final MigrationTrackerDAO migrationTrackerDAO;

  private final String type;

  protected AbstractAsyncDbMigration(
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.type = type;
  }

  protected String getType() {
    return type;
  }

  protected abstract boolean executeMigration();

  /** Called after {@link #executeMigration()}, regardless of whether it succeeded or failed. */
  protected void onCompletion() {
  }

  public String getMigrationName() {
    return getClass().getSimpleName();
  }

  public int migrationPriority() {
    return Integer.MAX_VALUE;
  }

  public void runMigration() {
    if (migrationTrackerDAO.isTrackerPresent(getMigrationName())) {
      log.debug("Migration of {} has already been completed", type);
      return;
    }

    log.info("Starting migration of {}\n" +
        "The server will continue to be operational and fully functional during this optimization", type);
    onStart();

    boolean completed = executeMigration();

    if (completed) {
      log.info("Migration of {} completed. Adding Migration Tracker \"{}\" entry to prevent running again.",
          type, getMigrationName());
      migrationTrackerDAO.insertTracker(getMigrationName());
    }
    else {
      log.error("Migration of {} did not complete. Will retry on next startup.", type);
    }

    onCompletion();
  }

  protected void onStart() {
  }

  @Override
  public int compareTo(final AbstractAsyncDbMigration o) {
    return COMPARATOR.compare(this, o);
  }
}
