/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;

/**
 * Entry for the cluster lock mechanism. Provides accessors for easy use of the locking system.
 */
public interface ClusterLockManager
{
  ClusterLock createForPolicyViolations(Owner owner);

  ClusterLock createForPolicyViolationAggregations(String applicationId);

  ClusterLock createForRepositoryComponent(String repositoryId, String componentPathname);

  ClusterLock createForRepositoryReevaluation(Repository repository);

  ClusterLock createForPolicyEvaluation(Owner owner, String scanId);

  ClusterLock createForAuditJsonFileStore(String ownerId);

  ClusterLock createForSchemaMigration();

  ClusterLock createForDataMigration();

  ClusterLock createForNewInstancePopulation();

  ClusterLock createForPdfGeneration(Owner owner, String scanId);

  ClusterLock createForInactiveRepositoryViolationCleaner();

  ClusterLock createForSupportZip();

  ClusterLock createForSearchIndexUpdate();
}
