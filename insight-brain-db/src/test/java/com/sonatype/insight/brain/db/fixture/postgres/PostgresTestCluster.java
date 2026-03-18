/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import java.nio.file.Path;
import java.util.StringJoiner;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.PostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.SimpleDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrators;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

/**
 * JVM level Postgres test cluster.
 * <p>
 * Uses testcontainers.org to start <B>ONE SINGLE</B> Postgresql cluster in <B>ONE SINGLE</B> container that will live
 * for the <B>ENTIRE DURATION</B> that the JVM is executing. This SINGLE cluster/container can be used for ALL tests
 * that require a Postgres database. A new database in the cluster (not new container) will be provisioned for every
 * test. This approach prevents the starting and stopping of an entire Postgres cluster for every test.
 * </p>
 * <p>
 * See 'Using Singleton Containers' in the TestContainers documentation <a
 * href="https://testcontainers.com/guides/testcontainers-container-lifecycle/#_using_singleton_containers">here</a>.
 * </p>
 * <p>
 * Additionally this class will create one fully migrated database and then clone that database when a new one is
 * requested.
 * </p>
 */
public class PostgresTestCluster
{
  private static final Logger log = LoggerFactory.getLogger(PostgresTestCluster.class);

  // This should match our minimum recommended version
  private static final String DEFAULT_IMAGE_VERSION = "14.17-alpine";

  protected static final String TEMPLATE_DATABASE = "template_database";

  public static final String DEFAULT_NAME = "testdata";

  public static final String DEFAULT_USERNAME = "testuser";

  public static final String DEFAULT_PASSWORD = "testpass";

  protected final PostgresTestContainer postgresTestContainer;

  private static PostgresTestCluster INSTANCE;

  public static PostgresTestCluster getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PostgresTestCluster(DEFAULT_IMAGE_VERSION);
    }
    return INSTANCE;
  }

  protected PostgresTestCluster(final String version) {
    this.postgresTestContainer = new PostgresTestContainer(version);
    postgresTestContainer.start();
    postgresTestContainer.followOutput(new Slf4jLogConsumer(log).withSeparateOutputStreams());
    postgresTestContainer.withDatabaseName(DEFAULT_NAME);
    postgresTestContainer.withUsername(DEFAULT_USERNAME);
    postgresTestContainer.withPassword(DEFAULT_PASSWORD);
    log.info("Started Postgres Test Cluster on version {} for this JVM execution.", version);

    createFullyMigratedTemplateDatabase();
  }

  /**
   * Creates a database in the cluster with all migrations executed which can then be used as a template.
   * This database will be cloned when a new test database is requested which greatly reduces the startup time as
   * migrations do not need to re-execute.
   */
  protected void createFullyMigratedTemplateDatabase() {
    createNewDatabase(TEMPLATE_DATABASE);

    DataSourceProvider dataSourceProvider = new PostgresDataSourceProvider();
    DatabaseConfig databaseConfig = getDatabaseConfig(TEMPLATE_DATABASE);
    OperationalDataStore operationalDataStore = new DefaultOperationalDataStore(dataSourceProvider, databaseConfig);
    AggregationDataStore aggregationDataStore = new DefaultAggregationDataStore(dataSourceProvider, databaseConfig);
    DataMartDataStore dataMartDataStore = new DefaultDataMartDataStore(dataSourceProvider, databaseConfig);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new DefaultThirdPartyScansDataStore(dataSourceProvider, databaseConfig);

    operationalDataStore.initialize();
    aggregationDataStore.initialize();
    dataMartDataStore.initialize();
    thirdPartyScansDataStore.initialize();

    new DatabaseMigrators(new SimpleDataStoreProvider(operationalDataStore, aggregationDataStore, dataMartDataStore,
        thirdPartyScansDataStore)).runMigrators();
  }

  /**
   * Create a new database for testing
   */
  public void createNewDatabase(final String databaseName) {
    log.info("Creating new Postgres test database '{}'", databaseName);
    try {
      runCommand("CREATE DATABASE " + databaseName);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not create new database", e);
    }
  }

  public void cloneFullyMigratedTemplateDatabase(final String databaseName) {
    log.info("Creating new Postgres test database '{}' by cloning '{}", databaseName, TEMPLATE_DATABASE);
    try {
      // a cloned database cannot have any existing connections
      killConnectionsToDatabase(TEMPLATE_DATABASE);

      runCommand("CREATE DATABASE " + databaseName + " WITH TEMPLATE " + TEMPLATE_DATABASE + " OWNER " + getUsername());
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not create new test database", e);
    }
  }

  public void destroyDatabase(final String databaseName) {
    log.info("Terminating connections and destroying Postgres test database '{}'", databaseName);
    try {
      // first kill all connections to this database to ensure the drop succeeds
      killConnectionsToDatabase(databaseName);

      runCommand("DROP DATABASE " + databaseName);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not destroy cluster '" + databaseName + "'", e);
    }
  }

  protected ExecResult runCommand(final String command) throws Exception {
    String[] cmd = {
      "/usr/local/bin/psql", "--variable", "ON_ERROR_STOP=1", "--dbname", postgresTestContainer.getDatabaseName(),
      "--username", postgresTestContainer.getUsername(), "--command", command
    };

    ExecResult execResult = postgresTestContainer.execInContainer(cmd);
    if (execResult.getExitCode() != 0) {
      maybeHandlePsqlError(execResult);
    }
    return execResult;
  }

  private void killConnectionsToDatabase(final String database) {
    try {
      runCommand("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity " +
          "WHERE pg_stat_activity.datname = '" + database + "' AND pid <> pg_backend_pid()");
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not create new test database", e);
    }
  }

  protected void maybeHandlePsqlError(final ExecResult execResult) throws Exception {
    if (execResult.getExitCode() != 0) {
      String message = new StringJoiner(", ").add("psql returned exit code " + execResult.getExitCode())
          .add("stdout='" + execResult.getStdout() + "'")
          .add("stderr='" + execResult.getStderr() + "'")
          .toString();
      throw new Exception(message);
    }
  }

  public String getUsername() {
    return postgresTestContainer.getUsername();
  }

  public String getPassword() {
    return postgresTestContainer.getPassword();
  }

  public String getJdbcUrl(final String databaseName) {
    return postgresTestContainer.getJdbcUrl(databaseName);
  }

  public DatabaseConfig getDatabaseConfig(String databaseName) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    databaseConfig.setUrl(getJdbcUrl(databaseName));
    databaseConfig.setUsername(getUsername());
    databaseConfig.setPassword(getPassword());
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }

  public void loadSqlDump(final String databaseName, final Path sqlFile) {
    log.info("Loading SQL dump '{}' into database '{}'", sqlFile, databaseName);
    try {
      postgresTestContainer.copyFileToContainer(MountableFile.forHostPath(sqlFile), "/tmp/" + sqlFile.getFileName());
      String[] cmd = {
        "/usr/local/bin/psql", "--variable", "ON_ERROR_STOP=1", "--dbname", databaseName,
        "--username", getUsername(), "--file", "/tmp/" + sqlFile.getFileName()
      };
      ExecResult execResult = postgresTestContainer.execInContainer(cmd);
      if (execResult.getExitCode() != 0) {
        throw new Exception("psql returned " + execResult.getExitCode());
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not load SQL dump into postgres server", e);
    }
  }

  public String dumpSchema(final String databaseName, final String schema) {
    log.info("Generating dump for schema {}", schema);
    try {
      String connectionUrl =
          String.format("postgresql://%s:%s@%s:%s/%s", getUsername(), getPassword(), "127.0.0.1", "5432", databaseName);
      String[] cmd = {
        "/usr/local/bin/pg_dump", "--schema-only", "--schema=" + schema, "--dbname=" + connectionUrl
      };
      ExecResult execResult = postgresTestContainer.execInContainer(cmd);
      if (execResult.getExitCode() != 0) {
        throw new Exception("pg_dump returned " + execResult.getExitCode());
      }
      return execResult.getStdout();
    }
    catch (Exception e) {
      throw new IllegalStateException(String.format("Could dump schema %s", schema), e);
    }
  }
}
