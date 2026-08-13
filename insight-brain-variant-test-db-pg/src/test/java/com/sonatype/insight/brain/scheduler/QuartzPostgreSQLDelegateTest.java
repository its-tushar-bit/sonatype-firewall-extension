/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Before;
import org.junit.Test;
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

public class QuartzPostgreSQLDelegateTest
    extends AbstractDatabaseTest
{
  private static final Logger log = LoggerFactory.getLogger(QuartzPostgreSQLDelegateTest.class);

  private OperationalDataStore operationalDataStore;

  @Before
  public void before() {
    this.operationalDataStore = databaseRule.getOperationalDataStore();
  }

  @Test
  @PostgresTest
  public void testSelectTriggerToAcquire() throws Exception {
    String instanceId = "me";
    QuartzPostgreSQLDelegate quartzPostgreSQLDelegate = createQuartzPostgreSQLDelegate(instanceId);
    JobDetail job = JobBuilder.newJob(TestJob.class).build();
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      quartzPostgreSQLDelegate.insertJobDetail(connection, job);
    }
    Trigger triggerForMe = createAndPersistTrigger(quartzPostgreSQLDelegate, job, instanceId, new Date());
    createAndPersistTrigger(quartzPostgreSQLDelegate, job, "other1", new Date());
    Trigger staleTriggerForOther = createAndPersistTrigger(quartzPostgreSQLDelegate, job, "other2",
        new Date(System.currentTimeMillis() - (StdJDBCDelegateUtils.ORPHANED_MILLIS + 1)));

    List<TriggerKey> triggerKeys;
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      triggerKeys = quartzPostgreSQLDelegate.selectTriggerToAcquire(connection, Long.MAX_VALUE, 0, Integer.MAX_VALUE);
    }

    assertThat(triggerKeys).extracting(Key::getName)
        .containsExactlyInAnyOrder(triggerForMe.getKey().getName(), staleTriggerForOther.getKey().getName());
  }

  private QuartzPostgreSQLDelegate createQuartzPostgreSQLDelegate(String instanceId) throws Exception {
    QuartzPostgreSQLDelegate quartzPostgreSQLDelegate = new QuartzPostgreSQLDelegate();
    ClassLoadHelper classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();
    String tablePrefix = databaseRule.getOperationalDataStore().getDatabaseSchema() + ".QRTZ_";
    quartzPostgreSQLDelegate.initialize(log, tablePrefix, TaskScheduler.DEFAULT_SCHEDULER_NAME,
        instanceId, classLoadHelper, false, null);
    return quartzPostgreSQLDelegate;
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
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      stdJDBCDelegate.insertTrigger(connection, operableTrigger, Constants.STATE_WAITING, job);
    }
    return trigger;
  }
}
