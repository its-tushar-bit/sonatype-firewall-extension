/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.152
 */
@Named
@Singleton
public class AutomaticQuarantineReleaseScheduler
    implements TenantManaged, ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(AutomaticQuarantineReleaseScheduler.class);

  private final Configuration configuration;

  private final ProductLicense productLicense;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public AutomaticQuarantineReleaseScheduler(
      Configuration configuration,
      ProductLicense productLicense,
      TaskScheduler taskScheduler)
  {
    this.configuration = configuration;
    this.productLicense = productLicense;
    this.taskScheduler = taskScheduler;
  }

  private synchronized void startAutomaticQuarantineRelease() {
    if (disableForTesting) {
      return;
    }
    scheduleAutomaticQuarantineRelease();
  }

  public void scheduleAutomaticQuarantineRelease() {
    if (!AutomaticQuarantineRelease.isLicensedForFirewall(productLicense)) {
      log.info("Not licensed for Firewall Automatic Quarantine Release.");
      return;
    }
    log.info("Licensed for Firewall Automatic Quarantine Release.");
    Duration timeInterval = Duration.ofMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes());
    taskScheduler.schedulePeriodicTask(AutomaticQuarantineReleaseTask.class, AutomaticQuarantineReleaseTask.NAME,
        timeInterval, getAutomaticQuarantineReleaseStartTime());
    log.info("Next Automatic Quarantine Release execution scheduled for {}",
        taskScheduler.getNextExecutionTime(AutomaticQuarantineReleaseTask.NAME));
  }

  private Date getAutomaticQuarantineReleaseStartTime() {
    LocalDateTime startTime =
        LocalDateTime.now().plusMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes());
    return Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private synchronized void stopAutomaticQuarantineRelease() {
    if (!disableForTesting && taskScheduler.unscheduleTask(AutomaticQuarantineReleaseTask.NAME)) {
      log.info("Automatic Quarantine Release stopped");
    }
  }

  @Override
  public void productLicenseChanged() {
    if (AutomaticQuarantineRelease.isLicensedForFirewall(productLicense)) {
      log.info("Licensed for Firewall Automatic Quarantine Release.");
      startAutomaticQuarantineRelease();
    }
    else {
      log.info("Not Licensed for Firewall Automatic Quarantine Release.");
      stopAutomaticQuarantineRelease();
    }
  }

  @Override
  public void register() {
    if (AutomaticQuarantineRelease.isLicensedForFirewall(productLicense)) {
      log.info("Licensed for Firewall Automatic Quarantine Release.");
      startAutomaticQuarantineRelease();
    }
    else {
      log.info("Not Licensed for Firewall Automatic Quarantine Release.");
    }
  }

  @Override
  public void deregister() {
    // noop
  }
}
