/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.EmbeddedDataSourceFactory;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class H2DataSourceFactory
    implements EmbeddedDataSourceFactory
{
  private static final Logger log = LoggerFactory.getLogger(H2DataSourceFactory.class);

  private static volatile DataSource inMemoryDatabase;

  private final String h2Port;

  public H2DataSourceFactory() {
    this(null);
  }

  public H2DataSourceFactory(String h2Port) {
    this.h2Port = h2Port;
  }

  @Override
  public DataSource getDataSource(String databaseName) {
    if (inMemoryDatabase != null) {
      return inMemoryDatabase;
    }

    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    // UGLY HACK: We need to specify DATABASE_TO_UPPER=FALSE for birt over H2
    // and this seems to be the only way to do it if we use the EmbeddedDatabaseBuilder.
    builder.setName("inMemoryDatabase;DATABASE_TO_UPPER=FALSE");
    builder.setType(EmbeddedDatabaseType.H2);
    inMemoryDatabase = builder.build();

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

    return inMemoryDatabase;
  }
}
