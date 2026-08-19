/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.SQLException;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

public class MultiTenantQuartzConnectionProvider
    extends QuartzConnectionProvider
{
  public MultiTenantQuartzConnectionProvider(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return TenantThreadLocal.runAsGlobal(() -> {
      try {
        return super.getConnection();
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
