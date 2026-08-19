/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseSchemaInitializer;

/**
 * TODO: This interface will be removed at the end of the liquibase move - CLM-26741
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
    return new DatabaseSchemaInitializer().populateDbSchema(dataSource, databaseEngine, dataStoreId, databaseSchema);
  }
}
