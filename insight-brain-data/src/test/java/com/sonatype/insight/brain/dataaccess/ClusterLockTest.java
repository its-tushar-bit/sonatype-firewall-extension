/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.ClusterLock.LockType;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

public class ClusterLockTest
{
  private LockDAO lockDAO;

  @Before
  public void before() {
    OperationalDataStoreProvider.init(null, false);
    lockDAO = new LockDAO();
  }

  @After
  public void after() {
    ClusterLock.LOCKS_BY_ID.clear();
  }

  @Test
  public void testConstructor_H2() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      assertThat(clusterLock.lockId).isEqualTo(lockId);
      Semaphore lock = ClusterLock.LOCKS_BY_ID.get(clusterLock.lockId);
      assertThat(lock).isNotNull();
      assertThat(clusterLock.lock).isEqualTo(lock);
      assertThat(lockDAO.getById(lockId)).isNull();
    }
  }

  @Test
  public void testConstructor_Postgres() {
    String lockId = "test-lock";
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        assertThat(clusterLock.lockId).isEqualTo(lockId);
        Semaphore lock = ClusterLock.LOCKS_BY_ID.get(clusterLock.lockId);
        assertThat(lock).isNull();
        assertThat(clusterLock.lock).isNull();
        assertThat(lockDAO.getById(lockId)).isNotNull();
      }
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private CountDownLatch startConcurrentLockThread(String lockId) throws Exception {
    CountDownLatch lockLatch = new CountDownLatch(1);
    CountDownLatch unlockLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        lockLatch.countDown();
        clusterLock.lock();
        clusterLock.unlock();
        unlockLatch.countDown();
      }
    });
    other.start();
    assertThat(lockLatch.await(10, TimeUnit.SECONDS)).isTrue();
    return unlockLatch;
  }

  @Test
  public void testBlockingOnSameLock_H2() throws Exception {
    testBlockingOnSameLock();
  }

  @Test
  public void testBlockingOnSameLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testBlockingOnSameLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testBlockingOnSameLock() throws Exception {
    String lockId = "test-lock";
    CountDownLatch latch;
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      clusterLock.lock();
      latch = startConcurrentLockThread(lockId);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      clusterLock.unlock();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testNonBlockingOnDifferentLock_H2() throws Exception {
    testNonBlockingOnDifferentLock();
  }

  @Test
  public void testNonBlockingOnDifferentLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testNonBlockingOnDifferentLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testNonBlockingOnDifferentLock() throws Exception {
    String lockId1 = "test-lock-1";
    String lockId2 = "test-lock-2";
    CountDownLatch latch;
    try (ClusterLock clusterLock = new ClusterLock(lockId1)) {
      clusterLock.lock();
      latch = startConcurrentLockThread(lockId2);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
      clusterLock.unlock();
    }
  }

  @Test
  public void testGetLockIdForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");

    assertThat(ClusterLock.getLockIdForPolicyViolations(application))
        .isEqualTo(ClusterLock.POLICY_VIOLATIONS_LOCK_PREFIX + application.getId());
  }

  @Test
  public void testCreateForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");
    try (ClusterLock clusterLock = ClusterLock.createForPolicyViolations(application)) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForPolicyViolations(application));
    }
  }

  @Test
  public void testCreateForPolicyViolationAggregations() {
    String appId = "app-id";
    try (ClusterLock clusterLock = ClusterLock.createForPolicyViolationAggregations(appId)) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForPolicyViolationAggregations(appId));
    }
  }

  @Test
  public void testGetLockIdForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    assertThat(ClusterLock.getLockIdForRepositoryComponent(repositoryId, componentPathname))
        .isEqualTo(ClusterLock.REPOSITORY_COMPONENT_LOCK_PREFIX + repositoryId + "-" + componentPathname);
  }

  @Test
  public void testCreateLockForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    try (ClusterLock clusterLock = ClusterLock
        .createForRepositoryComponent(repositoryId, componentPathname)) {
      assertThat(clusterLock.lockId)
          .isEqualTo(ClusterLock.getLockIdForRepositoryComponent(repositoryId, componentPathname));
    }
  }

  @Test
  public void testGetLockIdForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");

    assertThat(ClusterLock.getLockIdForRepositoryReevaluation(repository))
        .isEqualTo(ClusterLock.REPOSITORY_REEVALUATION_LOCK_PREFIX + repository.getId());
  }

  @Test
  public void testCreateLockForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");

    assertThat(ClusterLock.createForRepositoryReevaluation(repository).lockId)
        .isEqualTo(ClusterLock.getLockIdForRepositoryReevaluation(repository));
  }

  private CountDownLatch startConcurrentDeleteLockThread(String lockId) {
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        ClusterLock.deleteLock(tx, lockId);
        tx.commit();
        commitLatch.countDown();
      }
    });
    other.start();
    return commitLatch;
  }

  private void testDeleteLock() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      assertThat(ClusterLock.lockExists(lockId)).isTrue();
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        ClusterLock.deleteLock(tx, lockId);
        tx.commit();
      }
      assertThat(ClusterLock.lockExists(lockId)).isFalse();
    }
  }

  @Test
  public void testDeleteLock_H2() {
    testDeleteLock();
  }

  @Test
  public void testDeleteLock_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testDeleteLock_WaitsForLocks() throws Exception {
    String lockId = "test-lock";
    CountDownLatch latch;
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      clusterLock.lock();
      latch = startConcurrentDeleteLockThread(lockId);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      clusterLock.unlock();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(lockDAO.getById(lockId)).isNull();
  }

  @Test
  public void testDeleteLock_WaitsForLocks_H2() throws Exception {
    testDeleteLock_WaitsForLocks();
  }

  @Test
  public void testDeleteLock_WaitsForLocks_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteLock_WaitsForLocks();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testClose_ReleasesLock() throws Exception {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      clusterLock.lock();
    }
    CountDownLatch latch = startConcurrentLockThread(lockId);
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testClose_ReleasesLock_H2() throws Exception {
    testClose_ReleasesLock();
  }

  @Test
  public void testClose_ReleasesLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testClose_ReleasesLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testCannotLockIfDeleted_H2() {
    testCannotLockIfDeleted();
  }

  @Test
  public void testCannotLockIfDeleted_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testCannotLockIfDeleted();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testCannotLockIfDeleted() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        ClusterLock.deleteLock(tx, lockId);
        tx.commit();
      }
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(clusterLock::lock)
          .withMessage("Could not acquire lock test-lock");
    }
  }

  @Test
  public void testCloseLockInOtherThread_H2() throws Exception {
    testCloseLockInOtherThread();
  }

  @Test
  public void testCloseLockInOtherThread_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testCloseLockInOtherThread();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testCloseLockInOtherThread() throws Exception {
    String lockId = "test-lock";
    AtomicReference<ClusterLock> clusterLockReference = new AtomicReference<>();
    CountDownLatch lockInitializerAndTakerEnd = new CountDownLatch(1);
    Thread lockInitializerAndTaker = new Thread(() -> {
      ClusterLock clusterLock = new ClusterLock(lockId);
      clusterLockReference.set(clusterLock);
      clusterLock.lock();
      lockInitializerAndTakerEnd.countDown();
    });
    CountDownLatch lockWaiterEnd = new CountDownLatch(1);
    Thread lockWaiter = new Thread(() -> {
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        clusterLock.lock();
        lockWaiterEnd.countDown();
      }
    });
    CountDownLatch lockCloserEnd = new CountDownLatch(1);
    Thread lockCloser = new Thread(() -> {
      clusterLockReference.get().close();
      lockCloserEnd.countDown();
    });

    lockInitializerAndTaker.start();
    assertThat(lockInitializerAndTakerEnd.await(3, TimeUnit.SECONDS)).isTrue();
    lockWaiter.start();
    assertThat(lockWaiterEnd.await(3, TimeUnit.SECONDS)).isFalse();
    lockCloser.start();
    assertThat(lockCloserEnd.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(lockWaiterEnd.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testLockWithNoWait_H2() throws Exception {
    testLockWithNoWait();
  }

  @Test
  public void testLockWithNoWait_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLockWithNoWait();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLockWithNoWait() throws Exception {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      // Take the lock in the main thread
      clusterLock.lock();
      AtomicBoolean failedToLock = new AtomicBoolean();
      assertThat(startConcurrentLockWithNoWait(failedToLock, lockId).await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToLock.get()).isTrue();
      failedToLock = new AtomicBoolean();
      assertThat(startConcurrentLockWithNoWait(failedToLock, lockId).await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToLock.get()).isTrue();
    }
  }

  private CountDownLatch startConcurrentLockWithNoWait(AtomicBoolean failedToLock, String lockId) {
    CountDownLatch concurrentThreadEnd = new CountDownLatch(1);
    Thread concurrentThread = new Thread(() -> {
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        // Try to take the lock in the concurrent thread without waiting
        failedToLock.set(!clusterLock.tryLock());
      }
      finally {
        concurrentThreadEnd.countDown();
      }
    });
    concurrentThread.start();
    return concurrentThreadEnd;
  }

  @Test
  public void testLockTwice_H2() {
    testLockTwice();
  }

  @Test
  public void testLockTwice_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLockTwice();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLockTwice() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      clusterLock.lock();
      clusterLock.lock();
    }
  }

  @Test
  public void testTryLockTwice_H2() {
    testTryLockTwice();
  }

  @Test
  public void testTryLockTwice_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testTryLockTwice();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testTryLockTwice() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = new ClusterLock(lockId)) {
      assertThat(clusterLock.tryLock()).isTrue();
      assertThat(clusterLock.tryLock()).isTrue();
    }
  }

  @Test(timeout = 60_000)
  public void testLock_Postgres_LocksDoNotCompeteWithRegularQueriesForConnections() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      DatabaseConfig dbConfig = postgres.getDatabaseConfig();
      dbConfig.setMaxConnections(2);
      OperationalDataStoreProvider.init(dbConfig, false);

      String lockId = "test";
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        clusterLock.lock();
        CountDownLatch latch = startConcurrentLockThread(lockId);
        assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();

        // both this and the concurrent thread have one active tx/connection for the lock
        // if these two connections are from the same pool as for regular ODS queries, we'll deadlock next
        new ApplicationDAO().getAll();

        clusterLock.unlock();
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
      }
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testGetLockIdForPolicyEvaluation() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";

    assertThat(ClusterLock.getLockIdForPolicyEvaluation(application, scanId))
        .isEqualTo(ClusterLock.POLICY_EVALUATION_LOCK_PREFIX + application.getId() + "-" + scanId);
  }

  @Test
  public void testCreateForPolicyEvaluation() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";

    assertThat(ClusterLock.createForPolicyEvaluation(application, scanId).lockId)
        .isEqualTo(ClusterLock.getLockIdForPolicyEvaluation(application, scanId));
  }

  @Test
  public void testGetLockIdForAuditJsonFileStore() {
    String ownerId = "ownerId";
    assertThat(ClusterLock.getLockIdForAuditJsonFileStore(ownerId))
        .isEqualTo(ClusterLock.AUDIT_JSON_FILE_STORE_LOCK_PREFIX + ownerId);
  }

  @Test
  public void testCreateForAuditJsonFileStore() {
    String ownerId = "ownerId";
    try (ClusterLock clusterLock = ClusterLock.createForAuditJsonFileStore(ownerId)) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForAuditJsonFileStore(ownerId));
    }
  }

  @Test
  public void testGetLockIdForSchemaMigration() {
    assertThat(ClusterLock.getLockIdForSchemaMigration()).isEqualTo(ClusterLock.SCHEMA_MIGRATION)
        .isEqualTo("schema-migration");
  }

  @Test
  public void testCreateForSchemaMigration() {
    try (ClusterLock clusterLock = ClusterLock.createForSchemaMigration()) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForSchemaMigration());
    }
  }

  @Test
  public void testDeleteForSchemaMigration() {
    ClusterLock.createForSchemaMigration();
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForSchemaMigration())).isTrue();

    ClusterLock.deleteForSchemaMigration();

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForSchemaMigration())).isFalse();
  }

  @Test
  public void testGetLockIdForSchemaMigrationInProgress() {
    assertThat(ClusterLock.getLockIdForSchemaMigrationInProgress()).isEqualTo(ClusterLock.SCHEMA_MIGRATION_IN_PROGRESS)
        .isEqualTo("schema-migration-in-progress");
  }

  @Test
  public void testCreateForSchemaMigrationInProgress() {
    try (ClusterLock clusterLock = ClusterLock.createForSchemaMigrationInProgress()) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForSchemaMigrationInProgress());
    }
  }

  @Test
  public void testDeleteForSchemaMigrationInProgress() {
    ClusterLock.createForSchemaMigrationInProgress();
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForSchemaMigrationInProgress())).isTrue();

    ClusterLock.deleteForSchemaMigrationInProgress();

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForSchemaMigrationInProgress())).isFalse();
  }

  @Test
  public void testGetLockIdForDataMigration() {
    assertThat(ClusterLock.getLockIdForDataMigration()).isEqualTo(ClusterLock.DATA_MIGRATION)
        .isEqualTo("data-migration");
  }

  @Test
  public void testCreateForDataMigration() {
    try (ClusterLock clusterLock = ClusterLock.createForDataMigration()) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForDataMigration());
    }
  }

  @Test
  public void testDeleteForDataMigration() {
    ClusterLock.createForDataMigration();
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForDataMigration())).isTrue();

    ClusterLock.deleteForDataMigration();

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForDataMigration())).isFalse();
  }

  @Test
  public void testGetLockIdForNewInstancePopulation() {
    assertThat(ClusterLock.getLockIdForNewInstancePopulation()).isEqualTo(ClusterLock.NEW_INSTANCE_POPULATION)
        .isEqualTo("new-instance-population");
  }

  @Test
  public void testCreateForNewInstancePopulation() {
    try (ClusterLock clusterLock = ClusterLock.createForNewInstancePopulation()) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForNewInstancePopulation());
    }
  }

  @Test
  public void testDeleteForNewInstancePopulation() {
    ClusterLock.createForNewInstancePopulation();
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForNewInstancePopulation())).isTrue();

    ClusterLock.deleteForNewInstancePopulation();

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForNewInstancePopulation())).isFalse();
  }

  @Test
  public void testGetLockIdForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    assertThat(ClusterLock.getLockIdForPdfGeneration(application, scanId))
        .isEqualTo(ClusterLock.PDF_GENERATION_LOCK_PREFIX + application.getId() + "-" + scanId);
  }

  @Test
  public void testCreateForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    try (ClusterLock clusterLock = ClusterLock.createForPdfGeneration(application, scanId)) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForPdfGeneration(application, scanId));
    }
  }

  @Test
  public void testDeleteForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    ClusterLock.createForPdfGeneration(application, scanId);
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForPdfGeneration(application, scanId))).isTrue();

    try (TransactionContext tx = lockDAO.createTransactionContext()) {
      tx.begin();
      ClusterLock.deleteForPdfGeneration(tx, application);
      tx.commit();
    }

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForPdfGeneration(application, scanId))).isFalse();
  }

  @Test
  public void testGetLockIdForInactiveRepositoryViolationCleaner() {
    assertThat(ClusterLock.getLockIdForInactiveRepositoryViolationCleaner())
        .isEqualTo(ClusterLock.INACTIVE_REPOSITORY_VIOLATION_CLEANER);
  }

  @Test
  public void testCreateForInactiveRepositoryViolationCleaner() {
    try (ClusterLock clusterLock = ClusterLock.createForInactiveRepositoryViolationCleaner()) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForInactiveRepositoryViolationCleaner());
    }
  }

  @Test
  public void testDeleteForInactiveRepositoryViolationCleaner() {
    ClusterLock.createForInactiveRepositoryViolationCleaner();
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForInactiveRepositoryViolationCleaner())).isTrue();

    ClusterLock.deleteForInactiveRepositoryViolationCleaner();

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForInactiveRepositoryViolationCleaner())).isFalse();
  }

  @Test
  public void testGetLockIdForFilename() {
    String filename = "test";
    assertThat(ClusterLock.getLockIdForFilename(filename)).isEqualTo(ClusterLock.FILENAME_LOCK_PREFIX + filename);
  }

  @Test
  public void testCreateForFilename() {
    String filename = "test";
    try (ClusterLock clusterLock = ClusterLock.createForFilename(filename)) {
      clusterLock.lock();

      assertThat(clusterLock.lockId).isEqualTo(ClusterLock.getLockIdForFilename(filename));
    }
  }

  @Test
  public void testDeleteForFilename() {
    String filename = "test";
    ClusterLock.createForFilename(filename);
    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForFilename(filename))).isTrue();

    ClusterLock.deleteForFilename(filename);

    assertThat(ClusterLock.lockExists(ClusterLock.getLockIdForFilename(filename))).isFalse();
  }

  private static class ClusterLockThread
      extends Thread
  {
    private final String lockId;

    private final LockType lockType;

    private final boolean waitForLock;

    private final CountDownLatch endLatch = new CountDownLatch(1);

    private volatile boolean acquired;

    private volatile Exception exception;

    public ClusterLockThread(String lockId, LockType lockType, boolean waitForLock) {
      this.lockId = lockId;
      this.lockType = lockType;
      this.waitForLock = waitForLock;
    }

    @Override
    public void run() {
      try (ClusterLock clusterLock = new ClusterLock(lockId)) {
        if (waitForLock) {
          clusterLock.lock(lockType);
          acquired = true;
        }
        else {
          acquired = clusterLock.tryLock(lockType);
        }
        endLatch.await(10, TimeUnit.SECONDS);
      }
      catch (Exception e) {
        exception = e;
      }
    }

    public void allowClose() {
      endLatch.countDown();
    }
  }

  @Test
  public void testLock_AllowsConcurrentShared_H2() {
    testLock_AllowsConcurrentShared();
  }

  @Test
  public void testLock_AllowsConcurrentShared_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLock_AllowsConcurrentShared();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLock_AllowsConcurrentShared() {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, true);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);

    shared1.allowClose();
    shared2.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testLock_SharedDoesNotAllowExclusive_H2() {
    testLock_SharedDoesNotAllowExclusive();
  }

  @Test
  public void testLock_SharedDoesNotAllowExclusive_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLock_SharedDoesNotAllowExclusive();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLock_SharedDoesNotAllowExclusive() {
    String lockId = "test-lock";
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);

    shared.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  @Test
  public void testLock_ExclusiveDoesNotAllowShared_H2() {
    testLock_ExclusiveDoesNotAllowShared();
  }

  @Test
  public void testLock_ExclusiveDoesNotAllowShared_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLock_ExclusiveDoesNotAllowShared();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLock_ExclusiveDoesNotAllowShared() {
    String lockId = "test-lock";
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, true);

    exclusive.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
    shared.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared.acquired);
    exclusive.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared.acquired);

    shared.allowClose();
    assertThat(exclusive.exception).isNull();
    assertThat(shared.exception).isNull();
  }

  @Test
  public void testLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed_H2() {
    testLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed();
  }

  @Test
  public void testLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed() {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared1.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared2.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  @Test
  public void testLock_FIFO_H2() {
    testLock_FIFO(true);
  }

  @Test
  public void testLock_FIFO_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLock_FIFO(false);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLock_FIFO(boolean expectFIFO) {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, true);

    if (expectFIFO) {
      shared1.start();
      await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
      exclusive.start();
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
      shared2.start();
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared2.acquired);
      shared1.allowClose();
      await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared2.acquired);
      exclusive.allowClose();
      await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
      shared2.allowClose();
    }
    else {
      shared1.start();
      await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
      exclusive.start();
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
      shared2.start();
      await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
      shared1.allowClose();
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
      shared2.allowClose();
      await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
      exclusive.allowClose();
    }

    assertThat(shared1.exception).isNull();
    assertThat(exclusive.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testTryLock_AllowsConcurrentShared_H2() {
    testTryLock_AllowsConcurrentShared();
  }

  @Test
  public void testTryLock_AllowsConcurrentShared_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testTryLock_AllowsConcurrentShared();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testTryLock_AllowsConcurrentShared() {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, false);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);

    shared1.allowClose();
    shared2.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testTryLock_SharedDoesNotAllowExclusive_H2() {
    testTryLock_SharedDoesNotAllowExclusive();
  }

  @Test
  public void testTryLock_SharedDoesNotAllowExclusive_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testTryLock_SharedDoesNotAllowExclusive();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testTryLock_SharedDoesNotAllowExclusive() {
    String lockId = "test-lock";
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, false);

    shared.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  @Test
  public void testTryLock_ExclusiveDoesNotAllowShared_H2() {
    testTryLock_ExclusiveDoesNotAllowShared();
  }

  @Test
  public void testTryLock_ExclusiveDoesNotAllowShared_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testTryLock_ExclusiveDoesNotAllowShared();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testTryLock_ExclusiveDoesNotAllowShared() {
    String lockId = "test-lock";
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, false);
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, false);

    exclusive.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
    shared.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared.acquired);
    exclusive.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared.acquired);

    shared.allowClose();
    assertThat(exclusive.exception).isNull();
    assertThat(shared.exception).isNull();
  }

  @Test
  public void testTryLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed_H2() {
    testTryLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed();
  }

  @Test
  public void testTryLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testTryLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testTryLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed() {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, false);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared1.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared2.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }
}
