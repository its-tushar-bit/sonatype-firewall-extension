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

import static org.assertj.core.api.Assertions.assertThat;

public class DatamartProviderTest
    extends AbstractDatabaseProviderTest
{
  @Override
  protected DatabaseConfig getDatabaseConfig() {
    return DatamartProvider.getDatabaseConfig();
  }

  @Override
  protected void initDatabase(DatabaseConfig databaseConfig) {
    DatamartProvider.init(databaseConfig);
  }

  @Override
  protected DataSource getDataSource() {
    return DatamartProvider.getDataSource();
  }

  @Override
  protected String getSchemaName() {
    return DatamartProvider.ID;
  }

  @Test
  public void testInit_Migrate() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "dm");
    assertThat(databaseVersionFile.isFile()).isTrue();
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo("1");

    initDatabase(getDatabaseConfig(databaseDir, "dm"));

    int desiredDbVersion = H2DatabaseMigrator.determineDesiredVersion(DatamartProvider.ID);
    assertThat(readDatabaseVersion(databaseVersionFile)).isEqualTo(String.valueOf(desiredDbVersion));
  }
}
