/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.sonatype.insight.db.AbstractDataSourceFactory;
import com.sonatype.insight.db.DatabaseConfig;

public class DataSourceFactory
    extends AbstractDataSourceFactory
{
  private static Map<String, DataSource> dataSources = new LinkedHashMap<String, DataSource>();

  private static Map<DataSource, Boolean> newDataSources = new LinkedHashMap<DataSource, Boolean>();

  public DataSourceFactory() {
    super(null, null);
  }

  @Override
  protected Map<String, DataSource> getDataSources() {
    return dataSources;
  }

  @Override
  protected DataSource loadDataSource(DatabaseConfig databaseConfig, String databaseName) {
    DataSource dataSource = super.loadDataSource(databaseConfig, databaseName);
    boolean isNew = populateDatabaseSchema(dataSource, databaseName);
    newDataSources.put(dataSource, isNew);

    return dataSource;
  }

  boolean isNewDataSource(DataSource dataSource) {
    return newDataSources.get(dataSource);
  }

  public static void clear_ForTestsOnly() {
    synchronized (dataSources) {
      dataSources.clear();
      DatamartProvider.clear_ForTestsOnly();
      OperationalDataStoreProvider.clear_ForTestsOnly();
    }
  }
}
