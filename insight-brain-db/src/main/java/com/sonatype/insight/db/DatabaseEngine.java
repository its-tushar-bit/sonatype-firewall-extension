/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.SortedMap;

public abstract class DatabaseEngine
{
  public abstract String getId();

  public abstract String buildSetSchemaSql(String schemaName);

  public abstract SortedMap<String, String> getDatabaseSettings(Connection connection) throws SQLException;
}
