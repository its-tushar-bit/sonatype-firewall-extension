/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * This class implements a locking mechanism that can be used across multiple nodes in a cluster.
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
public class ClusterLock
    implements AutoCloseable
{
  // Visible for testing
  public static final ConcurrentMap<String, Semaphore> LOCKS_BY_ID = new ConcurrentHashMap<>();

  // Visible for testing
  static final String POLICY_VIOLATIONS_LOCK_PREFIX = "policy-violations-";

  // Visible for testing
  static final String POLICY_VIOLATION_AGGREGATIONS_LOCK_PREFIX = "policy-violation-aggregations-";

  // Visible for testing
  static final String REPOSITORY_COMPONENT_LOCK_PREFIX = "repository-component-";

  // Visible for testing
  static final String REPOSITORY_REEVALUATION_LOCK_PREFIX = "repository-reevaluation-";

  // Visible for testing
  static final String POLICY_EVALUATION_LOCK_PREFIX = "policy-evaluation-";

  // Visible for testing
  static final String AUDIT_JSON_FILE_STORE_LOCK_PREFIX = "audit-json-file-store-";

  // Visible for testing
  final String lockId;

  // Visible for testing
  final Semaphore lock;

  private volatile boolean acquired;

  private volatile TransactionContext tx;

  public ClusterLock(String lockId) {
    this.lockId = lockId;
    lock = createLock(lockId);
  }

  public static ClusterLock createForPolicyViolations(Application application) {
    return new ClusterLock(getLockIdForPolicyViolations(application));
  }

  public static void deleteForPolicyViolations(TransactionContext tx, Application application) {
    deleteLock(tx, getLockIdForPolicyViolations(application));
  }

  public static String getLockIdForPolicyViolations(Application application) {
    return POLICY_VIOLATIONS_LOCK_PREFIX + application.getId();
  }

  public static ClusterLock createForPolicyViolationAggregations(String applicationId) {
    return new ClusterLock(getLockIdForPolicyViolationAggregations(applicationId));
  }

  public static void deleteForPolicyViolationAggregations(TransactionContext tx, String applicationId) {
    deleteLock(tx, getLockIdForPolicyViolationAggregations(applicationId));
  }

  public static String getLockIdForPolicyViolationAggregations(String applicationId) {
    return POLICY_VIOLATION_AGGREGATIONS_LOCK_PREFIX + applicationId;
  }

  public static ClusterLock createForRepositoryComponent(String repositoryId, String componentPathname) {
    return new ClusterLock(getLockIdForRepositoryComponent(repositoryId, componentPathname));
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
    deleteLocksByPrefix(tx, getLockIdForRepositoryComponent(repositoryId, ""));
  }

  public static String getLockIdForRepositoryComponent(String repositoryId, String componentPathname) {
    return REPOSITORY_COMPONENT_LOCK_PREFIX + repositoryId + "-" + componentPathname;
  }

  public static ClusterLock createForRepositoryReevaluation(Repository repository) {
    return new ClusterLock(getLockIdForRepositoryReevaluation(repository));
  }

  public static void deleteForRepositoryReevaluation(TransactionContext tx, Repository repository) {
    deleteLock(tx, getLockIdForRepositoryReevaluation(repository));
  }

  public static String getLockIdForRepositoryReevaluation(Repository repository) {
    return REPOSITORY_REEVALUATION_LOCK_PREFIX + repository.getId();
  }

  public static ClusterLock createForPolicyEvaluation(Application application, String scanId) {
    return new ClusterLock(getLockIdForPolicyEvaluation(application, scanId));
  }

  public static void deleteForPolicyEvaluation(Application application, String scanId) {
    try (TransactionContext tx = new LockDAO().createTransactionContext()) {
      tx.begin();
      deleteLock(tx, getLockIdForPolicyEvaluation(application, scanId));
      tx.commit();
    }
  }

  public static void deleteForPolicyEvaluations(TransactionContext tx, Application application) {
    deleteLocksByPrefix(tx, getLockIdForPolicyEvaluation(application, ""));
  }

  public static String getLockIdForPolicyEvaluation(Application application, String scanId) {
    return POLICY_EVALUATION_LOCK_PREFIX + application.getId() + "-" + scanId;
  }

  public static ClusterLock createForAuditJsonFileStore(String ownerId) {
    return new ClusterLock(getLockIdForAuditJsonFileStore(ownerId));
  }

  public static void deleteForAuditJsonFileStore(TransactionContext tx, String ownerId) {
    deleteLock(tx, getLockIdForAuditJsonFileStore(ownerId));
  }

  public static String getLockIdForAuditJsonFileStore(String ownerId) {
    return AUDIT_JSON_FILE_STORE_LOCK_PREFIX + ownerId;
  }

  public void lock() {
    if (!acquired) {
      lock(true);
    }
  }

  public boolean tryLock() {
    if (!acquired) {
      lock(false);
    }
    return acquired;
  }

  @Override
  public void close() {
    unlock();
  }

  private Semaphore createLock(String lockId) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      return LOCKS_BY_ID.computeIfAbsent(lockId, key -> new Semaphore(1));
    }
    else {
      new LockDAO().createLock(lockId);
      return null;
    }
  }

  private void lock(boolean waitForLock) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      acquired = acquire(lock, waitForLock);
      // Locking prevents removal/replacement, but check that it wasn't removed/replaced before locking
      if (LOCKS_BY_ID.get(lockId) != lock) {
        lock.release();
        acquired = false;
        throw new RuntimeException("Could not acquire lock " + lockId);
      }
    }
    else {
      acquired = acquire(waitForLock);
    }
  }

  private boolean acquire(Semaphore lock, boolean waitForLock) {
    if (waitForLock) {
      lock.acquireUninterruptibly();
      return true;
    }
    else {
      return lock.tryAcquire();
    }
  }

  private boolean acquire(boolean waitForLock) {
    TransactionContext tempTx =
        new TransactionContext(OperationalDataStoreProvider.getEntityManagerFactoryForLocks().createEntityManager());
    tempTx.begin();
    try {
      if (waitForLock) {
        new LockDAO().acquireLock(tempTx, lockId);
        tx = tempTx;
        return true;
      }
      else {
        if (new LockDAO().tryAcquireLock(tempTx, lockId)) {
          tx = tempTx;
          return true;
        }
        // Failed to acquire lock
        tempTx.close();
        return false;
      }
    }
    catch (RuntimeException e) {
      try {
        tempTx.close();
      }
      catch (Exception closeException) {
        e.addSuppressed(closeException);
      }
      throw e;
    }
  }

  public void unlock() {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      if (acquired && lock != null && lock.availablePermits() < 1) {
        lock.release();
        acquired = false;
      }
    }
    else {
      acquired = false;
      if (tx != null) {
        tx.close();
        tx = null;
      }
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
    Semaphore lock = LOCKS_BY_ID.get(lockId);
    if (lock != null) {
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        clusterLock.lock(true);
        LOCKS_BY_ID.remove(lockId);
      }
    }
  }

  private static void deleteLocksByPrefix(TransactionContext tx, String prefix) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      LOCKS_BY_ID.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(ClusterLock::deleteLockH2);
    }
    else {
      new LockDAO().deleteByPrefix(tx, prefix);
    }
  }

  public static boolean lockExists(String lockId) {
    if (OperationalDataStoreProvider.isDatabaseEmbedded()) {
      return ClusterLock.LOCKS_BY_ID.containsKey(lockId);
    }
    else {
      return new LockDAO().getById(lockId) != null;
    }
  }
}
