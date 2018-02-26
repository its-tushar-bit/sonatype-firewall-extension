/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AggregationDataStoreProviderTest
    extends AbstractDatabaseProviderTest
{
  @Override
  protected DatabaseConfig getDatabaseConfig() {
    return AggregationDataStoreProvider.getDatabaseConfig();
  }

  @Override
  protected void initDatabase(DatabaseConfig databaseConfig) {
    AggregationDataStoreProvider.init(databaseConfig);
  }

  @Override
  protected DataSource getDataSource() {
    return AggregationDataStoreProvider.getDataSource();
  }

  @Test
  public void testInit_Migrate() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "aggregation");
    assertThat(databaseVersionFile.isFile(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile), is("1"));

    initDatabase(getDatabaseConfig(databaseDir, "aggregation"));

    int desiredDbVersion = H2DatabaseMigrator.determineDesiredVersion(AggregationDataStoreProvider.ID);
    assertThat(readDatabaseVersion(databaseVersionFile), is(String.valueOf(desiredDbVersion)));
  }
}
