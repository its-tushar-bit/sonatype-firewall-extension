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
import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseSchemaPopulator;
import com.sonatype.insight.db.PostgresDatabaseEngine;

public class MultiTenantDatabaseSchemaPopulator
    extends DatabaseSchemaPopulator
{
  public MultiTenantDatabaseSchemaPopulator(
      final DataSource dataSource,
      final String dataStoreId,
      final String databaseSchema)
  {
    super(dataSource, PostgresDatabaseEngine.INSTANCE, dataStoreId, databaseSchema);
  }

  @Override
  protected boolean isSchemaAlreadyPopulated(final Connection conn) throws SQLException {
    // first check if the actual database schema exists (e.g. `tenant_name.schema_version` table)
    boolean schemaAlreadyPopulated = super.isSchemaAlreadyPopulated(conn);
    if (!schemaAlreadyPopulated) {
      return false;
    }

    // multi-tenant requires a second check, to see if there is an entry for the schema in schema_version
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT * FROM " + schemaName +
            ".schema_version WHERE data_store_id = '" + dataStoreId + "'"))
    {
      return result.next() && result.isLast();
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to read " + schemaName + ".schema_version table.", e);
    }
  }
}
