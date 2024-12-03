/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Entry for the cluster lock mechanism. Provides accessors for easy use of the locking system.
 */
public interface ClusterLockManager
{
  boolean lockExists(ClusterLockId clusterLockId);

  ClusterLock createForPolicyViolations(Application application);

  void deleteForPolicyViolations(TransactionContext tx, Application application);

  ClusterLock createForPolicyViolationAggregations(String applicationId);

  void deleteForPolicyViolationAggregations(TransactionContext tx, String applicationId);

  ClusterLock createForRepositoryComponent(String repositoryId, String componentPathname);

  void deleteForRepositoryComponent(TransactionContext tx, String repositoryId, String componentPathname);

  void deleteForRepository(TransactionContext tx, String repositoryId);

  ClusterLock createForRepositoryReevaluation(Repository repository);

  void deleteForRepositoryReevaluation(TransactionContext tx, Repository repository);

  ClusterLock createForPolicyEvaluation(Application application, String scanId);

  void deleteForPolicyEvaluation(TransactionContext tx, Application application, String scanId);

  void deleteForPolicyEvaluations(TransactionContext tx, Application application);

  ClusterLock createForAuditJsonFileStore(String ownerId);

  void deleteForAuditJsonFileStore(TransactionContext tx, String ownerId);

  ClusterLock createForSchemaMigration();

  void deleteForSchemaMigration();

  ClusterLock createForSchemaMigrationInProgress();

  void deleteForSchemaMigrationInProgress();

  ClusterLock createForDataMigration();

  void deleteForDataMigration();

  ClusterLock createForNewInstancePopulation();

  void deleteForNewInstancePopulation();

  ClusterLock createForPdfGeneration(Application application, String scanId);

  void deleteForPdfGeneration(TransactionContext tx, Application application);

  ClusterLock createForInactiveRepositoryViolationCleaner();

  void deleteForInactiveRepositoryViolationCleaner();
}
