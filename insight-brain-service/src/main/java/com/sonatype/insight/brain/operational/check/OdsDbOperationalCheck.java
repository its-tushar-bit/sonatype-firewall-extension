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
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the process can access the ods db.
 *
 * @since 1.66
 */
@Named
@Singleton
public class OdsDbOperationalCheck
    extends AbstractOperationalCheck
{
  private static final Logger log = LoggerFactory.getLogger(OdsDbOperationalCheck.class);

  private final OperationalDataStore operationalDataStore;

  @Inject
  public OdsDbOperationalCheck(final OperationalDataStore operationalDataStore) {
    super("ods-database");
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  protected Result check() throws Exception {
    return checkConnection(operationalDataStore.getDataSource());
  }

  public static Result checkConnection(DataSource datasource) {
    try (Connection connection = datasource.getConnection()) {
      ResultBuilder resultBuilder = Result.builder();

      long start = System.currentTimeMillis();
      boolean isValidConnection = connection.isValid(3 /* timeout in seconds */);
      long duration = System.currentTimeMillis() - start;

      if (isValidConnection) {
        return resultBuilder.withDetail("roundTripTimeInMs", duration).build();
      }
      else {
        return Result.unhealthy("Cannot access the database. The connection timed out after " + duration + " ms.");
      }
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.unhealthy("Cannot access the database: " + e.getMessage());
    }
  }
}
