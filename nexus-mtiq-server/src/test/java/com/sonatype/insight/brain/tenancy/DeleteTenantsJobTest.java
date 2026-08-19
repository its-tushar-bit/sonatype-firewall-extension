/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS;
import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.JOB_FREQUENCY_IN_HOURS;
import static com.sonatype.insight.brain.tenancy.DeleteTenantsJob.TENANT_RETENTION_PERIOD_CONFIG_KEY;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setupNewTestTenant;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ApiSupplier;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class DeleteTenantsJobTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  public static final String BAD_APPLICATION_ID = "badId";

  public static final String GOOD_APPLICATION_ID = "appId";

  private DeleteTenantsJob deleteTenantsJob;

  private TenantManager tenantManager;

  private InsightConfig config;

  private OperationalDataStore dataStore;

  private DeletedTenantDAO deletedTenantDAO;

  private TaskScheduler taskScheduler;

  private TenantMetadataDAO tenantMetadataDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private QuartzJobSchedulingService quartzJobSchedulingService;

  @Before
  public void setup() {
    tenantManager = super.getTestCLMServer().getCLMServer().getInstance(TenantManager.class);
    deleteTenantsJob = spy(super.getTestCLMServer().getCLMServer().getInstance(DeleteTenantsJob.class));
    config = super.getTestCLMServer().getCLMServer().getInstance(InsightConfig.class);
    dataStore = super.getTestCLMServer().getCLMServer().getInstance(OperationalDataStore.class);
    taskScheduler = super.getTestCLMServer().getCLMServer().getInstance(MultiTenantTaskScheduler.class);
    deletedTenantDAO = getCLMServer().getInstance(DeletedTenantDAO.class);
    tenantMetadataDAO = getCLMServer().getInstance(TenantMetadataDAO.class);
    systemConfigurationPropertyDAO = getCLMServer().getInstance(SystemConfigurationPropertyDAO.class);
    quartzJobSchedulingService = getCLMServer().getInstance(QuartzJobSchedulingService.class);

    // Clean deleted tenant table
    for (DeletedTenant deletedTenant : deletedTenantDAO.getAll()) {
      deletedTenantDAO.delete(deletedTenant);
    }
  }

  /**
   * Test configuration that provides a test Auth0 management service.
   */
  @TestConfiguration
  static class DeleteTenantsJobTestConfig
  {
    @Bean
    @Primary
    MultiTenantAuth0ManagementService multiTenantAuth0ManagementService() {
      return new TestMultiTenantAuth0ManagementService();
    }
  }

  @Test
  public void testDeleteTenant() throws Exception {
    Tenant tenant = setupNewTestTenant(testName);
    provisionTenant(tenant, GOOD_APPLICATION_ID);
    scheduleTenantForDeletion(tenant.tenantSlug, DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS);
    initializeTenantDirectories();
    scheduleJobsForTenant();

    runDeleteTenantJob();

    assertTenantWasDeleted(tenant);
  }

  @Test
  public void testDeleteTenant_skipAuth0ResourcesAndDBSchemaDeletionIfSchemaDoesntExist() throws Exception {
    Tenant tenant = setupNewTestTenant(testName);
    DeletedTenant deletedTenant =
        scheduleTenantForDeletion(tenant.tenantSlug, DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS);
    initializeTenantDirectories();
    scheduleJobsForTenant();

    runDeleteTenantJob();

    assertTenantWasDeleted(tenant);
    verify(deleteTenantsJob, never()).deleteAuth0Resources(deletedTenant);
    verify(deleteTenantsJob, never()).deleteDatabaseSchema(deletedTenant);
  }

  @Test
  public void testDeleteTenant_notDeletedIfAuth0ResourcesNotDeleted() throws Exception {
    Tenant tenant = setupNewTestTenant(testName);
    provisionTenant(tenant, BAD_APPLICATION_ID);
    scheduleTenantForDeletion(tenant.tenantSlug, DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS);
    initializeTenantDirectories();
    scheduleJobsForTenant();

    runDeleteTenantJob();

    assertTenantResourcesExist(tenant);
    assertTenantIsNotDeleted(tenant.tenantSlug);
  }

  @Test
  public void testDeleteTenant_onDeleteErrorAllowsOtherDeletesToRun() throws Exception {
    Tenant tenant = setupNewTestTenant(testName);
    provisionTenant(tenant, BAD_APPLICATION_ID);
    scheduleTenantForDeletion(tenant.tenantSlug, DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS);
    initializeTenantDirectories();
    scheduleJobsForTenant();

    String partiallyDeletedTenant1 = "partially-deleted-tenant-1";
    String partiallyDeletedTenant2 = "partially-deleted-tenant-2";
    scheduleTenantForDeletion(partiallyDeletedTenant1, DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS);
    scheduleTenantForDeletion(partiallyDeletedTenant2, DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS);

    runDeleteTenantJob();

    assertTenantResourcesExist(tenant);
    assertTenantIsNotDeleted(tenant.tenantSlug);
    assertTenantDeletionIsCompleted(partiallyDeletedTenant1);
    assertTenantDeletionIsCompleted(partiallyDeletedTenant2);
  }

  @Test
  public void testDeleteTenant_jobRegistration() {
    MultiTenantTaskScheduler taskScheduler = mock(MultiTenantTaskScheduler.class);

    deleteTenantsJob = new DeleteTenantsJob(taskScheduler, null, null, null, null, null, null, null, null, null, null);

    deleteTenantsJob.register();

    verify(taskScheduler).schedulePeriodicTask(any(DeleteTenantsJob.class),
        eq(Duration.ofHours(JOB_FREQUENCY_IN_HOURS)));
  }

  @Test
  public void testDeleteTenant_retentionPeriodCanBeConfigured() throws Exception {
    long retentionPeriodInHours = 1L;

    Tenant tenant = setupNewTestTenant(testName);
    provisionTenant(tenant, GOOD_APPLICATION_ID);
    scheduleTenantForDeletion(tenant.tenantSlug, retentionPeriodInHours);
    initializeTenantDirectories();
    scheduleJobsForTenant();

    runDeleteTenantJobWithCustomRetentionPeriod(retentionPeriodInHours);

    assertTenantWasDeleted(tenant);
  }

  private void assertTenantResourcesExist(final Tenant tenant) throws SchedulerException {
    assertThat(DatabaseUtil.schemaExists(dataStore.getDataSource(), tenant.databaseSchema)).isTrue();

    assertThat(config.getSonatypeWork().exists()).isTrue();
    assertThat(config.getClusterDirectory().exists()).isTrue();

    Set<JobKey> jobs = taskScheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(tenant.tenantSlug));
    assertThat(jobs.size()).isNotZero();
  }

  private void assertTenantWasDeleted(Tenant tenant) throws SchedulerException {
    assertTenantDeletionIsCompleted(tenant.tenantSlug);

    assertThat(DatabaseUtil.schemaExists(dataStore.getDataSourceWithoutInit(), tenant.databaseSchema)).isFalse();
    assertThat(config.getSonatypeWork().exists()).isFalse();
    assertThat(config.getClusterDirectory().exists()).isFalse();

    Set<JobKey> jobs = taskScheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(tenant.tenantSlug));
    assertThat(jobs.size()).isZero();
  }

  private void assertTenantDeletionIsCompleted(String tenantSlug) {
    DeletedTenant deletedTenant = deletedTenantDAO.getTenantBySlug(tenantSlug);
    assertThat(deletedTenant.getDeleteCompletedDate()).isNotNull();
    assertThat(deletedTenant.getLastUpdated()).isNotNull();
  }

  private void assertTenantIsNotDeleted(String tenantSlug) {
    DeletedTenant deletedTenant = deletedTenantDAO.getTenantBySlug(tenantSlug);
    assertThat(deletedTenant.getDeleteCompletedDate()).isNull();
    assertThat(deletedTenant.getLastUpdated()).isNotNull();
  }

  private void runDeleteTenantJob() {
    TenantThreadLocal.runAsGlobal(() -> {
      try {
        deleteTenantsJob.execute(null);
      }
      catch (JobExecutionException e) {
        throw new RuntimeException(e);
      }
      return null;
    });
  }

  private void runDeleteTenantJobWithCustomRetentionPeriod(long retentionPeriodInHours) {
    TenantThreadLocal.runAsGlobal(() -> {
      try {
        systemConfigurationPropertyDAO.set(TENANT_RETENTION_PERIOD_CONFIG_KEY, String.valueOf(retentionPeriodInHours));

        deleteTenantsJob.execute(null);
      }
      catch (JobExecutionException e) {
        throw new RuntimeException(e);
      }
      finally {
        systemConfigurationPropertyDAO.delete(
            systemConfigurationPropertyDAO.getByName(TENANT_RETENTION_PERIOD_CONFIG_KEY));
      }
      return null;
    });
  }

  private DeletedTenant scheduleTenantForDeletion(String tenantSlug, long retentionPeriodInHours) {
    DeletedTenant deletedTenant =
        new DeletedTenant(tenantSlug, getBeforeDefaultRetentionPeriod(retentionPeriodInHours));
    deletedTenantDAO.insert(deletedTenant);
    return deletedTenant;
  }

  private Date getBeforeDefaultRetentionPeriod(long retentionPeriodInHours) {
    return new Date(System.currentTimeMillis() - (60 * 60 * 1000 * (retentionPeriodInHours + 1)));
  }

  private void provisionTenant(Tenant tenant, String auth0AppId) throws Exception {
    provisionTenant(tenant.tenantSlug);
    tenantManager.setTenant(tenant);
    tenantMetadataDAO.insert(
        new TenantMetadata(auth0AppId, "appName", "connId", "connName", "encKeyName", "orgId", "orgName"));
  }

  private void scheduleJobsForTenant() {
    for (int i = 0; i < 10; i++) {
      taskScheduler.schedulePeriodicTask(newJob(), Duration.ofHours(1L));
    }
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(quartzJobSchedulingService);
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
        // no-op
      }
    };
  }

  private void initializeTenantDirectories() throws IOException {
    Files.createDirectories(config.getSonatypeWork().toPath());
    Files.createDirectories(config.getClusterDirectory().toPath());
  }

  private static class TestMultiTenantAuth0ManagementService
      extends MultiTenantAuth0ManagementService
  {
    public TestMultiTenantAuth0ManagementService() {
      super(new MultiTenantInsightConfig(), new MultiTenantAuth0ApiSupplier());
    }

    @Override
    public boolean deleteTenant(final String applicationId, final String connectionId, final String organizationId) {
      if (BAD_APPLICATION_ID.equals(applicationId)) {
        return false;
      }

      return true;
    }
  }
}
