/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class OdsDbOperationalCheckTest
    extends AbstractDatabaseTest
{
  private OdsDbOperationalCheck odsDbOperationalCheck;

  private OperationalDataStore operationalDataStore;

  @Before
  public void before() {
    this.operationalDataStore = spy(databaseRule.getOperationalDataStore());
    odsDbOperationalCheck = new OdsDbOperationalCheck(this.operationalDataStore);
  }

  @Test
  public void testExecute_Healthy() {
    Result result = odsDbOperationalCheck.execute();
    assertThat(result.isHealthy()).isTrue();
    Map<String, Object> resultDetails = result.getDetails();
    assertThat((long) resultDetails.get("roundTripTimeInMs")).isGreaterThanOrEqualTo(0L);
  }

  @Test
  public void testExecute_Unhealthy() throws Exception {
    DataSource mockDataSource = mock();
    Connection mockConnection = mock();
    when(operationalDataStore.getDataSource()).thenReturn(mockDataSource);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.isValid(3)).thenReturn(false);

    Result result = odsDbOperationalCheck.execute();
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).startsWith("Cannot access the database. The connection timed out after");
  }
}
