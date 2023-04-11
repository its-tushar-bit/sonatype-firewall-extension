/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.postgres.PostgresServer;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.spi.OperableTrigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class QuartzJobStoreTXTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(QuartzJobStoreTX.class);

  @Inject
  private QuartzJobStoreTX quartzJobStoreTX;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private InsightConfig insightConfig;

  private QuartzJobStoreTX quartzJobStoreTXSpy;

  @Override
  public void configure(Properties properties) {
    properties.put("scheduler.name", TaskScheduler.DEFAULT_SCHEDULER_NAME + "-" + UUID.randomUUID());
  }

  @Before
  public void before() {
    taskScheduler.createScheduler();
    quartzJobStoreTXSpy = spy(quartzJobStoreTX);
  }

  @After
  public void after() throws Exception {
    deleteAllSchedulerStateRecords();
  }

  @Test
  public void testInitialize_H2() throws Exception {
    quartzJobStoreTX.initialize();

    assertThat(quartzJobStoreTX.isClustered()).isFalse();
    assertThat(quartzJobStoreTX.getDriverDelegateClass()).isEqualTo(QuartzHSQLDBDelegate.class.getName());
    assertJobStoreTX(quartzJobStoreTX);
  }

  @Test
  public void testInitialize_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgresServer = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgresServer.getDatabaseConfig(), false);
      quartzJobStoreTX.initialize();

      assertThat(quartzJobStoreTX.getDriverDelegateClass()).isEqualTo(QuartzPostgreSQLDelegate.class.getName());
      assertThat(quartzJobStoreTX.isClustered()).isTrue();
      assertJobStoreTX(quartzJobStoreTX);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void assertJobStoreTX(JobStoreTX jobStoreTX) {
    assertThat(jobStoreTX.getDataSource()).isEqualTo("ods");
    assertThat(jobStoreTX.getTablePrefix()).isEqualTo(OperationalDataStore.ID + ".QRTZ_");
    assertThat(jobStoreTX.canUseProperties()).isTrue();
    assertThat(jobStoreTX.getClusterCheckinInterval()).isEqualTo(QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS);
  }

  @Test
  public void testDoCheckin_NodeClusteringNotSupported() throws Exception {
    insightConfig.setClusterDirectory(
        Paths.get(insightConfig.getSonatypeWork().getAbsolutePath(), "clusterDirectory").toString());
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .contains(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS,
        QuartzJobStoreTX.UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
  }

  @Test
  public void testDoCheckin_SchemaMigrationInProgress() throws Exception {
    try {
      insightConfig.setClusterDirectory(
          Paths.get(insightConfig.getSonatypeWork().getAbsolutePath(), "clusterDirectory").toString());
      doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
      ClusterLock.createForSchemaMigrationInProgress();

      quartzJobStoreTXSpy.doCheckin();

      assertThat(logOutput).atErrorLevel().contains(QuartzJobStoreTX.SCHEMA_MIGRATION_UNFINISHED_MESSAGE);
      verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.SCHEMA_MIGRATION_UNFINISHED_EXIT_STATUS,
          QuartzJobStoreTX.SCHEMA_MIGRATION_UNFINISHED_SHUTDOWN_THREAD_NAME);
    }
    finally {
      ClusterLock.deleteForSchemaMigrationInProgress();
    }
  }

  @Test
  public void testDoCheckin_ClusterDirectoryNotSet() throws Exception {
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .contains(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .contains(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS,
        QuartzJobStoreTX.UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
  }

  @Test
  public void testDoCheckin_NodeClusteringNotSupportedAndClusterDirectoryNotSet() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .contains(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .contains(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS,
        QuartzJobStoreTX.UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
  }

  @Test
  public void testDoCheckin_NoLicenseLoaded() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other", System.currentTimeMillis());

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread(anyInt(), anyString());
  }

  @Test
  public void testDoCheckin_HasNodeClusteringFeatureAndClusterDirectoryIsSetByUser() throws Exception {
    insightConfig.setClusterDirectory(
        Paths.get(insightConfig.getSonatypeWork().getAbsolutePath(), "clusterDirectory").toString());
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread(anyInt(), anyString());
  }

  @Test
  public void testDoCheckin_NoOtherNodesCheckedInAfter() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() - 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread(anyInt(), anyString());
  }

  @Test
  public void testDoCheckin_OtherNodeCheckinSameTime_AlphaLess() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    doReturn("me").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other", quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp());

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread(anyInt(), anyString());
  }

  @Test
  public void testDoCheckin_OtherNodeCheckinSameTime_AlphaMore() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    doReturn("stillme").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other", quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp());

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .contains(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .contains(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS,
        QuartzJobStoreTX.UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
  }

  @Test
  public void testDoCheckin_NewNodeStartsIfAnotherNodeIsNotRunning() throws Exception {
    createSchedulerStateRecord("other", System.currentTimeMillis() - 10000);
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doReturn("stillme").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread(anyInt(), anyString());
  }

  @Test
  public void testDoCheckin_NewNodeStartsEvenIfAnotherNodeIsRunning() throws Exception {
    createSchedulerStateRecord("other", System.currentTimeMillis() - 10000);
    lenient().doReturn("stillme").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .doesNotContain(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread(anyInt(), anyString());
  }

  @Test
  public void testDoCheckin_NewNodeDoesNotStartIfAnotherNodeIsRunning() throws Exception {
    createSchedulerStateRecord("other", System.currentTimeMillis() - 10000);
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    doReturn("stillme").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .contains(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .contains(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS,
        QuartzJobStoreTX.UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
  }

  @Test
  public void testDoCheckin_NewNodeDoesNotStartIfAnotherNodeIsRunningAndAnotherNodeStopped() throws Exception {
    createSchedulerStateRecord("other", System.currentTimeMillis() - 500);
    createSchedulerStateRecord("one-more-other", System.currentTimeMillis() - 30000);
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread(anyInt(), anyString());
    doReturn("stillme").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel()
        .contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE)
        .contains(QuartzJobStoreTX.CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE)
        .contains(QuartzJobStoreTX.SHUTTING_DOWN_EXCESS_NODE_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread(QuartzJobStoreTX.NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS,
        QuartzJobStoreTX.UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
  }

  @Test
  public void testAquireNextTrigger() throws Exception {
    JobDetail job = JobBuilder.newJob(TestJob.class).build();
    quartzJobStoreTXSpy.storeJob(job, true);
    OperableTrigger triggerForMe = (OperableTrigger) TriggerBuilder.newTrigger().forJob(job)
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires())
        .usingJobData(TaskScheduler.QUARTZ_NODE_ID, quartzJobStoreTXSpy.getInstanceId()).build();
    triggerForMe.setNextFireTime(new Date());
    quartzJobStoreTXSpy.storeTrigger(triggerForMe, true);
    OperableTrigger triggerForOther = (OperableTrigger) TriggerBuilder.newTrigger().forJob(job)
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires())
        .usingJobData(TaskScheduler.QUARTZ_NODE_ID, "other1").build();
    triggerForOther.setNextFireTime(new Date());
    quartzJobStoreTXSpy.storeTrigger(triggerForOther, true);
    OperableTrigger staleTriggerForOther = (OperableTrigger) TriggerBuilder.newTrigger().forJob(job)
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires())
        .usingJobData(TaskScheduler.QUARTZ_NODE_ID, "other2").build();
    staleTriggerForOther
        .setNextFireTime(new Date(System.currentTimeMillis() - (StdJDBCDelegateUtils.ORPHANED_MILLIS + 1)));
    quartzJobStoreTXSpy.storeTrigger(staleTriggerForOther, true);

    List<OperableTrigger> operableTriggers = quartzJobStoreTXSpy.acquireNextTrigger(
        OperationalDataStoreProvider.getDataSource().getConnection(), Long.MAX_VALUE, 3, 0);

    assertThat(operableTriggers).hasSize(2);
    OperableTrigger actualTriggerForMe = operableTriggers.stream()
        .filter(operableTrigger -> operableTrigger.getKey().getName().equals(triggerForMe.getKey().getName()))
        .findFirst().orElse(null);
    assertThat(actualTriggerForMe).isNotNull();
    assertThat(actualTriggerForMe.getJobDataMap().getBoolean(QuartzTriggerListener.QUARTZ_VETO)).isFalse();
    OperableTrigger actualStaleTriggerForOther = operableTriggers.stream()
        .filter(operableTrigger -> operableTrigger.getKey().getName().equals(staleTriggerForOther.getKey().getName()))
        .findFirst().orElse(null);
    assertThat(actualStaleTriggerForOther).isNotNull();
    assertThat(actualStaleTriggerForOther.getJobDataMap().getBoolean(QuartzTriggerListener.QUARTZ_VETO)).isTrue();
  }

  private void createSchedulerStateRecord(String instanceId, long checkinTimestamp) throws Exception {
    String sQuery = "INSERT INTO QRTZ_SCHEDULER_STATE" + //
        " (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " + //
        " VALUES (?1, ?2, ?3, ?4)";
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sQuery)) {
      statement.setString(1, taskScheduler.getScheduler().getSchedulerName());
      statement.setString(2, instanceId);
      statement.setLong(3, checkinTimestamp);
      statement.setLong(4, quartzJobStoreTXSpy.getClusterCheckinInterval());
      statement.execute();
    }
  }

  private void deleteAllSchedulerStateRecords() throws Exception {
    String sQuery = "DELETE FROM QRTZ_SCHEDULER_STATE";
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sQuery)) {
      statement.execute();
    }
  }
}
