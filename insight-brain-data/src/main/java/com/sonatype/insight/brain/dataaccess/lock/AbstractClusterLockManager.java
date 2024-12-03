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
  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  protected abstract void deleteLock(TransactionContext tx, ClusterLockId clusterLockId);

  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  protected abstract void deleteLocksByPrefix(TransactionContext tx, String prefix);

  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  protected abstract void deleteFor(ClusterLockId clusterLockId);

  protected abstract ClusterLock createClusterLock(ClusterLockId clusterLockId);

  @Override
  public ClusterLock createForPolicyViolations(final Application application) {
    return createClusterLock(ClusterLockId.forPolicyViolations(application.getId()));
  }

  @Override
  public void deleteForPolicyViolations(final TransactionContext tx, final Application application) {
    deleteLock(tx, ClusterLockId.forPolicyViolations(application.getId()));
  }

  @Override
  public ClusterLock createForPolicyViolationAggregations(final String applicationId) {
    return createClusterLock(ClusterLockId.forPolicyViolationAggregations(applicationId));
  }

  @Override
  public void deleteForPolicyViolationAggregations(final TransactionContext tx, final String applicationId) {
    deleteLock(tx, ClusterLockId.forPolicyViolationAggregations(applicationId));
  }

  @Override
  public ClusterLock createForRepositoryComponent(final String repositoryId, final String componentPathname) {
    return createClusterLock(ClusterLockId.forRepositoryComponent(repositoryId, componentPathname));
  }

  @Override
  public void deleteForRepositoryComponent(
      final TransactionContext tx,
      final String repositoryId,
      final String componentPathname)
  {
    deleteLock(tx, ClusterLockId.forRepositoryComponent(repositoryId, componentPathname));
  }

  @Override
  public void deleteForRepository(final TransactionContext tx, final String repositoryId) {
    deleteLocksByPrefix(tx, ClusterLockId.prefixForRepositoryComponents(repositoryId));
  }

  @Override
  public ClusterLock createForRepositoryReevaluation(final Repository repository) {
    return createClusterLock(ClusterLockId.forRepositoryReevaluation(repository.getId()));
  }

  @Override
  public void deleteForRepositoryReevaluation(final TransactionContext tx, final Repository repository) {
    deleteLock(tx, ClusterLockId.forRepositoryReevaluation(repository.getId()));
  }

  @Override
  public ClusterLock createForPolicyEvaluation(final Application application, final String scanId) {
    return createClusterLock(ClusterLockId.forPolicyEvaluation(application.getId(), scanId));
  }

  @Override
  public void deleteForPolicyEvaluation(
      final TransactionContext tx,
      final Application application,
      final String scanId)
  {
    deleteLock(tx, ClusterLockId.forPolicyEvaluation(application.getId(), scanId));
  }

  @Override
  public void deleteForPolicyEvaluations(final TransactionContext tx, final Application application) {
    deleteLocksByPrefix(tx, ClusterLockId.prefixForPolicyEvaluations(application.getId()));
  }

  @Override
  public ClusterLock createForAuditJsonFileStore(final String ownerId) {
    return createClusterLock(ClusterLockId.forAuditJsonFileStore(ownerId));
  }

  @Override
  public void deleteForAuditJsonFileStore(final TransactionContext tx, final String ownerId) {
    deleteLock(tx, ClusterLockId.forAuditJsonFileStore(ownerId));
  }

  @Override
  public ClusterLock createForSchemaMigration() {
    return createClusterLock(ClusterLockId.forSchemaMigration());
  }

  @Override
  public void deleteForSchemaMigration() {
    deleteFor(ClusterLockId.forSchemaMigration());
  }

  @Override
  public ClusterLock createForSchemaMigrationInProgress() {
    return createClusterLock(ClusterLockId.forSchemaMigrationInProgress());
  }

  @Override
  public void deleteForSchemaMigrationInProgress() {
    deleteFor(ClusterLockId.forSchemaMigrationInProgress());
  }

  @Override
  public ClusterLock createForDataMigration() {
    return createClusterLock(ClusterLockId.forDataMigration());
  }

  @Override
  public void deleteForDataMigration() {
    deleteFor(ClusterLockId.forDataMigration());
  }

  @Override
  public ClusterLock createForNewInstancePopulation() {
    return createClusterLock(ClusterLockId.forNewInstancePopulation());
  }

  @Override
  public void deleteForNewInstancePopulation() {
    deleteFor(ClusterLockId.forNewInstancePopulation());
  }

  @Override
  public ClusterLock createForPdfGeneration(final Application application, final String scanId) {
    return createClusterLock(ClusterLockId.forPdfGeneration(application.getId(), scanId));
  }

  @Override
  public void deleteForPdfGeneration(final TransactionContext tx, final Application application) {
    deleteLocksByPrefix(tx, ClusterLockId.prefixForPdfGeneration(application.getId()));
  }

  @Override
  public ClusterLock createForInactiveRepositoryViolationCleaner() {
    return createClusterLock(ClusterLockId.forInactiveRepositoryViolationCleaner());
  }

  @Override
  public void deleteForInactiveRepositoryViolationCleaner() {
    deleteFor(ClusterLockId.forInactiveRepositoryViolationCleaner());
  }
}
