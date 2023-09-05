/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.fixture.h2;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.database.MtiqTempUtils;
import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.brain.database.datasource.H2DiskDataSourceProvider;
import com.sonatype.insight.brain.database.fixture.DatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DiskDatabaseFixture
    implements DatabaseFixture
{
  private static final Logger log = LoggerFactory.getLogger(H2DiskDatabaseFixture.class);

  private TemporaryFolder tempDir = new TemporaryFolder();

  private File tempFile;

  public H2DiskDatabaseFixture() {
    log.info("Creating new H2 disk test database '{}'");
    try {
      tempDir.create();
      tempFile = tempDir.newFile();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    tempFile.delete();
    tempDir.delete();
  }

  @Override
  public DatabaseConfig getDatabaseConfig() {
    final DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    MtiqTempUtils.logTodo("Ability to customize database/path (i.e. change testdb)");
    databaseConfig.setUrl("jdbc:h2:" + tempDir.getRoot() +
        "/testdb;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);

    return databaseConfig;
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return new H2DiskDataSourceProvider();
  }
}
