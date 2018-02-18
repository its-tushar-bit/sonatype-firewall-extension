/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class H2DatabaseMigratorTest
    extends AbstractDatabaseTest
{
  private String readDatabaseVersion(File versionFile) throws Exception {
    return FileUtils.fileRead(versionFile, "UTF-8");
  }

  @Test
  public void testMigrateOperationalDataStore() throws Exception {
    File databaseDir = tempDir.newFolder();
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertTrue(databaseVersionFile.exists());
    assertEquals("85", readDatabaseVersion(databaseVersionFile));

    OperationalDataStoreProvider.init(getDatabaseConfig(databaseDir, "ods"));
    assertEquals(String.valueOf(OperationalDataStoreProvider.DESIRED_DATABASE_VERSION),
        readDatabaseVersion(databaseVersionFile));
  }

  @Test
  public void testMigrateDatamart() throws Exception {
    File databaseDir = tempDir.newFolder();
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateDatamart"), databaseDir);
    File databaseVersionFile = new File(databaseDir, "dm.ver");
    assertTrue(databaseVersionFile.exists());
    assertEquals("1", readDatabaseVersion(databaseVersionFile));

    DatamartProvider.init(getDatabaseConfig(databaseDir, "dm"));
    assertEquals(String.valueOf(DatamartProvider.DESIRED_DATABASE_VERSION), readDatabaseVersion(databaseVersionFile));
  }

  @Test
  public void testMigrateAggregationDataStore() throws Exception {
    File databaseDir = tempDir.newFolder();
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateAggregationDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "aggregation.ver");
    assertTrue(databaseVersionFile.exists());
    assertEquals("1", FileUtils.fileRead(databaseVersionFile));

    AggregationDataStoreProvider.init(getDatabaseConfig(databaseDir, "aggregation"));
    assertEquals(String.valueOf(AggregationDataStoreProvider.DESIRED_DATABASE_VERSION),
        FileUtils.fileRead(databaseVersionFile));
  }

  @Test
  public void testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils
        .copyDirectory(
            new File(
                "target/test-classes/H2DatabaseMigratorTest/testMigrate_VersionFileUpdatedWhenMigrationFailsAfterAtLeastOneSuccessfulScript"),
            databaseDir);
    File databaseVersionFile = new File(databaseDir, "dm.ver");
    assertThat(databaseVersionFile.isFile(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile), is("3"));

    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "dm");

    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, DatamartProvider.ID);

    // The migration should fail because there is no incremental script for targetDatabaseVersion,
    // but the version file must be updated to contain the number of the last incremental script applied successfully.
    int targetDatabaseVersion = DatamartProvider.DESIRED_DATABASE_VERSION + 1;
    try {
      new H2DatabaseMigrator()
          .migrate(databaseConfig, DatamartProvider.ID, dataSource, targetDatabaseVersion, 1 /* defaultCurrentVersion */);
      fail("Expected exception");
    }
    catch (CannotReadScriptException expected) {
      assertThat(readDatabaseVersion(databaseVersionFile), is(String.valueOf(DatamartProvider.DESIRED_DATABASE_VERSION)));
    }
  }

  @Test
  public void testMigrate_CurrentVersionLessThanMinimumVersion() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertThat(databaseVersionFile.exists(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile), is("85"));
    DatabaseConfig databaseConfig = getODSDatabaseConfig(databaseDir);
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStoreProvider.ID);
    try {
      new H2DatabaseMigrator().migrate(databaseConfig, OperationalDataStoreProvider.ID, dataSource, 86, 87, 1);
      fail("Expected exception");
    }
    catch (UnsupportedOperationException e) {
      assertThat(e.getMessage(), is("Cannot migrate insight_brain_ods database to version 87, this requires " +
          "version 86 at minimum, but you have version 85." +
          "\nPlease upgrade to Nexus IQ Server version 1.16 before upgrading to this version."));
    }
  }

  @Test
  public void testMigrate_CurrentVersionEqualToMinimumVersion() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertThat(databaseVersionFile.exists(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile), is("85"));
    DatabaseConfig databaseConfig = getODSDatabaseConfig(databaseDir);
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStoreProvider.ID);
    new H2DatabaseMigrator().migrate(databaseConfig, OperationalDataStoreProvider.ID, dataSource, 85, 87, 1);
  }

  @Test
  public void testMigrate_CurrentVersionGreaterThanMinimumVersion() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertThat(databaseVersionFile.exists(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile), is("85"));
    DatabaseConfig databaseConfig = getODSDatabaseConfig(databaseDir);
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, OperationalDataStoreProvider.ID);
    new H2DatabaseMigrator().migrate(databaseConfig, OperationalDataStoreProvider.ID, dataSource, 84, 87, 1);
  }

  @Test
  public void testMigrate_OperationalDataStore_RunsPostIncrementalMigrators() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyDirectory(new File("target/test-classes/H2DatabaseMigratorTest/testMigrateOperationalDataStore"),
        databaseDir);
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    assertThat(databaseVersionFile.exists(), is(true));
    assertThat(readDatabaseVersion(databaseVersionFile), is("85"));
    int desiredVersion = 87;

    OperationalDataStoreProvider.init(getODSDatabaseConfig(databaseDir), true, desiredVersion);

    assertThat(readDatabaseVersion(databaseVersionFile), is(String.valueOf(desiredVersion)));
    assertThat(PostIncrementalMigratorVersionMinus1.invoked, is(false));
    assertThat(PostIncrementalMigratorVersion.invoked, is(false));
    assertThat(PostIncrementalMigratorVersionPlus1.invoked, is(true));
    assertThat(PostIncrementalMigratorVersionDesired.invoked, is(true));
    assertThat(PostIncrementalMigratorVersionDesiredPlus1.invoked, is(false));
  }

  @Test
  public void testMigrate_OperationalDataStore_ThrowsExceptionDuringExecute() {
    try {
      new H2DatabaseMigrator().runPostIncrementalMigrator("/H2DatabaseMigratorTest/" +
              "testMigrate_OperationalDataStore_ThrowsExecuteExceptionMessage/schema_incremental_0089.cls",
          mock(DataSource.class));
      fail("Expected exception");
    }
    catch (RuntimeException e) {
      assertThat(e.getMessage(), is("Failed to execute the PostIncrementalMigrator referenced in " +
          "/H2DatabaseMigratorTest/testMigrate_OperationalDataStore_ThrowsExecuteExceptionMessage/" +
          "schema_incremental_0089.cls."));
    }
  }

  @Test
  public void testMigrate_OperationalDataStore_ThrowsExceptionDuringLoad() {
    try {
      new H2DatabaseMigrator().runPostIncrementalMigrator("/H2DatabaseMigratorTest/" +
          "testMigrate_OperationalDataStore_ThrowsLoadExceptionMessage/schema_incremental_0090.cls", null);
      fail("Expected exception");
    }
    catch (RuntimeException e) {
      assertThat(e.getMessage(), is("Failed to execute the PostIncrementalMigrator referenced in " +
          "/H2DatabaseMigratorTest/testMigrate_OperationalDataStore_ThrowsLoadExceptionMessage/" +
          "schema_incremental_0090.cls."));
    }
  }

  private DatabaseConfig getODSDatabaseConfig(File databaseDir) {
    return getDatabaseConfig(databaseDir, "ods");
  }

  static class PostIncrementalMigratorVersionMinus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersion
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionPlus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionDesired
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorVersionDesiredPlus1
      implements PostIncrementalMigrator
  {
    static boolean invoked;

    @Override
    public void migrate(DataSource dataSource) {
      invoked = true;
    }
  }

  static class PostIncrementalMigratorFail
      implements PostIncrementalMigrator
  {
    @Override
    public void migrate(DataSource dataSource) throws Exception {
      throw new Exception();
    }
  }
}
