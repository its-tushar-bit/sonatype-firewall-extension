/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@H2DiskTest
@Category(SlowTest.class)
public class DbMigrationCommandTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public DatabaseContainerRule databaseContainerRule = DatabaseContainerRule.getInstance(DbMigrationCommandTest.class);

  @Mock
  private ClusterLockManager mockClusterLockManager;

  private DatabaseProvisioner spyDatabaseProvisioner;

  private DatabaseMigrations spyDatabaseMigrations;

  private DbMigrationCommand dbMigrationCommand;

  private InsightConfig insightConfig;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Before
  public void before() throws Exception {
    systemConfigurationPropertyDAO =
        new SystemConfigurationPropertyDAO(databaseContainerRule.getOperationalDataStore());

    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.newFolder().getAbsolutePath());

    when(mockClusterLockManager.createForSchemaMigration()).thenReturn(mock(ClusterLock.class));

    spyDatabaseMigrations = spy(new DatabaseMigrations(databaseContainerRule, mockClusterLockManager));
    spyDatabaseProvisioner = spy(new DatabaseProvisioner(databaseContainerRule, spyDatabaseMigrations));

    dbMigrationCommand = spy(new DbMigrationCommand()
    {
      @Override
      public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
        return new DefaultDatabaseContainer(databaseContainerRule.getDataSourceProvider(),
            databaseContainerRule,
            spyDatabaseProvisioner);
      }
    });
  }

  @Test
  public void testDbMigrationCommand() {
    assertThat(dbMigrationCommand.getName()).isEqualTo("migrate-db");
    assertThat(dbMigrationCommand.getDescription()).isEqualTo("Migrates the database to the latest schema version.");
    assertThat(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).isEqualTo(
        DbMigrationCommand.ATTEMPTS_TO_WAIT_FOR_LAST_CHECKIN_TO_NOT_BE_RECENT).isEqualTo(1);
    assertThat(DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS)
        .isEqualTo(QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS * 2);
  }

  @Test
  public void testRun_QuartzSchedulerStateTableDoesNotExist() throws Exception {
    deleteSchedulerStateTable();
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();

    databaseContainerRule.markFixtureAsDirty();
  }

  @Test
  public void testRun_IqServerRecentCheckin() throws Exception {
    long currentTime = System.currentTimeMillis();
    createSchedulerStateRecord(currentTime - DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS);
    when(dbMigrationCommand.getCurrentTimeMillis()).thenReturn(currentTime);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> dbMigrationCommand.run(null, null, insightConfig));

    verify(dbMigrationCommand).trySleep(1);
    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner, never()).migrateDatabase();
  }

  @Test
  public void testRun_IqServerRecentCheckin_NotRecentAfterSleeping() throws Exception {
    long currentTime = System.currentTimeMillis();
    createSchedulerStateRecord(currentTime - DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS);
    when(dbMigrationCommand.getCurrentTimeMillis()).thenReturn(currentTime).thenReturn(currentTime + 1);

    dbMigrationCommand.run(null, null, insightConfig);

    verify(dbMigrationCommand).trySleep(1);
    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  public void testRun_NoIqServerRecentCheckin() throws Exception {
    long currentTime = System.currentTimeMillis();
    createSchedulerStateRecord(currentTime - DbMigrationCommand.RECENT_CHECKIN_INTERVAL_MILLIS - 1);
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);
    when(dbMigrationCommand.getCurrentTimeMillis()).thenReturn(currentTime);

    dbMigrationCommand.run(null, null, insightConfig);

    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  public void testRun_NoIqServerCheckin() {
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testRun_IgnoresMigrationDisabled_ByEnvironmentVariable() {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "false");
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(mockClusterLockManager, never()).createForSchemaMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testRun_IgnoresMigrationDisabled_BySystemConfigurationProperty() {
    initH2DatabaseToDesiredVersion(
        OperationalDataStore.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods,
        databaseContainerRule.getOperationalDataStore().getDatabaseSchema());
    spyDatabaseProvisioner.initializeDatabaseWithoutMigration();
    reset(spyDatabaseProvisioner);
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "false");
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    verify(spyDatabaseProvisioner).initializeDatabaseWithoutMigration();
    verify(mockClusterLockManager).createForSchemaMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationDisabled_ByEnvironmentVariable_WithoutTables() {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "false");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    verify(mockClusterLockManager, never()).createForSchemaMigration();
    // DatabaseProvisioner invoked, DatabaseMigrations invoked and checked but migration method not executed
    verify(spyDatabaseProvisioner).migrateDatabase();
    verify(spyDatabaseMigrations).isMigrationEnabled();
    verify(spyDatabaseMigrations, never()).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationEnabled_ByEnvironmentVariable_WithoutTables() {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "true");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    verify(mockClusterLockManager, never()).createForSchemaMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationDisabled_ByEnvironmentVariable_WithTables() {
    initH2DatabaseToDesiredVersion(
        OperationalDataStore.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods,
        databaseContainerRule.getOperationalDataStore().getDatabaseSchema());
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "false");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    // DatabaseProvisioner invoked, DatabaseMigrations invoked and checked but migration method not executed
    verify(spyDatabaseProvisioner).migrateDatabase();
    verify(spyDatabaseMigrations).isMigrationEnabled();
    verify(spyDatabaseMigrations, never()).migrateDatabase();
    verifyNoInteractions(mockClusterLockManager);
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationEnabled_ByEnvironmentVariable_WithTables() {
    initH2DatabaseToDesiredVersion(OperationalDataStore.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods,
        databaseContainerRule.getOperationalDataStore().getDatabaseSchema());
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "true");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    verify(mockClusterLockManager).createForSchemaMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationDisabled_BySystemConfigurationProperty_WithTables() {
    initH2DatabaseToDesiredVersion(OperationalDataStore.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods,
        databaseContainerRule.getOperationalDataStore().getDatabaseSchema());
    spyDatabaseProvisioner.initializeDatabaseWithoutMigration();
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "false");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    // DatabaseProvisioner invoked, DatabaseMigrations invoked and checked but migration method not executed
    verify(spyDatabaseProvisioner).migrateDatabase();
    verify(spyDatabaseMigrations).isMigrationEnabled();
    verify(spyDatabaseMigrations, never()).migrateDatabase();
    verifyNoInteractions(mockClusterLockManager);
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationEnabled_BySystemConfigurationProperty_WithTables() {
    initH2DatabaseToDesiredVersion(OperationalDataStore.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods,
        databaseContainerRule.getOperationalDataStore().getDatabaseSchema());
    spyDatabaseProvisioner.initializeDatabaseWithoutMigration();
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, "true");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    verify(mockClusterLockManager).createForSchemaMigration();
    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationDisabled_ByEnvironmentVariable_NewDataSource() {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "false");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  @Category(SlowTest.class)
  public void testInitializeDatabases_MigrationEnabled_ByEnvironmentVariable_NewDataSource() {
    environmentVariables.set(DatabaseMigrations.NXIQ_SCHEMA_MIGRATION, "true");

    spyDatabaseProvisioner.initializeDatabaseWithMigration();

    verify(spyDatabaseProvisioner).migrateDatabase();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testRun_H2_WithoutTables() {
    // 85 is a historical ODS version relating to consent for the IQ 1.45 upgrade. See CLM-29089.
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseContainerRule.getOperationalDataStore()))).isEqualTo(
        String.valueOf(85));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseContainerRule.getAggregationDataStore()))).isEqualTo(
        String.valueOf(1));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseContainerRule.getDataMartDataStore()))).isEqualTo(
        String.valueOf(1));
    // TODO why does the 3rd party scans not have a ver file
    // assertThat(
    // readDatabaseVersion(getDatabaseVersionFile(databaseContainerRule.getThirdPartyScansDataStore()))).isEqualTo(
    // String.valueOf(1));

    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    assertMigrated();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DbMigrationCommandTest/h2")
  @Category(SlowTest.class)
  public void testRun_H2_WithTables() {
    initH2DatabaseToDesiredVersion(OperationalDataStore.LOCK_TABLE_DATABASE_VERSION,
        DatabaseName.ods,
        databaseContainerRule.getOperationalDataStore().getDatabaseSchema());
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseContainerRule.getAggregationDataStore()))).isEqualTo(
        String.valueOf(1));
    assertThat(readDatabaseVersion(getDatabaseVersionFile(databaseContainerRule.getDataMartDataStore()))).isEqualTo(
        String.valueOf(1));

    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getOperationalDataStore())).isEqualTo(
        OperationalDataStore.LOCK_TABLE_DATABASE_VERSION);
    assertThat(
        DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getThirdPartyScansDataStore())).isEqualTo(1);
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    assertMigrated();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(suppressMigrations = true)
  public void testRun_Postgres_WithoutTables() throws Exception {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/aggregation.sql"));
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/dm.sql"));
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/ods.sql"));
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/third_party_scans.sql"));
    try (Connection connection = databaseContainerRule.getOperationalDataStore().getDataSource().getConnection()) {
      resourceDatabasePopulator.populate(connection);
    }
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    assertMigrated();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(suppressMigrations = true)
  public void testRun_Postgres_WithTables() throws Exception {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/aggregation.sql"));
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/dm.sql"));
    resourceDatabasePopulator
        .addScript(new ClassPathResource(getClass().getSimpleName() + "/postgres/third_party_scans.sql"));

    try (Connection connection = databaseContainerRule.getOperationalDataStore().getDataSource().getConnection()) {
      resourceDatabasePopulator.populate(connection);
    }
    initPostgresToDesiredVersion(databaseContainerRule.getOperationalDataStore(),
        getClass().getSimpleName() + "/postgres/ods.sql",
        OperationalDataStore.LOCK_TABLE_DATABASE_VERSION);
    when(dbMigrationCommand.getAttemptsToWaitForLastCheckinToNotBeRecent()).thenReturn(0);

    dbMigrationCommand.run(null, null, insightConfig);

    assertMigrated();
  }

  @Test
  public void testOnError() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> dbMigrationCommand.onError(null, null, new IOException("test")));
  }

  private void assertMigrated() {
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getOperationalDataStore())).isEqualTo(
        LegacyDataStoreMigrator.determineDesiredVersion(OperationalDataStore.ID));
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getAggregationDataStore())).isEqualTo(
        LegacyDataStoreMigrator.determineDesiredVersion(AggregationDataStore.ID));
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getDataMartDataStore())).isEqualTo(
        LegacyDataStoreMigrator.determineDesiredVersion(DataMartDataStore.ID));
    assertThat(
        DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getThirdPartyScansDataStore())).isEqualTo(
            LegacyDataStoreMigrator.determineDesiredVersion(ThirdPartyScansDataStore.ID));
  }

  private void createSchedulerStateRecord(long checkinTimestamp) throws Exception {
    OperationalDataStore operationalDataStore = databaseContainerRule.getOperationalDataStore();
    String sQuery = "INSERT INTO " + operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE" + //
        " (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " + //
        " VALUES (?, ?, ?, ?)";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sQuery))
    {
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

  private void deleteTable(String tableName) throws Exception {
    OperationalDataStore operationalDataStore = databaseContainerRule.getOperationalDataStore();
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "DROP TABLE " + operationalDataStore.getDatabaseSchema() + "." + tableName))
    {
      statement.execute();
    }
  }

  private void initH2DatabaseToDesiredVersion(
      int desiredDbVersion,
      DatabaseName databaseName,
      String databaseSchemaName)
  {
    databaseContainerRule.getDatabaseConfig(databaseName.name());
    LegacyDataStoreMigrator spyLegacyDataStoreMigrator =
        spy(new LegacyDataStoreMigrator(databaseContainerRule.getOperationalDataStore()));
    when(spyLegacyDataStoreMigrator.getDesiredVersion(databaseSchemaName)).thenReturn(desiredDbVersion);

    spyLegacyDataStoreMigrator.migrate();
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(databaseContainerRule.getOperationalDataStore())).isEqualTo(
        desiredDbVersion);
  }

  private void initPostgresToDesiredVersion(
      DataStore dataStore,
      String databaseInitialSqlFile,
      int desiredDbVersion) throws Exception
  {
    ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
    resourceDatabasePopulator
        .addScript(new ClassPathResource(databaseInitialSqlFile));
    try (Connection connection = dataStore.getDataSource().getConnection()) {
      resourceDatabasePopulator.populate(connection);
    }
    LegacyDataStoreMigrator spyLegacyDataStoreMigrator = spy(new LegacyDataStoreMigrator(dataStore));
    when(spyLegacyDataStoreMigrator.getDesiredVersion(dataStore.getDatabaseSchema())).thenReturn(desiredDbVersion);

    spyLegacyDataStoreMigrator.migrate();
    assertThat(DatabaseUtil.getLegacyDatabaseSchemaVersion(dataStore)).isEqualTo(desiredDbVersion);
  }

  private File getDatabaseVersionFile(DataStore dataStore) {
    File databaseDir = H2DatabaseUtil.getDatabasePath(dataStore.getDatabaseConfig());
    return new File(databaseDir.getAbsolutePath() + ".ver");
  }

  private String readDatabaseVersion(File versionFile) {
    try {
      return new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
