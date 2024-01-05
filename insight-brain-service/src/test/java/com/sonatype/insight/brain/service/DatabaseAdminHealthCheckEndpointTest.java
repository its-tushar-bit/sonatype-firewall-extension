/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.AdminHealthCheckEndpoint.HealthCheckResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseAdminHealthCheckEndpointTest
    extends AbstractDatabaseTest
{
  private DatabaseAdminHealthCheckEndpoint databaseAdminHealthCheckEndpoint;

  @Before
  public void setUp() {
    databaseAdminHealthCheckEndpoint = new DatabaseAdminHealthCheckEndpoint(databaseRule.getOperationalDataStore(),
        databaseRule.getDataMartDataStore(), databaseRule.getAggregationDataStore(),
        databaseRule.getThirdPartyScansDataStore());
  }

  @Test
  public void testGetName() {
    assertThat(databaseAdminHealthCheckEndpoint.getName()).isEqualTo("Database");
  }

  @Test
  public void testGetPath() {
    assertThat(databaseAdminHealthCheckEndpoint.getPath()).isEqualTo("/healthcheck/database");
  }

  @Test
  public void testIsHealthy_ValidConnection() {
    assertThat(databaseAdminHealthCheckEndpoint.getHealthCheckResponse())
        .usingRecursiveComparison().isEqualTo(new HealthCheckResponse(true));
  }

  @Test
  public void testIsHealthy_InvalidConnection_ODS() throws Exception {
    databaseAdminHealthCheckEndpoint = new DatabaseAdminHealthCheckEndpoint(buildMockedOperationalDataStore(),
        databaseRule.getDataMartDataStore(), databaseRule.getAggregationDataStore(),
        databaseRule.getThirdPartyScansDataStore());
    assertInvalidConnection();
  }

  @Test
  public void testIsHealthy_InvalidConnection_ThirdParty() throws Exception {
    databaseAdminHealthCheckEndpoint = new DatabaseAdminHealthCheckEndpoint(databaseRule.getOperationalDataStore(),
        databaseRule.getDataMartDataStore(), databaseRule.getAggregationDataStore(),
        buildMockedThirdPartyScansDataStore());
    assertInvalidConnection();
  }

  @Test
  public void testIsHealthy_InvalidConnection_Aggregate() throws Exception {
    databaseAdminHealthCheckEndpoint = new DatabaseAdminHealthCheckEndpoint(databaseRule.getOperationalDataStore(),
        databaseRule.getDataMartDataStore(), buildMockedAggregationDataStore(),
        databaseRule.getThirdPartyScansDataStore());
    assertInvalidConnection();
  }

  @Test
  public void testIsHealthy_InvalidConnection_Datamart() throws Exception {
    databaseAdminHealthCheckEndpoint = new DatabaseAdminHealthCheckEndpoint(databaseRule.getOperationalDataStore(),
        buildMockedDataMartDataStore(), databaseRule.getAggregationDataStore(),
        databaseRule.getThirdPartyScansDataStore());
    assertInvalidConnection();
  }

  private OperationalDataStore buildMockedOperationalDataStore() throws Exception {
    OperationalDataStore operationalDataStore = mock(OperationalDataStore.class);
    DataSource dataSource = mockInvalidConnection();
    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    return operationalDataStore;
  }

  private DataMartDataStore buildMockedDataMartDataStore() throws Exception {
    DataMartDataStore dataMartDataStore = mock(DataMartDataStore.class);
    DataSource dataSource = mockInvalidConnection();
    when(dataMartDataStore.getDataSource()).thenReturn(dataSource);
    return dataMartDataStore;
  }

  private AggregationDataStore buildMockedAggregationDataStore() throws Exception {
    AggregationDataStore aggregationDataStore = mock(AggregationDataStore.class);
    DataSource dataSource = mockInvalidConnection();
    when(aggregationDataStore.getDataSource()).thenReturn(dataSource);
    return aggregationDataStore;
  }

  private ThirdPartyScansDataStore buildMockedThirdPartyScansDataStore() throws Exception {
    ThirdPartyScansDataStore thirdPartyScansDataStore = mock(ThirdPartyScansDataStore.class);
    DataSource dataSource = mockInvalidConnection();
    when(thirdPartyScansDataStore.getDataSource()).thenReturn(dataSource);
    return thirdPartyScansDataStore;
  }

  private void assertInvalidConnection() {
    HealthCheckResponse response = databaseAdminHealthCheckEndpoint.getHealthCheckResponse();
    assertThat(response.isHealthy()).isFalse();
    assertThat(response.getContent())
        .matches("^Cannot access the database\\. The connection timed out after \\d+ ms\\.$");
  }

  private DataSource mockInvalidConnection() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(connection.isValid(anyInt())).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(connection);
    return dataSource;
  }
}
