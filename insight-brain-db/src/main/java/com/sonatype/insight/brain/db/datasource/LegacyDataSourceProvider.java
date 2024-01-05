/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.db.DatabaseEngine;

/**
 * TODO: This class will be removed at the end of the liquibase move - CLM-26741
 * The legacy {@link DataSourceFactory} performed more duties than as a factory for {@link DataSource} objects. It
 * additionally did 'population' which is setting up a new database. This responsibility to be moved to new classes
 * with the Liquibase work.
 */
@Deprecated
public interface LegacyDataSourceProvider
{
  @Deprecated
  default boolean populateDbSchema(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new DataSourceFactory().populateDbSchema(dataSource, databaseEngine, dataStoreId, databaseSchema);
  }
}
