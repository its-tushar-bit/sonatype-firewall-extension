/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AbstractPolicyViolationDAO;
import com.sonatype.insight.brain.migration.AsyncDbMigration;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

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
  protected AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration(
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final InsightConfig config)
  {
    super(dao, migrationTrackerDAO, type, config);
  }

  @Override
  protected void migrate(
      final AbstractSqlDAO<T> dao,
      final T violation,
      final TransactionContext tx)
  {
    String constraintFactsJson = violation.getConstraintFactsJsonWithoutLoading();
    if (constraintFactsJson != null) {
      // AbstractPolicyViolationDAO automatically removes the JSON and sets the ID when policy violations are saved
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
}
