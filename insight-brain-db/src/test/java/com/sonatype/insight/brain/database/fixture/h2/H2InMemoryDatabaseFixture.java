/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.fixture.h2;

import com.sonatype.insight.brain.database.H2InMemoryTestDataSourceProvider;
import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.brain.database.fixture.DatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note that we are using H2 in-memory 'private' databases for testing. These have no name and basically if you open a
 * connection to one using the 'jdbc:h2:mem' syntax you get a new one. See the <a
 * href="http://www.h2database.com/html/features.html#in_memory_databases">H2 in-memory docs</a> for more info. This
 * means that we do not use the dataStoreId and databaseSchema parameters.
 */
public class H2InMemoryDatabaseFixture
    implements DatabaseFixture
{
  private static final Logger log = LoggerFactory.getLogger(H2InMemoryDatabaseFixture.class);

  public H2InMemoryDatabaseFixture() {
    log.info("Creating new H2 in-memory test database");
  }

  @Override
  public void close() throws Exception {
    log.info("Destroying H2 in-memory test database");
  }

  @Override
  public DatabaseConfig getDatabaseConfig() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setUrl("jdbc:h2:mem:;DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    return databaseConfig;
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return new H2InMemoryTestDataSourceProvider();
  }
}
