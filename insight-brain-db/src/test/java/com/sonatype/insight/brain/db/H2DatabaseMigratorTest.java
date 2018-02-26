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
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class H2DatabaseMigratorTest
    extends AbstractDatabaseTest
{
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

    // The migration should fail because schema_incremental_0007.sql drops the license_category table, but we already
    // removed this table.
    // The version file must be updated to contain the number of the last incremental script applied successfully (in
    // this case, schema_incremental_0006.sql).
    try {
      new H2DatabaseMigrator().migrate(databaseConfig, DatamartProvider.ID, dataSource);
      fail("Expected exception");
    }
    catch (ScriptStatementFailedException expected) {
      assertThat(readDatabaseVersion(databaseVersionFile), is("6"));
    }
  }

  @Test
  public void testMigrate_RunsPostIncrementalMigrators() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir,  getClass().getSimpleName() + "/PostIncrementalMigrator");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "test");
    int desiredVersion = 12;
    assertThat(readDatabaseVersion(databaseVersionFile), is(String.valueOf(desiredVersion - 2)));
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, "PostIncrementalMigrator");

    new H2DatabaseMigrator().migrate(databaseConfig, "PostIncrementalMigrator", dataSource);

    assertThat(readDatabaseVersion(databaseVersionFile), is(String.valueOf(desiredVersion)));
    assertThat(PostIncrementalMigratorVersionMinus1.invoked, is(false));
    assertThat(PostIncrementalMigratorVersion.invoked, is(false));
    assertThat(PostIncrementalMigratorVersionPlus1.invoked, is(true));
    assertThat(PostIncrementalMigratorVersionDesired.invoked, is(true));
    assertThat(PostIncrementalMigratorVersionDesiredPlus1.invoked, is(false));
  }

  @Test
  public void testMigrate_MissingVersionFile() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/MissingVersionFile");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "test");
    assertThat(databaseVersionFile.exists(), is(false));
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "test");
    DataSource dataSource = new DataSourceFactory().newDataSource(databaseConfig, "MissingVersionFile");

    try {
      new H2DatabaseMigrator().migrate(databaseConfig, "MissingVersionFile", dataSource);
      fail("Expected exception");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(), is("Missing the database version file " + databaseVersionFile + "."));
    }
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

  @Test
  public void testDetermineDesiredVersion() {
    assertThat(H2DatabaseMigrator.determineDesiredVersion("DetermineDesiredVersion"), is(12));
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
