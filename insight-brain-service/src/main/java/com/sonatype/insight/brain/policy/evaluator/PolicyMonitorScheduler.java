/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.LocalTime;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.policy.PolicyMonitoringTask;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules policy monitoring in accordance with licensed features and server configuration.
 *
 * @since 1.9
 */
@Named
@Singleton
public class PolicyMonitorScheduler
    implements TenantManaged, ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(PolicyMonitorScheduler.class);

  private final Configuration configuration;

  private final ProductLicense productLicense;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public PolicyMonitorScheduler(
      Configuration configuration,
      ProductLicense productLicense,
      TaskScheduler taskScheduler)
  {
    this.configuration = configuration;
    this.productLicense = productLicense;
    this.taskScheduler = taskScheduler;
  }

  private synchronized void startMonitoring() {
    if (disableForTesting) {
      return;
    }
    schedulePolicyMonitoring();
  }

  public void schedulePolicyMonitoring() {
    if (!PolicyMonitor.isLicensed(productLicense)) {
      log.info("Policy Monitor is not licensed");
      return;
    }
    log.info("Policy Monitor is licensed");
    // randomize minute to avoid coordinated load spike for HDS scan processing
    LocalTime startTime = LocalTime.of(configuration.getPolicyMonitoringHour(), new Random().nextInt(15));
    taskScheduler.scheduleDailyTask(PolicyMonitoringTask.class, PolicyMonitoringTask.NAME, startTime);
    log.info("Next Policy Monitor execution scheduled for {}",
        taskScheduler.getNextExecutionTime(PolicyMonitoringTask.NAME));
  }

  private synchronized void stopMonitoring() {
    if (!disableForTesting && taskScheduler.unscheduleTask(PolicyMonitoringTask.NAME)) {
      log.info("Policy Monitor stopped");
    }
  }

  @Override
  public void productLicenseChanged() {
    if (PolicyMonitor.isLicensed(productLicense)) {
      log.info("Policy Monitor is licensed");
      startMonitoring();
    }
    else {
      log.info("Policy Monitor is not licensed");
      stopMonitoring();
    }
  }

  @Override
  public void register() {
    if (PolicyMonitor.isLicensed(productLicense)) {
      log.info("Policy Monitor is licensed");
      startMonitoring();
    }
    else {
      log.info("Policy Monitor is not licensed");
    }
  }

  @Override
  public void deregister() {
    // noop
  }
}
