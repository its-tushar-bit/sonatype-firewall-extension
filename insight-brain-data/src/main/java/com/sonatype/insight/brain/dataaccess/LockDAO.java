/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.RollbackException;

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
      throw new EntityNotFoundException("Could not acquire lock " + lockId);
    }
    return lock;
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
