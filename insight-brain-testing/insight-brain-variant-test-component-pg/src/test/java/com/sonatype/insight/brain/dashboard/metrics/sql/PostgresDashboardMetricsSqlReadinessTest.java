/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.sql.Connection;
import java.sql.Statement;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.INVALID;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.MISSING;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ComponentPgTest
public class PostgresDashboardMetricsSqlReadinessTest
    extends AbstractComponentPgTest
{
  private static final String INDEX_NAME = "policy_violation_app_stage_open_unfixed_idx";

  @Inject
  private OperationalDataStore operationalDataStore;

  @BeforeEach
  public void dropReadinessIndex() throws Exception {
    dropIndex();
  }

  @AfterEach
  public void restoreReadinessIndex() throws Exception {
    dropIndex();
    createIndex();
  }

  @Test
  public void validIndexIsReportedValid() throws Exception {
    createIndex();

    assertThat(newReadiness().state()).isEqualTo(VALID);
  }

  @Test
  public void invalidIndexIsReportedInvalid() throws Exception {
    createIndex();
    execute("UPDATE pg_index SET indisvalid = false WHERE indexrelid = '"
        + schema() + "." + INDEX_NAME + "'::regclass");

    assertThat(newReadiness().state()).isEqualTo(INVALID);
  }

  @Test
  public void missingIndexIsReportedMissing() {
    assertThat(newReadiness().state()).isEqualTo(MISSING);
  }

  private DashboardMetricsSqlReadiness newReadiness() {
    return new DashboardMetricsSqlReadiness(
        operationalDataStore,
        mock(DashboardMetricsSqlModeProvider.class),
        mock(DashboardMetricsSqlTelemetry.class));
  }

  private void createIndex() throws Exception {
    execute("CREATE INDEX " + INDEX_NAME + " ON " + schema() + ".policy_violation "
        + "(owner_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id) "
        + "WHERE fix_time IS NULL");
  }

  private void dropIndex() throws Exception {
    execute("DROP INDEX IF EXISTS " + schema() + "." + INDEX_NAME);
  }

  private void execute(final String sql) throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      statement.execute(sql);
    }
  }

  private String schema() {
    return operationalDataStore.getDatabaseSchema();
  }
}
