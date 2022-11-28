/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultOperationalDataStoreTest
    extends AbstractDataStoreTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(OperationalDataStoreProvider.class);

  @Override
  protected DataStore createTestDataStore() {
    return new DefaultOperationalDataStore();
  }

  @Test
  public void testInit_Migrate() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = getDatabaseVersionFile(databaseDir, "ods");
    assertThat(databaseVersionFile).isFile();
    assertThat(readDatabaseVersion(databaseVersionFile))
        .isEqualTo(String.valueOf(DefaultOperationalDataStore.MINIMUM_DATABASE_VERSION));

    initDatabase(getDatabaseConfig(databaseDir, "ods"));

    assertThat(databaseVersionFile).doesNotExist();
    int desiredDbVersion = DatabaseMigrator.determineDesiredVersion(OperationalDataStore.ID);
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataStore.getDataSource(), dataStore.getID()))
        .isEqualTo(desiredDbVersion);
  }

  @Test
  public void testInit_Migrate_CurrentVersionLessThanMinimumVersion() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/Migrate");
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    String oldVersion = String.valueOf(DefaultOperationalDataStore.MINIMUM_DATABASE_VERSION - 1);
    Files.write(databaseVersionFile.toPath(), oldVersion.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> {
      initDatabase(getDatabaseConfig(databaseDir, "ods"));
    }).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Cannot migrate insight_brain_ods database, this requires version "
            + DefaultOperationalDataStore.MINIMUM_DATABASE_VERSION + " at minimum, but you have version " + oldVersion
            + ".\nPlease upgrade to Nexus IQ Server version 1.16 before upgrading to this version.");
  }

  @Test
  public void testInit_Migrate_NoConsentForNewViolationModel_NotYetMigratedToNewModel() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/MigrateNewViolationModel");
    File databaseVersionFile = new File(databaseDir, "ods.ver");
    String oldVersion = String.valueOf(DefaultOperationalDataStore.OLD_VIOLATION_MODEL_DATABASE_VERSION);
    Files.write(databaseVersionFile.toPath(), oldVersion.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> {
      dataStore.initWithMigration(getDatabaseConfig(databaseDir, "ods"), false);
    }).isInstanceOf(UnsupportedOperationException.class).hasMessage("Consent to upgrade has not been given.");
    assertThat(logOutput).atErrorLevel().contains("Upgrade requires consent to proceed")
        .contains("https://links.sonatype.com/products/clm/doc/upgrade/1.45");
  }

  @Test
  public void testInit_Migrate_NoConsentForNewViolationModel_AlreadyMigratedToNewModel() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/MigrateNewViolationModel");

    dataStore.initWithMigration(getDatabaseConfig(databaseDir, "ods"), false);

    int desiredDbVersion = DatabaseMigrator.determineDesiredVersion(OperationalDataStore.ID);
    assertThat(DatabaseUtil
        .getDatabaseSchemaVersion(dataStore.getDataSource(), OperationalDataStore.ID))
        .isEqualTo(desiredDbVersion);
  }
}
