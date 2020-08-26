/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityNotFoundException;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * This class extends a standard TransactionContext to provide the ability to lock it on a given String-based id.
 * <p>
 * Locking happens immediately after the transaction begins, and unlocking happens either immediately after committing
 * the transaction, or immediately after closing the transaction.
 * <p>
 * This class also hides (abstracts away) the differences between H2 and Postgres.
 * <p>
 * Postgres uses row-level locks instead of in-memory locks to ensure locking works in a clustered environment.
 * <p>
 * H2 uses in-memory locks instead of row-level locks because it does not support row-level locking, due to it being
 * configured to use the PageStore engine, and will only be used in a non-clustered environment.
 *
 * @since 1.97
 */
public class LockedTransactionContext
    extends TransactionContext
{
  // Visible for testing
  public static final ConcurrentMap<String, ReentrantLock> LOCKS_BY_ID = new ConcurrentHashMap<>();

  // Visible for testing
  static final String POLICY_VIOLATIONS_LOCK_PREFIX = "policy-violations-";

  // Visible for testing
  static final String POLICY_VIOLATION_AGGREGATIONS_LOCK_PREFIX = "policy-violation-aggregations-";

  // Visible for testing
  static final String REPOSITORY_COMPONENT_LOCK_PREFIX = "repository-component-";

  // Visible for testing
  final String lockId;

  // Visible for testing
  final ReentrantLock reentrantLock;

  public LockedTransactionContext(String lockId) {
    super(OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager());
    this.lockId = lockId;
    reentrantLock = createLock(lockId);
  }

  public static LockedTransactionContext createForPolicyViolations(Application application) {
    return new LockedTransactionContext(getLockIdForPolicyViolations(application));
  }

  public static void deleteForPolicyViolations(TransactionContext tx, Application application) {
    deleteLock(tx, getLockIdForPolicyViolations(application));
  }

  public static String getLockIdForPolicyViolations(Application application) {
    return POLICY_VIOLATIONS_LOCK_PREFIX + application.getId();
  }

  public static LockedTransactionContext createForPolicyViolationAggregations(String applicationId) {
    return new LockedTransactionContext(getLockIdForPolicyViolationAggregations(applicationId));
  }

  public static void deleteForPolicyViolationAggregations(TransactionContext tx, String applicationId) {
    deleteLock(tx, getLockIdForPolicyViolationAggregations(applicationId));
  }

  public static String getLockIdForPolicyViolationAggregations(String applicationId) {
    return POLICY_VIOLATION_AGGREGATIONS_LOCK_PREFIX + applicationId;
  }

  public static LockedTransactionContext createForRepositoryComponent(String repositoryId, String componentPathname) {
    return new LockedTransactionContext(getLockIdForRepositoryComponent(repositoryId, componentPathname));
  }

  public static void deleteForRepositoryComponent(String repositoryId, String componentPathname) {
    try (TransactionContext tx = new LockDAO().createTransactionContext()) {
      tx.begin();
      deleteForRepositoryComponent(tx, repositoryId, componentPathname);
      tx.commit();
    }
  }

  public static void deleteForRepositoryComponent(
      TransactionContext tx,
      String repositoryId,
      String componentPathname)
  {
    deleteLock(tx, getLockIdForRepositoryComponent(repositoryId, componentPathname));
  }

  public static void deleteForRepository(TransactionContext tx, String repositoryId) {
    String prefix = getLockIdForRepositoryComponent(repositoryId, "");
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      LOCKS_BY_ID.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(lockId -> deleteLockH2(lockId));
    }
    else {
      new LockDAO().deleteByPrefix(tx, prefix);
    }
  }

  public static String getLockIdForRepositoryComponent(String repositoryId, String componentPathname) {
    return REPOSITORY_COMPONENT_LOCK_PREFIX + repositoryId + "-" + componentPathname;
  }

  @Override
  public void begin() {
    super.begin();
    lock();
  }

  @Override
  public void commit() {
    try {
      super.commit();
    }
    finally {
      unlock();
    }
  }

  @Override
  public void close() {
    try {
      super.close();
    }
    finally {
      unlock();
    }
  }

  private ReentrantLock createLock(String lockId) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      return LOCKS_BY_ID.computeIfAbsent(lockId, key -> new ReentrantLock());
    }
    else {
      new LockDAO().createLock(lockId);
      return null;
    }
  }

  private void lock() {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      reentrantLock.lock();
      // Locking prevents removal/replacement, but check that it wasn't removed/replaced before locking
      if (LOCKS_BY_ID.get(lockId) != reentrantLock) {
        reentrantLock.unlock();
        throw new EntityNotFoundException("Could not acquire lock " + lockId);
      }
    }
    else {
      new LockDAO().acquireLock(this, lockId);
    }
  }

  private void unlock() {
    if (reentrantLock != null && reentrantLock.getHoldCount() > 0) {
      reentrantLock.unlock();
    }
  }

  public static void deleteLock(TransactionContext tx, String lockId) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      deleteLockH2(lockId);
    }
    else {
      new LockDAO().deleteLock(tx, lockId);
    }
  }

  private static void deleteLockH2(String lockId) {
    ReentrantLock lock = LOCKS_BY_ID.get(lockId);
    if (lock != null) {
      lock.lock();
      LOCKS_BY_ID.remove(lockId);
      lock.unlock();
    }
  }

  public static boolean lockExists(String lockId) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      return LockedTransactionContext.LOCKS_BY_ID.containsKey(lockId);
    }
    else {
      return new LockDAO().getById(lockId) != null;
    }
  }
}
