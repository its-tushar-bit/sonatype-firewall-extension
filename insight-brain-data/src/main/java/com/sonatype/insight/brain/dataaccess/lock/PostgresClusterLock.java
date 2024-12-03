/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.sql.Connection;
import java.sql.SQLException;

import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import datadog.trace.api.Trace;

public class PostgresClusterLock
    extends AbstractClusterLock
{
  private final OperationalDataStore operationalDataStore;

  private final LockDAO lockDAO;

  private final PostgresAdvisoryLockDAO advisoryLockDAO;

  private volatile Connection connection;

  protected PostgresClusterLock(
      final ClusterLockId clusterLockId,
      final OperationalDataStore operationalDataStore,
      final LockDAO lockDAO,
      final PostgresAdvisoryLockDAO advisoryLockDAO)
  {
    super(clusterLockId);

    this.operationalDataStore = operationalDataStore;
    this.lockDAO = lockDAO;
    this.advisoryLockDAO = advisoryLockDAO;
  }

  @Override
  @Trace
  public void lock(final LockType lockType, final boolean waitForLock) {
    this.lockType = lockType;
    this.waitForLock = waitForLock;
    this.acquired = acquire();
  }

  @Override
  @Trace
  public void unlock() {
    acquired = false;
    if (connection != null) {
      try {
        rollbackAndCloseConnection(connection);
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
      finally {
        connection = null;
      }
    }
  }

  @SuppressWarnings("PMD.DoNotThrowExceptionInFinally")
  private boolean acquire() {
    String oldStyleLockId = clusterLockId.getOldStyleLockId();
    Connection connection = getNewConnection();

    RuntimeException runtimeException = null;
    try {
      if (waitForLock) {

        // TEMPORARY: as we migrate from the old table-based locks to the new advisory lock mechanism, we use both.
        // Once all pods are running this code, another deployment will be done to remove the call to lockDAO,
        // and then once all pods are running _that_ code, a third deployment will be done to delete the lock table.
        lockDAO.acquireLock(connection, oldStyleLockId, lockType.getLockModeType());
        advisoryLockDAO.acquireLock(connection, clusterLockId, lockType.getLockModeType());
        this.connection = connection;
        return true;
      }
      else {
        if (lockDAO.tryAcquireLock(connection, oldStyleLockId, lockType.getLockModeType())) {
          if (advisoryLockDAO.tryAcquireLock(connection, clusterLockId, lockType.getLockModeType())) {
            this.connection = connection;
            return true;
          }
          // Else failed to acquire pg_advisory lock, likely because a future-impl node that doesn't use the old lock
          // table has it.
        }
        // Else failed to acquire lock. Return false, do not assign this.connection, and allow the finally block to
        // close `connection`.
        return false;
      }
    }
    catch (Exception e) {
      if (e instanceof RuntimeException re) {
        throw re;
      }
      else {
        runtimeException = new RuntimeException(e);
        throw runtimeException;
      }
    }
    finally {
      if (connection != this.connection) {
        // If we didn't save the connection in this.connection, we need to dispose of it
        try {
          rollbackAndCloseConnection(connection);
        }
        catch (SQLException e) {
          if (runtimeException != null) {
            runtimeException.addSuppressed(e);
          }
          else {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private Connection getNewConnection() {
    try {
      var connection = operationalDataStore.getDataSourceForLocks().getConnection();

      // Note: attempting to set default auto-commit on the DataSource (in DefaultOperationalDataStore) had no effect.
      // JPA might be interfering with it. So we set it per-connection.
      connection.setAutoCommit(false);
      return connection;
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("PMD.DoNotThrowExceptionInFinally")
  private void rollbackAndCloseConnection(Connection connection) throws SQLException {
    SQLException sqlException = null;
    try {
      connection.rollback();
    }
    catch (SQLException e1) {
      sqlException = e1;
      throw e1;
    }
    finally {
      try {
        connection.close();
      }
      catch (SQLException e2) {
        if (sqlException != null) {
          sqlException.addSuppressed(e2);
        }
        else {
          throw e2;
        }
      }
    }
  }
}
