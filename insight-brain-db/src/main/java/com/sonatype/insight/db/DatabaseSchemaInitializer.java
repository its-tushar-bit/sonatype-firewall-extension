/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.db;

import javax.sql.DataSource;

/**
 * Initializes database schemas for new databases. Checks whether a schema already exists and, if not, populates it
 * using {@link DatabaseSchemaPopulator}.
 */
public class DatabaseSchemaInitializer
{
  /**
   * @return Returns true only if the database schema is new and it is populated at this time.
   */
  public boolean populateDbSchema(
      DataSource dataSource,
      DatabaseEngine databaseEngine,
      String dataStoreId,
      String databaseSchema)
  {
    AbstractDatabaseSchemaPopulator databasePopulator =
        createDatabaseSchemaPopulator(dataSource, databaseEngine, dataStoreId, databaseSchema);
    return databasePopulator.populate();
  }

  protected AbstractDatabaseSchemaPopulator createDatabaseSchemaPopulator(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new DatabaseSchemaPopulator(dataSource, databaseEngine, dataStoreId, databaseSchema);
  }
}
