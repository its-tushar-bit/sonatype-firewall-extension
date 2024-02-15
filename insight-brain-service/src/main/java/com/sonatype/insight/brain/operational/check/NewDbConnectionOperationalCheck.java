/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the process can open new connections to the databases.
 * 
 * Usage: curl -u admin:admin123 http://localhost:8071/healthcheck?pretty=true
 */
@Named
@Singleton
public class NewDbConnectionOperationalCheck
    extends AbstractDbOperationalCheck
{
  private static final Logger log = LoggerFactory.getLogger(NewDbConnectionOperationalCheck.class);

  @Inject
  public NewDbConnectionOperationalCheck(
      final OperationalDataStore operationalDataStore,
      final DataMartDataStore dataMartDataStore,
      final AggregationDataStore aggregationDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    super("newDatabaseConnections", operationalDataStore, dataMartDataStore, aggregationDataStore,
        thirdPartyScansDataStore);
  }

  @Override
  protected void checkConnection(ResultBuilder resultBuilder, DataStore dataStore) {
    if (operationalDataStore.isDatabaseInMemory()) {
      // For the in memory db (tests only), this check is not interesting.
      return;
    }

    String messageKey = dataStore.getID() + " database";

    try (
        BasicDataSource tempDataSource =
            (BasicDataSource) dataStore.getDataSourceProvider().createNewDataSource(dataStore.getDatabaseConfig());
        Connection tempConnection = tempDataSource.getConnection()) {
      long start = System.currentTimeMillis();
      boolean isValidConnection = tempConnection.isValid(5 /* timeout in seconds */);
      long duration = System.currentTimeMillis() - start;

      if (!isValidConnection) {
        resultBuilder.withDetail(messageKey,
            "Cannot open new connections to the database. The connection failed after " + duration + " ms.");
        resultBuilder.unhealthy();
        return;
      }
      resultBuilder.withDetail(messageKey, "roundTripTimeInMs=" + duration);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      resultBuilder.withDetail(messageKey, "Cannot open new connections to the database: " + e.getMessage());
      resultBuilder.unhealthy(e);
    }
  }
}
