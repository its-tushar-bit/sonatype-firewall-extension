/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.MigrationTracker;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

/**
 * Deletes all inactive repository policy violations.
 *
 * @since 1.90
 */
@Named
@Singleton
public class InactiveRepositoryViolationCleaner
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(InactiveRepositoryViolationCleaner.class);

  // Visible for tests
  static final int BATCH_SIZE = 100;

  static final String MIGRATION_ID = "inactive-repository-violations";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final OperationalDataStore operationalDataStore;

  private final ClusterLockManager clusterLockManager;

  // Visible for tests
  Thread workerThread;

  @Inject
  public InactiveRepositoryViolationCleaner(
      final MigrationTrackerDAO migrationTrackerDAO,
      final OperationalDataStore operationalDataStore,
      final ClusterLockManager clusterLockManager)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.operationalDataStore = operationalDataStore;
    this.clusterLockManager = clusterLockManager;
  }

  @Override
  public void start() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Inactive repository violations already deleted.");
      return;
    }

    workerThread = new InactiveRepositoryViolationCleanerWorker();
    workerThread.start();
  }

  @Override
  public void stop() {
    // noop
  }

  // Visible for testing
  class InactiveRepositoryViolationCleanerWorker
      extends Thread
  {
    InactiveRepositoryViolationCleanerWorker() {
      setName("InactiveRepositoryViolationCleanerWorker");
      setDaemon(true);
      setPriority(getPriority() - 1);
    }

    @Override
    public void run() {
      log.info("Starting deletion of inactive repository policy violations.");

      try (ClusterLock clusterLock = clusterLockManager.createForInactiveRepositoryViolationCleaner()) {
        if (clusterLock.tryLock()) {
          log.info("Starting deletion of inactive repository policy violations.");
          doDeleteInactiveRepositoryPolicyViolations();
        }
        else {
          log.info("Skipping, deletion of inactive repository policy violations is already in progress.");
          clusterLock.unlock();
        }
      }
      catch (Exception e) {
        log.error("Failed while deleting inactive repository policy violations, will retry upon next server start.", e);
      }
      catch (Throwable t) {
        // Try to log to stderr before trying the standard logging because the standard logging may not be operational
        // at this point.
        t.printStackTrace();
        log.error(t.getMessage(), t);
        System.exit(2);
      }
    }

    // Visible for testing
    void doDeleteInactiveRepositoryPolicyViolations() throws SQLException, InterruptedException {
      long start = System.currentTimeMillis();
      int inactiveViolationCount = 0;
      while (true) {
        // Get a batch of inactive policy violations and delete them.
        List<String> inactivePolicyViolationIds = getInactivePolicyViolationIds();

        if (inactivePolicyViolationIds.size() > 0) {
          inactiveViolationCount += deleteInactivePolicyViolations(inactivePolicyViolationIds);
          log.trace("Deleted {} inactive repository policy violations.", inactiveViolationCount);
          // Allow other threads to access the database.
          Thread.sleep(50);
        }
        else {
          log.info("Finished deletion of {} inactive repository policy violations in {} ms.", inactiveViolationCount,
              System.currentTimeMillis() - start);
          migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
          return;
        }
      }
    }

    private List<String> getInactivePolicyViolationIds() throws SQLException {
      String query = "SELECT proxy_repository_policy_violation_id FROM " + OperationalDataStore.ID
          + ".proxy_repository_policy_violation" + " WHERE active = false FETCH FIRST " + BATCH_SIZE + " ROWS ONLY";
      try (Connection connection = operationalDataStore.getDataSource().getConnection();
          PreparedStatement preparedStatement = connection.prepareStatement(query);
          ResultSet result = preparedStatement.executeQuery())
      {
        List<String> inactivePolicyViolationIds = new ArrayList<>();
        while (result.next()) {
          inactivePolicyViolationIds.add(result.getString(1));
        }
        return inactivePolicyViolationIds;
      }
    }

    private int deleteInactivePolicyViolations(List<String> inactivePolicyViolationIds) throws SQLException {
      String in = inactivePolicyViolationIds.stream()
          .map(violationId -> "'" + violationId + "'")
          .collect(Collectors.joining(","));
      String query = "DELETE FROM " + OperationalDataStore.ID
          + ".proxy_repository_policy_violation WHERE proxy_repository_policy_violation_id IN (" + in + ")";
      try (Connection connection = operationalDataStore.getDataSource().getConnection();
          Statement statement = connection.createStatement())
      {
        connection.setAutoCommit(true);
        return statement.executeUpdate(query);
      }
    }
  }
}
