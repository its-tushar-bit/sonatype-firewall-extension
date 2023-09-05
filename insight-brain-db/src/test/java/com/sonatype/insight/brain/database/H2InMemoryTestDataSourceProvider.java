/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database;

import javax.sql.DataSource;

import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * {@link DataSourceProvider} for unit/integration tests using an H2 in-memory {@link DataSource}
 */
public class H2InMemoryTestDataSourceProvider
    implements DataSourceProvider
{
  @Override
  public DataSource getDataSource(
      final DatabaseConfig databaseConfig /* unused */,
      final String dataStoreId /* unused */)
  {
    return createNewInMemoryDataSource();
  }

  private DataSource createNewInMemoryDataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    // UGLY HACK: We need to specify DATABASE_TO_UPPER=FALSE for birt over H2
    // and this seems to be the only way to do it if we use the EmbeddedDatabaseBuilder.
    builder.setName("inMemoryDatabase;DATABASE_TO_UPPER=FALSE");
    builder.setType(EmbeddedDatabaseType.H2);
    EmbeddedDatabase inMemoryDatabase = builder.build();

    MtiqTempUtils.logTodo("Is h2Port needed still? Doesn't appear to be used anywhere in original code");
    /*
    if (h2Port != null && !h2Port.isEmpty()) {
      try {
        // To connect to the in memory h2 database with a sql client, use user name "sa" and url:
        // jdbc:h2:tcp://localhost:9092/mem:inMemoryDatabase;DATABASE_TO_UPPER=FALSE
        Server.createTcpServer("-tcpPort", h2Port, "-tcpDaemon").start();
        log.info("Exposed embedded DB via TCP at port {}", h2Port);
      }
      catch (Exception e) {
        log.warn("Could not expose embedded DB via TCP", e);
      }
    }
    */
    return inMemoryDatabase;
  }
}
