/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.sql.Connection;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the process can access the databases using existing/pooled connections.
 *
 * Usage: curl -u admin:admin123 http://localhost:8071/healthcheck?pretty=true
 *
 * @since 1.66
 */
@Named
@Singleton
public class ExistingDbConnectionOperationalCheck
    extends AbstractDbOperationalCheck
{
  private static final Logger log = LoggerFactory.getLogger(ExistingDbConnectionOperationalCheck.class);

  @Inject
  public ExistingDbConnectionOperationalCheck(
      final OperationalDataStore operationalDataStore,
      final DataMartDataStore dataMartDataStore,
      final AggregationDataStore aggregationDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    super("database", operationalDataStore, dataMartDataStore, aggregationDataStore, thirdPartyScansDataStore);
  }

  @Override
  protected void checkConnection(ResultBuilder resultBuilder, DataStore dataStore) {
    String messageKey = dataStore.getID() + " database";

    try (Connection connection = dataStore.getDataSource().getConnection()) {
      long start = System.currentTimeMillis();
      boolean isValidConnection = connection.isValid(3 /* timeout in seconds */);
      long duration = System.currentTimeMillis() - start;

      if (!isValidConnection) {
        resultBuilder.withDetail(messageKey,
            "Cannot access the database. The connection failed after " + duration + " ms.");
        resultBuilder.unhealthy();
        return;
      }
      resultBuilder.withDetail(messageKey, "roundTripTimeInMs=" + duration);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      resultBuilder.withDetail(messageKey, "Cannot access the database: " + e.getMessage());
      resultBuilder.unhealthy(e);
    }
  }
}
