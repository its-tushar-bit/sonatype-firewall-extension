/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;

public class QuartzPostgreSQLDelegate
    extends PostgreSQLDelegate
{
  @Override
  public List<TriggerKey> selectTriggerToAcquire(
      Connection conn,
      long noLaterThan,
      long noEarlierThan,
      int maxCount) throws SQLException
  {
    return StdJDBCDelegateUtils.selectTriggerToAcquire(this, conn, super.selectTriggerToAcquire(conn, noLaterThan,
        noEarlierThan, maxCount), instanceId);
  }
}
