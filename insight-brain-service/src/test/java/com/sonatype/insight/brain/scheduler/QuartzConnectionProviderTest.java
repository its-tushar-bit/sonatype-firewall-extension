/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuartzConnectionProviderTest
{
  @Test
  public void testGetConnection() throws Exception {
    try (Connection connection = new QuartzConnectionProvider().getConnection()) {
      assertThat(connection.getSchema()).isEqualTo(OperationalDataStore.ID);
    }
  }
}
