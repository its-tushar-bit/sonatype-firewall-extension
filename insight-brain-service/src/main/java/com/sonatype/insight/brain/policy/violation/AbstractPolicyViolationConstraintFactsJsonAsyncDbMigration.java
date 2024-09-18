/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.AbstractPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.migration.AsyncDbMigration;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Moves the constraints facts JSON out of the given table, deduplicates it and stores it in the constraints table. This
 * is done to reduce the size of the given table, to allow for more efficient querying and to reduce the amount of
 * memory needed to load violations.
 * -
 * This is used for policy_violations and repository_policy_violations.
 */
public abstract class AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration<T extends AbstractPolicyViolation>
    extends AsyncDbMigration<T>
{
  private final Cache<String, String> hashCache = CacheBuilder.newBuilder()
      .maximumSize(10_000)
      .expireAfterWrite(100, TimeUnit.MINUTES)
      .build();

  private final PolicyViolationConstraintFactsDAO constraintsDAO;

  protected AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration(
      final String name,
      final TaskScheduler taskScheduler,
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final PolicyViolationConstraintFactsDAO constraintsDAO,
      final InsightConfig config,
      final ClusterLockManager clusterLockManager)
  {
    super(name, taskScheduler, dao, migrationTrackerDAO, type, config, clusterLockManager);
    this.constraintsDAO = constraintsDAO;
  }

  @Override
  protected void migrate(
      final AbstractSqlDAO<T> dao,
      final T violation,
      final TransactionContext tx)
  {
    String constraintFactsJson = violation.getConstraintFactsJsonWithoutLoading();
    if (constraintFactsJson != null) {
      String hash = AbstractPolicyViolation.calculateConstraintFactsId(constraintFactsJson);

      if (hashCache.getIfPresent(hash) == null) {
        constraintsDAO.createIfNotExists(tx, constraintFactsJson);
        hashCache.put(hash, hash);
      }

      violation.setConstraintFactsId(hash);
      violation.clearConstraintFactsJson();

      dao.update(tx, violation);
    }
  }

  @Override
  protected boolean validateFinished(final AbstractSqlDAO<T> dao, final long processed, final long rows) {
    if (dao instanceof AbstractPolicyViolationDAO) {
      long count = ((AbstractPolicyViolationDAO) dao).getCountWhereConstraintFactsJsonNotNull();

      return count == 0;
    }
    else {
      return super.validateFinished(dao, processed, rows);
    }
  }

  @Override
  protected void onCompletion() {
    hashCache.invalidateAll();
  }

  @Override
  public String getJobName() {
    return getClass().getSimpleName();
  }
}
