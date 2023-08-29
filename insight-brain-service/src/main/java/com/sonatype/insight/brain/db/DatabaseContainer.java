/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.database.MtiqTempUtils;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

/**
 * <p>
 *   Encapsulate all the database layer objects.
 * </p>
 * <p>
 * The application performs all database connection and initialization before the Guice injection occurs. We need some
 * similar control over the instances created of database classes so this class can be considered to be a simple way to
 * track and manage those classes.
 * </p>
 */
public class DatabaseContainer
{
  private final DataSourceFactory dataSourceFactory;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  public DatabaseContainer(
      final DataSourceFactory dataSourceFactory,
      final DatabaseProvisionUtils databaseProvisionUtils)
  {
    MtiqTempUtils.logTodo("Constructor to likely be removed");
    this.dataSourceFactory = dataSourceFactory;
    this.databaseProvisionUtils = databaseProvisionUtils;
  }

  /**
   * Default constructor which will produce all database related objects
   */
  public DatabaseContainer() {
    MtiqTempUtils.logTodo("Constructor to be replaced with a single parameter InsightConfig");
    this.dataSourceFactory = new DataSourceFactory();
    DatabaseMigrator databaseMigrator = new DatabaseMigrator(dataSourceFactory);

    OperationalDataStore operationalDataStore = new DefaultOperationalDataStore(dataSourceFactory, databaseMigrator);
    AggregationDataStore aggregationDataStore = new DefaultAggregationDataStore(dataSourceFactory, databaseMigrator);
    DataMartDataStore dataMartDataStore = new DefaultDataMartDataStore(dataSourceFactory, databaseMigrator);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new DefaultThirdPartyScansDataStore(dataSourceFactory, databaseMigrator);

    this.databaseProvisionUtils =
        new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
            thirdPartyScansDataStore);
  }

  public DataSourceFactory getDataSourceFactory() {
    return dataSourceFactory;
  }

  public DatabaseProvisionUtils getDatabaseProvisionUtils() {
    return databaseProvisionUtils;
  }
}
