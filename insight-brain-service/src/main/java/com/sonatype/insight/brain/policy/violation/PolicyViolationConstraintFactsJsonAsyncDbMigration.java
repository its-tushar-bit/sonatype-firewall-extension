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
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;

import org.quartz.DisallowConcurrentExecution;

@Named
@DisallowConcurrentExecution
@Singleton
public class PolicyViolationConstraintFactsJsonAsyncDbMigration
    extends AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration<PolicyViolation>
{
  private final PolicyViolationDAO dao;

  @Inject
  public PolicyViolationConstraintFactsJsonAsyncDbMigration(
      final TaskScheduler taskScheduler,
      final PolicyViolationDAO dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final InsightConfig insightConfig,
      final ClusterLockManager clusterLockManager)
  {
    super("PolicyViolationConstraintJsonAsyncDbMigration", taskScheduler,
        dao, migrationTrackerDAO, "policy violations",
        insightConfig, clusterLockManager);
    this.dao = dao;
  }
}
