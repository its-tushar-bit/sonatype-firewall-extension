/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.Constants;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class QuartzHSQLDBDelegateTest
    extends AbstractDatabaseTest
{
  private static final Logger log = LoggerFactory.getLogger(QuartzHSQLDBDelegateTest.class);

  private DataSource dataSource;

  @BeforeEach
  public void before() throws Exception {
    dataSource = databaseRule.getOperationalDataStore().getDataSource();
    clearQuartzTables();
  }

  @Test
  public void testSelectTriggerToAcquire() throws Exception {
    String instanceId = "me";
    QuartzHSQLDBDelegate quartzHSQLDBDelegate = createQuartzHSQLDBDelegate(instanceId);
    JobDetail job = JobBuilder.newJob(TestJob.class).build();
    quartzHSQLDBDelegate.insertJobDetail(dataSource.getConnection(), job);
    Trigger triggerForMe = createAndPersistTrigger(quartzHSQLDBDelegate, job, instanceId, new Date());
    createAndPersistTrigger(quartzHSQLDBDelegate, job, "other1", new Date());
    Trigger staleTriggerForOther = createAndPersistTrigger(quartzHSQLDBDelegate, job, "other2",
        new Date(System.currentTimeMillis() - (StdJDBCDelegateUtils.ORPHANED_MILLIS + 1)));

    List<TriggerKey> triggerKeys =
        quartzHSQLDBDelegate.selectTriggerToAcquire(dataSource.getConnection(),
            Long.MAX_VALUE, 0, Integer.MAX_VALUE);

    assertThat(triggerKeys).extracting(Key::getName)
        .containsExactlyInAnyOrder(triggerForMe.getKey().getName(), staleTriggerForOther.getKey().getName());
  }

  private void clearQuartzTables() throws Exception {
    String schema = databaseRule.getOperationalDataStore().getDatabaseSchema();
    String[] tables = {
      "QRTZ_FIRED_TRIGGERS",
      "QRTZ_SIMPLE_TRIGGERS",
      "QRTZ_SIMPROP_TRIGGERS",
      "QRTZ_CRON_TRIGGERS",
      "QRTZ_BLOB_TRIGGERS",
      "QRTZ_TRIGGERS",
      "QRTZ_JOB_DETAILS",
      "QRTZ_CALENDARS",
      "QRTZ_PAUSED_TRIGGER_GRPS",
      "QRTZ_SCHEDULER_STATE",
      "QRTZ_LOCKS"
    };
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      for (String table : tables) {
        statement.executeUpdate("DELETE FROM " + schema + "." + table);
      }
    }
  }

  private QuartzHSQLDBDelegate createQuartzHSQLDBDelegate(String instanceId) throws Exception {
    QuartzHSQLDBDelegate quartzHSQLDBDelegate = new QuartzHSQLDBDelegate();
    ClassLoadHelper classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();
    String tablePrefix = databaseRule.getOperationalDataStore().getDatabaseSchema() + ".QRTZ_";
    quartzHSQLDBDelegate.initialize(log, tablePrefix, TaskScheduler.DEFAULT_SCHEDULER_NAME,
        instanceId, classLoadHelper, false, null);
    return quartzHSQLDBDelegate;
  }

  private Trigger createAndPersistTrigger(
      StdJDBCDelegate stdJDBCDelegate,
      JobDetail job,
      String instanceId,
      Date nextFireTime) throws Exception
  {
    Trigger trigger = TriggerBuilder.newTrigger()
        .forJob(job)
        .usingJobData(TaskScheduler.QUARTZ_NODE_ID, instanceId)
        .build();
    assertThat(trigger).isInstanceOf(OperableTrigger.class);
    OperableTrigger operableTrigger = (OperableTrigger) trigger;
    operableTrigger.setNextFireTime(nextFireTime);
    stdJDBCDelegate.insertTrigger(dataSource.getConnection(), operableTrigger,
        Constants.STATE_WAITING, job);
    return trigger;
  }
}
