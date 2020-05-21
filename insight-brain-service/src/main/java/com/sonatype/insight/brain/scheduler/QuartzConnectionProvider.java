/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.SQLException;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;

import org.quartz.utils.ConnectionProvider;

public class QuartzConnectionProvider
    implements ConnectionProvider
{
  @Override
  public Connection getConnection() throws SQLException {
    return OperationalDataStoreProvider.getDataSource().getConnection();
  }

  @Override
  public void shutdown() {
    // noop
  }

  @Override
  public void initialize() {
    // noop
  }
}
