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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.152
 */
@Named
@Singleton
public class AutomaticQuarantineReleaseScheduler
    implements TenantManaged, ProductLicenseListener, MtiqBatchJob
{
  private static final Logger log = LoggerFactory.getLogger(AutomaticQuarantineReleaseScheduler.class);

  private final Configuration configuration;

  private final ProductLicense productLicense;

  private final TaskScheduler taskScheduler;

  private final AutomaticQuarantineReleaseTask automaticQuarantineReleaseTask;

  private final TenantUtil tenantUtil;

  public boolean disableForTesting;

  @Inject
  public AutomaticQuarantineReleaseScheduler(
      Configuration configuration,
      ProductLicense productLicense,
      TaskScheduler taskScheduler,
      AutomaticQuarantineReleaseTask automaticQuarantineReleaseTask,
      TenantUtil tenantUtil)
  {
    this.configuration = configuration;
    this.productLicense = productLicense;
    this.taskScheduler = taskScheduler;
    this.automaticQuarantineReleaseTask = automaticQuarantineReleaseTask;
    this.tenantUtil = tenantUtil;
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
    // The automaticQuarantineReleaseTask instance used here is used only for scheduling.
    // When the task is actually run, quartz creates a new AutomaticQuarantineReleaseTask instance, which for MTIQ means
    // one AutomaticQuarantineReleaseTask instance per tenant.
    taskScheduler.schedulePeriodicTask(automaticQuarantineReleaseTask,
        timeInterval, getAutomaticQuarantineReleaseStartTime());
  }

  private Date getAutomaticQuarantineReleaseStartTime() {
    LocalDateTime startTime =
        LocalDateTime.now().plusMinutes(configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes());
    return Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private synchronized void stopAutomaticQuarantineRelease() {
    if (!disableForTesting && taskScheduler.unscheduleTask(automaticQuarantineReleaseTask)) {
      log.info("Automatic Quarantine Release stopped");
    }
  }

  @Override
  public void productLicenseChanged() {
    if (tenantUtil.isSingleTenant() || tenantUtil.isCustomerTenantInBatchMode()) {
      if (AutomaticQuarantineRelease.isLicensedForFirewall(productLicense)) {
        log.info("Licensed for Firewall Automatic Quarantine Release.");
        startAutomaticQuarantineRelease();
      }
      else {
        log.info("Not Licensed for Firewall Automatic Quarantine Release.");
        stopAutomaticQuarantineRelease();
      }
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
