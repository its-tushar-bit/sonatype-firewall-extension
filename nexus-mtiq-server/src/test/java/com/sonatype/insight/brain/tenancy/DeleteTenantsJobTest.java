/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.JOB_FREQUENCY_IN_HOURS;
import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.TENANT_RETENTION_PERIOD_CONFIG_KEY;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeleteTenantsJobTest
    extends AbstractMultiTenantResourceTest
{
  DeleteTenantsJob deleteTenantsJob;

  TenantManager tenantManager;

  InsightConfig config;

  OperationalDataStore dataStore;

  DeletedTenantDAO deletedTenantDAO;

  @Before
  public void setup() {
    tenantManager = super.getTestCLMServer().getCLMServer().getInstance(TenantManager.class);
    deleteTenantsJob = super.getTestCLMServer().getCLMServer().getInstance(DeleteTenantsJob.class);
    config = super.getTestCLMServer().getCLMServer().getInstance(InsightConfig.class);
    dataStore = super.getTestCLMServer().getCLMServer().getInstance(OperationalDataStore.class);
    deletedTenantDAO = new DeletedTenantDAO();
  }

  @Test
  public void testDeleteTenant() {
    testAsNewTenant(testName, t -> {
      provisionTenant(t.tenantSlug);

      tenantManager.setTenant(t);

      long beforeDefaultRetentionPeriod =
          System.currentTimeMillis() - (60 * 1000 * (DeleteTenantsJob.DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS + 1));

      deletedTenantDAO.insert(new DeletedTenant(t.tenantSlug, beforeDefaultRetentionPeriod));

      File sonatypeWork = config.getSonatypeWork();
      File clusterDirectory = config.getClusterDirectory();

      initializeTenantDirectories();

      assertThat(DatabaseUtil.schemaExists(dataStore.getDataSource(), t.databaseSchema)).isTrue();
      assertThat(sonatypeWork.exists()).isTrue();
      assertThat(clusterDirectory.exists()).isTrue();

      deleteTenantsJob.execute(null);

      assertThat(DatabaseUtil.schemaExists(dataStore.getDataSource(), t.databaseSchema)).isFalse();
      assertThat(sonatypeWork.exists()).isFalse();
      assertThat(clusterDirectory.exists()).isFalse();

      assertThat(deletedTenantDAO.getTenantBySlug(t.tenantSlug)).isNull();
    });
  }

  @Test
  public void testRetentionPeriodCanBeConfigured() {
    testAsNewTenant(testName, t -> {
      int retentionPeriodInHours = 1;

      provisionTenant(t.tenantSlug);

      tenantManager.setTenant(t);

      tempEntity.newSystemConfigurationProperty(TENANT_RETENTION_PERIOD_CONFIG_KEY,
          String.valueOf(retentionPeriodInHours));

      long beforeDefaultRetentionPeriod =
          System.currentTimeMillis() - (60 * 1000 * (retentionPeriodInHours + 1));

      deletedTenantDAO.insert(new DeletedTenant(t.tenantSlug, beforeDefaultRetentionPeriod));

      deleteTenantsJob.execute(null);

      assertThat(deletedTenantDAO.getTenantBySlug(t.tenantSlug)).isNull();
    });
  }

  @Test
  public void testRegistration() {
    TaskScheduler taskScheduler = mock(TaskScheduler.class);

    deleteTenantsJob = new DeleteTenantsJob(taskScheduler, null, null, null, null);

    deleteTenantsJob.register();

    verify(taskScheduler).schedulePeriodicTask(any(DeleteTenantsJob.class),
        eq(Duration.ofHours(JOB_FREQUENCY_IN_HOURS)));
  }

  private void initializeTenantDirectories() throws IOException {
    Files.createDirectories(config.getSonatypeWork().toPath());
    Files.createDirectories(config.getClusterDirectory().toPath());
  }
}
