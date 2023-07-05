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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ApiSupplier;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.impl.matchers.GroupMatcher;

import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.JOB_FREQUENCY_IN_HOURS;
import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.TENANT_RETENTION_PERIOD_CONFIG_KEY;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DeleteTenantsJobTest
    extends AbstractMultiTenantResourceTest
{
  DeleteTenantsJob deleteTenantsJob;

  TenantManager tenantManager;

  InsightConfig config;

  OperationalDataStore dataStore;

  DeletedTenantDAO deletedTenantDAO;

  TaskScheduler taskScheduler;

  TenantMetadataDAO tenantMetadataDAO;

  @Before
  public void setup() {
    tenantManager = super.getTestCLMServer().getCLMServer().getInstance(TenantManager.class);
    deleteTenantsJob = spy(super.getTestCLMServer().getCLMServer().getInstance(DeleteTenantsJob.class));
    config = super.getTestCLMServer().getCLMServer().getInstance(InsightConfig.class);
    dataStore = super.getTestCLMServer().getCLMServer().getInstance(OperationalDataStore.class);
    taskScheduler = super.getTestCLMServer().getCLMServer().getInstance(MultiTenantTaskScheduler.class);
    deletedTenantDAO = new DeletedTenantDAO();
    tenantMetadataDAO = new TenantMetadataDAO();
  }

  @Override
  protected List<Module> getBrainModules() {
    List<Module> brainModules = super.getBrainModules();
    brainModules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MultiTenantAuth0ManagementService.class).toInstance(new TestMultiTenantAuth0ManagementService());
      }
    });
    return brainModules;
  }

  @Test
  public void testDeleteTenant() {
    testAsNewTenant(testName, t -> {
      provisionTenant(t.tenantSlug);

      tenantManager.setTenant(t);

      long beforeDefaultRetentionPeriod = System.currentTimeMillis() -
          (60 * 60 * 1000 * (DeleteTenantsJob.DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS + 1));

      tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName"));

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
          System.currentTimeMillis() - (60 * 60 * 1000 * (retentionPeriodInHours + 1));

      tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName"));

      deletedTenantDAO.insert(new DeletedTenant(t.tenantSlug, beforeDefaultRetentionPeriod));

      deleteTenantsJob.execute(null);

      assertThat(deletedTenantDAO.getTenantBySlug(t.tenantSlug)).isNull();
    });
  }

  @Test
  public void testDeleteErrorAllowsOtherDeletesToRun() {
    testAsNewTenant(testName, t -> {
      int retentionPeriodInHours = 1;

      provisionTenant(t.tenantSlug);

      tenantManager.setTenant(t);

      tempEntity.newSystemConfigurationProperty(TENANT_RETENTION_PERIOD_CONFIG_KEY,
          String.valueOf(retentionPeriodInHours));

      long beforeDefaultRetentionPeriod =
          System.currentTimeMillis() - (60 * 60 * 1000 * (retentionPeriodInHours + 1));

      tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName"));

      deletedTenantDAO.insert(new DeletedTenant("error-tenant-1", beforeDefaultRetentionPeriod));
      deletedTenantDAO.insert(new DeletedTenant("error-tenant-2", beforeDefaultRetentionPeriod));
      deletedTenantDAO.insert(new DeletedTenant(t.tenantSlug, beforeDefaultRetentionPeriod));

      deleteTenantsJob.execute(null);

      assertThat(deletedTenantDAO.getTenantBySlug(t.tenantSlug)).isNull();

      verify(deleteTenantsJob, times(3)).deleteTenant(any(DeletedTenant.class));
    });
  }

  @Test
  public void testRegistration() {
    MultiTenantTaskScheduler taskScheduler = mock(MultiTenantTaskScheduler.class);

    deleteTenantsJob = new DeleteTenantsJob(taskScheduler, null, null, null, null, null, null, null);

    deleteTenantsJob.register();

    verify(taskScheduler).schedulePeriodicTask(any(DeleteTenantsJob.class),
        eq(Duration.ofHours(JOB_FREQUENCY_IN_HOURS)));
  }

  @Test
  public void testDeleteTenantDeletesQuartzJobs() {
    testAsNewTenant(testName, t -> {
      provisionTenant(t.tenantSlug);

      tenantManager.setTenant(t);

      long beforeDefaultRetentionPeriod = System.currentTimeMillis() -
          (60 * 60 * 1000 * (DeleteTenantsJob.DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS + 1));

      tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName"));

      deletedTenantDAO.insert(new DeletedTenant(t.tenantSlug, beforeDefaultRetentionPeriod));

      for (int i = 0; i < 10; i++) {
        taskScheduler.schedulePeriodicTask(newJob(), Duration.ofHours(1L));
      }

      Set<JobKey> jobs = taskScheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(t.tenantSlug));
      assertThat(jobs.size()).isNotZero();

      deleteTenantsJob.execute(null);

      jobs = taskScheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(t.tenantSlug));
      assertThat(jobs.size()).isZero();
    });
  }

  private InsightJob newJob() {
    return new InsightJob()
    {
      @Override
      public String getJobName() {
        return UUID.randomUUID().toString();
      }

      @Override
      public void execute(JobExecutionContext context) throws JobExecutionException {
        //no-op
      }
    };
  }

  private void initializeTenantDirectories() throws IOException {
    Files.createDirectories(config.getSonatypeWork().toPath());
    Files.createDirectories(config.getClusterDirectory().toPath());
  }

  private class TestMultiTenantAuth0ManagementService extends MultiTenantAuth0ManagementService
  {
    public TestMultiTenantAuth0ManagementService() {
      super(new MultiTenantInsightConfig(), new MultiTenantAuth0ApiSupplier());
    }

    @Override
    public boolean deleteTenant(final String applicationId, final String connectionId) {
      return true;
    }
  }
}
