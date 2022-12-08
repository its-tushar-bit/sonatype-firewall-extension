/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

public class MultiTenantDbMigrationCommand
    extends DbMigrationCommand
{
  public MultiTenantDbMigrationCommand(final DatabaseProvisionUtils databaseProvisionUtils) {
    super(databaseProvisionUtils);
  }

  @Override
  protected boolean quartzSchedulerStateTableExists() {
    // Multi-tenant quartz is in the global table
    return DatabaseUtil.tableExists(OperationalDataStoreProvider.getDataSource(), "global", "qrtz_scheduler_state");
  }
}
