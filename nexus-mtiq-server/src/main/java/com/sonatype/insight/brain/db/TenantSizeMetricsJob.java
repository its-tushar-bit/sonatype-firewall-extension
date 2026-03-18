/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the tenant schema size MATERIALIZED VIEW once per day.
 * <br>
 * It is useful to see tenant schema sizes over time. However, the queries to retrieve tenant schemas are expensive,
 * especially as the number of tenants grow. This compounded with the fact that the custom database metrics on the
 * Datadog side run often (multiple times per minute) make it too expensive to run this way. To solve this we use a
 * MATERIALIZED VIEW in the database which the Datadog DB APM queries often but is only updated by this class daily.
 * <br>
 * This view is created in the `public` schema as we don't want to create a diff in a tenant schema (and at the schema
 * level `global` is still a tenant).
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class TenantSizeMetricsJob
    implements InsightJob, GlobalTenantJob
{
  public static final String NAME = "TenantSizeMetrics";

  // There is no 'OR REPLACE' syntax for materialized view. This is the same as a REFRESH and makes for easy start.
  private static final String DROP_MATERIALIZE_VIEW = "DROP MATERIALIZED VIEW IF EXISTS public.schema_size";

  private static final String VIEW_EXISTS = "SELECT COUNT(*) FROM pg_matviews WHERE matviewname = 'schema_size'";

  private static final String CREATE_MATERIALIZE_VIEW = """
      CREATE MATERIALIZED VIEW public.schema_size AS
      SELECT
        schema_name,
        sum(table_size) as schema_size,
        round((sum(table_size) / database_size) * 100, 2) as percentage_of_db
        FROM (
            SELECT pg_catalog.pg_namespace.nspname as schema_name,
            pg_relation_size(pg_catalog.pg_class.oid) as table_size,
        sum(pg_relation_size(pg_catalog.pg_class.oid)) over () as database_size
        FROM   pg_catalog.pg_class
        JOIN pg_catalog.pg_namespace ON relnamespace = pg_catalog.pg_namespace.oid
      ) t
      GROUP BY schema_name, database_size
      ORDER BY sum(table_size) DESC;""";

  private static final String REFRESH_MATERIALIZED_VIEW = "REFRESH MATERIALIZED VIEW public.schema_size";

  private static final Logger log = LoggerFactory.getLogger(TenantSizeMetricsJob.class);

  private final TaskScheduler taskScheduler;

  private final OperationalDataStore operationalDataStore;

  public boolean disableForTesting;

  @Inject
  public TenantSizeMetricsJob(final TaskScheduler taskScheduler, final OperationalDataStore operationalDataStore) {
    this.taskScheduler = taskScheduler;
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  public String getJobName() {
    return NAME;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }

    taskScheduler.scheduleDailyTask(this, LocalTime.of(2, 0));
  }

  @Override
  public void deregister() {
    // no-op
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.info("Updating materialized view for tenant database schema sizes");
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      if (viewExists(statement)) {
        refreshMaterializedView(statement);
      }
      else {
        createMaterializedView(statement);
      }
    }
    catch (Exception e) {
      log.error("Failed to execute MATERIALIZED VIEW for tenant schema size", e);
    }
  }

  private boolean viewExists(final Statement statement) throws SQLException {
    ResultSet resultSet = statement.executeQuery(VIEW_EXISTS);
    if (resultSet.next()) {
      return resultSet.getInt(1) > 0;
    }
    return false;
  }

  private void refreshMaterializedView(final Statement statement) throws SQLException {
    statement.executeUpdate(REFRESH_MATERIALIZED_VIEW);
  }

  /**
   * Since we don't have MTIQ-specific db migrations yet... create the view internally here
   */
  private void createMaterializedView(final Statement statement) throws SQLException {
    statement.executeUpdate(DROP_MATERIALIZE_VIEW);
    statement.executeUpdate(CREATE_MATERIALIZE_VIEW);
  }
}
