/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OdsDbOperationalCheckTest
    extends AbstractComponentTest
{
  @Inject
  private OdsDbOperationalCheck odsDbOperationalCheck;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Test
  public void testExecute_Healthy() {
    Result result = odsDbOperationalCheck.execute();
    assertThat(result.isHealthy()).isTrue();
    Map<String, Object> resultDetails = result.getDetails();
    assertThat((long) resultDetails.get("roundTripTimeInMs")).isGreaterThanOrEqualTo(0L);
  }

  @Test
  public void testExecute_Unhealthy() throws Exception {
    try {
      try (Connection connection = operationalDataStore.getDataSource().getConnection();
          Statement statement = connection.createStatement()) {
        statement.execute("DROP SCHEMA " + OperationalDataStore.ID);
      }
      Result result = odsDbOperationalCheck.execute();
      assertThat(result.isHealthy()).isFalse();
      assertThat(result.getMessage()).contains("Cannot access the database: ");
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
