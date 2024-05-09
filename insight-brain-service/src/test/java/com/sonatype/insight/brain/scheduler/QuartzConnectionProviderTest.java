/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuartzConnectionProviderTest
    extends AbstractDatabaseTest
{
  @Test
  @Ignore // CLM-30374
  public void testGetConnection() throws Exception {
    try (Connection connection = new QuartzConnectionProvider(databaseRule.getOperationalDataStore()).getConnection()) {
      assertThat(connection.getSchema()).isEqualTo(OperationalDataStore.ID);
    }
  }
}
