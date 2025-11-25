/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;

public abstract class AbstractClusterLockManager
    implements ClusterLockManager
{
  protected abstract ClusterLock createClusterLock(ClusterLockId clusterLockId);

  @Override
  public ClusterLock createForPolicyViolations(final Application application) {
    return createClusterLock(ClusterLockId.forPolicyViolations(application.getId()));
  }

  @Override
  public ClusterLock createForPolicyViolationAggregations(final String applicationId) {
    return createClusterLock(ClusterLockId.forPolicyViolationAggregations(applicationId));
  }

  @Override
  public ClusterLock createForRepositoryComponent(final String repositoryId, final String componentPathname) {
    return createClusterLock(ClusterLockId.forRepositoryComponent(repositoryId, componentPathname));
  }

  @Override
  public ClusterLock createForRepositoryReevaluation(final Repository repository) {
    return createClusterLock(ClusterLockId.forRepositoryReevaluation(repository.getId()));
  }

  @Override
  public ClusterLock createForPolicyEvaluation(final Application application, final String scanId) {
    return createClusterLock(ClusterLockId.forPolicyEvaluation(application.getId(), scanId));
  }

  @Override
  public ClusterLock createForAuditJsonFileStore(final String ownerId) {
    return createClusterLock(ClusterLockId.forAuditJsonFileStore(ownerId));
  }

  @Override
  public ClusterLock createForSchemaMigration() {
    return createClusterLock(ClusterLockId.forSchemaMigration());
  }

  @Override
  public ClusterLock createForDataMigration() {
    return createClusterLock(ClusterLockId.forDataMigration());
  }

  @Override
  public ClusterLock createForNewInstancePopulation() {
    return createClusterLock(ClusterLockId.forNewInstancePopulation());
  }

  @Override
  public ClusterLock createForPdfGeneration(final Application application, final String scanId) {
    return createClusterLock(ClusterLockId.forPdfGeneration(application.getId(), scanId));
  }

  @Override
  public ClusterLock createForInactiveRepositoryViolationCleaner() {
    return createClusterLock(ClusterLockId.forInactiveRepositoryViolationCleaner());
  }

  @Override
  public ClusterLock createForSupportZip() {
    return createClusterLock(ClusterLockId.forSupportZip());
  }

  @Override
  public ClusterLock createForSearchIndexUpdate() {
    return createClusterLock(ClusterLockId.forSearchIndexUpdate());
  }
}
