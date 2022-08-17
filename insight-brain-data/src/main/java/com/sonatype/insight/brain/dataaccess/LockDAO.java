/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Lock;
import com.sonatype.insight.dataaccess.TransactionContext;

public class LockDAO
    extends AbstractOperationalSqlDAO<Lock>
{
  @Override
  public Lock getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Lock entity WHERE entity.id=?1";
    return get(tx, sQuery, id);
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

  public void acquireLock(TransactionContext tx, String lockId, LockModeType lockModeType) {
    acquireLock(tx, lockId, lockModeType, true);
  }

  public boolean tryAcquireLock(TransactionContext tx, String lockId, LockModeType lockModeType) {
    return acquireLock(tx, lockId, lockModeType, false);
  }

  public boolean acquireLock(TransactionContext tx, String lockId, LockModeType lockModeType, boolean waitForLock) {
    if (isDatabaseEmbedded() && LockModeType.PESSIMISTIC_WRITE != lockModeType) {
      throw new UnsupportedOperationException("Embedded database only supports acquiring an exclusive lock.");
    }
    // NOTE: This query does by design not match/lock any row (and hence not block).
    // But it crucially forces JPA to start a JDBC transaction for the native query to participate in.
    createQuery("SELECT entity FROM Lock entity WHERE entity.id IS NULL").setLockModeType(lockModeType).get(tx);
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
      tx.createNativeQuery("SELECT * FROM " + OperationalDataStoreProvider.ID + ".lock" +
              " WHERE lock_id = ?1 FOR " + lockType + (waitForLock ? "" : " NOWAIT")).setParameter(1, lockId)
          .getSingleResult();
    }
    catch (NoResultException e) {
      throw new EntityNotFoundException("Could not acquire lock " + lockId);
    }
    catch (OptimisticLockException e) {
      return false;
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
