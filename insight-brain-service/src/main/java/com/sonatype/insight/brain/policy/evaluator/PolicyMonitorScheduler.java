/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.LocalTime;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.policy.PolicyMonitoringTask;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import io.dropwizard.lifecycle.Managed;
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
    implements Managed, ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(PolicyMonitorScheduler.class);

  private final InsightConfig config;

  private final ProductLicense productLicense;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public PolicyMonitorScheduler(InsightConfig config, ProductLicense productLicense, TaskScheduler taskScheduler) {
    this.config = config;
    this.productLicense = productLicense;
    this.taskScheduler = taskScheduler;
  }

  private synchronized void startMonitoring() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.scheduleDailyTask(PolicyMonitoringTask.class, PolicyMonitoringTask.NAME,
        LocalTime.of(config.getPolicyMonitoringHour(), 0));
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
    if (productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)) {
      startMonitoring();
    }
    else {
      stopMonitoring();
    }
  }

  @Override
  public void start() {
    if (productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)) {
      startMonitoring();
    }
  }

  @Override
  public void stop() {
    // noop
  }
}
