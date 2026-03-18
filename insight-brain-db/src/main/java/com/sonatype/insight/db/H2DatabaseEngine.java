/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.SortedMap;
import java.util.TreeMap;

public class H2DatabaseEngine
    extends DatabaseEngine
{
  public static final DatabaseEngine INSTANCE = new H2DatabaseEngine();

  private H2DatabaseEngine() {
    // hide
  }

  @Override
  public String getId() {
    return "h2";
  }

  @Override
  public String buildSetSchemaSql(String schemaName) {
    return "SET SCHEMA " + schemaName;
  }

  @Override
  public SortedMap<String, String> getDatabaseSettings(Connection connection) throws SQLException {
    SortedMap<String, String> databaseSettings = new TreeMap<>();
    try (
        PreparedStatement statement =
            connection.prepareStatement("SELECT NAME, VALUE FROM INFORMATION_SCHEMA.SETTINGS");
        ResultSet result = statement.executeQuery())
    {
      while (result.next()) {
        String name = result.getString(1);
        String value = result.getString(2);
        databaseSettings.put(name, value);
      }
    }
    return databaseSettings;
  }
}
