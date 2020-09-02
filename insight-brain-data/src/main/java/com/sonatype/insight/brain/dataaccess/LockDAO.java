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
      insert(new Lock(lockId));
    }
    catch (RollbackException e) {
      if (!(e.getCause() instanceof EntityExistsException)) {
        throw e;
      }
      // it already exists, great
    }
  }

  public Lock acquireLock(TransactionContext tx, String lockId) {
    Query<Lock> query = createQuery("SELECT entity FROM Lock entity WHERE entity.id = ?1", lockId)
        .setLockModeType(LockModeType.PESSIMISTIC_WRITE);
    Lock lock = query.get(tx);
    if (lock == null) {
      throw generateEntityNotFoundException(lockId);
    }
    return lock;
  }

  public boolean tryAcquireLock(TransactionContext tx, String lockId) {
    // NOTE: This query does by design not match/lock any row (and hence not block).
    // But it crucially forces JPA to start a JDBC transaction for the native query to participate in.
    createQuery("SELECT entity FROM Lock entity WHERE entity.id IS NULL")
        .setLockModeType(LockModeType.PESSIMISTIC_WRITE).get(tx);
    try {
      tx.createNativeQuery("SELECT * FROM " + OperationalDataStoreProvider.ID + ".lock" +
          " WHERE lock_id = ?1 FOR UPDATE NOWAIT").setParameter(1, lockId).getSingleResult();
    }
    catch (NoResultException e) {
      throw generateEntityNotFoundException(lockId);
    }
    catch (OptimisticLockException e) {
      return false;
    }
    return true;
  }

  private EntityNotFoundException generateEntityNotFoundException(String lockId) {
    return new EntityNotFoundException("Could not acquire lock " + lockId);
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
