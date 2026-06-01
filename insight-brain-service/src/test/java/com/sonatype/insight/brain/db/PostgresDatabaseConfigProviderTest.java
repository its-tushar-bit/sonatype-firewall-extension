/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.postgresql.Driver;

import static org.assertj.core.api.Assertions.assertThat;

@Category(PostgresTestCategory.class)
public class PostgresDatabaseConfigProviderTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  private InsightConfig insightConfig;

  private PostgresDatabaseConfigProvider postgresDatabaseConfigProvider;

  @Before
  public void init() {
    insightConfig = new InsightConfig();
    postgresDatabaseConfigProvider = new PostgresDatabaseConfigProvider(insightConfig);
  }

  @Test
  public void testGetDatabaseEngine() {
    assertThat(postgresDatabaseConfigProvider.getDatabaseEngine().equals(PostgresDatabaseEngine.INSTANCE));
  }

  @Test
  public void testGetDatabaseConfig_NoPortOrParameters() {
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    insightConfig.setDatabase(dbConfig);

    DatabaseConfig databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(DatabaseName.ods);

    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getDriverClassName()).isEqualTo(Driver.class.getName());
    assertThat(databaseConfig.getUsername()).isEqualTo("testuser");
    assertThat(databaseConfig.getPassword()).isEqualTo("testpass");
    assertThat(databaseConfig.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db");
  }

  @Test
  public void testGetDatabaseConfig_CustomPort() {
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setPort(6543);
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("");
    insightConfig.setDatabase(dbConfig);

    DatabaseConfig databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(DatabaseName.ods);

    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getDriverClassName()).isEqualTo(Driver.class.getName());
    assertThat(databaseConfig.getUsername()).isEqualTo("testuser");
    assertThat(databaseConfig.getPassword()).isEqualTo("");
    assertThat(databaseConfig.getUrl()).isEqualTo("jdbc:postgresql://localhost:6543/test-db");
  }

  @Test
  public void testGetDatabaseConfig_CustomParameters() {
    Map<String, String> dbParams = new LinkedHashMap<>();
    dbParams.put("user", "paramuser");
    dbParams.put("password", "parampass");
    dbParams.put("key1", "value1");
    dbParams.put("key2", "value2");
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    dbConfig.setParameters(dbParams);
    insightConfig.setDatabase(dbConfig);

    DatabaseConfig databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(DatabaseName.ods);

    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getDriverClassName()).isEqualTo(Driver.class.getName());
    assertThat(databaseConfig.getUsername()).isEqualTo("testuser");
    assertThat(databaseConfig.getPassword()).isEqualTo("testpass");
    assertThat(databaseConfig.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db?key1=value1&key2=value2");
  }

  @Test
  public void testGetDatabaseConfig_CustomOdsMaxIdle() {
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    insightConfig.setDatabase(dbConfig);

    // ODS is null
    DatabaseConfig databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(DatabaseName.ods);
    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getMaxIdleConnections()).isNull();

    // Non-ODS is 3
    databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(DatabaseName.dm);
    assertThat(databaseConfig).isNotNull();
    assertThat(databaseConfig.getMaxIdleConnections()).isEqualTo(3);
  }

  @Test
  public void testGetDatabaseConfig_CustomMaxConnections() {
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setPort(5432);
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    dbConfig.setMaxConnections(50);
    insightConfig.setDatabase(dbConfig);

    assertThat(DatabaseName.values()).allSatisfy(databaseName -> {
      DatabaseConfig databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(databaseName);
      assertThat(databaseConfig).isNotNull();
      assertThat(databaseConfig.getMaxConnections()).isEqualTo(50);
    });
  }

  @Test
  public void testGetDatabaseConfig_DefaultMaxConnections() {
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setType("postgresql");
    dbConfig.setHostname("localhost");
    dbConfig.setPort(5432);
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    insightConfig.setDatabase(dbConfig);

    assertThat(DatabaseName.values()).allSatisfy(databaseName -> {
      DatabaseConfig databaseConfig = postgresDatabaseConfigProvider.getDatabaseConfig(databaseName);
      assertThat(databaseConfig).isNotNull();
      assertThat(databaseConfig.getMaxConnections()).isEqualTo(45);
    });
  }

  @Test
  public void testGetDatabaseConfig_copyDoesNotMutateOriginal() {
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setHostname("localhost");
    dbConfig.setName("test-db");
    dbConfig.setUsername("testuser");
    dbConfig.setPassword("testpass");
    insightConfig.setDatabase(dbConfig);

    postgresDatabaseConfigProvider.getDatabaseConfig(DatabaseName.dm);

    assertThat(dbConfig.getDriverClassName()).isNull();
    assertThat(dbConfig.getMaxIdleConnections()).isNull();
    assertThat(dbConfig.getMaxConnections()).isNull();
    assertThat(dbConfig.getApplicationName()).isNull();
  }

}
