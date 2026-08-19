/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import java.util.Properties;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.sonatype.insight.brain.db.datasource.AbstractDataSourceProvider.DEFAULT_MAX_CONNECTIONS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public abstract class AbstractDataSourceProviderTest
{
  static Stream<Arguments> testParameters() {
    return Stream.of(
        Arguments.of("default DatabaseConfig", newTestDatabaseConfig()),
        Arguments.of("customized DatabaseConfig",
            newTestDatabaseConfig(10, 11, "sessionVariables", "options", "applicationName")));
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("testParameters")
  public void testGetDataSource(final String testDescription, final DatabaseConfig testDatabaseConfig) {
    DataSourceProvider dataSourceProvider = createTestDataSourceProvider();
    DataSource odsDataSource = dataSourceProvider.getDataSource(testDatabaseConfig, "insight_brain_ods");
    DataSource dmDataSource = dataSourceProvider.getDataSource(testDatabaseConfig, "insight_brain_dm");
    assertDataSource(odsDataSource, testDatabaseConfig);
    assertDataSource(dmDataSource, testDatabaseConfig);
    assertEquality(odsDataSource, dmDataSource);
  }

  protected abstract void assertEquality(final DataSource dataSource1, final DataSource dataSource2);

  protected abstract DataSourceProvider createTestDataSourceProvider();

  static void assertDataSource(final DataSource dataSource, final DatabaseConfig databaseConfig) {
    assertThat(dataSource).isNotNull();
    assertThat(dataSource).isExactlyInstanceOf(BasicDataSource.class);

    BasicDataSource basicDataSource = (BasicDataSource) dataSource;

    assertThat(basicDataSource.getDriverClassName()).isEqualTo("DriverClassName");
    assertThat(basicDataSource.getUrl()).isEqualTo("Url");
    assertThat(basicDataSource.getUserName()).isEqualTo("Username");
    assertThat(basicDataSource.getMaxConnDuration().toMillis()).isEqualTo(60000);
    assertThat(basicDataSource.getLogExpiredConnections()).isFalse();
    assertThat(basicDataSource.getDefaultReadOnly()).isEqualTo(databaseConfig.isReadOnly());
    assertThat(basicDataSource.getAutoCommitOnReturn()).isEqualTo(databaseConfig.isAutoCommitOnReturnToPool());
    assertThat(basicDataSource.getTestOnBorrow()).isTrue();
    assertThat(basicDataSource.getValidationQueryTimeoutDuration().getSeconds())
        .isEqualTo(databaseConfig.getConnectionValidationTimeoutSeconds());
    assertThat(basicDataSource.isAccessToUnderlyingConnectionAllowed()).isEqualTo(
        databaseConfig.isAccessToUnderlyingConnectionAllowed());

    assertThat(basicDataSource.getMaxTotal()).isEqualTo(
        databaseConfig.getMaxConnections() == null ? DEFAULT_MAX_CONNECTIONS : databaseConfig.getMaxConnections());
    assertThat(basicDataSource.getMaxIdle()).isEqualTo(
        databaseConfig.getMaxIdleConnections() == null
            ? DEFAULT_MAX_CONNECTIONS
            : databaseConfig.getMaxIdleConnections());

    // BasicDataSource#connectionProperties is package-private :(. Enter boss mode.
    try {
      Properties properties = (Properties) FieldUtils.readField(basicDataSource, "connectionProperties", true);
      assertThat(properties.getProperty("sessionVariables")).isEqualTo(databaseConfig.getSessionVariables());
      assertThat(properties.getProperty("options")).isEqualTo(databaseConfig.getOptions());
      assertThat(properties.getProperty("ApplicationName")).isEqualTo(databaseConfig.getApplicationName());
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Produce a default {@link DatabaseConfig} for testing
   *
   * @return
   */
  static DatabaseConfig newTestDatabaseConfig() {
    return newTestDatabaseConfig(null, null, null, null, null);
  }

  /**
   * Produce a customized {@link DatabaseConfig} for testing
   */
  static DatabaseConfig newTestDatabaseConfig(
      final Integer maxConnections,
      final Integer maxIdleConnections,
      final String sessionVariables,
      final String options,
      final String applicationName)
  {
    DatabaseConfig databaseConfig = new DatabaseConfig();

    databaseConfig.setDriverClassName("DriverClassName");
    databaseConfig.setUrl("Url");
    databaseConfig.setUsername("Username");
    databaseConfig.setPassword("Password");
    databaseConfig.setMaxConnectionLifetimeSeconds(60);

    databaseConfig.isReadOnly();
    databaseConfig.isAutoCommitOnReturnToPool();
    databaseConfig.getConnectionValidationTimeoutSeconds();
    databaseConfig.isAccessToUnderlyingConnectionAllowed();

    // optional settings
    databaseConfig.setMaxConnections(maxConnections);
    databaseConfig.setMaxIdleConnections(maxIdleConnections);
    databaseConfig.setSessionVariables(sessionVariables);
    databaseConfig.setOptions(options);
    databaseConfig.setApplicationName(applicationName);

    return databaseConfig;
  }
}
