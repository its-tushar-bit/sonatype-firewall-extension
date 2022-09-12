/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.service.AdminHealthCheckEndpoint.HealthCheckResponse;

import org.junit.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class DatabaseAdminHealthCheckEndpointTest
    extends AbstractComponentTest
{
  @Inject
  private DatabaseAdminHealthCheckEndpoint databaseAdminHealthCheckEndpoint;

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
    try (MockedStatic<OperationalDataStoreProvider> odsMock = mockStatic(OperationalDataStoreProvider.class)) {
      DataSource dataSource = mockInvalidConnection();
      odsMock.when(OperationalDataStoreProvider::getDataSource).thenReturn(dataSource);
      assertInvalidConnection();
    }
  }

  @Test
  public void testIsHealthy_InvalidConnection_ThirdParty() throws Exception {
    try (MockedStatic<ThirdPartyScansProvider> tpMock = mockStatic(ThirdPartyScansProvider.class)) {
      DataSource dataSource = mockInvalidConnection();
      tpMock.when(ThirdPartyScansProvider::getDataSource).thenReturn(dataSource);
      assertInvalidConnection();
    }
  }

  @Test
  public void testIsHealthy_InvalidConnection_Aggregate() throws Exception {
    try (MockedStatic<AggregationDataStoreProvider> aggregationMock = mockStatic(AggregationDataStoreProvider.class)) {
      DataSource dataSource = mockInvalidConnection();
      aggregationMock.when(AggregationDataStoreProvider::getDataSource).thenReturn(dataSource);
      assertInvalidConnection();
    }
  }

  @Test
  public void testIsHealthy_InvalidConnection_Datamart() throws Exception {
    try (MockedStatic<DatamartProvider> dmMock = mockStatic(DatamartProvider.class)) {
      DataSource dataSource = mockInvalidConnection();
      dmMock.when(DatamartProvider::getDataSource).thenReturn(dataSource);
      assertInvalidConnection();
    }
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
