/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.insight.db.DatabaseEngine;

public class PostgresDatabaseEngine
    extends DatabaseEngine
{
  public static final DatabaseEngine INSTANCE = new PostgresDatabaseEngine();

  private PostgresDatabaseEngine() {
    // hide
  }

  @Override
  public String getId() {
    return "postgresql";
  }

  @Override
  public String buildSetSchemaSql(String schemaName) {
    return "SET SCHEMA '" + schemaName + "'";
  }

  @Override
  public SortedMap<String, String> getDatabaseSettings(Connection connection) throws SQLException {
    SortedMap<String, String> databaseSettings = new TreeMap<>();
    try (Statement statement = connection.createStatement(); ResultSet result = statement
        .executeQuery("SHOW ALL")) {
      while (result.next()) {
        String name = result.getString(1);
        String value = result.getString(2);
        databaseSettings.put(name, value);
      }
    }
    return databaseSettings;
  }
}
