/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDatabaseSchemaPopulator
{
  private static final Logger log = LoggerFactory.getLogger(AbstractDatabaseSchemaPopulator.class);

  protected final DataSource dataSource;

  protected final DatabaseEngine databaseEngine;

  protected final String dataStoreId;

  protected final String schemaName;

  protected AbstractDatabaseSchemaPopulator(
      DataSource dataSource,
      DatabaseEngine databaseEngine,
      String dataStoreId,
      String schemaName)
  {
    this.dataSource = dataSource;
    this.databaseEngine = databaseEngine;
    this.dataStoreId = dataStoreId;
    this.schemaName = schemaName;
  }

  abstract void doPopulate(Connection connection);

  /**
   * @return Returns true only if the database schema is new and it is populated at this time.
   */
  boolean populate() {
    try (Connection conn = dataSource.getConnection()) {
      if (isSchemaAlreadyPopulated(conn)) {
        log.info("Ignoring request to populate database schema {} because it already exists.", schemaName);
        return false;
      }

      log.info("Creating schema '{}' with provider '{}'", schemaName, dataStoreId);
      exec(conn, "CREATE SCHEMA IF NOT EXISTS " + schemaName);
      runPopulate(conn);
      return true;
    }
    catch (Exception e) {
      throw new DatabaseException(e);
    }
  }

  protected boolean isSchemaAlreadyPopulated(final Connection conn) throws SQLException {
    DatabaseMetaData dbMetaData = conn.getMetaData();
    String escapedSchemaName = escapeWildcards(schemaName, dbMetaData.getSearchStringEscape());
    try (ResultSet results = dbMetaData.getSchemas(null, escapedSchemaName)) {
      if (results.next()) {
        return true;
      }
    }
    return false;
  }

  private void runPopulate(Connection conn) throws Exception {
    try {
      exec(conn, databaseEngine.buildSetSchemaSql(schemaName));
      doPopulate(conn);
    }
    catch (Exception e) {
      // best effort to revert to clean slate so schema init can be retried
      revertPopulation(conn, e);
      throw e;
    }
  }

  private void revertPopulation(Connection conn, Exception e) {
    try {
      exec(conn, "DROP SCHEMA " + schemaName + " CASCADE");
    }
    catch (Exception suppressed) {
      e.addSuppressed(suppressed);
    }
  }

  private String escapeWildcards(String search, String escape) {
    return search.replace("_", escape + "_").replace("%", escape + "%");
  }

  private void exec(Connection conn, String sql) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.execute();
    }
  }
}
