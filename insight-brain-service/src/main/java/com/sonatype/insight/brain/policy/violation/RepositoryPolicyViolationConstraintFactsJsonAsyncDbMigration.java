/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;

import org.quartz.DisallowConcurrentExecution;

@Named
@DisallowConcurrentExecution
@Singleton
public class RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration
    extends AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration<RepositoryPolicyViolation>
{
  private final RepositoryPolicyViolationDAO dao;

  @Inject
  public RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration(
      final TaskScheduler taskScheduler,
      final RepositoryPolicyViolationDAO dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final InsightConfig config,
      final ClusterLockManager clusterLockManager)
  {
    super("RepositoryPolicyViolationConstraintJsonAsyncDbMigration", taskScheduler,
        dao, migrationTrackerDAO, "repository policy violations", config, clusterLockManager);
    this.dao = dao;
  }
}
