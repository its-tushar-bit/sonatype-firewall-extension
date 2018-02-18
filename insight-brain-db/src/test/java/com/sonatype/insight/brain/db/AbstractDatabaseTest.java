/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractDatabaseTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Before
  @After
  public void clearDataSources() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  protected DatabaseConfig getDatabaseConfig(File databaseDir, String databaseName) {
    File databasePath = new File(databaseDir, databaseName);
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUrl(
        "jdbc:h2:" + databasePath.getAbsolutePath() + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }
}
