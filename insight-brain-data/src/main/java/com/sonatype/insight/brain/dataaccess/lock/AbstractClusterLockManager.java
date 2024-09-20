/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

public abstract class AbstractClusterLockManager
    implements ClusterLockManager
{
  protected abstract ClusterLock createClusterLock(String lockId);

  protected abstract void deleteLock(TransactionContext tx, String lockId);

  protected abstract void deleteLocksByPrefix(TransactionContext tx, String prefix);

  protected abstract void deleteFor(String lockId);

  @Override
  public ClusterLock createForPolicyViolations(final Application application) {
    return createClusterLock(ClusterLockManager.getLockIdForPolicyViolations(application));
  }

  @Override
  public void deleteForPolicyViolations(final TransactionContext tx, final Application application) {
    deleteLock(tx, ClusterLockManager.getLockIdForPolicyViolations(application));
  }

  @Override
  public ClusterLock createForPolicyViolationAggregations(final String applicationId) {
    return createClusterLock(ClusterLockManager.getLockIdForPolicyViolationAggregations(applicationId));
  }

  @Override
  public void deleteForPolicyViolationAggregations(final TransactionContext tx, final String applicationId) {
    deleteLock(tx, ClusterLockManager.getLockIdForPolicyViolationAggregations(applicationId));
  }

  @Override
  public ClusterLock createForRepositoryComponent(final String repositoryId, final String componentPathname) {
    return createClusterLock(ClusterLockManager.getLockIdForRepositoryComponent(repositoryId, componentPathname));
  }

  @Override
  public void deleteForRepositoryComponent(
      final TransactionContext tx,
      final String repositoryId,
      final String componentPathname)
  {
    deleteLock(tx, ClusterLockManager.getLockIdForRepositoryComponent(repositoryId, componentPathname));
  }

  @Override
  public void deleteForRepository(final TransactionContext tx, final String repositoryId) {
    deleteLocksByPrefix(tx, ClusterLockManager.getLockIdForRepositoryComponent(repositoryId, ""));
  }

  @Override
  public ClusterLock createForRepositoryReevaluation(final Repository repository) {
    return createClusterLock(ClusterLockManager.getLockIdForRepositoryReevaluation(repository));
  }

  @Override
  public void deleteForRepositoryReevaluation(final TransactionContext tx, final Repository repository) {
    deleteLock(tx, ClusterLockManager.getLockIdForRepositoryReevaluation(repository));
  }

  @Override
  public ClusterLock createForPolicyEvaluation(final Application application, final String scanId) {
    return createClusterLock(ClusterLockManager.getLockIdForPolicyEvaluation(application, scanId));
  }

  @Override
  public void deleteForPolicyEvaluation(
      final TransactionContext tx,
      final Application application,
      final String scanId)
  {
    deleteLock(tx, ClusterLockManager.getLockIdForPolicyEvaluation(application, scanId));
  }

  @Override
  public void deleteForPolicyEvaluations(final TransactionContext tx, final Application application) {
    deleteLocksByPrefix(tx, ClusterLockManager.getLockIdForPolicyEvaluation(application, ""));
  }

  @Override
  public ClusterLock createForAuditJsonFileStore(final String ownerId) {
    return createClusterLock(ClusterLockManager.getLockIdForAuditJsonFileStore(ownerId));
  }

  @Override
  public void deleteForAuditJsonFileStore(final TransactionContext tx, final String ownerId) {
    deleteLock(tx, ClusterLockManager.getLockIdForAuditJsonFileStore(ownerId));
  }

  @Override
  public ClusterLock createForSchemaMigration() {
    return createClusterLock(ClusterLockManager.getLockIdForSchemaMigration());
  }

  @Override
  public void deleteForSchemaMigration() {
    deleteFor(ClusterLockManager.getLockIdForSchemaMigration());
  }

  @Override
  public ClusterLock createForSchemaMigrationInProgress() {
    return createClusterLock(ClusterLockManager.getLockIdForSchemaMigrationInProgress());
  }

  @Override
  public void deleteForSchemaMigrationInProgress() {
    deleteFor(ClusterLockManager.getLockIdForSchemaMigrationInProgress());
  }

  @Override
  public ClusterLock createForDataMigration() {
    return createClusterLock(ClusterLockManager.getLockIdForDataMigration());
  }

  @Override
  public void deleteForDataMigration() {
    deleteFor(ClusterLockManager.getLockIdForDataMigration());
  }

  @Override
  public ClusterLock createForAsyncDbMigration(final String jobName) {
    return createClusterLock(ClusterLockManager.getLockIdForAsyncDbMigration(jobName));
  }

  @Override
  public void deleteForAsyncDbMigration(final String jobName) {
    deleteFor(ClusterLockManager.getLockIdForAsyncDbMigration(jobName));
  }

  @Override
  public ClusterLock createForNewInstancePopulation() {
    return createClusterLock(ClusterLockManager.getLockIdForNewInstancePopulation());
  }

  @Override
  public void deleteForNewInstancePopulation() {
    deleteFor(ClusterLockManager.getLockIdForNewInstancePopulation());
  }

  @Override
  public ClusterLock createForPdfGeneration(final Application application, final String scanId) {
    return createClusterLock(ClusterLockManager.getLockIdForPdfGeneration(application, scanId));
  }

  @Override
  public void deleteForPdfGeneration(final TransactionContext tx, final Application application) {
    deleteLocksByPrefix(tx, ClusterLockManager.getLockIdForPdfGeneration(application, ""));
  }

  @Override
  public ClusterLock createForInactiveRepositoryViolationCleaner() {
    return createClusterLock(ClusterLockManager.getLockIdForInactiveRepositoryViolationCleaner());
  }

  @Override
  public void deleteForInactiveRepositoryViolationCleaner() {
    deleteFor(ClusterLockManager.getLockIdForInactiveRepositoryViolationCleaner());
  }

  @Override
  public ClusterLock createForFilename(final String filename) {
    return createClusterLock(ClusterLockManager.getLockIdForFilename(filename));
  }

  @Override
  public void deleteForFilename(final String filename) {
    deleteFor(ClusterLockManager.getLockIdForFilename(filename));
  }
}
