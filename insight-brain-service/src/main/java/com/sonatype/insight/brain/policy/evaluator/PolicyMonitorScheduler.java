/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.joda.time.DateTime;
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

  private final PolicyMonitor policyMonitor;

  private ScheduledExecutorService executor;

  public boolean disableForTesting;

  @Inject
  public PolicyMonitorScheduler(InsightConfig config, ProductLicense productLicense, PolicyMonitor policyMonitor) {
    this.config = config;
    this.productLicense = productLicense;
    this.policyMonitor = policyMonitor;
  }

  private synchronized void startMonitoring() {
    if (executor != null || disableForTesting) {
      return;
    }
    DateTime dateTime = determineNextExecutionTime();
    executor = newExecutor();
    executor.scheduleAtFixedRate(new SystemRunnable(new Runnable()
    {
      @Override
      public void run() {
        policyMonitor.run();
        if (productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)) {
          log.info("Next Policy Monitor execution scheduled for {}", determineNextExecutionTime());
        }
      }
    }), dateTime.getMillis() - System.currentTimeMillis(), TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    log.info("First Policy Monitor execution scheduled for {}", dateTime);
  }

  ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("PolicyMonitoring-%d").setDaemon(true)
        .build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  private DateTime determineNextExecutionTime() {
    int policyMonitoringHour = config.getPolicyMonitoringHour();
    DateTime dateTime = new DateTime().withHourOfDay(policyMonitoringHour).withMinuteOfHour(0).withSecondOfMinute(0)
        .withMillisOfSecond(0);
    // set for tomorrow if this time has already passed today
    if (dateTime.isBeforeNow()) {
      dateTime = dateTime.plusDays(1);
    }
    return dateTime;
  }

  private synchronized void stopMonitoring() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
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
    stopMonitoring();
  }
}
