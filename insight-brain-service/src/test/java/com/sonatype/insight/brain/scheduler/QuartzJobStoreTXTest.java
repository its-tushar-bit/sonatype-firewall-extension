/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.postgres.PostgresServer;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
  public QuartzJobStoreTX quartzJobStoreTX;

  @Inject
  public TestProductLicense testProductLicense;

  @Inject
  public TaskScheduler taskScheduler;

  private QuartzJobStoreTX quartzJobStoreTXSpy;

  @Before
  public void before() throws Exception {
    taskScheduler.createScheduler();
    quartzJobStoreTXSpy = spy(quartzJobStoreTX);
  }

  @After
  public void after() throws Exception {
    tryCleanup(taskScheduler.getScheduler());
    deleteAllSchedulerStateRecords();
  }

  private void tryCleanup(Scheduler scheduler) {
    if (scheduler != null) {
      try {
        scheduler.clear();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      try {
        scheduler.shutdown(false);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(taskScheduler.getScheduler()).isNull());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void testInitialize_H2() throws Exception {
    quartzJobStoreTX.initialize();

    assertThat(quartzJobStoreTX.isClustered()).isFalse();
    assertThat(quartzJobStoreTX.getDriverDelegateClass()).isEqualTo(HSQLDBDelegate.class.getName());
    assertJobStoreTX(quartzJobStoreTX);
  }

  @Test
  public void testInitialize_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgresServer = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgresServer.getDatabaseConfig(), false);
      quartzJobStoreTX.initialize();

      assertThat(quartzJobStoreTX.getDriverDelegateClass()).isEqualTo(PostgreSQLDelegate.class.getName());
      assertThat(quartzJobStoreTX.isClustered()).isTrue();
      assertJobStoreTX(quartzJobStoreTX);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void assertJobStoreTX(JobStoreTX jobStoreTX) {
    assertThat(jobStoreTX.getDataSource()).isEqualTo("ods");
    assertThat(jobStoreTX.getTablePrefix()).isEqualTo(OperationalDataStoreProvider.ID + ".QRTZ_");
    assertThat(jobStoreTX.canUseProperties()).isTrue();
  }

  @Test
  public void testDoCheckin() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel().contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread();
  }

  @Test
  public void testDoCheckin_NoLicenseLoaded() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread();
  }

  @Test
  public void testDoCheckin_HasNodeClusteringFeature() throws Exception {
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() + 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread();
  }

  @Test
  public void testDoCheckin_NoOtherNodesCheckedInAfter() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other",
        quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp() - 1);

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread();
  }

  @Test
  public void testDoCheckin_OtherNodeCheckinSameTime_AlphaLess() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    lenient().doNothing().when(quartzJobStoreTXSpy).exitInNewThread();
    doReturn("me").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other", quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp());

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).doesNotContain(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
    verify(quartzJobStoreTXSpy, never()).exitInNewThread();
  }

  @Test
  public void testDoCheckin_OtherNodeCheckinSameTime_AlphaMore() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.NODE_CLUSTERING);
    doNothing().when(quartzJobStoreTXSpy).exitInNewThread();
    doReturn("stillme").when(quartzJobStoreTXSpy).getInstanceId();
    quartzJobStoreTXSpy.productLicenseChanged();
    quartzJobStoreTXSpy.doCheckin();
    createSchedulerStateRecord("other", quartzJobStoreTXSpy.getSchedulerStateRecords().get(0).getCheckinTimestamp());

    quartzJobStoreTXSpy.doCheckin();

    assertThat(logOutput).atErrorLevel().contains(QuartzJobStoreTX.NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
    verify(quartzJobStoreTXSpy).exitInNewThread();
  }

  private void createSchedulerStateRecord(String instanceId, long checkinTimestamp) throws Exception {
    String sQuery = "INSERT INTO QRTZ_SCHEDULER_STATE" + //
        " (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " + //
        " VALUES (?1, ?2, ?3, ?4)";
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sQuery)) {
      statement.setString(1, TaskScheduler.DEFAULT_SCHEDULER_NAME);
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
