/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * {@link DataSourceProvider} for H2 disk based databases. With H2 IQ maintains a separate physical database for each
 * {@link DataStore}.
 */
public class H2DiskDataSourceProvider
    extends AbstractDataSourceProvider
    implements DataSourceProvider, LegacyDataSourceProvider
{
  // H2 has one DataSource object for each of the four data stores
  private final Map<String, DataSource> dataSources = new LinkedHashMap<>();

  @Override
  public DataSource getDataSource(final DatabaseConfig databaseConfig, final String dataStoreId) {
    synchronized (dataSources) {
      return dataSources.computeIfAbsent(dataStoreId, val -> createNewDataSource(databaseConfig));
    }
  }

  public void shutDownDatabase() {
    dataSources.forEach((id, dataSource) -> {
      if (dataSource instanceof BasicDataSource bds && bds.isClosed()) {
        log.debug("Skipping SHUTDOWN for data store '{}' — pool already closed", id);
        return;
      }
      try (Connection connection = dataSource.getConnection()) {
        connection.createStatement().execute("SHUTDOWN");
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Close all DBCP2 connection pools managed by this provider.
   * Called after SHUTDOWN commands have been sent to each H2 database.
   */
  public void closeAllDataSources() {
    dataSources.forEach((id, dataSource) -> {
      if (dataSource instanceof BasicDataSource bds && !bds.isClosed()) {
        try {
          bds.close();
        }
        catch (Exception e) {
          log.warn("Error closing pool for data store '{}': {}", id, e.getMessage());
        }
      }
    });
  }
}
