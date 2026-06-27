/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryComponentDedupKeysetIndexAsyncDbMigration} (CLM-41005). Verifies
 * the H2 skip path, the Postgres {@code CREATE INDEX CONCURRENTLY} path, and the invalid-index
 * cleanup branch shared with the sibling migration.
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryComponentDedupKeysetIndexAsyncDbMigrationTest
{
  @Mock
  private MigrationTrackerDAO migrationTrackerDAO;

  @Mock
  private OperationalDataStore operationalDataStore;

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement invalidIndexStmt;

  @Mock
  private Statement createIndexStmt;

  @Mock
  private ResultSet invalidIndexResultSet;

  private RepositoryComponentDedupKeysetIndexAsyncDbMigration underTest;

  @Before
  public void setup() {
    underTest = new RepositoryComponentDedupKeysetIndexAsyncDbMigration(migrationTrackerDAO, operationalDataStore);
  }

  @Test
  public void executeMigration_skipsOnEmbeddedH2() throws Exception {
    when(operationalDataStore.isDatabaseEmbedded()).thenReturn(true);

    assertThat(underTest.executeMigration()).isTrue();

    verifyNoInteractions(dataSource);
    verifyNoInteractions(connection);
  }

  @Test
  public void executeMigration_createsIndexConcurrentlyOnPostgresWhenNoInvalidExists() throws Exception {
    stubPostgresPath();
    when(invalidIndexResultSet.next()).thenReturn(false); // no invalid index to drop

    assertThat(underTest.executeMigration()).isTrue();

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(createIndexStmt).execute(sqlCaptor.capture());
    assertThat(sqlCaptor.getValue())
        .contains("CREATE INDEX CONCURRENTLY IF NOT EXISTS repository_component_dedup_keyset_idx")
        .contains("ON public.repository_component "
            + "(repository_id, hash, time DESC, repository_component_id DESC)");
    verify(createIndexStmt, never()).execute(
        org.mockito.ArgumentMatchers.startsWith("DROP INDEX CONCURRENTLY"));
  }

  @Test
  public void executeMigration_dropsInvalidIndexBeforeCreate() throws Exception {
    stubPostgresPath();
    when(invalidIndexResultSet.next()).thenReturn(true); // an invalid index left by a prior failed attempt
    Statement dropStmt = org.mockito.Mockito.mock(Statement.class);
    // First call returns the drop stmt; second call returns the create stmt (sequential createStatement() invocations).
    when(connection.createStatement()).thenReturn(dropStmt, createIndexStmt);

    assertThat(underTest.executeMigration()).isTrue();

    verify(dropStmt).execute("DROP INDEX CONCURRENTLY public.repository_component_dedup_keyset_idx");
    verify(createIndexStmt).execute(anyString());
  }

  @Test
  public void executeMigration_returnsFalseOnSqlError() throws Exception {
    when(operationalDataStore.isDatabaseEmbedded()).thenReturn(false);
    when(operationalDataStore.getDatabaseSchema()).thenReturn("public");
    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("connection refused"));

    assertThat(underTest.executeMigration()).isFalse();
  }

  private void stubPostgresPath() throws Exception {
    when(operationalDataStore.isDatabaseEmbedded()).thenReturn(false);
    when(operationalDataStore.getDatabaseSchema()).thenReturn("public");
    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(invalidIndexStmt);
    when(invalidIndexStmt.executeQuery()).thenReturn(invalidIndexResultSet);
    when(connection.createStatement()).thenReturn(createIndexStmt);
  }
}
