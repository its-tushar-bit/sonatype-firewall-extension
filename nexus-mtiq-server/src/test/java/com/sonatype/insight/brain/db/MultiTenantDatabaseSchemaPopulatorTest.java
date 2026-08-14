/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantDatabaseSchemaPopulatorTest
{
  @Mock
  private DataSource mockDataSource;

  @Mock
  private Connection mockConnection;

  private final String dataStoreId = "test";

  private final String databaseSchema = "test";

  @Test
  public void testDatabaseSchemaDoesNotExist() throws SQLException {
    mockSuperIsSchemaAlreadyPopulated(false);

    MultiTenantDatabaseSchemaPopulator populator =
        new MultiTenantDatabaseSchemaPopulator(mockDataSource, dataStoreId, databaseSchema);
    boolean result = populator.isSchemaAlreadyPopulated(mockConnection);
    assertThat(result).isFalse();
    // verify connection for the second check was not used
    verify(mockDataSource, times(0)).getConnection();
  }

  @Test
  public void testDatabaseSchemaExistsButNoSchemaVersion() throws SQLException {
    mockSuperIsSchemaAlreadyPopulated(true);
    mockMultiTenantCheck(false);

    MultiTenantDatabaseSchemaPopulator populator =
        new MultiTenantDatabaseSchemaPopulator(mockDataSource, dataStoreId, databaseSchema);
    boolean result = populator.isSchemaAlreadyPopulated(mockConnection);
    assertThat(result).isFalse();
    // verify connection for the second check was used
    verify(mockDataSource, times(1)).getConnection();
  }

  @Test
  public void testDatabaseSchemaExistsAndHasSchemaVersion() throws SQLException {
    mockSuperIsSchemaAlreadyPopulated(true);
    mockMultiTenantCheck(true);

    MultiTenantDatabaseSchemaPopulator populator =
        new MultiTenantDatabaseSchemaPopulator(mockDataSource, dataStoreId, databaseSchema);
    boolean result = populator.isSchemaAlreadyPopulated(mockConnection);
    assertThat(result).isTrue();
    // verify connection for the second check was used
    verify(mockDataSource, times(1)).getConnection();
  }

  /**
   * Mock the logic in the parent isSchemaAlreadyPopulated. This is far too much insight into that super method... but
   * don't see other options right now
   */
  private void mockSuperIsSchemaAlreadyPopulated(boolean result) throws SQLException {
    DatabaseMetaData mockDatabaseMetaData = mock(DatabaseMetaData.class);
    ResultSet mockResultSet = mock(ResultSet.class);

    when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
    when(mockDatabaseMetaData.getSchemas(isNull(), anyString())).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(result);
  }

  private void mockMultiTenantCheck(final Boolean result) throws SQLException {
    Statement mockStatement = mock(Statement.class);
    ResultSet mockResultSet = mock(ResultSet.class);

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(result);
    lenient().when(mockResultSet.isLast()).thenReturn(result);
  }
}
