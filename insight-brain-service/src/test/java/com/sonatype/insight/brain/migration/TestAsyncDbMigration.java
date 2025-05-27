/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

class TestAsyncDbMigration<T extends HasStringId>
    extends AsyncDbMigration<T>
{
  private final String migrationName;

  private final int migrationPriority;

  protected TestAsyncDbMigration(
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final InsightConfig config,
      final String migrationName,
      final int migrationPriority)
  {
    super(dao, migrationTrackerDAO, type, config);
    this.migrationName = migrationName;
    this.migrationPriority = migrationPriority;
  }

  protected TestAsyncDbMigration(
      final AbstractSqlDAO<T> dao,
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type,
      final int pageSize,
      final String migrationName,
      final int migrationPriority)
  {
    super(dao, migrationTrackerDAO, type, pageSize);
    this.migrationName = migrationName;
    this.migrationPriority = migrationPriority;
  }

  @Override
  protected void migrate(
      final AbstractSqlDAO<T> dao,
      final T entity,
      final TransactionContext tx)
  {
    // No-op, just to test the migration with the spy
  }

  @Override
  public int migrationPriority() {
    return migrationPriority;
  }

  @Override
  public String getMigrationName() {
    return migrationName;
  }
}
