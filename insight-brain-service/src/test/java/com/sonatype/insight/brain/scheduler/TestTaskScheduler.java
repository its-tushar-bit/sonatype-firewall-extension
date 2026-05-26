/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.UUID;
import org.quartz.Scheduler;
import org.quartz.impl.SchedulerRepository;
import org.quartz.spi.JobFactory;

@Named
@Singleton
@Priority(TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY + 1)
public class TestTaskScheduler
    extends TaskScheduler
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public TestTaskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      JobFactory jobFactory,
      QuartzTriggerListener quartzTriggerListener,
      QuartzConcurrencyListener quartzConcurrencyListener,
      OperationalDataStore operationalDataStore,
      ShutdownHandler shutdownHandler,
      QuartzJobSchedulingService quartzJobSchedulingService)
  {
    super(quartzJobStoreTX, jobFactory, getUniqueSchedulerName(), quartzTriggerListener, quartzConcurrencyListener,
        shutdownHandler, quartzJobSchedulingService);
    this.operationalDataStore = operationalDataStore;
  }

  private static String getUniqueSchedulerName() {
    // ensure we don't reuse/hijack a pre-existing scheduler from another test out of the SchedulerRepository
    return TestTaskScheduler.class.getSimpleName() + "-" + UUID.randomUUID();
  }

  @Override
  public void stop() {
    Scheduler scheduler = getScheduler();
    if (scheduler != null) {
      try {
        // sometimes the test db is shut down before Jetty/IQ calls the shutdown
        // so do a quick check to see if there is anything to clear
        if (DatabaseUtil.legacySchemaVersionTableExists(operationalDataStore)) {
          scheduler.clear();
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      try {
        scheduler.shutdown(false);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        SchedulerRepository.getInstance().remove(schedulerName);
        waitForShutdown();
      }
    }
  }

  @Override
  public void clear() throws Exception {
    super.clear();

    // Ensuring the default Never-Past Calendar is added back after is cleared. This will leave the TaskScheduler on
    // the same state it was when the test started
    Scheduler scheduler = getScheduler(schedulerName);
    if (!scheduler.getCalendarNames().contains(NeverPastCalendar.CALENDAR_NAME)) {
      scheduler.addCalendar(NeverPastCalendar.CALENDAR_NAME, new NeverPastCalendar(), true, false);
    }
  }

  private void waitForShutdown() {
    long start = System.currentTimeMillis();
    while (getScheduler() != null && (System.currentTimeMillis() - start) < 10000) {
      Thread.yield();
    }
  }
}
