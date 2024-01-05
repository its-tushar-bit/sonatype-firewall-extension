/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.util.SortedMap;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresDatabaseEngineTest
    extends AbstractDatabaseTest
{
  @Test
  @PostgresTest
  public void testGetDatabaseSettings() throws Exception {
    SortedMap<String, String> databaseSettings;
    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection()) {
      databaseSettings = PostgresDatabaseEngine.INSTANCE.getDatabaseSettings(connection);
    }
    assertThat(databaseSettings).containsEntry("server_encoding", "UTF8");
  }
}
