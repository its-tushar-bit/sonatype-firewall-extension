/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.function.Function;
import java.util.function.IntConsumer;

import javax.sql.DataSource;

import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DbMigrationCommandTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private InsightConfig insightConfig;

  @Before
  public void before() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.newFolder().getAbsolutePath());
  }

  @After
  public void after() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testDbMigrationCommand() {
    DbMigrationCommand dbMigrationCommand = new DbMigrationCommand();

    assertThat(dbMigrationCommand.getName()).isEqualTo("migrate-db");
    assertThat(dbMigrationCommand.getDescription()).isEqualTo("Migrates the database to the latest schema version.");
    assertThat(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).isEqualTo(
        DbMigrationCommand.ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT).isEqualTo(1);
    assertThat(DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS).isEqualTo(
        (long) (QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS * 3.5));
  }

  @Test
  public void testRun_QuartzSchedulerStateTableDoesNotExist() throws Exception {
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.init(databaseConfig, true);
    deleteSchedulerStateTable();
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      databaseUtils.verify(() -> DatabaseProvisionUtils.migrateDatabasesIfNeeded(insightConfig));
    }
  }

  @Test
  public void testRun_IqServerRecentCheckin() throws Exception {
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.init(databaseConfig, true);
    long currentTime = System.currentTimeMillis();
    createSchedulerStateRecord(currentTime - DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS);
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    doNothing().when(spyDbMigrationCommand).trySleep(anyInt());
    when(spyDbMigrationCommand.getCurrentTimeMillis()).thenReturn(currentTime);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
          () -> spyDbMigrationCommand.run(null, null, insightConfig));

      verify(spyDbMigrationCommand).trySleep(1);
      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      databaseUtils.verify(() -> DatabaseProvisionUtils.migrateDatabasesIfNeeded(insightConfig), never());
    }
  }

  @Test
  public void testRun_IqServerRecentCheckin_NotRecentAfterSleeping() throws Exception {
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.init(databaseConfig, true);
    long currentTime = System.currentTimeMillis();
    createSchedulerStateRecord(currentTime - DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS);
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    doNothing().when(spyDbMigrationCommand).trySleep(anyInt());
    when(spyDbMigrationCommand.getCurrentTimeMillis()).thenReturn(currentTime).thenReturn(currentTime + 1);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      verify(spyDbMigrationCommand).trySleep(1);
      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      databaseUtils.verify(() -> DatabaseProvisionUtils.migrateDatabasesIfNeeded(insightConfig));
    }
  }

  @Test
  public void testRun_NoIqServerRecentCheckin() throws Exception {
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.init(databaseConfig, true);
    long currentTime = System.currentTimeMillis();
    createSchedulerStateRecord(currentTime - DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS - 1);
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);
    when(spyDbMigrationCommand.getCurrentTimeMillis()).thenReturn(currentTime);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      databaseUtils.verify(() -> DatabaseProvisionUtils.migrateDatabasesIfNeeded(insightConfig));
    }
  }

  @Test
  public void testRun_NoIqServerCheckin() {
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.init(databaseConfig, true);
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      databaseUtils.verify(() -> DatabaseProvisionUtils.migrateDatabasesIfNeeded(insightConfig));
    }
  }

  @Test
  public void testRun_LockTableDoesNotExist() throws Exception {
    DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
    OperationalDataStoreProvider.init(databaseConfig, true);
    deleteLockTable();
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
    }
  }

  @Test
  public void testRun_NoMigrationNeeded() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress, never());
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig), never());
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress, never());
    }
  }

  @Test
  public void testRun_IgnoresMigrationDisabled_ByEnvironmentVariable() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress, never());
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress, never());
    }
  }

  @Test
  public void testRun_IgnoresMigrationDisabled_BySystemConfigurationProperty() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    initH2DatabaseToDesiredVersion(databaseDir, OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods, OperationalDataStoreProvider.ID, databaseConfig -> {
          OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
          return OperationalDataStoreProvider.getDataSource();
        }, OperationalDataStoreProvider.getUpgradeGuard(true));
    DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig);
    new SystemConfigurationPropertyDAO().set(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "false");
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      spyDbMigrationCommand.run(null, null, insightConfig);

      databaseUtils.verify(() -> DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig));
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress);
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress);
    }
  }

  @Test
  public void testInitializeDatabases_MigrationDisabled_ByEnvironmentVariable_WithoutTables() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      clusterLock.verify(ClusterLock::createForSchemaMigration, never());
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress, never());
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig), never());
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress, never());
    }
  }

  @Test
  public void testInitializeDatabases_MigrationEnabled_ByEnvironmentVariable_WithoutTables() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "true");
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      clusterLock.verify(ClusterLock::createForSchemaMigration, never());
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress, never());
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress, never());
    }
  }

  @Test
  public void testInitializeDatabases_MigrationDisabled_ByEnvironmentVariable_WithTables() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    initH2DatabaseToDesiredVersion(databaseDir, OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods, OperationalDataStoreProvider.ID, databaseConfig -> {
          OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
          return OperationalDataStoreProvider.getDataSource();
        }, OperationalDataStoreProvider.getUpgradeGuard(true));
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      clusterLock.verify(ClusterLock::createForSchemaMigration);
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress, never());
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig), never());
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress, never());
    }
  }

  @Test
  public void testInitializeDatabases_MigrationEnabled_ByEnvironmentVariable_WithTables() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    initH2DatabaseToDesiredVersion(databaseDir, OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods, OperationalDataStoreProvider.ID, databaseConfig -> {
          OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
          return OperationalDataStoreProvider.getDataSource();
        }, OperationalDataStoreProvider.getUpgradeGuard(true));
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "true");

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      clusterLock.verify(ClusterLock::createForSchemaMigration);
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress);
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress);
    }
  }

  @Test
  public void testInitializeDatabases_MigrationDisabled_BySystemConfigurationProperty_WithTables() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    initH2DatabaseToDesiredVersion(databaseDir, OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods, OperationalDataStoreProvider.ID, databaseConfig -> {
          OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
          return OperationalDataStoreProvider.getDataSource();
        }, OperationalDataStoreProvider.getUpgradeGuard(true));
    DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig);
    new SystemConfigurationPropertyDAO().set(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "false");
    DataSourceFactory.clear_ForTestsOnly();

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      clusterLock.verify(ClusterLock::createForSchemaMigration);
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress, never());
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig), never());
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress, never());
    }
  }

  @Test
  public void testInitializeDatabases_MigrationEnabled_BySystemConfigurationProperty_WithTables() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    initH2DatabaseToDesiredVersion(databaseDir, OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods, OperationalDataStoreProvider.ID, databaseConfig -> {
          OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
          return OperationalDataStoreProvider.getDataSource();
        }, OperationalDataStoreProvider.getUpgradeGuard(true));
    DatabaseProvisionUtils.initializeDatabasesWithoutMigration(insightConfig);
    new SystemConfigurationPropertyDAO().set(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "true");
    DataSourceFactory.clear_ForTestsOnly();

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS); MockedStatic<ClusterLock> clusterLock = mockStatic(ClusterLock.class,
            CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      clusterLock.verify(ClusterLock::createForSchemaMigration);
      clusterLock.verify(ClusterLock::createForSchemaMigrationInProgress);
      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
      clusterLock.verify(ClusterLock::deleteForSchemaMigrationInProgress);
    }
  }

  @Test
  public void testInitializeDatabases_MigrationDisabled_ByEnvironmentVariable_NewDataSource() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
    }
  }

  @Test
  public void testInitializeDatabases_MigrationEnabled_ByEnvironmentVariable_NewDataSource() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "true");
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();

    try (MockedStatic<DatabaseProvisionUtils> databaseUtils = mockStatic(DatabaseProvisionUtils.class,
        CALLS_REAL_METHODS)) {
      DatabaseProvisionUtils.initializeDatabases(insightConfig, new DatabaseConfigProvider(insightConfig));

      databaseUtils.verify(() -> DatabaseProvisionUtils.doMigrateDatabases(insightConfig));
    }
  }

  @Test
  public void testRun_H2_WithoutTables() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseDir, DatabaseName.ods.name()))).isEqualTo(
        String.valueOf(OperationalDataStoreProvider.MINIMUM_DATABASE_VERSION));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseDir, DatabaseName.aggregation.name()))).isEqualTo(
        String.valueOf(1));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseDir, DatabaseName.dm.name()))).isEqualTo(
        String.valueOf(1));
    ThirdPartyScansProvider.initWithoutMigration(
        getH2DatabaseConfig(databaseDir, DatabaseName.third_party_scans.name()));
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(ThirdPartyScansProvider.getDataSource(),
        ThirdPartyScansProvider.ID)).isEqualTo(1);
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    spyDbMigrationCommand.run(null, null, insightConfig);

    assertMigrated();
  }

  @Test
  public void testRun_H2_WithTables() {
    insightConfig.setConsentToUpgradeToVersion_1_45(true);
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    databaseDir.mkdirs();
    copyDatabase(databaseDir, getClass().getSimpleName() + "/h2");
    initH2DatabaseToDesiredVersion(databaseDir, OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods, OperationalDataStoreProvider.ID, databaseConfig -> {
          OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
          return OperationalDataStoreProvider.getDataSource();
        }, OperationalDataStoreProvider.getUpgradeGuard(true));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseDir, DatabaseName.aggregation.name()))).isEqualTo(
        String.valueOf(1));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseDir, DatabaseName.dm.name()))).isEqualTo(
        String.valueOf(1));
    OperationalDataStoreProvider.initWithoutMigration(getH2DatabaseConfig(databaseDir, DatabaseName.ods.name()));
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(OperationalDataStoreProvider.getDataSource(),
        OperationalDataStoreProvider.ID)).isEqualTo(OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION);
    ThirdPartyScansProvider.initWithoutMigration(
        getH2DatabaseConfig(databaseDir, DatabaseName.third_party_scans.name()));
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(ThirdPartyScansProvider.getDataSource(),
        ThirdPartyScansProvider.ID)).isEqualTo(1);
    DataSourceFactory.clear_ForTestsOnly();
    DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
    when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    spyDbMigrationCommand.run(null, null, insightConfig);

    assertMigrated();
  }

  @Test
  public void testRun_Postgres_WithoutTables() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/aggregation.sql"));
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/dm.sql"));
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/ods.sql"));
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/third_party_scans.sql"));
      try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
          postgres.getPassword())) {
        resourceDatabasePopulator.populate(connection);
      }
      insightConfig.setDatabase(getPostgresDatabaseConfig(postgres.getDatabaseConfig()));
      DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
      when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

      spyDbMigrationCommand.run(null, null, insightConfig);

      assertMigrated();
    }
  }

  @Test
  public void testRun_Postgres_WithTables() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/aggregation.sql"));
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/dm.sql"));
      resourceDatabasePopulator
          .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/third_party_scans.sql"));
      try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
          postgres.getPassword())) {
        resourceDatabasePopulator.populate(connection);
      }
      initPostgresToDesiredVersion(getClass().getSimpleName() + "/postgres/ods.sql",
          OperationalDataStoreProvider.LOCK_TABLE_DATABASE_VERSION, postgres.getDatabaseConfig(),
          OperationalDataStoreProvider.ID, databaseConfig -> {
            OperationalDataStoreProvider.initWithoutMigration(databaseConfig);
            return OperationalDataStoreProvider.getDataSource();
          }, OperationalDataStoreProvider.getUpgradeGuard(true));
      insightConfig.setDatabase(getPostgresDatabaseConfig(postgres.getDatabaseConfig()));
      DbMigrationCommand spyDbMigrationCommand = spy(new DbMigrationCommand());
      when(spyDbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

      spyDbMigrationCommand.run(null, null, insightConfig);

      assertMigrated();
    }
  }

  @Test
  public void testOnError() throws Exception {
    DbMigrationCommand dbMigrationCommand = new DbMigrationCommand();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> dbMigrationCommand.onError(null, null, new IOException("test")));
  }

  private void assertMigrated() {
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(OperationalDataStoreProvider.getDataSource(),
        OperationalDataStoreProvider.ID)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(OperationalDataStoreProvider.ID));
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(AggregationDataStoreProvider.getDataSource(),
        AggregationDataStoreProvider.ID)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(AggregationDataStoreProvider.ID));
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(DatamartProvider.getDataSource(),
        DatamartProvider.ID)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(DatamartProvider.ID));
    assertThat(DatabaseUtil.getDatabaseSchemaVersion(ThirdPartyScansProvider.getDataSource(),
        ThirdPartyScansProvider.ID)).isEqualTo(
        DatabaseMigrator.determineDesiredVersion(ThirdPartyScansProvider.ID));
  }

  private void createSchedulerStateRecord(long checkinTimestamp) throws Exception {
    String sQuery = "INSERT INTO QRTZ_SCHEDULER_STATE" + //
        " (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " + //
        " VALUES (?, ?, ?, ?)";
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sQuery)) {
      statement.setString(1, TaskScheduler.DEFAULT_SCHEDULER_NAME);
      statement.setString(2, "instanceId");
      statement.setLong(3, checkinTimestamp);
      statement.setLong(4, QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS);
      statement.execute();
    }
  }

  private void deleteSchedulerStateTable() throws Exception {
    deleteTable("QRTZ_SCHEDULER_STATE");
  }

  private void deleteLockTable() throws Exception {
    deleteTable("lock");
  }

  private void deleteTable(String tableName) throws Exception {
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement("DROP TABLE " + tableName)) {
      statement.execute();
    }
  }

  private File initH2DatabaseToDesiredVersion(
      File databaseDir,
      int desiredDbVersion,
      DatabaseName databaseName,
      String databaseSchemaName,
      Function<DatabaseConfig, DataSource> databaseConfigToDataSourceFunction,
      IntConsumer upgradeGuard)
  {
    DataSourceFactory.clear_ForTestsOnly();
    try {
      DatabaseConfig databaseConfig = getH2DatabaseConfig(databaseDir, databaseName.name());
      DatabaseMigrator spyDatabaseMigrator = spy(new DatabaseMigrator());
      when(spyDatabaseMigrator.getDesiredVersion(databaseSchemaName)).thenReturn(desiredDbVersion);

      DataSource dataSource = databaseConfigToDataSourceFunction.apply(databaseConfig);
      spyDatabaseMigrator.migrate(databaseConfig, databaseSchemaName, dataSource, upgradeGuard);
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseSchemaName)).isEqualTo(desiredDbVersion);
      return databaseDir;
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void initPostgresToDesiredVersion(
      String databaseInitialSqlFile,
      int desiredDbVersion,
      DatabaseConfig databaseConfig,
      String databaseSchemaName,
      Function<DatabaseConfig, DataSource> databaseConfigToDataSourceFunction,
      IntConsumer upgradeGuard) throws Exception
  {
    DataSourceFactory.clear_ForTestsOnly();
    try {
      ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
      resourceDatabasePopulator
          .addScript(new ClassPathResource(databaseInitialSqlFile));
      try (Connection connection =
               DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(),
                   databaseConfig.getPassword())) {
        resourceDatabasePopulator.populate(connection);
      }
      DatabaseMigrator spyDatabaseMigrator = spy(new DatabaseMigrator());
      when(spyDatabaseMigrator.getDesiredVersion(databaseSchemaName)).thenReturn(desiredDbVersion);

      DataSource dataSource = databaseConfigToDataSourceFunction.apply(databaseConfig);
      spyDatabaseMigrator.migrate(databaseConfig, databaseSchemaName, dataSource, upgradeGuard);
      assertThat(DatabaseUtil.getDatabaseSchemaVersion(dataSource, databaseSchemaName)).isEqualTo(desiredDbVersion);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private File getDatabaseVersionFile(File databaseDir, String databaseName) {
    return new File(databaseDir, databaseName + ".ver");
  }

  private String readDatabaseVersion(File versionFile) {
    try {
      return new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void copyDatabase(File databaseDir, String resourceDir) {
    try {
      FileUtils.copyDirectory(new File("target/test-classes", resourceDir), databaseDir);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private DatabaseConfig getH2DatabaseConfig(File databaseDir, String databaseName) {
    File databasePath = new File(databaseDir, databaseName);
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUrl("jdbc:h2:" + databasePath.getAbsolutePath() +
        ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }

  private com.sonatype.insight.brain.service.DatabaseConfig getPostgresDatabaseConfig(DatabaseConfig databaseConfig) {
    com.sonatype.insight.brain.service.DatabaseConfig result =
        new com.sonatype.insight.brain.service.DatabaseConfig();
    result.setType("postgresql");
    URI uri = URI.create(databaseConfig.getUrl().substring("jdbc:postgresql:".length()));
    result.setHostname(uri.getHost());
    result.setPort(uri.getPort());
    result.setName(uri.getPath().substring(1));
    result.setUsername(databaseConfig.getUsername());
    result.setPassword(databaseConfig.getPassword());
    return result;
  }
}
