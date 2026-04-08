/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.AbstractDatabaseSchemaPopulator;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseSchemaInitializer;

/**
 * MTIQ-specific schema initializer that uses {@link MultiTenantDatabaseSchemaPopulator} to handle the multi-tenant
 * schema version checking.
 */
public class MultiTenantDatabaseSchemaInitializer
    extends DatabaseSchemaInitializer
{
  @Override
  protected AbstractDatabaseSchemaPopulator createDatabaseSchemaPopulator(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new MultiTenantDatabaseSchemaPopulator(dataSource, dataStoreId, databaseSchema);
  }
}
