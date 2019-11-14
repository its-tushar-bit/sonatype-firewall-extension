/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

  private final PolicyMonitor policyMonitor;

  private ScheduledExecutorService executor;

  // Visible for tests
  SystemRunnable policyMonitoringRunnable;

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
    executor = newExecutor();
    policyMonitoringRunnable = newPolicyMonitoringRunnable();

    schedule();
  }

  private SystemRunnable newPolicyMonitoringRunnable() {
    return new SystemRunnable(new Runnable()
    {
      @Override
      public void run() {
        try {
          policyMonitor.run();
        }
        catch (Exception e) {
          log.error("Policy monitoring error: {}", e.getMessage(), e);
        }
        catch (Throwable t) {
          // Try to log to stderr before trying the standard logging because the standard logging may not be operational
          // at this point.
          t.printStackTrace();
          log.error(t.getMessage(), t);
          System.exit(1);
        }

        // Re-schedule the policy monitoring
        schedule();
      }
    });
  }

  private void schedule() {
    ZonedDateTime nextExecutionTime = determineNextExecutionTime(ZonedDateTime.now());
    executor.schedule(policyMonitoringRunnable, Duration.between(ZonedDateTime.now(), nextExecutionTime).toMillis(),
        TimeUnit.MILLISECONDS);

    log.info("Next Policy Monitor execution scheduled for {}", nextExecutionTime);
  }

  ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("PolicyMonitoring-%d").setDaemon(true)
        .build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  // Visible for tests
  ZonedDateTime determineNextExecutionTime(ZonedDateTime currentTime) {
    int policyMonitoringHour = config.getPolicyMonitoringHour();
    ZonedDateTime nextExecutionTime =
        currentTime.withHour(policyMonitoringHour).withMinute(0).withSecond(0).withNano(0);
    // set for tomorrow if this time has already passed
    if (!nextExecutionTime.isAfter(currentTime)) {
      nextExecutionTime = nextExecutionTime.plusDays(1);
      // Adding a day when the daylight savings time changes may result in a datetime that
      // has a different hour, so we need to change it to the configured policyMonitoringHour.
      nextExecutionTime = nextExecutionTime.withHour(policyMonitoringHour);
    }
    return nextExecutionTime;
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
