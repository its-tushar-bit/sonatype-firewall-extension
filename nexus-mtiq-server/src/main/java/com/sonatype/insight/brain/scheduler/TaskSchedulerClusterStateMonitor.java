/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class TaskSchedulerClusterStateMonitor
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(TaskSchedulerClusterStateMonitor.class);

  private static final Duration PERIOD = Duration.ofSeconds(10);

  private final MultiTenantTaskScheduler multiTenantTaskScheduler;

  private final ScheduledExecutorService scheduledExecutorService;

  @Inject
  public TaskSchedulerClusterStateMonitor(final MultiTenantTaskScheduler multiTenantTaskScheduler) {
    this(multiTenantTaskScheduler, createScheduledExecutorService());
  }

  // Visible for testing
  TaskSchedulerClusterStateMonitor(
      final MultiTenantTaskScheduler multiTenantTaskScheduler,
      final ScheduledExecutorService scheduledExecutorService)
  {
    this.multiTenantTaskScheduler = multiTenantTaskScheduler;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void start() {
    TenantThreadLocal.runAsGlobal(() -> {
      log.info("Scheduling task scheduler cluster state monitoring to run periodically every {} ms.",
          PERIOD.toMillis());
      scheduledExecutorService.scheduleAtFixedRate(
          this::tryMonitor,
          0,
          PERIOD.toMillis(),
          TimeUnit.MILLISECONDS
      );
      return null;
    });
  }

  @Override
  public void stop() {
    TenantThreadLocal.runAsGlobal(() -> {
      log.info("Stopping task scheduler cluster state monitoring.");
      scheduledExecutorService.shutdown();
      return null;
    });
  }

  private void tryMonitor() {
    try {
      multiTenantTaskScheduler.startOrStandbyTaskSchedulers();
    }
    catch (Exception e) {
      log.error("Failed to monitor the cluster state.", e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  private static ScheduledExecutorService createScheduledExecutorService() {
    return Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("TaskSchedulerClusterStateMonitor-%d").build()
    );
  }
}
