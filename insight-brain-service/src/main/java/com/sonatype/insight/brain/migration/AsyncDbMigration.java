/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for asynchronous database migrations run after the server has started.
 * All classes extending this class will be run by the {@link AsyncDbMigrationScheduler} in order of their priority
 * determined by the {@link AsyncDbMigration#migrationPriority()} method.
 * -
 * If there is a lot of churn on a table (additions and deletions) then this might not be a suitable approach.
 * The marker to prevent future migrations only occurs if the processed count is equal to the table count at the end
 * of the migration, this is an attempt to prevent issues with incomplete migrations, but it isn't guaranteed.
 */
public abstract class AsyncDbMigration<T extends HasStringId>
    implements Comparable<AsyncDbMigration<T>>
{
  private static final int DEFAULT_POSTGRES_PAGE_SIZE = 30_000;

  private static final int DEFAULT_H2_PAGE_SIZE = 100;

  private static final Comparator<AsyncDbMigration<? extends HasStringId>> COMPARATOR = Comparator
      .comparingInt((AsyncDbMigration<? extends HasStringId> a) -> a.migrationPriority())
      .thenComparing(AsyncDbMigration::getMigrationName);

  private final Logger log = LoggerFactory.getLogger(getClass());

  protected final AbstractSqlDAO<T> dao;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final String type;

  private final int pageSize;

  protected AsyncDbMigration(
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final InsightConfig config)
  {
    this(dao, migrationTrackerDAO, type, getDefaultPageSize(config));
  }

  protected AsyncDbMigration(
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final int pageSize)
  {
    this.dao = dao;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.type = type;
    this.pageSize = pageSize;
  }

  protected abstract void migrate(final AbstractSqlDAO<T> dao, final T entity, final TransactionContext tx);

  protected void onCompletion() {
    // no-op - hook for subclasses to run on completion if needed
  }

  /**
   * The name of the migration. This is used to prevent the migration from running again.
   */
  public String getMigrationName() {
    return getClass().getSimpleName();
  }

  /**
   * Priority of the migration. The lower the number the higher the priority.
   * - The default is {@link Integer#MAX_VALUE} which is the lowest possible priority.
   */
  public int migrationPriority() {
    return Integer.MAX_VALUE;
  }

  public void runMigration() {
    if (shouldMigrate()) {

      onStart();

      long processed = loopAndMigrateEntities();

      validateAndMarkMigrationFinished(processed);

      onCompletion();
    }
    else {
      log.debug("Migration of {} has already been completed", type);
    }
  }

  private boolean shouldMigrate() {
    return !migrationTrackerDAO.isTrackerPresent(getMigrationName());
  }

  protected void onStart() {
    log.info("Starting migration of {}\n" +
        "The server will continue to be operational and fully functional during this optimization", type);
  }

  private long loopAndMigrateEntities() {
    long count = dao.getCount();
    long processed = 0;
    String lastProcessedId = "";
    long batchStartTime = System.currentTimeMillis();
    long batchFinishTime = System.currentTimeMillis();

    while (processed < count) {

      String processedPercent = String.format("%.0f", (double) processed / count * 100);
      long lastProcessTime = batchFinishTime - batchStartTime;
      log.debug("{}% : Processed {} {} of {}. Previous page migration time = {} ms",
          processedPercent, processed, type, count, lastProcessTime);

      batchStartTime = System.currentTimeMillis();
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();

        List<T> entities = dao.getPage(tx, lastProcessedId, pageSize);

        if (entities.isEmpty()) {
          log.debug("No more entities to process for {} migration", type);

          break;
        }

        for (T entity : entities) {
          migrate(dao, entity, tx);
          lastProcessedId = entity.getId();
          processed++;
        }

        // If we are on the last page then we check the count to make sure we have processed all entities
        if (entities.size() < pageSize) {
          count = dao.getCount(tx);
        }

        tx.commit();
      }

      batchFinishTime = System.currentTimeMillis();
    }
    return processed;
  }

  protected void validateAndMarkMigrationFinished(final long processed) {
    long rows = dao.getCount();

    if (validateFinished(dao, processed, rows)) {
      log.info("Migration of {} completed. Adding Migration Tracker \"{}\" entry to prevent running again.",
          type, getMigrationName());
      migrationTrackerDAO.insertTracker(getMigrationName());
    }
    else {
      log.error("Migration of {} failed. Expected {} rows but only processed {}", type, rows, processed);
    }
  }

  protected boolean validateFinished(final AbstractSqlDAO<T> dao, final long processed, final long rows) {
    return processed == rows;
  }

  protected static int getDefaultPageSize(final InsightConfig config) {
    return config.isDatabaseEmbedded() ? DEFAULT_H2_PAGE_SIZE : DEFAULT_POSTGRES_PAGE_SIZE;
  }

  @Override
  public int compareTo(final AsyncDbMigration<T> o) {
    return COMPARATOR.compare(this, o);
  }
}
