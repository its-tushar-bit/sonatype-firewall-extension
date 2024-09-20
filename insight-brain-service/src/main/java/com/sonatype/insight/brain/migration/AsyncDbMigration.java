/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for asynchronous database migrations after the server has started.
 * -
 * If there is a lot of churn on the table (additions and deletions) then this might not be a suitable approach. The
 * marker to prevent future migrations only occurs if the processed count is equal to the table count at the end of the
 * migration to attempt to prevent it being an issue, but it isn't guaranteed.
 */
public abstract class AsyncDbMigration<T extends HasStringId>
    extends Task
    implements InsightJob
{
  private static final int DEFAULT_POSTGRES_PAGE_SIZE = 30_000;

  private static final int DEFAULT_H2_PAGE_SIZE = 100;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final TaskScheduler taskScheduler;

  protected final AbstractSqlDAO<T> dao;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final String type;

  private final ClusterLockManager clusterLockManager;

  private final int pageSize;

  protected AsyncDbMigration(
      final String name,
      final TaskScheduler taskScheduler,
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final InsightConfig config,
      final ClusterLockManager clusterLockManager)
  {
    this(name, taskScheduler, dao, migrationTrackerDAO, type, clusterLockManager, getDefaultPageSize(config));
  }

  protected AsyncDbMigration(
      final String name,
      final TaskScheduler taskScheduler,
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final ClusterLockManager clusterLockManager,
      final int pageSize)
  {
    super(name);
    this.taskScheduler = taskScheduler;
    this.dao = dao;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.type = type;
    this.clusterLockManager = clusterLockManager;
    this.pageSize = pageSize;
  }

  @Override
  public void register() {
    taskScheduler.scheduleOneTimeTask(this);
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run {}", getJobName());

    checkAndRunMigration();
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run {}", getJobName());

    checkAndRunMigration();

    printWriter.write("Completed manual " + getJobName() + "\n");
  }

  private void checkAndRunMigration() {
    if (shouldMigrate()) {
      boolean lockAcquired = false;
      try (ClusterLock clusterLock = clusterLockManager.createForAsyncDbMigration(getJobName())) {
        lockAcquired = clusterLock.tryLock();
        if (lockAcquired) {
          migrate();
        }
      }
      finally {
        if (lockAcquired) {
          /*
           * Deleting the lock on H2 first waits for the lock to be released. If this isn't the code that acquired the
           * lock then it shouldn't be attempting to delete it otherwise the deletion hangs.
           */
          clusterLockManager.deleteForAsyncDbMigration(getJobName());
        }
      }
    }
    else {
      log.debug("Migration of {} has already been completed", type);
    }
  }

  private void migrate() {
    onStart();

    long processed = loopAndMigrateEntities();

    validateAndMarkMigrationFinished(processed);

    onCompletion();
  }

  private boolean shouldMigrate() {
    return !migrationTrackerDAO.isTrackerPresent(getJobName());
  }

  protected void onStart() {
    log.info("Starting migration of {}", type);
    log.info("The server will continue to be operational and fully functional during this optimization");
  }

  private long loopAndMigrateEntities() {
    long count = dao.getCount();
    long processed = 0;
    String lastProcessedId = "";
    long batchStartTime = System.currentTimeMillis();
    long batchFinishTime = System.currentTimeMillis();

    while (processed < count) {

      String processedPercent = String.format("%.0f", (double) processed / count * 100);
      double lastProcessTime = batchFinishTime - batchStartTime;
      log.info("{}% : Processed {} {} of {}. Previous page migration time = {} ms",
          processedPercent, processed, type, count, lastProcessTime);

      batchStartTime = System.currentTimeMillis();
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();

        List<T> entities = dao.getPage(tx, lastProcessedId, pageSize);

        if (entities.isEmpty()) {
          log.info("No more entities to process for {} migration", type);

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

        tempDebug(entities);

        tx.commit();
      }

      batchFinishTime = System.currentTimeMillis();
    }
    return processed;
  }

  protected abstract void tempDebug(final List<T> entities);

  protected void validateAndMarkMigrationFinished(final long processed) {
    long rows = dao.getCount();

    if (validateFinished(dao, processed, rows)) {
      log.info("Migration of {} completed. Adding Migration Tracker entry to prevent running again.", type);
      migrationTrackerDAO.insertTracker(getJobName());
    }
    else {
      log.error("Migration of {} failed. Expected {} rows but only processed {}", type, rows, processed);
    }
  }

  protected boolean validateFinished(final AbstractSqlDAO<T> dao, final long processed, final long rows) {
    return processed == rows;
  }

  protected void onCompletion() {

  }

  protected abstract void migrate(final AbstractSqlDAO<T> dao, final T entity, final TransactionContext tx);

  protected static int getDefaultPageSize(final InsightConfig config) {
    return config.isDatabaseEmbedded() ? DEFAULT_H2_PAGE_SIZE : DEFAULT_POSTGRES_PAGE_SIZE;
  }
}
