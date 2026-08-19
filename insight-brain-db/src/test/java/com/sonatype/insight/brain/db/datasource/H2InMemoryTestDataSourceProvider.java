/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * {@link DataSourceProvider} for unit/integration tests using an H2 in-memory {@link DataSource}
 */
public class H2InMemoryTestDataSourceProvider
    implements DataSourceProvider, LegacyDataSourceProvider
{
  private static final String H2_IN_MEMORY_URL_PREFIX = "jdbc:h2:mem:";

  private EmbeddedDatabase inMemoryDatabase;

  @Override
  public DataSource getDataSource(
      final DatabaseConfig databaseConfig,
      final String dataStoreId /* unused */)
  {
    return createNewInMemoryDataSource(databaseConfig);
  }

  /**
   * This method should be used only to create the data source(s) at startup and in very limited case where a new
   * DataSource is needed. For all other purposes, use getDataSource.
   */
  @Override
  public DataSource createNewDataSource(DatabaseConfig databaseConfig) {
    return createNewInMemoryDataSource(databaseConfig);
  }

  private DataSource createNewInMemoryDataSource(DatabaseConfig databaseConfig) {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    // UGLY HACK: We need to specify DATABASE_TO_UPPER=FALSE for birt over H2, and we may need other settings
    // and this seems to be the only way to do it if we use the EmbeddedDatabaseBuilder.
    builder.setName(getNameAndSettings(databaseConfig));
    builder.setType(EmbeddedDatabaseType.H2);
    inMemoryDatabase = builder.build();

    return inMemoryDatabase;
  }

  public void shutDownDatabase() {
    inMemoryDatabase.shutdown();
  }

  private String getNameAndSettings(final DatabaseConfig databaseConfig) {
    return databaseConfig.getUrl().trim().replace(H2_IN_MEMORY_URL_PREFIX, "");
  }
}
