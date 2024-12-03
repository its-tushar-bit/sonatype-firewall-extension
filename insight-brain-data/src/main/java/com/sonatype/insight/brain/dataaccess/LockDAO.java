/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.RollbackException;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Lock;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class LockDAO
    extends AbstractOperationalSqlDAO<Lock>
{
  // as documented on https://www.postgresql.org/docs/current/errcodes-appendix.html
  private static final String POSTGRES_LOCK_NOT_AVAILABLE = "55P03";

  @Inject
  public LockDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void update(TransactionContext tx, Lock entity) {
    throw new UnsupportedOperationException();
  }

  public void createLock(String lockId) {
    try {
      if (getById(lockId) == null) {
        insert(new Lock(lockId));
      }
    }
    catch (RollbackException e) {
      if (!(e.getCause() instanceof EntityExistsException)) {
        throw e;
      }
      // it already exists, great
    }
  }

  /**
   * Note: Connection must have auto-commit set to false
   */
  public void acquireLock(Connection connection, String lockId, LockModeType lockModeType) {
    acquireLock(connection, lockId, lockModeType, true);
  }

  /**
   * Note: Connection must have auto-commit set to false
   */
  public boolean tryAcquireLock(Connection connection, String lockId, LockModeType lockModeType) {
    return acquireLock(connection, lockId, lockModeType, false);
  }

  /**
   * Note: Connection must have auto-commit set to false
   */
  public boolean acquireLock(Connection connection, String lockId, LockModeType lockModeType, boolean waitForLock) {
    if (isDatabaseEmbedded()) {
      throw new UnsupportedOperationException("Embedded database not supported for database advisory locks");
    }

    try {
      String lockType;
      switch (lockModeType) {
        case PESSIMISTIC_WRITE: {
          lockType = "UPDATE";
          break;
        }
        case PESSIMISTIC_READ: {
          lockType = "SHARE";
          break;
        }
        default: {
          throw new IllegalStateException(String.format("Unknown lock mode type: %s.", lockModeType));
        }
      }

      String sQuery = "SELECT 1 FROM " + getDatabaseSchema() + ".lock" +
          " WHERE lock_id = ? FOR " + lockType + (waitForLock ? "" : " NOWAIT");

      try (PreparedStatement stmt = connection.prepareStatement(sQuery)) {
        stmt.setObject(1, lockId);

        try (ResultSet results = stmt.executeQuery()) {
          if (!results.next()) {
            // no rows selected
            throw new EntityNotFoundException("Lock row does not exist: " + lockId);
          }
        }
      }
    }
    catch (SQLException e) {
      if (POSTGRES_LOCK_NOT_AVAILABLE.equals(e.getSQLState())) {
        return false;
      }
      else {
        throw new RuntimeException(e);
      }
    }

    return true;
  }

  public void deleteLock(TransactionContext tx, String lockId) {
    delete(tx, getById(lockId));
  }

  public void deleteByPrefix(TransactionContext tx, String prefix) {
    String sQuery = "DELETE FROM Lock entity WHERE entity.id >= ?1 AND entity.id < ?2";
    createQuery(sQuery, prefix, prefix.substring(0, prefix.length() - 1) + (prefix.charAt(prefix.length() - 1) + 1))
        .executeUpdate(tx);
  }
}
