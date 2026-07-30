/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.diagnostics;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;

import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Recover;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticsCli
{
  private static final Logger log = LoggerFactory.getLogger(DiagnosticsCli.class);

  private static final String DB_USERNAME = "sa";

  private static final String DB_PASSWORD = "";

  public static void main(String[] args) {
    Parameters params = new Parameters(args);
    if (params.getError() != null) {
      params.printUsage();
      log.error("Actual arguments were: {}", Arrays.asList(params.getArgs()));
      System.exit(1);
    }
    if (params.isHelp()) {
      params.printUsage();
      return;
    }
    try {
      new DiagnosticsCli().run(params);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  public void run(Parameters params) throws Exception {
    Locale.setDefault(Locale.ENGLISH);

    File ods = new File(params.getWorkDirectory(), "data/ods").getAbsoluteFile().toPath().normalize().toFile();
    File h2 = new File(ods.getPath() + ".h2.db");
    if (!h2.isFile()) {
      throw new IllegalArgumentException("The specified work directory is invalid, found no database file at " + h2);
    }
    log.info("-- Database Diagnostics --");
    log.info("Total database size: {} bytes", h2.length());

    logSchemaVersionFromFile(ods);
    // NOTE: Unless explicitly set, DB_CLOSE_DELAY is restored to the value from the last connection which usually was
    // IQ Server setting it to -1. But -1 causes unclosed file handles and does not work well for e.g. recovery mode.
    String dbUrl =
        "jdbc:h2:" + ods.getPath() + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=0;LOCK_TIMEOUT=10000;MV_STORE=FALSE";
    logSchemaVersionFromDatabase(dbUrl);

    if (params.isRecover()) {
      recoverDatabase(ods);
      return;
    }

    if (!params.isCompact()) {
      logDiskSpeed(h2);
    }

    try (Connection connection = DriverManager.getConnection(dbUrl, DB_USERNAME, DB_PASSWORD)) {
      connection.setAutoCommit(true);
      if (params.isCompact()) {
        compactDatabase(connection);
        log.info("New database size: {} bytes", h2.length());
      }
      else {
        try (Statement statement = connection.createStatement()) {
          statement.execute("SET SCHEMA insight_brain_ods;");
        }
        logRowCounts(connection);
        logOldestEvaluation(connection);
        logUniqueCoordinates(connection);
        logAverageColumnSizes(connection);
        logTableSizes(connection);
        logDatabaseSettings(connection);
      }
    }
  }

  private void logSchemaVersionFromFile(File ods) throws Exception {
    File versionFile = new File(ods + ".ver");
    String version;
    if (versionFile.isFile()) {
      version = new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8);
    }
    else {
      version = "(unknown - " + versionFile + " missing)";
    }
    log.info("Schema version from file: {}", version);
  }

  private void logSchemaVersionFromDatabase(String dbUrl) throws Exception {
    try (Connection connection = DriverManager.getConnection(dbUrl, DB_USERNAME, DB_PASSWORD)) {
      try (Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery(
              "SELECT * FROM INFORMATION_SCHEMA.TABLES " +
                  "WHERE TABLE_SCHEMA = 'insight_brain_ods' AND TABLE_NAME = 'schema_version'"))
      {
        if (!result.next()) {
          log.info("Schema version from database: {}", "(unknown - insight_brain_ods version table missing)");
          return;
        }
      }
      try (Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT * FROM insight_brain_ods.schema_version"))
      {
        if (result.last() && result.getRow() == 1) {
          log.info("Schema version from database: {}", result.getInt("schema_version"));
        }
        else {
          log.info("Schema version from database: {}",
              "(unknown - insight_brain_ods schema_version table should have 1 entry but has " + result.getRow() +
                  ")");
        }
      }
    }
  }

  private void logDiskSpeed(File dbFile) throws Exception {
    byte[] buffer = new byte[4 * 1024 * 1024];
    long length = Math.min(dbFile.length(), 1024 * 1024 * 1024);
    long total = 0;
    try (RandomAccessFile raf = new RandomAccessFile(dbFile, "r")) {
      long start = System.nanoTime();
      while (total < length) {
        int read = raf.read(buffer);
        if (read < 0) {
          break;
        }
        total += read;
      }
      long stop = System.nanoTime();
      int mbPerSec = (int) ((double) total / (stop - start) * 1000 * 1000 * 1000 / 1024 / 1024);
      log.info("Read throughput: {} MB/sec", mbPerSec);
    }
  }

  private void logRowCounts(Connection connection) throws Exception {
    log.info("Row counts:");
    String[] tables = {"organization", "application", "policy", "policy_evaluation", "last_policy_evaluation",
      "policy_violation", "first_occurrence_policy_violation", "waived_policy_violation", "owner_component",
      "repository_manager", "repository", "repository_component", "repository_policy_violation",
      "proprietary_component_name_pattern"};
    for (String table : tables) {
      try (Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table))
      {
        while (result.next()) {
          log.info("  {}: {}", table, result.getLong(1));
        }
      }
      catch (SQLException e) {
        if (e.getMessage().contains(" not found")) {
          continue;
        }
        log.error("  {}", table, e);
      }
    }
  }

  private void logTableSizes(Connection connection) throws Exception {
    log.info("Table sizes:");
    String[] tables = {"organization", "application", "policy", "policy_evaluation", "last_policy_evaluation",
      "policy_violation", "first_occurrence_policy_violation", "waived_policy_violation", "owner_component",
      "repository_manager", "repository", "repository_component", "repository_policy_violation",
      "proprietary_component_name_pattern"};
    for (String table : tables) {
      try (Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT DISK_SPACE_USED('" + table + "')"))
      {
        while (result.next()) {
          log.info("  {}: {}", table, result.getLong(1));
        }
      }
      catch (SQLException e) {
        if (e.getMessage().contains(" not found")) {
          continue;
        }
        log.error("  {}", table, e);
      }
    }
  }

  private void logOldestEvaluation(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT MIN(time) FROM policy_evaluation"))
    {
      while (result.next()) {
        log.info("Oldest policy evaluation: {}", result.getDate(1));
      }
    }
  }

  private void logUniqueCoordinates(Connection connection) throws Exception {
    log.info("Unique coordinates:");
    String[] tables = {"policy_violation", "owner_component"};
    for (String table : tables) {
      try (Statement statement = connection.createStatement();
          ResultSet result = query(statement, "SELECT COUNT(DISTINCT component_id_coordinates_json) FROM " + table,
              "SELECT COUNT(DISTINCT CONCAT(group_id, artifact_id, version)) FROM " + table))
      {
        while (result.next()) {
          log.info("  {}: {}", table, result.getLong(1));
        }
      }
    }
  }

  private void logAverageColumnSizes(Connection connection) throws Exception {
    log.info("Average column sizes:");
    try (Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery("SELECT AVG(LENGTH(policy_name)) FROM policy_violation")) {
        while (result.next()) {
          log.info("  policy_name: {}", result.getLong(1));
        }
      }
      try (ResultSet result =
          statement.executeQuery("SELECT AVG(LENGTH(constraint_facts_json)) FROM policy_violation"))
      {
        while (result.next()) {
          log.info("  constraints: {}", result.getLong(1));
        }
      }
      try (ResultSet result = query(statement, "SELECT AVG(LENGTH(pathnames)) FROM policy_violation",
          "SELECT AVG(LENGTH(filename)) FROM policy_violation"))
      {
        while (result.next()) {
          log.info("  path-/filename: {}", result.getLong(1));
        }
      }
      try (
          ResultSet result = query(statement, "SELECT AVG(LENGTH(component_id_coordinates_json)) FROM policy_violation",
              "SELECT AVG(LENGTH(CONCAT(group_id, artifact_id, version))) FROM policy_violation"))
      {
        while (result.next()) {
          log.info("  coordinates: {}", result.getLong(1));
        }
      }
    }
  }

  private ResultSet query(Statement statement, String... queries) throws Exception {
    Exception exception = new IllegalArgumentException("no queries specified");
    for (String query : queries) {
      try {
        return statement.executeQuery(query);
      }
      catch (SQLException e) {
        exception = e;
      }
    }
    throw exception;
  }

  private void compactDatabase(Connection connection) throws Exception {
    log.info("Compacting database...");
    try (Statement statement = connection.createStatement()) {
      statement.execute("SHUTDOWN COMPACT");
    }
  }

  private void logDatabaseSettings(Connection connection) {
    try (Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery("SELECT NAME, VALUE FROM INFORMATION_SCHEMA.SETTINGS ORDER BY NAME"))
    {
      log.info("Database settings:");
      while (result.next()) {
        String name = result.getString(1);
        String value = result.getString(2);
        value = value != null ? value.replace("\r", "\\r").replace("\n", "\\n") : value;
        log.info("  {}={}", name, value);
      }
    }
    catch (Exception e) {
      log.error("Failed to load database settings: " + e.getMessage(), e);
    }
  }

  private void recoverDatabase(File ods) throws Exception {
    File sqlFile = new File(ods.getParentFile(), ods.getName() + ".h2.sql");
    File recoveredOds = new File(ods.getParentFile(), "recovered-ods");
    File recoveredDb = new File(recoveredOds.getParentFile(), recoveredOds.getName() + ".h2.db");
    String dbUrl = "jdbc:h2:" + recoveredOds.getAbsolutePath() + ";TRACE_LEVEL_FILE=0;MV_STORE=FALSE";

    log.info("Recovering database to {}", sqlFile);
    Recover.execute(ods.getParent(), ods.getName());

    log.info("Loading database into {}", recoveredDb);
    DeleteDbFiles.execute(recoveredOds.getParent(), recoveredOds.getName(), true);
    log.info("  This might take a while, please be patient...");
    RunScript.execute(dbUrl, DB_USERNAME, DB_PASSWORD, sqlFile.getAbsolutePath(), null, false);

    if (!sqlFile.delete()) {
      log.warn("{} could not be deleted, please delete this temporary file manually", sqlFile);
    }

    // recovered databases are bloated
    try (Connection connection = DriverManager.getConnection(dbUrl, DB_USERNAME, DB_PASSWORD)) {
      compactDatabase(connection);
    }

    File odsDb = new File(ods.getParentFile(), ods.getName() + ".h2.db");
    File originalDb = new File(ods.getParentFile(), "original-ods" + ".h2.db");
    try {
      Files.move(odsDb.toPath(), originalDb.toPath());
      log.info("The original/corrupted database was backed up to {}", originalDb);
      Files.move(recoveredDb.toPath(), odsDb.toPath());
      log.info("The recovered database was moved to {}", odsDb);
    }
    catch (Exception e) {
      log.warn("The recovered database could not be moved to replace the corrupted database:");
      log.warn("  {}", e.toString());
      log.info("Please manually backup {} and move {} into its place", odsDb, recoveredDb);
    }
  }
}
