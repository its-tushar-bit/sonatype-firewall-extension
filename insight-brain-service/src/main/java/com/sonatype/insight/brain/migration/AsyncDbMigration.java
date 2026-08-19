/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

/**
 * Base class for asynchronous database migrations that iterate over entities in a table.
 * All classes extending this class will be run by the {@link AsyncDbMigrationScheduler} in order of their priority
 * determined by the {@link AbstractAsyncDbMigration#migrationPriority()} method.
 * -
 * If there is a lot of churn on a table (additions and deletions) then this might not be a suitable approach.
 * The marker to prevent future migrations only occurs if the processed count is equal to the table count at the end
 * of the migration, this is an attempt to prevent issues with incomplete migrations, but it isn't guaranteed.
 */
public abstract class AsyncDbMigration<T extends HasStringId>
    extends AbstractAsyncDbMigration
{
  private static final int DEFAULT_POSTGRES_PAGE_SIZE = 30_000;

  private static final int DEFAULT_H2_PAGE_SIZE = 100;

  protected final AbstractSqlDAO<T> dao;

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
    super(migrationTrackerDAO, type);
    this.dao = dao;
    this.pageSize = pageSize;
  }

  protected abstract void migrate(final AbstractSqlDAO<T> dao, final T entity, final TransactionContext tx);

  @Override
  protected boolean executeMigration() {
    long processed = loopAndMigrateEntities();
    long rows = dao.getCount();
    boolean finished = validateFinished(processed, rows);
    if (!finished) {
      log.error("Migration of {} failed. Expected {} rows but only processed {}", getType(), rows, processed);
    }
    return finished;
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
          processedPercent, processed, getType(), count, lastProcessTime);

      batchStartTime = System.currentTimeMillis();
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();

        List<T> entities = dao.getPage(tx, lastProcessedId, pageSize);

        if (entities.isEmpty()) {
          log.debug("No more entities to process for {} migration", getType());

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

  protected boolean validateFinished(final long processed, final long rows) {
    return processed == rows;
  }

  protected static int getDefaultPageSize(final InsightConfig config) {
    return config.isDatabaseEmbedded() ? DEFAULT_H2_PAGE_SIZE : DEFAULT_POSTGRES_PAGE_SIZE;
  }
}
