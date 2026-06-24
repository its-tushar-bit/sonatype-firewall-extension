/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVM level Postgres test cluster.
 * <p>
 * Uses embedded-postgres (zonky) to start <B>ONE SINGLE</B> Postgresql cluster that will live
 * for the <B>ENTIRE DURATION</B> that the JVM is executing. This SINGLE cluster can be used for ALL tests
 * that require a Postgres database. A new database in the cluster (not new instance) will be provisioned for every
 * test. This approach prevents the starting and stopping of an entire Postgres cluster for every test.
 * </p>
 * <p>
 * Additionally this class will create one fully migrated database and then clone that database when a new one is
 * requested.
 * </p>
 */
public class PostgresTestCluster
{
  private static final Logger log = LoggerFactory.getLogger(PostgresTestCluster.class);

  protected static final String TEMPLATE_DATABASE = "template_database";

  public static final String DEFAULT_NAME = "testdata";

  public static final String DEFAULT_USERNAME = "testuser";

  public static final String DEFAULT_PASSWORD = "testpass";

  private static final String DUPLICATE_OBJECT_SQL_STATE = "42710";

  protected final EmbeddedPostgres embeddedPostgres;

  protected final int port;

  private static PostgresTestCluster INSTANCE;

  public static PostgresTestCluster getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PostgresTestCluster();
    }
    return INSTANCE;
  }

  protected PostgresTestCluster() {
    try {
      this.embeddedPostgres = EmbeddedPostgres.builder()
          .start();
      this.port = embeddedPostgres.getPort();
    }
    catch (IOException e) {
      throw new IllegalStateException("Could not start embedded postgres", e);
    }

    // Tolerate a pre-existing role: parallel surefire forks and reused CI agents can leave it behind, and a
    // hard failure would fail the whole test class. Any such role was created by a prior run of this same
    // constructor, so its attributes match. To stop tolerating it, drop the duplicate_object check.
    try (Connection conn = getAdminConnection("postgres");
        Statement stmt = conn.createStatement())
    {
      stmt.execute("CREATE ROLE " + DEFAULT_USERNAME + " WITH LOGIN PASSWORD '" + DEFAULT_PASSWORD +
          "' SUPERUSER CREATEDB");
    }
    catch (SQLException e) {
      if (DUPLICATE_OBJECT_SQL_STATE.equals(e.getSQLState())) {
        log.info("Test user role '{}' already exists; reusing it.", DEFAULT_USERNAME);
      }
      else {
        throw new IllegalStateException("Could not create test user role", e);
      }
    }

    log.info("Started Embedded Postgres Test Cluster on port {} for this JVM execution.", port);

    createFullyMigratedTemplateDatabase();
  }

  /**
   * Get a connection to the admin database (postgres) using the embedded postgres default superuser.
   */
  private Connection getAdminConnection(final String databaseName) {
    try {
      // EmbeddedPostgres creates a default superuser named "postgres"
      return DriverManager.getConnection(
          "jdbc:postgresql://localhost:" + port + "/" + databaseName, "postgres", null);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not get admin connection", e);
    }
  }

  /**
   * Get a connection using the test user credentials.
   */
  protected Connection getTestUserConnection(final String databaseName) {
    try {
      return DriverManager.getConnection(
          "jdbc:postgresql://localhost:" + port + "/" + databaseName, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not get test user connection to " + databaseName, e);
    }
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
    runCommand("CREATE DATABASE " + databaseName + " OWNER " + DEFAULT_USERNAME);
  }

  public void cloneFullyMigratedTemplateDatabase(final String databaseName) {
    log.info("Creating new Postgres test database '{}' by cloning '{}", databaseName, TEMPLATE_DATABASE);
    // a cloned database cannot have any existing connections
    killConnectionsToDatabase(TEMPLATE_DATABASE);
    runCommand("CREATE DATABASE " + databaseName + " WITH TEMPLATE " + TEMPLATE_DATABASE + " OWNER " + getUsername());
  }

  public void destroyDatabase(final String databaseName) {
    log.info("Terminating connections and destroying Postgres test database '{}'", databaseName);
    // first kill all connections to this database to ensure the drop succeeds
    killConnectionsToDatabase(databaseName);
    runCommand("DROP DATABASE " + databaseName);
  }

  /**
   * Execute a SQL command against the default database using admin credentials.
   * Used for DDL operations like CREATE/DROP DATABASE which cannot be run inside a transaction.
   */
  protected void runCommand(final String command) {
    try (Connection conn = getAdminConnection("postgres")) {
      conn.setAutoCommit(true);
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(command);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not execute command: " + command, e);
    }
  }

  private void killConnectionsToDatabase(final String database) {
    try (Connection conn = getAdminConnection("postgres");
        Statement stmt = conn.createStatement())
    {
      stmt.execute("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity " +
          "WHERE pg_stat_activity.datname = '" + database + "' AND pid <> pg_backend_pid()");
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not kill connections to database " + database, e);
    }
  }

  public String getUsername() {
    return DEFAULT_USERNAME;
  }

  public String getPassword() {
    return DEFAULT_PASSWORD;
  }

  public String getJdbcUrl(final String databaseName) {
    return "jdbc:postgresql://localhost:" + port + "/" + databaseName;
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
      String sql = Files.readString(sqlFile, StandardCharsets.UTF_8);
      try (Connection conn = getTestUserConnection(databaseName);
          Statement stmt = conn.createStatement())
      {
        stmt.execute(sql);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not load SQL dump into postgres server", e);
    }
  }

  public String dumpSchema(final String databaseName, final String schema) {
    log.info("Generating dump for schema {}", schema);
    try (Connection conn = getTestUserConnection(databaseName);
        Statement stmt = conn.createStatement())
    {
      StringBuilder dump = new StringBuilder();

      String currentTable = null;
      // Dump tables with columns, types, nullability, defaults — ordered deterministically
      try (var tables = stmt.executeQuery(
          "SELECT table_name, column_name, data_type, character_maximum_length, " +
              "is_nullable, column_default, ordinal_position " +
              "FROM information_schema.columns " +
              "WHERE table_schema = '" + schema + "' " +
              "ORDER BY table_name, ordinal_position"))
      {
        while (tables.next()) {
          String tableName = tables.getString("table_name");
          if (!tableName.equals(currentTable)) {
            if (currentTable != null) {
              dump.append(");\n\n");
            }
            currentTable = tableName;
            dump.append("CREATE TABLE ").append(schema).append(".").append(tableName).append(" (\n");
          }
          else {
            dump.append(",\n");
          }
          String colName = tables.getString("column_name");
          String dataType = tables.getString("data_type");
          Integer maxLen =
              tables.getObject("character_maximum_length") != null ? tables.getInt("character_maximum_length") : null;
          String nullable = tables.getString("is_nullable");
          String colDefault = tables.getString("column_default");

          dump.append("    ").append(colName).append(" ").append(dataType);
          if (maxLen != null) {
            dump.append("(").append(maxLen).append(")");
          }
          if ("NO".equals(nullable)) {
            dump.append(" NOT NULL");
          }
          if (colDefault != null) {
            dump.append(" DEFAULT ").append(colDefault);
          }
        }
      }
      if (currentTable != null) {
        dump.append(");\n\n");
      }

      // Dump indexes
      try (var indexes = stmt.executeQuery(
          "SELECT indexname, indexdef FROM pg_indexes " +
              "WHERE schemaname = '" + schema + "' ORDER BY indexname"))
      {
        while (indexes.next()) {
          dump.append(indexes.getString("indexdef")).append(";\n");
        }
      }

      // Dump constraints
      try (var constraints = stmt.executeQuery(
          "SELECT tc.constraint_name, tc.table_name, " +
              "pg_get_constraintdef(c.oid) as constraint_def " +
              "FROM information_schema.table_constraints tc " +
              "JOIN pg_constraint c ON c.conname = tc.constraint_name " +
              "JOIN pg_namespace n ON n.oid = c.connamespace AND n.nspname = tc.table_schema " +
              "WHERE tc.table_schema = '" + schema + "' " +
              "ORDER BY tc.table_name, tc.constraint_name"))
      {
        while (constraints.next()) {
          dump.append("ALTER TABLE ").append(schema).append(".").append(constraints.getString("table_name"));
          dump.append(" ADD CONSTRAINT ").append(constraints.getString("constraint_name"));
          dump.append(" ").append(constraints.getString("constraint_def")).append(";\n");
        }
      }

      // Dump sequences
      try (var sequences = stmt.executeQuery(
          "SELECT sequence_name FROM information_schema.sequences " +
              "WHERE sequence_schema = '" + schema + "' ORDER BY sequence_name"))
      {
        while (sequences.next()) {
          dump.append("CREATE SEQUENCE ").append(schema).append(".");
          dump.append(sequences.getString("sequence_name")).append(";\n");
        }
      }

      return dump.toString();
    }
    catch (Exception e) {
      throw new IllegalStateException(String.format("Could not dump schema %s", schema), e);
    }
  }
}
