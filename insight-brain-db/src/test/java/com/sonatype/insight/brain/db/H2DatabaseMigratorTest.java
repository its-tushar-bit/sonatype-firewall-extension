/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.sonatype.insight.db.DatabaseConfig;

public class H2DatabaseMigratorTest
{
  @After
  public void cleanup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testMigrateOperationalDataStore() throws Exception {
    File databaseDir = new File("target/H2DatabaseMigratorTest/testMigrateOperationalDataStore");
    FileUtils.deleteDirectory(databaseDir);
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertTrue(databaseVersionFile.exists());
    assertEquals("6", FileUtils.fileRead(databaseVersionFile));

    DatabaseConfig odsDatabaseConfig = new DatabaseConfig();
    odsDatabaseConfig.setDriverClassName("org.h2.Driver");
    odsDatabaseConfig
        .setUrl("jdbc:h2:target/H2DatabaseMigratorTest/testMigrateOperationalDataStore/ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    odsDatabaseConfig.setUsername("sa");
    odsDatabaseConfig.setPassword("");
    odsDatabaseConfig.setMaxConnections(50);
    OperationalDataStoreProvider.init(odsDatabaseConfig);
    assertEquals(String.valueOf(OperationalDataStoreProvider.DESIRED_DATABASE_VERSION),
        FileUtils.fileRead(databaseVersionFile));
  }

  @Test
  public void testMigrateDatamart() throws Exception {
    File databaseDir = new File("target/H2DatabaseMigratorTest/testMigrateDatamart");
    FileUtils.deleteDirectory(databaseDir);
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateDatamart"), databaseDir);
    File databaseVersionFile = new File(databaseDir, "dm.ver");
    assertTrue(databaseVersionFile.exists());
    assertEquals("1", FileUtils.fileRead(databaseVersionFile));

    DatabaseConfig dmDatabaseConfig = new DatabaseConfig();
    dmDatabaseConfig.setDriverClassName("org.h2.Driver");
    dmDatabaseConfig
        .setUrl("jdbc:h2:target/H2DatabaseMigratorTest/testMigrateDatamart/dm;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    dmDatabaseConfig.setUsername("sa");
    dmDatabaseConfig.setPassword("");
    dmDatabaseConfig.setMaxConnections(50);
    DatamartProvider.init(dmDatabaseConfig);
    assertEquals(String.valueOf(DatamartProvider.DESIRED_DATABASE_VERSION), FileUtils.fileRead(databaseVersionFile));
  }
}
