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

public class ThirdPartyScansProviderTest
    extends AbstractDatabaseProviderTest
{
  @Override
  protected DatabaseConfig getDatabaseConfig() {
    return ThirdPartyScansProvider.getDatabaseConfig();
  }

  @Override
  protected void initDatabase(DatabaseConfig databaseConfig) {
    ThirdPartyScansProvider.init(databaseConfig);
  }

  @Override
  protected DataSource getDataSource() {
    return ThirdPartyScansProvider.getDataSource();
  }

  @Override
  protected String getSchemaName() {
    return ThirdPartyScansProvider.ID;
  }

  @Test
  public void testInit_Migrate() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");

    initDatabase(getDatabaseConfig(databaseDir, "third_party_scans"));

    int desiredDbVersion = DatabaseMigrator.determineDesiredVersion(ThirdPartyScansProvider.ID);
    assertThat(
        DatabaseUtil.getDatabaseSchemaVersion(ThirdPartyScansProvider.getDataSource(), ThirdPartyScansProvider.ID))
        .isEqualTo(desiredDbVersion);
  }
}
