/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.nio.file.Path;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * PostgreSQL-backed tests relocated from {@link DbMigrationCommandTest} (CLM-45228).
 */
@ExtendWith(MockitoExtension.class)
@PostgresTest
public class DbMigrationCommandPgTest
{
  @TempDir
  public Path temporaryFolder;

  public DatabaseContainerRule databaseContainerRule =
      DatabaseContainerRule.getInstance(DbMigrationCommandPgTest.class);

  @Mock
  private ClusterLockManager mockClusterLockManager;

  private DatabaseProvisioner spyDatabaseProvisioner;

  private DatabaseMigrations spyDatabaseMigrations;

  private DbMigrationCommand dbMigrationCommand;

  private InsightConfig insightConfig;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @BeforeEach
  public void before(final TestInfo testInfo) throws Exception {
    databaseContainerRule.beforeFromJupiter(testInfo.getTestClass().orElse(null),
        testInfo.getTestMethod().orElse(null));
    systemConfigurationPropertyDAO =
        new SystemConfigurationPropertyDAO(databaseContainerRule.getOperationalDataStore());

    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.toFile().getAbsolutePath());

    lenient().when(mockClusterLockManager.createForSchemaMigration()).thenReturn(mock(ClusterLock.class));

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

  @AfterEach
  public void afterEach() {
    databaseContainerRule.afterFromJupiter();
  }

  @Test
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
}
