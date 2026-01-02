/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(SlowTest.class)
public class TenantSizeMetricsJobTest
    extends AbstractMultiTenantDatabaseTest
{
  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(suppressMigrations = true, cleanDatabase = true)
  public void testQuartzTableDoesExist() {
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    TenantSizeMetricsJob tenantSizeMetricsJob =
        new TenantSizeMetricsJob(taskScheduler, databaseRule.getOperationalDataStore());

    // with a clean database assert the view does not yet exist
    assertViewExists(false);

    // run the job and now the view should exist
    tenantSizeMetricsJob.execute(mock(JobExecutionContext.class));
    assertViewExists(true);

    // create a tenant
    Tenant tenant = provisionTestTenant();

    // assert view has NOT been updated (since it is a materialized view and has not been refreshed)
    assertSchemaResults(false, tenant.databaseSchema);

    // execute the job again and the tenant should now be present
    tenantSizeMetricsJob.execute(mock(JobExecutionContext.class));
    assertSchemaResults(true, tenant.databaseSchema);
  }

  private void assertViewExists(final boolean exists) {
    String sql = "SELECT COUNT(*) FROM pg_matviews WHERE matviewname = 'schema_size'";
    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      Integer count = result.getInt(1);
      assertThat(count == (exists ? 1 : 0)).isTrue();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertSchemaResults(boolean testTenantExists, final String tenantSchemaName) {
    String sql = "SELECT * FROM public.schema_size";
    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection();
         Statement statement = connection.createStatement();
         ResultSet results = statement.executeQuery(sql)) {
      boolean foundTestTenant = false;
      while (results.next()) {
        String schemaName = results.getString("schema_name");
        if (schemaName.equals(tenantSchemaName)) {
          foundTestTenant = true;
        }
        Long schemaSize = results.getLong("schema_size");
        assertThat(schemaSize).isGreaterThan(0);
      }
      assertThat(foundTestTenant).isEqualTo(testTenantExists);
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
