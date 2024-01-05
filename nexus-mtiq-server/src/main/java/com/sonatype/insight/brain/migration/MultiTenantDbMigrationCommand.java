/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

public class MultiTenantDbMigrationCommand
    extends DbMigrationCommand
{
  public MultiTenantDbMigrationCommand() {
    super();
  }

  @Override
  protected boolean quartzSchedulerStateTableExists(final OperationalDataStore operationalDataStore) {
    // Multi-tenant quartz is in the global table
    return DatabaseUtil.tableExists(operationalDataStore.getDataSource(), "global", "qrtz_scheduler_state");
  }
}
